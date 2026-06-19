package com.sihiver.mqltv2.data.parser

import com.sihiver.mqltv2.domain.model.Channel
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class M3uParser @Inject constructor() {

    fun parse(content: String, defaultStreamUrl: String = DEMO_HLS): List<Channel> {
        val lines = content.lines()
        val channels = mutableListOf<Channel>()
        var pendingName: String? = null
        var pendingLogo: String? = null
        var pendingGroup: String? = null
        var id = 1000

        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.startsWith("#EXTINF:", ignoreCase = true)) {
                pendingName = extractAttribute(trimmed, "tvg-name")
                    ?: trimmed.substringAfterLast(",").trim().ifBlank { null }
                pendingLogo = extractAttribute(trimmed, "tvg-logo")
                pendingGroup = extractAttribute(trimmed, "group-title") ?: "Lainnya"
            } else if (trimmed.isNotEmpty() && !trimmed.startsWith("#")) {
                val name = pendingName ?: "Channel ${id - 999}"
                val category = pendingGroup ?: "Lainnya"
                channels += Channel(
                    id = id++,
                    name = name,
                    category = category,
                    logo = pendingLogo?.let { "🎬" } ?: "📺",
                    colorHex = 0xFF3182CE,
                    live = true,
                    viewers = "—",
                    program = "Live",
                    time = "—",
                    streamUrl = trimmed,
                )
                pendingName = null
                pendingLogo = null
                pendingGroup = null
            }
        }
        return channels.ifEmpty {
            listOf(
                Channel(
                    id = 1,
                    name = "Demo HLS",
                    category = "Demo",
                    logo = "📺",
                    colorHex = 0xFF3182CE,
                    live = true,
                    viewers = "—",
                    program = "Test Stream",
                    time = "—",
                    streamUrl = defaultStreamUrl,
                ),
            )
        }
    }

    private fun extractAttribute(line: String, key: String): String? {
        val pattern = """$key="([^"]*)""""
        return Regex(pattern).find(line)?.groupValues?.getOrNull(1)?.takeIf { it.isNotBlank() }
    }

    companion object {
        const val DEMO_HLS =
            "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"
    }
}
