package com.sihiver.mqltv.data.stream

data class ParsedStreamUrl(
    val url: String,
    val userAgent: String? = null,
    val referer: String? = null,
)

object IptvStreamUrl {
    private const val DEFAULT_USER_AGENT =
        "Mozilla/5.0 (Linux; Android 10; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    fun parse(raw: String): ParsedStreamUrl {
        val trimmed = raw.trim()
        val pipeIndex = trimmed.indexOf('|')
        if (pipeIndex < 0) {
            return ParsedStreamUrl(url = trimmed)
        }

        var url = trimmed.substring(0, pipeIndex).trim()
        var userAgent: String? = null
        var referer: String? = null

        val opts = trimmed.substring(pipeIndex + 1)
        for (segment in opts.split('|')) {
            val eq = segment.indexOf('=')
            if (eq < 0) continue
            val key = segment.substring(0, eq).trim().lowercase().replace('-', '_')
            val value = segment.substring(eq + 1).trim()
            if (value.isEmpty()) continue
            when (key) {
                "user_agent" -> userAgent = value
                "referer" -> referer = value
            }
        }

        return ParsedStreamUrl(url = url, userAgent = userAgent, referer = referer)
    }

    fun resolvePlaybackUrl(raw: String): String = parse(raw).url

    fun resolveHeaders(
        rawUrl: String,
        apiUserAgent: String? = null,
        apiReferer: String? = null,
    ): Pair<String, Map<String, String>> {
        val parsed = parse(rawUrl)
        val url = parsed.url

        var userAgent = apiUserAgent?.trim().takeUnless { it.isNullOrEmpty() }
            ?: parsed.userAgent?.trim().takeUnless { it.isNullOrEmpty() }
        if (userAgent.isNullOrEmpty() || userAgent == "Mozilla/5.0") {
            userAgent = DEFAULT_USER_AGENT
        }

        var referer = apiReferer?.trim().takeUnless { it.isNullOrEmpty() }
            ?: parsed.referer?.trim().takeUnless { it.isNullOrEmpty() }
        if (referer.isNullOrEmpty()) {
            referer = runCatching {
                val u = java.net.URI(url)
                "${u.scheme}://${u.host}/"
            }.getOrNull()
        }

        val headers = buildMap {
            put("User-Agent", userAgent)
            referer?.let {
                put("Referer", it)
                put("Origin", runCatching { java.net.URI(it).let { uri -> "${uri.scheme}://${uri.host}" } }.getOrNull() ?: it)
            }
        }

        return url to headers
    }
}
