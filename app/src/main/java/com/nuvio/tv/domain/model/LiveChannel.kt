package com.nuvio.tv.domain.model

import androidx.compose.runtime.Immutable

@Immutable
data class LiveChannel(
    val id: String,
    val name: String,
    val poster: String?,
    val category: String,
    val scraperId: String = "",
    val isDisabled: Boolean = false
)
