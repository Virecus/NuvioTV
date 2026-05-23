@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.livetv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import coil3.compose.AsyncImage
import com.nuvio.tv.R
import com.nuvio.tv.domain.model.LiveChannel
import com.nuvio.tv.domain.model.LocalScraperResult
import com.nuvio.tv.domain.model.ScraperInfo
import com.nuvio.tv.ui.screens.settings.SettingsDetailHeader
import com.nuvio.tv.ui.screens.settings.SettingsGroupCard
import com.nuvio.tv.ui.screens.settings.SettingsVerticalScrollIndicators
import com.nuvio.tv.ui.theme.NuvioColors

// ─── Kaynak seçim ekranı (InatBox, RecTV…) ───────────────────────────────────

@Composable
fun LiveTvSourceScreen(
    onSourceSelected: (ScraperInfo) -> Unit,
    viewModel: LiveTvSourceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.nav_live_tv),
            subtitle = stringResource(R.string.live_tv_sources_subtitle)
        )

        SettingsGroupCard(
            modifier = Modifier.fillMaxWidth().weight(1f)
        ) {
            when {
                uiState.isLoading -> LoadingBox(stringResource(R.string.live_tv_sources_loading))

                uiState.sources.isEmpty() -> EmptyBox(
                    message = stringResource(R.string.live_tv_sources_empty),
                    onRetry = viewModel::loadSources
                )

                else -> {
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(uiState.sources, key = { it.id }) { source ->
                                LiveSourceRow(source = source, onClick = { onSourceSelected(source) })
                            }
                        }
                        SettingsVerticalScrollIndicators(state = listState)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSourceRow(source: ScraperInfo, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .onFocusChanged { isFocused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = if (isFocused) NuvioColors.FocusBackground else Color.Transparent,
            focusedContainerColor = NuvioColors.FocusBackground
        ),
        border = CardDefaults.border(
            border = androidx.tv.material3.Border.None,
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, NuvioColors.FocusRing),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (source.logo != null) {
                AsyncImage(
                    model = source.logo,
                    contentDescription = source.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)).background(NuvioColors.SurfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)).background(NuvioColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NuvioColors.TextTertiary, modifier = Modifier.size(20.dp))
                }
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = source.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) NuvioColors.TextPrimary else NuvioColors.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                if (source.description.isNotBlank()) {
                    Text(
                        text = source.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = NuvioColors.TextTertiary,
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// ─── Kategori listesi ekranı ──────────────────────────────────────────────────

@Composable
fun LiveTvCategoryScreen(
    scraperName: String,
    onCategorySelected: (String) -> Unit,
    viewModel: LiveTvChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsDetailHeader(title = scraperName, subtitle = stringResource(R.string.nav_live_tv))

        SettingsGroupCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.isLoading -> LoadingBox(stringResource(R.string.live_tv_loading))

                uiState.error != null && uiState.channels.isEmpty() -> EmptyBox(
                    message = uiState.error ?: stringResource(R.string.live_tv_error),
                    onRetry = viewModel::loadChannels
                )

                uiState.channels.isEmpty() -> EmptyBox(
                    message = stringResource(R.string.live_tv_empty),
                    onRetry = null
                )

                else -> {
                    val categories = remember(uiState.channels) {
                        uiState.channels.map { it.category }.distinct()
                    }
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(categories, key = { it }) { category ->
                                LiveCategoryRow(
                                    category = category,
                                    channelCount = uiState.channels.count { it.category == category },
                                    onClick = { onCategorySelected(category) }
                                )
                            }
                        }
                        SettingsVerticalScrollIndicators(state = listState)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveCategoryRow(category: String, channelCount: Int, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .onFocusChanged { isFocused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = if (isFocused) NuvioColors.FocusBackground else Color.Transparent,
            focusedContainerColor = NuvioColors.FocusBackground
        ),
        border = CardDefaults.border(
            border = androidx.tv.material3.Border.None,
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, NuvioColors.FocusRing),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)).background(NuvioColors.SurfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.List, contentDescription = null, tint = NuvioColors.TextTertiary, modifier = Modifier.size(20.dp))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isFocused) NuvioColors.TextPrimary else NuvioColors.TextSecondary,
                    maxLines = 1, overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$channelCount kanal",
                    style = MaterialTheme.typography.bodySmall,
                    color = NuvioColors.TextTertiary
                )
            }
        }
    }
}

// ─── Kanal listesi ekranı ─────────────────────────────────────────────────────

@Composable
fun LiveTvChannelScreen(
    scraperName: String,
    category: String,
    onPlayChannel: (LocalScraperResult, LiveChannel) -> Unit,
    viewModel: LiveTvChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val channels = remember(uiState.channels, category) {
        viewModel.channelsForCategory(category)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsDetailHeader(title = category, subtitle = scraperName)

        SettingsGroupCard(modifier = Modifier.fillMaxWidth().weight(1f)) {
            when {
                uiState.isLoading -> LoadingBox(stringResource(R.string.live_tv_loading))

                channels.isEmpty() && !uiState.isLoading -> EmptyBox(
                    message = stringResource(R.string.live_tv_empty),
                    onRetry = null
                )

                else -> {
                    val listState = rememberLazyListState()
                    Box(modifier = Modifier.fillMaxSize()) {
                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            items(channels, key = { it.id }) { channel ->
                                LiveChannelRow(
                                    channel = channel,
                                    isLoading = uiState.loadingChannelId == channel.id,
                                    onClick = {
                                        viewModel.onChannelSelected(channel) { streams ->
                                            streams.firstOrNull()?.let { onPlayChannel(it, channel) }
                                        }
                                    }
                                )
                            }
                        }
                        SettingsVerticalScrollIndicators(state = listState)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveChannelRow(channel: LiveChannel, isLoading: Boolean, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
            .onFocusChanged { isFocused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = if (isFocused) NuvioColors.FocusBackground else Color.Transparent,
            focusedContainerColor = NuvioColors.FocusBackground
        ),
        border = CardDefaults.border(
            border = androidx.tv.material3.Border.None,
            focusedBorder = androidx.tv.material3.Border(
                border = androidx.compose.foundation.BorderStroke(2.dp, NuvioColors.FocusRing),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (channel.poster != null) {
                AsyncImage(
                    model = channel.poster,
                    contentDescription = channel.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)).background(NuvioColors.SurfaceVariant)
                )
            } else {
                Box(
                    modifier = Modifier.size(width = 60.dp, height = 40.dp).clip(RoundedCornerShape(6.dp)).background(NuvioColors.SurfaceVariant),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null, tint = NuvioColors.TextTertiary, modifier = Modifier.size(20.dp))
                }
            }
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium,
                color = if (isFocused) NuvioColors.TextPrimary else NuvioColors.TextSecondary,
                maxLines = 1, overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(20.dp), color = NuvioColors.Primary, strokeWidth = 2.dp)
            }
        }
    }
}

// ─── Ortak yardımcılar ────────────────────────────────────────────────────────

@Composable
private fun LoadingBox(message: String) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NuvioColors.Primary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = NuvioColors.TextSecondary)
        }
    }
}

@Composable
private fun EmptyBox(message: String, onRetry: (() -> Unit)?) {
    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = message, style = MaterialTheme.typography.bodyMedium, color = NuvioColors.TextSecondary)
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onRetry,
                    colors = ButtonDefaults.colors(containerColor = NuvioColors.Primary, focusedContainerColor = NuvioColors.Primary)
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(stringResource(R.string.live_tv_retry))
                }
            }
        }
    }
}
