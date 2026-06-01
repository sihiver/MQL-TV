package com.sihiver.mqltv.domain.model

data class StreamQualityOption(
    val id: String,
    val label: String,
    val height: Int? = null,
    val url: String? = null,
) {
    val isAuto: Boolean get() = id == "auto"
}
