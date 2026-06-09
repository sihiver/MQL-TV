package com.sihiver.mqltv.tv

import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import androidx.tvprovider.media.tv.Channel as TvChannel
import androidx.tvprovider.media.tv.ChannelLogoUtils
import androidx.tvprovider.media.tv.PreviewProgram
import androidx.tvprovider.media.tv.TvContractCompat
import com.sihiver.mqltv.R
import com.sihiver.mqltv.data.datastore.RecentChannelEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Mengelola saluran "Terakhir Ditonton" di layar utama Android TV (TvProvider API).
 * Hanya aktif pada API 26 (Android Oreo) ke atas.
 */
@Singleton
class TvHomeChannelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val PREFS_NAME = "tv_home_channel"
        private const val KEY_CHANNEL_ID = "home_channel_id"
        private const val CHANNEL_DISPLAY_NAME = "Terakhir Ditonton"
        private const val DEEP_LINK_SCHEME = "mqltv"
        private const val DEEP_LINK_HOST = "channel"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /** Buat deep link URI yang membuka app dan langsung memutar channel. */
    fun buildChannelDeepLink(channelId: Int): Uri =
        Uri.parse("$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$channelId")

    /**
     * Buat atau perbarui saluran "Terakhir Ditonton" di launcher Android TV.
     * Aman dipanggil berulang kali; operasi berjalan di [Dispatchers.IO].
     */
    suspend fun updateLauncherChannel(channels: List<RecentChannelEntry>) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        if (channels.isEmpty()) return

        withContext(Dispatchers.IO) {
            try {
                val channelId = ensureChannelExists()
                if (channelId < 0) return@withContext
                refreshPrograms(channelId, channels)
            } catch (e: SecurityException) {
                // Izin WRITE_EPG_DATA belum diberikan; abaikan saja
            } catch (_: Exception) {
            }
        }
    }

    /** Buat saluran default jika belum ada, lalu kembalikan ID-nya. */
    @Suppress("QueryPermissionsNeeded")
    private fun ensureChannelExists(): Long {
        val saved = prefs.getLong(KEY_CHANNEL_ID, -1L)
        if (saved > 0 && channelStillExists(saved)) return saved

        val appLinkUri = Uri.parse(
            "android-app://${context.packageName}/mqltv/home",
        )
        val builder = TvChannel.Builder()
            .setType(TvContractCompat.Channels.TYPE_PREVIEW)
            .setDisplayName(CHANNEL_DISPLAY_NAME)
            .setAppLinkIntentUri(appLinkUri)
            .setInternalProviderId("recent_watched")

        val uri = context.contentResolver.insert(
            TvContractCompat.Channels.CONTENT_URI,
            builder.build().toContentValues(),
        ) ?: return -1L

        val newId = ContentUris.parseId(uri)
        prefs.edit().putLong(KEY_CHANNEL_ID, newId).apply()

        // Simpan logo app sebagai logo saluran
        val logoUri = Uri.parse(
            "android.resource://${context.packageName}/${R.mipmap.ic_launcher}",
        )
        runCatching {
            ChannelLogoUtils.storeChannelLogo(context, newId, logoUri)
        }

        // Jadikan saluran default agar langsung muncul di launcher tanpa perlu persetujuan
        TvContractCompat.requestChannelBrowsable(context, newId)

        return newId
    }

    private fun channelStillExists(channelId: Long): Boolean = runCatching {
        context.contentResolver
            .query(TvContractCompat.buildChannelUri(channelId), null, null, null, null)
            ?.use { it.moveToFirst() } == true
    }.getOrDefault(false)

    /** Hapus semua program lama lalu masukkan program baru. */
    private fun refreshPrograms(channelId: Long, channels: List<RecentChannelEntry>) {
        deleteAllPrograms(channelId)
        channels.forEachIndexed { index, entry ->
            val intentUri = buildChannelDeepLink(entry.id)
            val posterUri = entry.logo.toUriOrNull()

            val builder = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                .setTitle(entry.name)
                .setDescription(entry.category)
                .setWeight(channels.size - index)
                .setIntentUri(intentUri)

            if (posterUri != null) builder.setPosterArtUri(posterUri)

            runCatching {
                context.contentResolver.insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    builder.build().toContentValues(),
                )
            }
        }
    }

    private fun deleteAllPrograms(channelId: Long) {
        runCatching {
            val cursor = context.contentResolver.query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                arrayOf(TvContractCompat.BaseTvColumns._ID),
                null, null, null,
            )
            cursor?.use {
                while (it.moveToNext()) {
                    val progId = it.getLong(0)
                    context.contentResolver.delete(
                        TvContractCompat.buildPreviewProgramUri(progId), null, null,
                    )
                }
            }
        }
    }

    private fun String.toUriOrNull(): Uri? = if (isNotBlank()) runCatching { Uri.parse(this) }.getOrNull() else null
}
