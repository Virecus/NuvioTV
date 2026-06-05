package com.nuvio.tv.core.plugin

import android.util.Log
import com.nuvio.tv.data.local.AddonPreferences
import com.nuvio.tv.data.local.CollectionsDataStore
import com.nuvio.tv.data.local.PluginDataStore
import com.nuvio.tv.data.repository.AddonRepositoryImpl
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DefaultRepoBootstrap"
private const val CONFIG_URL =
    "https://raw.githubusercontent.com/Virecus/NuvioTV/refs/heads/dev/repo.json"

@JsonClass(generateAdapter = true)
data class AppConfig(
    val plugins: List<String> = emptyList(),
    val addons: List<String> = emptyList(),
    val collections: List<String> = emptyList()
)

@Singleton
class DefaultRepoBootstrapService @Inject constructor(
    private val pluginManager: PluginManager,
    private val pluginDataStore: PluginDataStore,
    private val addonRepository: AddonRepositoryImpl,
    private val addonPreferences: AddonPreferences,
    private val collectionsDataStore: CollectionsDataStore
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    fun bootstrap() {
        scope.launch {
            try {
                val config = fetchConfig() ?: return@launch

                bootstrapPlugins(config.plugins)
                bootstrapAddons(config.addons)
                bootstrapCollections(config.collections)
            } catch (e: Exception) {
                Log.e(TAG, "Bootstrap hatası", e)
            }
        }
    }

    private suspend fun bootstrapPlugins(urls: List<String>) {
        if (urls.isEmpty()) return

        val existingUrls = pluginDataStore.repositories.first()
            .map { it.url.trim().trimEnd('/').lowercase() }
            .toSet()

        val newUrls = urls.filter { it.trim().trimEnd('/').lowercase() !in existingUrls }
        if (newUrls.isEmpty()) {
            Log.d(TAG, "Plugins: tüm depolar zaten ekli")
            return
        }

        Log.d(TAG, "Plugins: ${newUrls.size} yeni depo ekleniyor")
        newUrls.forEach { url ->
            val result = pluginManager.addRepository(url)
            if (result.isSuccess) {
                Log.d(TAG, "Plugin eklendi: ${result.getOrNull()?.name}")
            } else {
                Log.e(TAG, "Plugin eklenemedi: $url — ${result.exceptionOrNull()?.message}")
            }
        }
    }

    private suspend fun bootstrapAddons(urls: List<String>) {
        if (urls.isEmpty()) return

        // AddonRepository strips /manifest.json from the URL when storing — normalize the same way
        fun normalizeAddonUrl(url: String) = url.trim().trimEnd('/')
            .let { if (it.endsWith("/manifest.json", ignoreCase = true)) it.dropLast(14).trimEnd('/') else it }
            .lowercase()

        val existingUrls = addonPreferences.installedAddonUrls.first()
            .map { normalizeAddonUrl(it) }
            .toSet()

        val newUrls = urls.filter { normalizeAddonUrl(it) !in existingUrls }
        if (newUrls.isEmpty()) {
            Log.d(TAG, "Addons: tüm addonlar zaten ekli")
            return
        }

        Log.d(TAG, "Addons: ${newUrls.size} yeni addon ekleniyor")
        newUrls.forEach { url ->
            try {
                addonRepository.addAddon(url)
                Log.d(TAG, "Addon eklendi: $url")
            } catch (e: Exception) {
                Log.e(TAG, "Addon eklenemedi: $url — ${e.message}")
            }
        }
    }

    private suspend fun bootstrapCollections(urls: List<String>) {
        if (urls.isEmpty()) return

        val existingCollections = collectionsDataStore.getCurrentCollections()

        urls.forEach { url ->
            try {
                val json = fetchText(url) ?: return@forEach
                val imported = collectionsDataStore.importFromJson(json)
                if (imported.isEmpty()) {
                    Log.w(TAG, "Collections: boş liste geldi: $url")
                    return@forEach
                }

                val existingIds = existingCollections.map { it.id }.toSet()
                val newCollections = imported.filter { it.id !in existingIds }
                if (newCollections.isEmpty()) {
                    Log.d(TAG, "Collections: zaten ekli ($url)")
                    return@forEach
                }

                val merged = existingCollections + newCollections
                collectionsDataStore.setCollections(merged)
                Log.d(TAG, "Collections: ${newCollections.size} yeni koleksiyon eklendi ($url)")
            } catch (e: Exception) {
                Log.e(TAG, "Collections eklenemedi: $url — ${e.message}")
            }
        }
    }

    private fun fetchConfig(): AppConfig? {
        return try {
            val body = fetchText(CONFIG_URL) ?: return null
            moshi.adapter(AppConfig::class.java).fromJson(body)
        } catch (e: Exception) {
            Log.e(TAG, "Config fetch hatası", e)
            null
        }
    }

    private fun fetchText(url: String): String? {
        return try {
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "NuvioTV/1.0")
                .header("Cache-Control", "no-cache")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "HTTP ${response.code}: $url")
                    return null
                }
                response.body?.string()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fetch hatası: $url — ${e.message}")
            null
        }
    }
}
