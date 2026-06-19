package com.sihiver.mqltv2.data.parser

import android.util.Xml
import com.sihiver.mqltv2.domain.model.StreamQualityOption
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.xmlpull.v1.XmlPullParser
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Parser lokal untuk manifes MPEG-DASH (.mpd).
 *
 * Mengunduh XML manifes dari [mpdUrl] dan mengekstrak semua elemen
 * `<Representation>` di dalam `<AdaptationSet>` bertipe video yang
 * memiliki atribut `width` dan `height`. Tidak memerlukan library
 * tambahan — hanya memakai [XmlPullParser] bawaan Android.
 *
 * Hasilnya adalah daftar [StreamQualityOption] dengan "Otomatis" di
 * urutan pertama, diikuti resolusi dari tertinggi ke terendah.
 *
 * @param mpdUrl URL absolut manifes `.mpd`.
 * @param userAgent Nilai header `User-Agent` opsional.
 * @param referer Nilai header `Referer` opsional.
 * @return Daftar opsi resolusi, minimal berisi opsi "Otomatis".
 */
object MpdQualityParser {

    private const val CONNECT_TIMEOUT_MS = 10_000
    private const val READ_TIMEOUT_MS = 15_000
    private const val DEFAULT_UA =
        "Mozilla/5.0 (Linux; Android 10; Android TV) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36"

    suspend fun parse(
        mpdUrl: String,
        userAgent: String? = null,
        referer: String? = null,
    ): List<StreamQualityOption> = withContext(Dispatchers.IO) {
        runCatching {
            val stream = fetchManifest(mpdUrl, userAgent, referer)
            val representations = parseXml(stream)
            buildOptions(representations)
        }.getOrElse {
            listOf(autoOption())
        }
    }

    // ── HTTP fetch ────────────────────────────────────────────────────────────

    private fun fetchManifest(url: String, userAgent: String?, referer: String?): InputStream {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = CONNECT_TIMEOUT_MS
        conn.readTimeout = READ_TIMEOUT_MS
        conn.setRequestProperty("User-Agent", userAgent?.takeIf { it.isNotBlank() } ?: DEFAULT_UA)
        referer?.takeIf { it.isNotBlank() }?.let { conn.setRequestProperty("Referer", it) }
        conn.connect()
        check(conn.responseCode in 200..299) {
            "HTTP ${conn.responseCode} saat mengambil manifes MPD"
        }
        return conn.inputStream
    }

    // ── XML parse ─────────────────────────────────────────────────────────────

    /**
     * Data sementara satu `<Representation>` video.
     * [bandwidth] dipakai sebagai tie-breaker jika height sama.
     */
    private data class Rep(val width: Int, val height: Int, val bandwidth: Int)

    /**
     * Parse XmlPullParser secara sekuensial.
     *
     * Struktur MPD yang relevan:
     * ```xml
     * <AdaptationSet contentType="video" | mimeType="video/...">
     *   <Representation width="1920" height="1080" bandwidth="4000000" .../>
     * </AdaptationSet>
     * ```
     *
     * `contentType` atau `mimeType` yang mengandung "video" menandai
     * adaptation set video. `width`/`height` bisa ada di
     * `<Representation>` atau diwarisi dari `<AdaptationSet>`.
     */
    private fun parseXml(stream: InputStream): List<Rep> {
        val parser = Xml.newPullParser()
        parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false)
        parser.setInput(stream, null)

        val reps = mutableListOf<Rep>()
        var inVideoAdaptation = false

        // Dimensi yang mungkin diwarisi dari <AdaptationSet>
        var inheritedWidth = 0
        var inheritedHeight = 0

        var eventType = parser.eventType
        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tag = parser.name ?: ""
            when (eventType) {
                XmlPullParser.START_TAG -> when (tag) {
                    "AdaptationSet" -> {
                        val contentType = parser.getAttributeValue(null, "contentType") ?: ""
                        val mimeType = parser.getAttributeValue(null, "mimeType") ?: ""
                        inVideoAdaptation =
                            contentType.contains("video", ignoreCase = true) ||
                            mimeType.contains("video", ignoreCase = true)
                        inheritedWidth = parser.getAttributeValue(null, "width")?.toIntOrNull() ?: 0
                        inheritedHeight = parser.getAttributeValue(null, "height")?.toIntOrNull() ?: 0
                    }
                    "Representation" -> if (inVideoAdaptation) {
                        val w = parser.getAttributeValue(null, "width")?.toIntOrNull()
                            ?: inheritedWidth
                        val h = parser.getAttributeValue(null, "height")?.toIntOrNull()
                            ?: inheritedHeight
                        val bw = parser.getAttributeValue(null, "bandwidth")?.toIntOrNull() ?: 0
                        if (h > 0) reps += Rep(w, h, bw)
                    }
                }
                XmlPullParser.END_TAG -> if (tag == "AdaptationSet") {
                    inVideoAdaptation = false
                    inheritedWidth = 0
                    inheritedHeight = 0
                }
            }
            eventType = parser.next()
        }
        return reps
    }

    // ── Build options ─────────────────────────────────────────────────────────

    private fun buildOptions(reps: List<Rep>): List<StreamQualityOption> {
        val unique = reps
            .distinctBy { it.height }
            .sortedByDescending { it.height }

        if (unique.isEmpty()) return listOf(autoOption())

        val items = unique.map { rep ->
            StreamQualityOption(
                id = "${rep.height}p",
                label = resolutionLabel(rep.width, rep.height),
                height = rep.height,
            )
        }
        return listOf(autoOption()) + items
    }

    private fun autoOption() = StreamQualityOption(
        id = "auto",
        label = "Otomatis",
        height = null,
    )

    /**
     * Label resolusi yang ramah pengguna.
     *
     * Contoh: height=1080 → "1080p HD", height=720 → "720p HD",
     * height=480 → "480p SD", height=2160 → "2160p 4K".
     */
    private fun resolutionLabel(width: Int, height: Int): String {
        val tag = when {
            height >= 2160 -> "4K"
            height >= 1080 -> "HD"
            height >= 720 -> "HD"
            height >= 480 -> "SD"
            else -> "SD"
        }
        return if (width > 0) "${height}p $tag (${width}×${height})" else "${height}p $tag"
    }
}
