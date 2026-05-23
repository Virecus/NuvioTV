@file:OptIn(ExperimentalTvMaterial3Api::class, ExperimentalFoundationApi::class)

package com.nuvio.tv.ui.screens.livetv

import androidx.compose.animation.core.AnimationSpec
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.focusGroup
import androidx.compose.foundation.gestures.BringIntoViewSpec
import androidx.compose.foundation.gestures.LocalBringIntoViewSpec
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.focusRestorer
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Border
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
import com.nuvio.tv.ui.theme.NuvioColors

// ─── Kaynak seçim ekranı ─────────────────────────────────────────────────────

@Composable
fun LiveTvSourceScreen(
    onSourceSelected: (ScraperInfo) -> Unit,
    viewModel: LiveTvSourceViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> FullScreenLoading(stringResource(R.string.live_tv_sources_loading))
        uiState.sources.isEmpty() -> FullScreenEmpty(stringResource(R.string.live_tv_sources_empty), viewModel::loadSources)
        else -> {
            val bivSpec = rememberHorizontalBivSpec(48)
            CompositionLocalProvider(LocalBringIntoViewSpec provides bivSpec) {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
                    verticalArrangement = Arrangement.spacedBy(32.dp)
                ) {
                    item(key = "header") {
                        Text(stringResource(R.string.nav_live_tv), style = MaterialTheme.typography.headlineMedium, color = NuvioColors.TextPrimary, modifier = Modifier.padding(start = 48.dp))
                    }
                    item(key = "sources_row") {
                        LiveSourcesRow(uiState.sources, onSourceSelected)
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveSourcesRow(sources: List<ScraperInfo>, onSourceSelected: (ScraperInfo) -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(stringResource(R.string.live_tv_platforms_title), style = MaterialTheme.typography.headlineMedium, color = NuvioColors.TextPrimary, modifier = Modifier.padding(start = 48.dp, bottom = 12.dp))
        LazyRow(
            modifier = Modifier.fillMaxWidth().focusRestorer().focusGroup(),
            contentPadding = PaddingValues(start = 48.dp, end = 200.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(sources, key = { _, s -> s.id }) { _, source ->
                LiveSourceCard(source, onClick = { onSourceSelected(source) })
            }
        }
    }
}

@Composable
private fun LiveSourceCard(source: ScraperInfo, onClick: () -> Unit) {
    val shape = RoundedCornerShape(12.dp)
    Card(
        onClick = onClick,
        modifier = Modifier.width(220.dp).height(124.dp),
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(containerColor = NuvioColors.BackgroundCard, focusedContainerColor = NuvioColors.BackgroundCard),
        border = CardDefaults.border(focusedBorder = Border(border = BorderStroke(2.dp, NuvioColors.FocusRing), shape = shape)),
        scale = CardDefaults.scale(focusedScale = 1.04f)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (!source.logo.isNullOrBlank()) {
                AsyncImage(model = source.logo, contentDescription = source.name, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize().clip(shape))
                Box(Modifier.fillMaxSize().background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(.7f)), startY = 40f)))
            } else {
                Box(Modifier.fillMaxSize().background(NuvioColors.BackgroundCard), contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Tv, null, tint = NuvioColors.TextTertiary, modifier = Modifier.size(40.dp))
                }
            }
            Column(Modifier.align(Alignment.BottomStart).fillMaxWidth().padding(10.dp)) {
                Text(source.name, style = MaterialTheme.typography.titleMedium, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                if (source.description.isNotBlank()) Text(source.description, style = MaterialTheme.typography.bodySmall, color = Color.White.copy(.7f), maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

// ─── Kaynak içerik ekranı — kategori listesi, tıklayınca yeni sayfa ──────────

@Composable
fun LiveTvCategoryScreen(
    scraperName: String,
    onCategorySelected: (String) -> Unit,
    onPlayChannel: (LocalScraperResult, LiveChannel) -> Unit,
    viewModel: LiveTvChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    when {
        uiState.isLoading -> FullScreenLoading(stringResource(R.string.live_tv_loading))
        uiState.error != null && uiState.channels.isEmpty() -> FullScreenEmpty(uiState.error ?: stringResource(R.string.live_tv_error), viewModel::loadChannels)
        uiState.channels.isEmpty() -> FullScreenEmpty(stringResource(R.string.live_tv_empty), null)
        else -> {
            val channelsByCategory = remember(uiState.channels) { uiState.channels.groupBy { it.category } }
            val categories = remember(uiState.channels) { uiState.channels.map { it.category }.distinct() }

            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                item(key = "header") {
                    Column(Modifier.padding(start = 48.dp, bottom = 16.dp)) {
                        Text(scraperName, style = MaterialTheme.typography.headlineMedium, color = NuvioColors.TextPrimary)
                        Text(stringResource(R.string.nav_live_tv), style = MaterialTheme.typography.bodyMedium, color = NuvioColors.TextTertiary)
                    }
                }

                itemsIndexed(categories, key = { _, cat -> cat }) { _, category ->
                    val count = channelsByCategory[category]?.size ?: 0
                    LiveCategoryListItem(
                        category = category,
                        count = count,
                        onClick = { onCategorySelected(category) }
                    )
                }
            }
        }
    }
}

@Composable
private fun LiveCategoryListItem(
    category: String,
    count: Int,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(8.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 48.dp, vertical = 2.dp)
            .onFocusChanged { isFocused = it.hasFocus },
        colors = CardDefaults.colors(
            containerColor = NuvioColors.SurfaceVariant.copy(alpha = 0.4f),
            focusedContainerColor = NuvioColors.FocusBackground
        ),
        border = CardDefaults.border(
            border = Border.None,
            focusedBorder = Border(border = BorderStroke(2.dp, NuvioColors.FocusRing), shape = shape)
        ),
        shape = CardDefaults.shape(shape = shape)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = category,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isFocused) NuvioColors.TextPrimary else NuvioColors.TextSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "$count içerik",
                style = MaterialTheme.typography.bodySmall,
                color = NuvioColors.TextTertiary
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = NuvioColors.TextTertiary,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

// ─── Kanal listesi ekranı (kategori → kanallar alt alta yatay kart) ──────────

@Composable
fun LiveTvChannelScreen(
    scraperName: String,
    category: String,
    onPlayChannel: (LocalScraperResult, LiveChannel) -> Unit,
    viewModel: LiveTvChannelViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val channels = remember(uiState.channels, category) { viewModel.channelsForCategory(category) }

    when {
        uiState.isLoading -> FullScreenLoading(stringResource(R.string.live_tv_loading))
        channels.isEmpty() && !uiState.isLoading -> FullScreenEmpty(stringResource(R.string.live_tv_empty), null)
        else -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(top = 24.dp, bottom = 32.dp, start = 48.dp, end = 48.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                item(key = "header") {
                    Column(Modifier.padding(bottom = 16.dp)) {
                        Text(category, style = MaterialTheme.typography.headlineMedium, color = NuvioColors.TextPrimary)
                        Text("$scraperName · ${channels.size} içerik", style = MaterialTheme.typography.bodyMedium, color = NuvioColors.TextTertiary)
                    }
                }
                itemsIndexed(channels, key = { _, ch -> ch.id }) { _, channel ->
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
        }
    }
}

@Composable
private fun LiveChannelRow(
    channel: LiveChannel,
    isLoading: Boolean,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }
    val shape = RoundedCornerShape(10.dp)
    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(88.dp)
            .onFocusChanged { isFocused = it.isFocused },
        shape = CardDefaults.shape(shape = shape),
        colors = CardDefaults.colors(
            containerColor = NuvioColors.BackgroundCard,
            focusedContainerColor = NuvioColors.BackgroundCard
        ),
        border = CardDefaults.border(
            focusedBorder = Border(border = BorderStroke(2.dp, NuvioColors.FocusRing), shape = shape)
        ),
        scale = CardDefaults.scale(focusedScale = 1.02f)
    ) {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Sol: poster/thumbnail
            Box(
                modifier = Modifier
                    .width(140.dp)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(topStart = 10.dp, bottomStart = 10.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (!channel.poster.isNullOrBlank()) {
                    AsyncImage(
                        model = channel.poster,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(Modifier.fillMaxSize().background(Brush.horizontalGradient(listOf(Color.Transparent, NuvioColors.BackgroundCard), startX = 80f)))
                } else {
                    Box(Modifier.fillMaxSize().background(NuvioColors.SurfaceVariant)) {
                        Icon(Icons.Default.PlayArrow, null, tint = NuvioColors.TextTertiary, modifier = Modifier.align(Alignment.Center).size(28.dp))
                    }
                }
                if (isLoading) {
                    CircularProgressIndicator(Modifier.size(24.dp), color = NuvioColors.Primary, strokeWidth = 2.dp)
                }
            }
            // Sağ: kanal adı
            Text(
                text = channel.name,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Medium),
                color = if (isFocused) NuvioColors.TextPrimary else NuvioColors.TextSecondary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f).padding(horizontal = 16.dp)
            )
        }
    }
}

// ─── Yardımcılar ──────────────────────────────────────────────────────────────

@Composable
private fun rememberHorizontalBivSpec(startDp: Int): BringIntoViewSpec {
    val density = LocalDensity.current
    val defaultSpec = LocalBringIntoViewSpec.current
    return remember(density, defaultSpec, startDp) {
        val startPx = with(density) { startDp.dp.roundToPx() }
        @Suppress("DEPRECATION", "OVERRIDE_DEPRECATION")
        object : BringIntoViewSpec {
            override val scrollAnimationSpec: AnimationSpec<Float> = defaultSpec.scrollAnimationSpec
            override fun calculateScrollDistance(offset: Float, size: Float, containerSize: Float): Float {
                val childSize = kotlin.math.abs(size)
                val space = containerSize - startPx
                val leading = if (childSize <= containerSize && space < childSize) containerSize - childSize else startPx.toFloat()
                return offset - leading
            }
        }
    }
}

@Composable
private fun FullScreenLoading(message: String) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(color = NuvioColors.Primary)
            Spacer(Modifier.height(12.dp))
            Text(message, style = MaterialTheme.typography.bodyMedium, color = NuvioColors.TextSecondary)
        }
    }
}

@Composable
private fun FullScreenEmpty(message: String, onRetry: (() -> Unit)?) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(message, style = MaterialTheme.typography.bodyMedium, color = NuvioColors.TextSecondary)
            if (onRetry != null) {
                Spacer(Modifier.height(16.dp))
                Button(onRetry, colors = ButtonDefaults.colors(containerColor = NuvioColors.Primary, focusedContainerColor = NuvioColors.Primary)) {
                    Icon(Icons.Default.Refresh, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text(stringResource(R.string.live_tv_retry))
                }
            }
        }
    }
}
