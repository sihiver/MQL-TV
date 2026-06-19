package com.sihiver.mqltv2.data.source

import com.sihiver.mqltv2.domain.model.Channel
import com.sihiver.mqltv2.domain.model.EpgProgram

/** Demo HLS — Apple BipBop test stream (works offline in emulator with network). */
private const val DEMO_HLS =
    "https://devstreaming-cdn.apple.com/videos/streaming/examples/bipbop_16x9/bipbop_16x9_variant.m3u8"

object LocalChannelDataSource {

    private val channels = listOf(
        ch(1, "RCTI", "Nasional", "📺", 0xFFE53E3E, true, "1.2M", "Sinetron Cinta Abadi", "20:00–22:00"),
        ch(2, "SCTV", "Nasional", "📡", 0xFF3182CE, true, "890K", "Liputan 6 Malam", "21:00–21:30"),
        ch(3, "Trans7", "Nasional", "🎬", 0xFF38A169, true, "750K", "Jejak Petualang", "20:30–21:30"),
        ch(4, "ESPN", "Sport", "⚽", 0xFFD69E2E, true, "2.1M", "UEFA Champions League", "21:45–23:45"),
        ch(5, "CNN Indonesia", "News", "📰", 0xFFE53E3E, true, "430K", "Prime Time News", "21:00–22:00"),
        ch(6, "BeIN Sport 1", "Sport", "🏆", 0xFF805AD5, true, "980K", "La Liga Highlights", "22:00–23:00"),
        ch(7, "Nat Geo", "Documentary", "🌍", 0xFFDD6B20, false, "320K", "Wild Indonesia", "20:00–21:00"),
        ch(8, "HBO Asia", "Movie", "🎥", 0xFF553C9A, true, "560K", "Dune: Part Two", "20:00–22:30"),
        ch(9, "Cartoon Net", "Kids", "🎨", 0xFFF6AD55, true, "1.5M", "Adventure Time", "19:00–21:00"),
        ch(10, "Metro TV", "News", "📻", 0xFF4299E1, true, "670K", "Metro Malam", "21:30–22:00"),
        ch(11, "GTV", "Nasional", "✨", 0xFF48BB78, false, "410K", "FTV Spesial", "20:00–22:00"),
        ch(12, "One Sport", "Sport", "🥊", 0xFFFC8181, true, "290K", "Boxing Night", "22:00–00:00"),
        ch(13, "iNews", "News", "📲", 0xFF63B3ED, true, "380K", "Top News", "21:00–21:30"),
        ch(14, "ANTV", "Nasional", "🎭", 0xFFF687B3, true, "610K", "Bollywood Night", "21:00–23:00"),
        ch(15, "tvOne", "News", "🗞️", 0xFF68D391, true, "520K", "Kabar Malam", "21:00–22:00"),
        ch(16, "MNC TV", "Movie", "🍿", 0xFFB794F4, true, "470K", "Oscar Night Cinema", "20:00–22:30"),
    )

    val categories = listOf("Semua", "Nasional", "Sport", "News", "Movie", "Documentary", "Kids")

    fun getAll(): List<Channel> = channels

    fun getById(id: Int): Channel? = channels.find { it.id == id }

    fun getByCategory(category: String): List<Channel> =
        channels.filter { it.category == category }

    fun search(query: String): List<Channel> {
        if (query.isBlank()) return emptyList()
        val q = query.trim().lowercase()
        return channels.filter {
            it.name.lowercase().contains(q) ||
                it.category.lowercase().contains(q) ||
                it.program.lowercase().contains(q)
        }
    }

    private fun ch(
        id: Int,
        name: String,
        category: String,
        logo: String,
        colorHex: Long,
        live: Boolean,
        viewers: String,
        program: String,
        time: String,
    ) = Channel(
        id = id,
        name = name,
        category = category,
        logo = logo,
        colorHex = colorHex,
        live = live,
        viewers = viewers,
        program = program,
        time = time,
        streamUrl = DEMO_HLS,
    )
}

object LocalEpgDataSource {

    private val schedule = listOf(
        epg(0, "18:00", "Berita Petang", "60 min", done = true),
        epg(0, "19:00", "Sinetron Pilihan", "60 min", done = true),
        epg(0, "20:00", "Film Unggulan", "90 min"),
        epg(0, "21:45", "UEFA Champions League", "120 min", active = true),
        epg(0, "23:45", "Highlight Malam", "30 min"),
    )

    fun forChannel(channelId: Int): List<EpgProgram> =
        schedule.map { it.copy(channelId = channelId) }

    fun allPrograms(): List<EpgProgram> =
        LocalChannelDataSource.getAll().flatMap { forChannel(it.id) }

    private fun epg(
        channelId: Int,
        time: String,
        title: String,
        duration: String,
        done: Boolean = false,
        active: Boolean = false,
    ) = EpgProgram(channelId, time, title, duration, done, active)
}
