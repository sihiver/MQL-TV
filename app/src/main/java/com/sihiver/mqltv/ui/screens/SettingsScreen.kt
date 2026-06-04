package com.sihiver.mqltv.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Text
import com.sihiver.mqltv.data.AppScreen
import com.sihiver.mqltv.data.AppSettings
import com.sihiver.mqltv.data.SettingsSection
import com.sihiver.mqltv.domain.model.RegisteredDevice
import com.sihiver.mqltv.domain.model.UserProfile
import com.sihiver.mqltv.domain.repository.SubscriptionStatus
import com.sihiver.mqltv.presentation.viewmodel.AboutInfo
import com.sihiver.mqltv.ui.components.SettingsTextField
import com.sihiver.mqltv.ui.components.SelectInput
import com.sihiver.mqltv.ui.components.SelectOption
import com.sihiver.mqltv.ui.components.SettingRow
import com.sihiver.mqltv.ui.components.SettingsActionButton
import com.sihiver.mqltv.ui.components.SettingsSectionTitle
import com.sihiver.mqltv.ui.components.Sidebar
import com.sihiver.mqltv.ui.components.TvFocusableBox
import com.sihiver.mqltv.ui.components.TvToggle
import com.sihiver.mqltv.ui.components.useClock
import com.sihiver.mqltv.ui.theme.AccentOrange
import com.sihiver.mqltv.ui.theme.TextDim
import com.sihiver.mqltv.ui.theme.TextMuted

@Composable
fun SettingsScreen(
    settings: AppSettings,
    profile: UserProfile? = null,
    subscription: SubscriptionStatus? = null,
    devices: List<RegisteredDevice> = emptyList(),
    channelCount: Int = 0,
    isOnline: Boolean = false,
    isBusy: Boolean = false,
    statusMessage: String? = null,
    about: AboutInfo = AboutInfo(),
    onSettingsChange: (AppSettings) -> Unit,
    onSaveDeviceName: (String) -> Unit = {},
    onRemoveDevice: (Int) -> Unit = {},
    onRefreshChannels: () -> Unit = {},
    onLogout: () -> Unit = {},
    onNavigate: (AppScreen) -> Unit,
) {
    val clock = useClock()
    var activeSection by rememberSaveable { mutableStateOf(SettingsSection.VIDEO.name) }
    val section = SettingsSection.valueOf(activeSection)

    fun update(transform: (AppSettings) -> AppSettings) {
        onSettingsChange(transform(settings))
    }

    Row(modifier = Modifier.fillMaxSize()) {
        Sidebar(
            currentScreen = AppScreen.SETTINGS,
            onNavigate = onNavigate,
            clock = clock,
            packageName = subscription?.packageName,
            channelCount = subscription?.channelCount,
        )

        Row(modifier = Modifier.weight(1f).fillMaxHeight()) {
            // Settings nav
            Column(
                modifier = Modifier
                    .width(220.dp)
                    .fillMaxHeight()
                    .background(Color(0x990A0E16))
                    .border(width = 1.dp, color = Color(0x0FFFFFFF))
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 22.dp),
            ) {
                Text(
                    text = "PENGATURAN",
                    fontSize = 11.sp,
                    color = AccentOrange,
                    letterSpacing = 2.sp,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 16.dp),
                )
                SettingsSection.entries.forEach { s ->
                    val active = section == s
                    TvFocusableBox(
                        onClick = { activeSection = s.name },
                        modifier = Modifier.fillMaxWidth(),
                        accentColor = AccentOrange,
                        shape = RoundedCornerShape(0.dp),
                        backgroundColor = if (active) AccentOrange.copy(alpha = 0.12f) else Color.Transparent,
                        focusedBackgroundColor = AccentOrange.copy(alpha = 0.22f),
                        unfocusedBorderWidth = 0.dp,
                        focusedScale = 1f,
                    ) { isFocused ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .then(
                                    if (active || isFocused) {
                                        Modifier.border(
                                            width = 3.dp,
                                            color = if (isFocused) AccentOrange else AccentOrange.copy(alpha = 0.5f),
                                        )
                                    } else Modifier,
                                )
                                .padding(horizontal = 20.dp, vertical = 13.dp),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = s.icon, fontSize = 17.sp)
                            Text(
                                text = s.label,
                                fontSize = 13.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active || isFocused) AccentOrange else Color(0xFFBBBBBB),
                            )
                        }
                    }
                }
            }

            // Content
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 40.dp, vertical = 28.dp),
            ) {
                when (section) {
                    SettingsSection.VIDEO -> VideoSettings(settings, ::update)
                    SettingsSection.AUDIO -> AudioSettings(settings, ::update)
                    SettingsSection.SUBTITLE -> SubtitleSettings(settings, ::update)
                    SettingsSection.NETWORK -> NetworkSettings(
                        settings = settings,
                        channelCount = channelCount,
                        isOnline = isOnline,
                        isBusy = isBusy,
                        update = ::update,
                        onRefreshChannels = onRefreshChannels,
                    )
                    SettingsSection.PARENTAL -> ParentalSettings(settings, ::update)
                    SettingsSection.ACCOUNT -> AccountSettings(
                        settings = settings,
                        profile = profile,
                        subscription = subscription,
                        devices = devices,
                        update = ::update,
                        onSaveDeviceName = onSaveDeviceName,
                        onRemoveDevice = onRemoveDevice,
                        onLogout = onLogout,
                    )
                    SettingsSection.APPEARANCE -> AppearanceSettings(settings, ::update)
                    SettingsSection.ABOUT -> AboutSettings(about = about)
                }
                if (statusMessage != null) {
                    Text(
                        text = statusMessage,
                        fontSize = 12.sp,
                        color = AccentOrange,
                        modifier = Modifier.padding(top = 16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun VideoSettings(settings: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsSectionTitle("🎬 Pengaturan Video")
    SettingRow("HDR", "Aktifkan High Dynamic Range jika TV mendukung") {
        TvToggle(settings.hdr) { v -> update { it.copy(hdr = v) } }
    }
    SettingRow("Dolby Vision", "Membutuhkan TV dan konten yang kompatibel") {
        TvToggle(settings.dolby) { v -> update { it.copy(dolby = v) } }
    }
    SettingRow("Rasio Aspek", "Cara video ditampilkan di layar") {
        SelectInput(settings.aspectRatio, aspectOptions) { v -> update { it.copy(aspectRatio = v) } }
    }
    SettingRow("Deinterlace", "Mengurangi efek sisir pada konten interlaced") {
        TvToggle(settings.deinterlace) { v -> update { it.copy(deinterlace = v) } }
    }
    SettingRow("Hardware Decoding", "Menggunakan GPU untuk decoding video") {
        TvToggle(settings.hardwareDecode) { v -> update { it.copy(hardwareDecode = v) } }
    }
    SettingRow("Buffer", "Ukuran buffer stream sebelum diputar") {
        SelectInput(settings.bufferSize, bufferOptions) { v -> update { it.copy(bufferSize = v) } }
    }
}

@Composable
private fun AudioSettings(settings: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsSectionTitle("🔊 Pengaturan Audio")
    SettingRow("Bahasa Audio Default", "Pilih bahasa audio yang diutamakan") {
        SelectInput(settings.audioTrack, audioTrackOptions) { v -> update { it.copy(audioTrack = v) } }
    }
    SettingRow("Output Audio", "Mode keluaran suara") {
        SelectInput(settings.audioOutput, audioOutputOptions) { v -> update { it.copy(audioOutput = v) } }
    }
    SettingRow("Dolby Atmos", "Aktifkan jika sistem audio mendukung") {
        TvToggle(settings.dolbyAtmos) { v -> update { it.copy(dolbyAtmos = v) } }
    }
    SettingRow("Normalisasi Volume", "Menjaga volume tetap konsisten antar channel") {
        TvToggle(settings.audioNorm) { v -> update { it.copy(audioNorm = v) } }
    }
    SettingRow("Tunda Audio", "Sesuaikan sinkronisasi audio dengan video") {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            TvFocusableBox(
                onClick = { update { it.copy(audioDelay = (it.audioDelay - 50).coerceIn(-500, 500)) } },
                accentColor = AccentOrange,
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color(0x14FFFFFF),
                unfocusedBorderWidth = 1.dp,
            ) { Text("−", fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(8.dp)) }
            Text(
                text = "${if (settings.audioDelay > 0) "+" else ""}${settings.audioDelay}ms",
                fontSize = 13.sp,
                color = AccentOrange,
                modifier = Modifier.width(60.dp),
            )
            TvFocusableBox(
                onClick = { update { it.copy(audioDelay = (it.audioDelay + 50).coerceIn(-500, 500)) } },
                accentColor = AccentOrange,
                shape = RoundedCornerShape(8.dp),
                backgroundColor = Color(0x14FFFFFF),
                unfocusedBorderWidth = 1.dp,
            ) { Text("+", fontSize = 16.sp, color = Color.White, modifier = Modifier.padding(8.dp)) }
        }
    }
}

@Composable
private fun SubtitleSettings(settings: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsSectionTitle("💬 Pengaturan Subtitle")
    SettingRow("Bahasa Subtitle Default", "Bahasa subtitle yang diprioritaskan") {
        SelectInput(settings.subtitleLang, subtitleLangOptions) { v -> update { it.copy(subtitleLang = v) } }
    }
    SettingRow("Ukuran Subtitle", "Ukuran teks subtitle yang ditampilkan") {
        SelectInput(settings.subtitleSize, subtitleSizeOptions) { v -> update { it.copy(subtitleSize = v) } }
    }
    SettingRow("Latar Belakang Subtitle", "Tambah kotak gelap di belakang teks") {
        TvToggle(settings.subtitleBg) { v -> update { it.copy(subtitleBg = v) } }
    }
    Box(
        modifier = Modifier
            .padding(top = 24.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .padding(16.dp),
    ) {
        Column {
            Text(text = "PRATINJAU SUBTITLE", fontSize = 12.sp, color = TextMuted, modifier = Modifier.padding(bottom = 8.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFF111111))
                    .padding(vertical = 40.dp, horizontal = 20.dp),
                contentAlignment = Alignment.Center,
            ) {
                val fontSize = when (settings.subtitleSize) {
                    "small" -> 13.sp
                    "large" -> 20.sp
                    "xlarge" -> 26.sp
                    else -> 16.sp
                }
                Text(
                    text = "Ini adalah contoh teks subtitle",
                    fontSize = fontSize,
                    color = Color.White,
                    modifier = Modifier
                        .then(
                            if (settings.subtitleBg) {
                                Modifier
                                    .background(Color(0xB3000000), RoundedCornerShape(4.dp))
                                    .padding(horizontal = 12.dp, vertical = 4.dp)
                            } else Modifier,
                        ),
                )
            }
        }
    }
}

@Composable
private fun NetworkSettings(
    settings: AppSettings,
    channelCount: Int,
    isOnline: Boolean,
    isBusy: Boolean,
    update: ((AppSettings) -> AppSettings) -> Unit,
    onRefreshChannels: () -> Unit,
) {
    SettingsSectionTitle("🌐 Sumber Playlist M3U")
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(AccentOrange.copy(alpha = 0.08f))
            .border(1.dp, AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
    ) {
        Column {
            Text(text = "STATUS KONEKSI", fontSize = 12.sp, color = AccentOrange, letterSpacing = 1.sp)
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(top = 4.dp),
            ) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .background(if (isOnline) Color(0xFF68D391) else Color(0xFFF6AD55)),
                )
                Text(
                    text = if (isOnline) "Terhubung • $channelCount channel dimuat" else "Offline • data cache lokal",
                    fontSize = 13.sp,
                    color = if (isOnline) Color(0xFF68D391) else Color(0xFFF6AD55),
                )
            }
        }
    }
    SettingRow("URL Playlist M3U", "Alamat URL playlist channel IPTV kamu") {
        SettingsTextField(
            value = settings.m3uUrl,
            onValueChange = { v -> update { it.copy(m3uUrl = v) } },
            modifier = Modifier.width(280.dp),
        )
    }
    SettingRow("URL EPG (XMLTV)", "Sumber data jadwal program elektronik") {
        SettingsTextField(
            value = settings.epgUrl,
            onValueChange = { v -> update { it.copy(epgUrl = v) } },
            modifier = Modifier.width(280.dp),
        )
    }
    SettingRow("Auto Refresh Playlist", "Perbarui daftar channel secara otomatis") {
        TvToggle(settings.autoRefresh) { v -> update { it.copy(autoRefresh = v) } }
    }
    if (settings.autoRefresh) {
        SettingRow("Interval Refresh", "Seberapa sering playlist diperbarui") {
            SelectInput(settings.refreshInterval, refreshOptions) { v -> update { it.copy(refreshInterval = v) } }
        }
    }
    SettingRow("User Agent", "Identitas client saat mengakses stream") {
        SelectInput(settings.userAgent, userAgentOptions) { v -> update { it.copy(userAgent = v) } }
    }
    if (settings.userAgent == "custom") {
        SettingRow("User Agent Kustom", "String User-Agent untuk request stream") {
            SettingsTextField(
                value = settings.customUserAgent,
                onValueChange = { v -> update { it.copy(customUserAgent = v) } },
                modifier = Modifier.width(280.dp),
            )
        }
    }
    Row(modifier = Modifier.padding(top = 20.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        SettingsActionButton(
            text = if (isBusy) "Memperbarui…" else "🔄 Refresh Sekarang",
            onClick = onRefreshChannels,
            primary = true,
        )
    }
}

@Composable
private fun ParentalSettings(settings: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    var pinInput by rememberSaveable { mutableStateOf("") }
    SettingsSectionTitle("🔒 Kontrol Orang Tua")
    SettingRow("Aktifkan Kontrol Orang Tua", "Batasi akses konten berdasarkan rating usia") {
        TvToggle(settings.parentalLock) { v -> update { it.copy(parentalLock = v) } }
    }
    if (settings.parentalLock) {
        SettingRow("PIN Kontrol Orang Tua", "PIN 4 digit untuk membuka kunci") {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SettingsTextField(
                    value = pinInput,
                    onValueChange = { v ->
                        pinInput = v.filter { it.isDigit() }.take(4)
                    },
                    modifier = Modifier.width(120.dp),
                )
                SettingsActionButton(
                    text = if (settings.pinSet) "✓ Perbarui PIN" else "Simpan PIN",
                    onClick = {
                        if (pinInput.length == 4) {
                            update { it.copy(parentalPin = pinInput, pinSet = true) }
                            pinInput = ""
                        }
                    },
                    primary = !settings.pinSet,
                )
            }
        }
        SettingRow("Batasan Rating", "Konten di atas rating ini akan diblokir") {
            SelectInput(settings.rating, ratingOptions) { v -> update { it.copy(rating = v) } }
        }
    }
    Box(
        modifier = Modifier
            .padding(top = 20.dp)
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0x0AFFFFFF))
            .padding(18.dp),
    ) {
        Text(
            text = "Kontrol orang tua memerlukan PIN untuk mengakses channel dan konten yang melebihi batas rating yang ditentukan.",
            fontSize = 11.sp,
            color = TextMuted,
            lineHeight = 18.sp,
        )
    }
}

@Composable
private fun AccountSettings(
    settings: AppSettings,
    profile: UserProfile?,
    subscription: SubscriptionStatus?,
    devices: List<RegisteredDevice>,
    update: ((AppSettings) -> AppSettings) -> Unit,
    onSaveDeviceName: (String) -> Unit,
    onRemoveDevice: (Int) -> Unit,
    onLogout: () -> Unit,
) {
    var deviceNameDraft by rememberSaveable(settings.deviceName) { mutableStateOf(settings.deviceName) }
    val maxDevices = subscription?.maxDevices ?: settings.maxDevices
    val daysTotal = (subscription?.daysRemaining ?: 0).coerceAtLeast(1)
    val progress = ((subscription?.daysRemaining ?: 0).toFloat() / daysTotal.coerceAtLeast(30)).coerceIn(0f, 1f)

    SettingsSectionTitle("👤 Akun & Perangkat")
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 24.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(AccentOrange.copy(alpha = 0.08f))
            .border(1.dp, AccentOrange.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(22.dp),
        horizontalArrangement = Arrangement.spacedBy(18.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(60.dp)
                .clip(RoundedCornerShape(30.dp))
                .background(Brush.linearGradient(listOf(AccentOrange, Color(0xFFF7C59F)))),
            contentAlignment = Alignment.Center,
        ) {
            Text(text = "👤", fontSize = 26.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = profile?.name ?: "Belum login",
                fontSize = 18.sp,
                fontWeight = FontWeight.ExtraBold,
                color = Color.White,
            )
            Text(
                text = profile?.email ?: "Masuk untuk sinkron channel",
                fontSize = 12.sp,
                color = TextMuted,
            )
            Box(
                modifier = Modifier
                    .padding(top = 6.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(AccentOrange.copy(alpha = 0.2f))
                    .padding(horizontal = 12.dp, vertical = 3.dp),
            ) {
                Text(
                    text = "✨ ${subscription?.plan ?: profile?.plan ?: "—"}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentOrange,
                )
            }
        }
        Column(horizontalAlignment = Alignment.End) {
            Text(text = "Berlaku hingga", fontSize = 12.sp, color = TextMuted)
            Text(
                text = subscription?.expiresAt ?: profile?.expiresAt ?: "—",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(top = 2.dp),
            )
            Box(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .width(100.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(Color(0x1AFFFFFF)),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(2.dp))
                        .background(AccentOrange),
                )
            }
            Text(
                text = "${subscription?.daysRemaining ?: profile?.daysRemaining ?: 0} hari tersisa",
                fontSize = 10.sp,
                color = TextMuted,
                modifier = Modifier.padding(top = 3.dp),
            )
        }
    }

    SettingRow("Nama Perangkat", "Nama yang muncul di daftar perangkat aktif") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            SettingsTextField(
                value = deviceNameDraft,
                onValueChange = { deviceNameDraft = it.take(100) },
                modifier = Modifier.width(240.dp),
            )
            SettingsActionButton(
                text = "Simpan Nama",
                onClick = {
                    update { it.copy(deviceName = deviceNameDraft) }
                    onSaveDeviceName(deviceNameDraft)
                },
                primary = true,
            )
        }
    }
    Text(
        text = "PERANGKAT AKTIF (${devices.size} / $maxDevices)",
        fontSize = 12.sp,
        color = TextMuted,
        letterSpacing = 1.sp,
        modifier = Modifier.padding(top = 20.dp, bottom = 12.dp),
    )
    if (devices.isEmpty()) {
        Text(text = "Belum ada perangkat terdaftar", fontSize = 12.sp, color = TextMuted)
    } else {
        devices.forEach { device ->
            DeviceRow(
                name = device.name,
                type = device.type,
                lastSeen = device.lastSeenLabel,
                current = device.isCurrent,
                onRemove = if (!device.isCurrent) ({ onRemoveDevice(device.id) }) else null,
            )
            Spacer(modifier = Modifier.height(8.dp))
        }
    }
    Spacer(modifier = Modifier.height(16.dp))
    SettingsActionButton("🚪 Keluar dari Akun", onClick = onLogout, danger = true)
}

@Composable
private fun DeviceRow(
    name: String,
    type: String,
    lastSeen: String,
    current: Boolean,
    onRemove: (() -> Unit)?,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(if (current) AccentOrange.copy(alpha = 0.08f) else Color(0x0AFFFFFF))
            .border(
                1.dp,
                if (current) AccentOrange.copy(alpha = 0.25f) else Color(0x0FFFFFFF),
                RoundedCornerShape(12.dp),
            )
            .padding(horizontal = 18.dp, vertical = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = if (current) "📺" else "📱", fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text(text = "$type • $lastSeen", fontSize = 11.sp, color = TextMuted)
        }
        if (current) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(Color(0xFF68D391))
                    .padding(horizontal = 8.dp, vertical = 2.dp),
            ) {
                Text(text = "PERANGKAT INI", fontSize = 10.sp, color = Color(0xFF0D4D2A))
            }
        } else if (onRemove != null) {
            SettingsActionButton("Keluarkan", onClick = onRemove, danger = true)
        }
    }
}

@Composable
private fun AppearanceSettings(settings: AppSettings, update: ((AppSettings) -> AppSettings) -> Unit) {
    SettingsSectionTitle("🎨 Tampilan & Bahasa")
    SettingRow("Bahasa Antarmuka", "Bahasa tampilan aplikasi") {
        SelectInput(settings.language, languageOptions) { v -> update { it.copy(language = v) } }
    }
    SettingRow("Format Jam", "Tampilan waktu di aplikasi") {
        SelectInput(settings.clockFormat, clockFormatOptions) { v -> update { it.copy(clockFormat = v) } }
    }
    SettingRow("Putar Otomatis", "Langsung memutar saat channel dipilih") {
        TvToggle(settings.autoplay) { v -> update { it.copy(autoplay = v) } }
    }
    SettingRow("Ingat Posisi Terakhir", "Lanjutkan dari posisi terakhir saat buka ulang") {
        TvToggle(settings.rememberPosition) { v -> update { it.copy(rememberPosition = v) } }
    }
}

@Composable
private fun AboutSettings(about: AboutInfo) {
    SettingsSectionTitle("ℹ️ Tentang Aplikasi")
    listOf(
        "Nama Aplikasi" to about.appName,
        "Versi" to "${about.version} (build ${about.versionCode})",
        "Platform" to about.platform,
        "ExoPlayer" to about.exoPlayerVersion,
    ).forEach { (label, value) ->
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 14.dp)
                .border(width = 1.dp, color = Color(0x0FFFFFFF)),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, fontSize = 14.sp, color = Color(0xFFAAAAAA))
            Text(text = value, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
        }
    }
    Row(modifier = Modifier.padding(top = 28.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        SettingsActionButton("📋 Lisensi Open Source", onClick = { /* TODO: buka layar lisensi */ })
    }
}

private val aspectOptions = listOf(
    SelectOption("16:9", "16:9 Widescreen"),
    SelectOption("4:3", "4:3 Standard"),
    SelectOption("fill", "Penuhi Layar"),
    SelectOption("fit", "Sesuaikan Layar"),
)
private val bufferOptions = listOf(
    SelectOption("small", "Kecil (2 detik)"),
    SelectOption("medium", "Sedang (5 detik)"),
    SelectOption("large", "Besar (10 detik)"),
)
private val audioTrackOptions = listOf(
    SelectOption("id", "Indonesia"),
    SelectOption("en", "English"),
    SelectOption("auto", "Auto (dari stream)"),
)
private val audioOutputOptions = listOf(
    SelectOption("stereo", "Stereo"),
    SelectOption("surround", "5.1 Surround"),
    SelectOption("passthrough", "Passthrough"),
)
private val subtitleLangOptions = listOf(
    SelectOption("none", "Tidak Ada"),
    SelectOption("id", "Indonesia"),
    SelectOption("en", "English"),
    SelectOption("auto", "Auto"),
)
private val subtitleSizeOptions = listOf(
    SelectOption("small", "Kecil"),
    SelectOption("medium", "Sedang"),
    SelectOption("large", "Besar"),
    SelectOption("xlarge", "Sangat Besar"),
)
private val refreshOptions = listOf(
    SelectOption("1h", "Setiap 1 jam"),
    SelectOption("6h", "Setiap 6 jam"),
    SelectOption("12h", "Setiap 12 jam"),
    SelectOption("24h", "Setiap 24 jam"),
)
private val userAgentOptions = listOf(
    SelectOption("default", "Default (ExoPlayer)"),
    SelectOption("vlc", "VLC Media Player"),
    SelectOption("custom", "Custom..."),
)
private val ratingOptions = listOf(
    SelectOption("all", "Semua Usia (G)"),
    SelectOption("7+", "7 Tahun ke atas"),
    SelectOption("13+", "13 Tahun ke atas"),
    SelectOption("17+", "17 Tahun ke atas (D)"),
    SelectOption("18+", "18 Tahun ke atas (BO)"),
)
private val languageOptions = listOf(
    SelectOption("id", "Bahasa Indonesia"),
    SelectOption("en", "English"),
)
private val clockFormatOptions = listOf(
    SelectOption("24h", "24 Jam (14:30)"),
    SelectOption("12h", "12 Jam (2:30 PM)"),
)
