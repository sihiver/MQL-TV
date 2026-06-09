package com.sihiver.mqltv.tv

import android.annotation.SuppressLint
import android.app.Activity
import android.content.ContentUris
import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Build
import android.util.Log
import androidx.core.content.edit
import androidx.core.net.toUri
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
 */
@Singleton
class TvHomeChannelManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "TvHomeChannel"
        private const val PREFS_NAME = "tv_home_channel"
        private const val KEY_CHANNEL_ID = "home_channel_id"
        private const val CHANNEL_DISPLAY_NAME = "Terakhir Ditonton"
        private const val DEEP_LINK_SCHEME = "mqltv"
        private const val DEEP_LINK_HOST = "channel"
    }

    private val prefs: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun buildChannelDeepLink(channelId: Int): Uri =
        "$DEEP_LINK_SCHEME://$DEEP_LINK_HOST/$channelId".toUri()

    suspend fun syncLauncherChannel(
        channels: List<RecentChannelEntry>,
        activity: Activity? = null,
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return

        withContext(Dispatchers.IO) {
            try {
                val channelId = ensureChannelExists()
                if (channelId < 0) {
                    Log.w(TAG, "Gagal membuat saluran launcher")
                    return@withContext
                }
                if (channels.isNotEmpty()) {
                    refreshPrograms(channelId, channels)
                }
                if (activity != null && !isChannelBrowsable(channelId)) {
                    withContext(Dispatchers.Main) {
                        requestChannelBrowsable(activity, channelId)
                    }
                }
            } catch (e: SecurityException) {
                Log.w(TAG, "Izin WRITE_EPG_DATA ditolak", e)
            } catch (e: Exception) {
                Log.w(TAG, "Gagal sinkron launcher", e)
            }
        }
    }

    suspend fun updateLauncherChannel(channels: List<RecentChannelEntry>) {
        syncLauncherChannel(channels, activity = null)
    }

    private fun requestChannelBrowsable(activity: Activity, channelId: Long) {
        runCatching {
            TvContractCompat.requestChannelBrowsable(activity, channelId)
        }.onFailure { e ->
            Log.w(TAG, "requestChannelBrowsable gagal", e)
        }
    }

    @Suppress("QueryPermissionsNeeded")
    private fun ensureChannelExists(): Long {
        val saved = prefs.getLong(KEY_CHANNEL_ID, -1L)
        if (saved > 0 && channelStillExists(saved)) {
            updateChannelLogo(saved)
            return saved
        }

        val appLinkUri = "android-app://${context.packageName}/mqltv/home".toUri()
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
        prefs.edit { putLong(KEY_CHANNEL_ID, newId) }
        updateChannelLogo(newId)
        return newId
    }

    private fun updateChannelLogo(channelId: Long) {
        runCatching {
            val bitmap = TvLauncherImageCache.createAppChannelLogoBitmap(context)
            ChannelLogoUtils.storeChannelLogo(context, channelId, bitmap)
            bitmap.recycle()
        }.onFailure { e ->
            Log.w(TAG, "Gagal simpan logo saluran", e)
        }
    }

    private fun isChannelBrowsable(channelId: Long): Boolean = runCatching {
        context.contentResolver
            .query(TvContractCompat.buildChannelUri(channelId), null, null, null, null)
            ?.use { cursor ->
                cursor.moveToFirst() && TvChannel.fromCursor(cursor).isBrowsable
            } == true
    }.getOrDefault(false)

    private fun channelStillExists(channelId: Long): Boolean = runCatching {
        context.contentResolver
            .query(TvContractCompat.buildChannelUri(channelId), null, null, null, null)
            ?.use { it.moveToFirst() } == true
    }.getOrDefault(false)

    @SuppressLint("RestrictedApi")
    private fun refreshPrograms(channelId: Long, channels: List<RecentChannelEntry>) {
        deleteAllPrograms(channelId)
        channels.forEachIndexed { index, entry ->
            val intentUri = buildChannelDeepLink(entry.id)
            val posterUri = resolvePosterUri(entry)

            val builder = PreviewProgram.Builder()
                .setChannelId(channelId)
                .setType(TvContractCompat.PreviewPrograms.TYPE_CLIP)
                .setTitle(entry.name)
                .setDescription(entry.category.ifBlank { "Live TV" })
                .setWeight(channels.size - index)
                .setIntentUri(intentUri)
                .setInternalProviderId("recent_${entry.id}")
                .setPosterArtUri(posterUri)
                .setPosterArtAspectRatio(TvContractCompat.PreviewPrograms.ASPECT_RATIO_16_9)

            runCatching {
                context.contentResolver.insert(
                    TvContractCompat.PreviewPrograms.CONTENT_URI,
                    builder.build().toContentValues(),
                )
            }.onFailure { e ->
                Log.w(TAG, "Gagal insert program ${entry.name}", e)
            }
        }
        Log.d(TAG, "Program launcher diperbarui: ${channels.size} channel")
    }

    /**
     * Poster kartu: URL logo langsung (seperti commit dd5b2d2) — launcher TV fetch sendiri.
     * Fallback ke drawable ic_channel jika logo bukan URL.
     */
    private fun resolvePosterUri(entry: RecentChannelEntry): Uri {
        val logo = entry.logo.trim()
        if (logo.startsWith("http://", ignoreCase = true) ||
            logo.startsWith("https://", ignoreCase = true)
        ) {
            return logo.toUri()
        }
        return "android.resource://${context.packageName}/${R.drawable.ic_channel_logo}".toUri()
    }

    private fun deleteAllPrograms(channelId: Long) {
        runCatching {
            context.contentResolver.query(
                TvContractCompat.buildPreviewProgramsUriForChannel(channelId),
                arrayOf(TvContractCompat.BaseTvColumns._ID),
                null, null, null,
            )?.use { cursor ->
                while (cursor.moveToNext()) {
                    val progId = cursor.getLong(0)
                    context.contentResolver.delete(
                        TvContractCompat.buildPreviewProgramUri(progId), null, null,
                    )
                }
            }
        }
    }
}
