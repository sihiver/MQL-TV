package com.sihiver.mqltv.data

import androidx.compose.ui.graphics.Color

private fun hex(color: Long) = Color(color)

val sampleChannels = listOf(
    Channel(1, "RCTI", "Nasional", "📺", hex(0xFFE53E3E), true, "1.2M", "Sinetron Cinta Abadi", "20:00–22:00"),
    Channel(2, "SCTV", "Nasional", "📡", hex(0xFF3182CE), true, "890K", "Liputan 6 Malam", "21:00–21:30"),
    Channel(3, "Trans7", "Nasional", "🎬", hex(0xFF38A169), true, "750K", "Jejak Petualang", "20:30–21:30"),
    Channel(4, "ESPN", "Sport", "⚽", hex(0xFFD69E2E), true, "2.1M", "UEFA Champions League", "21:45–23:45"),
    Channel(5, "CNN Indonesia", "News", "📰", hex(0xFFE53E3E), true, "430K", "Prime Time News", "21:00–22:00"),
    Channel(6, "BeIN Sport 1", "Sport", "🏆", hex(0xFF805AD5), true, "980K", "La Liga Highlights", "22:00–23:00"),
    Channel(7, "Nat Geo", "Documentary", "🌍", hex(0xFFDD6B20), false, "320K", "Wild Indonesia", "20:00–21:00"),
    Channel(8, "HBO Asia", "Movie", "🎥", hex(0xFF553C9A), true, "560K", "Dune: Part Two", "20:00–22:30"),
    Channel(9, "Cartoon Net", "Kids", "🎨", hex(0xFFF6AD55), true, "1.5M", "Adventure Time", "19:00–21:00"),
    Channel(10, "Metro TV", "News", "📻", hex(0xFF4299E1), true, "670K", "Metro Malam", "21:30–22:00"),
    Channel(11, "GTV", "Nasional", "✨", hex(0xFF48BB78), false, "410K", "FTV Spesial", "20:00–22:00"),
    Channel(12, "One Sport", "Sport", "🥊", hex(0xFFFC8181), true, "290K", "Boxing Night", "22:00–00:00"),
    Channel(13, "iNews", "News", "📲", hex(0xFF63B3ED), true, "380K", "Top News", "21:00–21:30"),
    Channel(14, "ANTV", "Nasional", "🎭", hex(0xFFF687B3), true, "610K", "Bollywood Night", "21:00–23:00"),
    Channel(15, "tvOne", "News", "🗞️", hex(0xFF68D391), true, "520K", "Kabar Malam", "21:00–22:00"),
    Channel(16, "MNC TV", "Movie", "🍿", hex(0xFFB794F4), true, "470K", "Oscar Night Cinema", "20:00–22:30"),
)

val categories = listOf("Semua", "Nasional", "Sport", "News", "Movie", "Documentary", "Kids")

val sampleEpgData = listOf(
    EpgItem("18:00", "Berita Petang", "60 min", done = true),
    EpgItem("19:00", "Sinetron Pilihan", "60 min", done = true),
    EpgItem("20:00", "Film Unggulan", "90 min"),
    EpgItem("21:45", "UEFA Champions League", "120 min", active = true),
    EpgItem("23:45", "Highlight Malam", "30 min"),
)
