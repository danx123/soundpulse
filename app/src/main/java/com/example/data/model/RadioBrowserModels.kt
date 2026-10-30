package com.example.data.model

data class RadioCountry(
    val name: String,
    val code: String,
    val stationCount: Int
)

data class RadioBrowserState(
    val stations: List<StreamingStation> = emptyList(),
    val isLoading: Boolean = false,
    val error: String? = null,
    val canLoadMore: Boolean = false
)

data class RadioPage(
    val stations: List<StreamingStation>,
    val hasMore: Boolean
)
