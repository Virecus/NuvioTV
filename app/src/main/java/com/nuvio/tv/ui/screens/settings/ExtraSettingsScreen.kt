@file:OptIn(ExperimentalTvMaterial3Api::class)

package com.nuvio.tv.ui.screens.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.ExperimentalTvMaterial3Api
import com.nuvio.tv.R

@Composable
fun ExtraSettingsContent(
    viewModel: ExtraSettingsViewModel = hiltViewModel(),
    initialFocusRequester: FocusRequester? = null
) {
    val uiState = viewModel.uiState.collectAsStateWithLifecycle().value

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        SettingsDetailHeader(
            title = stringResource(R.string.settings_extra),
            subtitle = stringResource(R.string.settings_extra_subtitle)
        )

        SettingsGroupCard(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            val listState = rememberLazyListState()
            Box(modifier = Modifier.fillMaxSize()) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item(key = "translate_trakt_comments_to_turkish") {
                        SettingsToggleRow(
                            title = stringResource(R.string.extra_translate_trakt_comments_title),
                            subtitle = stringResource(R.string.extra_translate_trakt_comments_subtitle),
                            checked = uiState.translateTraktCommentsToTurkish,
                            onToggle = {
                                viewModel.onEvent(
                                    ExtraSettingsEvent.ToggleTranslateTraktCommentsToTurkish(
                                        !uiState.translateTraktCommentsToTurkish
                                    )
                                )
                            },
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .then(
                                    if (initialFocusRequester != null) {
                                        Modifier.focusRequester(initialFocusRequester)
                                    } else {
                                        Modifier
                                    }
                                )
                        )
                    }
                }
                SettingsVerticalScrollIndicators(state = listState)
            }
        }
    }
}
