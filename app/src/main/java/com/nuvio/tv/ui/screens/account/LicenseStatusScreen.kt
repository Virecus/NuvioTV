@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.VpnKey
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import com.nuvio.tv.R
import com.nuvio.tv.core.license.LicenseRecord
import com.nuvio.tv.core.license.LicenseStatus
import com.nuvio.tv.ui.theme.NuvioColors
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
fun LicenseStatusScreen(
    gateMode: Boolean,
    onBackPress: () -> Unit = {},
    viewModel: LicenseViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    BackHandler(enabled = !gateMode) {
        onBackPress()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(NuvioColors.Background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth(0.62f)
                .background(
                    color = NuvioColors.BackgroundElevated,
                    shape = RoundedCornerShape(20.dp)
                )
                .padding(28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            androidx.compose.foundation.Image(
                painter = painterResource(id = R.drawable.app_logo_wordmark),
                contentDescription = stringResource(R.string.license_status_title),
                modifier = Modifier
                    .fillMaxWidth(0.78f)
                    .height(56.dp)
            )

            Text(
                text = stringResource(R.string.license_status_title),
                style = MaterialTheme.typography.headlineSmall,
                color = NuvioColors.TextPrimary,
                fontWeight = FontWeight.SemiBold
            )

            Text(
                text = if (gateMode) {
                    stringResource(R.string.license_status_gate_subtitle)
                } else {
                    stringResource(R.string.license_status_subtitle)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = NuvioColors.TextSecondary,
                textAlign = TextAlign.Center
            )

            LicenseStatusSummaryCard(status = uiState.status)

            InputField(
                value = uiState.draftCode,
                onValueChange = viewModel::updateDraftCode,
                placeholder = stringResource(R.string.license_status_code_placeholder),
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Done,
                onImeAction = viewModel::submit
            )

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = viewModel::submit,
                    enabled = !uiState.isSubmitting,
                    colors = ButtonDefaults.colors(
                        containerColor = NuvioColors.Secondary,
                        focusedContainerColor = NuvioColors.SecondaryVariant,
                        contentColor = NuvioColors.OnSecondary,
                        focusedContentColor = NuvioColors.OnSecondaryVariant
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(50))
                ) {
                    Icon(
                        imageVector = Icons.Default.VpnKey,
                        contentDescription = null
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.license_status_activate))
                }

                Button(
                    onClick = viewModel::clearLicenseCode,
                    enabled = !uiState.isSubmitting,
                    colors = ButtonDefaults.colors(
                        containerColor = NuvioColors.BackgroundCard,
                        focusedContainerColor = NuvioColors.FocusBackground,
                        contentColor = NuvioColors.TextPrimary,
                        focusedContentColor = NuvioColors.TextPrimary
                    ),
                    shape = ButtonDefaults.shape(RoundedCornerShape(50))
                ) {
                    Text(stringResource(R.string.license_status_clear))
                }
            }

            if (gateMode && uiState.status !is LicenseStatus.Valid) {
                Text(
                    text = stringResource(R.string.license_status_gate_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioColors.TextTertiary,
                    textAlign = TextAlign.Center
                )
            }
        }
    }
}

@Composable
private fun LicenseStatusSummaryCard(status: LicenseStatus) {
    val (title, subtitle, icon, accent) = when (status) {
        LicenseStatus.Loading -> LicenseSummaryContent(
            title = stringResource(R.string.license_status_loading),
            subtitle = stringResource(R.string.license_status_loading_subtitle),
            icon = Icons.Default.VpnKey,
            accent = NuvioColors.TextSecondary
        )

        LicenseStatus.Missing -> LicenseSummaryContent(
            title = stringResource(R.string.license_status_missing_title),
            subtitle = stringResource(R.string.license_status_missing_subtitle),
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFFFB74D)
        )

        is LicenseStatus.Invalid -> LicenseSummaryContent(
            title = stringResource(R.string.license_status_invalid_title),
            subtitle = stringResource(R.string.license_status_invalid_subtitle),
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFE57373)
        )

        LicenseStatus.NetworkError -> LicenseSummaryContent(
            title = stringResource(R.string.license_status_network_title),
            subtitle = stringResource(R.string.license_status_network_subtitle),
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFE57373)
        )

        is LicenseStatus.NotStarted -> LicenseSummaryContent(
            title = stringResource(R.string.license_status_not_started_title),
            subtitle = stringResource(
                R.string.license_status_not_started_subtitle,
                formatLicenseInstant(status.startsAt)
            ),
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFFFB74D),
            record = status.record
        )

        is LicenseStatus.Expired -> LicenseSummaryContent(
            title = stringResource(R.string.license_status_expired_title),
            subtitle = stringResource(
                R.string.license_status_expired_subtitle,
                formatLicenseInstant(status.deadlineAt)
            ),
            icon = Icons.Default.ErrorOutline,
            accent = Color(0xFFE57373),
            record = status.record
        )

        is LicenseStatus.Valid -> LicenseSummaryContent(
            title = stringResource(R.string.license_status_valid_title),
            subtitle = stringResource(
                R.string.license_status_valid_subtitle,
                status.remainingDays.toString(),
                formatLicenseInstant(status.deadlineAt)
            ),
            icon = Icons.Default.CheckCircle,
            accent = Color(0xFF7CFF9B),
            record = status.record
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(NuvioColors.BackgroundCard, RoundedCornerShape(16.dp))
            .padding(18.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = accent
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = NuvioColors.TextPrimary,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = NuvioColors.TextSecondary
            )
            if (status is LicenseStatus.Valid || status is LicenseStatus.Expired || status is LicenseStatus.NotStarted) {
                val record = when (status) {
                    is LicenseStatus.Valid -> status.record
                    is LicenseStatus.Expired -> status.record
                    is LicenseStatus.NotStarted -> status.record
                    else -> null
                }
                if (record != null) {
                    Text(
                        text = "${record.firstName} ${record.lastName}  •  ${record.email}",
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioColors.TextTertiary
                    )
                }
            }
        }
    }
}

private data class LicenseSummaryContent(
    val title: String,
    val subtitle: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
    val accent: Color,
    val record: LicenseRecord? = null
)

internal fun formatLicenseInstant(instant: Instant): String {
    val formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy")
        .withZone(ZoneId.systemDefault())
    return formatter.format(instant)
}
