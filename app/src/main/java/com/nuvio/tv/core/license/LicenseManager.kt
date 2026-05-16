package com.nuvio.tv.core.license

import com.nuvio.tv.data.local.LicenseCodeDataStore
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.transformLatest
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

@Serializable
data class LicenseRecord(
    val id: String,
    @SerialName("ad") val firstName: String,
    @SerialName("soyad") val lastName: String,
    @SerialName("tel") val phone: String,
    @SerialName("mail") val email: String,
    @SerialName("created_at") val createdAt: String,
    @SerialName("started_at") val startedAt: String,
    @SerialName("deadline_at") val deadlineAt: String,
    @SerialName("licence_code") val licenceCode: String
)

sealed interface LicenseStatus {
    data object Loading : LicenseStatus
    data object Missing : LicenseStatus
    data object NetworkError : LicenseStatus
    data class Invalid(val attemptedCode: String?) : LicenseStatus
    data class NotStarted(val record: LicenseRecord, val startsAt: Instant) : LicenseStatus
    data class Expired(val record: LicenseRecord, val deadlineAt: Instant, val expiredDaysAgo: Long) : LicenseStatus
    data class Valid(val record: LicenseRecord, val deadlineAt: Instant, val remainingDays: Long) : LicenseStatus
}

@Singleton
class LicenseManager @Inject constructor(
    private val licenseCodeDataStore: LicenseCodeDataStore
) {
    companion object {
        private const val LICENSE_REGISTRY_URL =
            "https://raw.githubusercontent.com/v1rtech/v1rtvbox/refs/heads/master/Nuvio/nuvio_license_registry.json"
    }

    private val json = Json { ignoreUnknownKeys = true }

    val savedLicenseCode: Flow<String?> = licenseCodeDataStore.licenseCode

    @OptIn(ExperimentalCoroutinesApi::class)
    val status: Flow<LicenseStatus> = savedLicenseCode.transformLatest { code ->
        emit(LicenseStatus.Loading)
        emit(resolveStatus(code))
    }

    suspend fun submitLicenseCode(rawCode: String): LicenseStatus {
        val normalized = rawCode.trim()
        if (normalized.isBlank()) {
            licenseCodeDataStore.clearLicenseCode()
            return LicenseStatus.Missing
        }
        licenseCodeDataStore.setLicenseCode(normalized)
        return resolveStatus(normalized)
    }

    suspend fun clearLicenseCode() {
        licenseCodeDataStore.clearLicenseCode()
    }

    suspend fun resolveStatus(rawCode: String?): LicenseStatus {
        val code = rawCode?.trim().orEmpty()
        if (code.isBlank()) return LicenseStatus.Missing

        val registry = fetchRegistry() ?: return LicenseStatus.NetworkError
        val record = registry.firstOrNull { it.licenceCode.equals(code, ignoreCase = true) }
            ?: return LicenseStatus.Invalid(rawCode)

        val now = Instant.now()
        val startedAt = runCatching { Instant.parse(record.startedAt) }.getOrElse {
            return LicenseStatus.Invalid(rawCode)
        }
        val deadlineAt = runCatching { Instant.parse(record.deadlineAt) }.getOrElse {
            return LicenseStatus.Invalid(rawCode)
        }

        if (startedAt.isAfter(now)) {
            return LicenseStatus.NotStarted(record, startedAt)
        }
        if (deadlineAt.isBefore(now)) {
            val expiredDaysAgo = ChronoUnit.DAYS.between(deadlineAt, now).coerceAtLeast(0)
            return LicenseStatus.Expired(record, deadlineAt, expiredDaysAgo)
        }

        val remainingDays = ChronoUnit.DAYS.between(now, deadlineAt).coerceAtLeast(0)
        return LicenseStatus.Valid(record, deadlineAt, remainingDays)
    }

    private suspend fun fetchRegistry(): List<LicenseRecord>? = withContext(Dispatchers.IO) {
        runCatching {
            val connection = (URL(LICENSE_REGISTRY_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 10_000
                setRequestProperty("Accept", "application/json")
            }
            connection.inputStream.bufferedReader().use { reader ->
                json.decodeFromString<List<LicenseRecord>>(reader.readText())
            }
        }.getOrNull()
    }
}
