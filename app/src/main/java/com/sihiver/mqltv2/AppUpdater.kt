package com.sihiver.mqltv2

import android.app.Activity
import android.app.AlertDialog
import android.app.DownloadManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.FileProvider
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.File
import java.io.IOException

object AppUpdater {
    private const val TAG = "AppUpdater"
    private var downloadId: Long = -1

    private const val PREFS_NAME = "AppUpdaterPrefs"
    private const val KEY_LAST_CHECKED = "last_checked_time"
    private const val COOLDOWN_MS = 6 * 60 * 60 * 1000L // 6 hours

    fun checkForUpdates(activity: Activity) {
        val prefs = activity.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val lastChecked = prefs.getLong(KEY_LAST_CHECKED, 0)

        val isDebug = (activity.applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE) != 0

        if (System.currentTimeMillis() - lastChecked < COOLDOWN_MS && !isDebug) {
            return
        }

        var baseUrl = BuildConfig.API_BASE_URL
        if (!baseUrl.endsWith("/")) baseUrl += "/"
        val url = "${baseUrl}api/app-updates/latest?appId=${BuildConfig.APPLICATION_ID}"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Gagal mengecek update: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) return

                prefs.edit().putLong(KEY_LAST_CHECKED, System.currentTimeMillis()).apply()

                try {
                    val json = response.body?.string() ?: return
                    val obj = JSONObject(json)

                    val latestVersionCode = obj.optInt("versionCode", 0)
                    val latestVersionName = obj.optString("versionName", "")
                    val releaseNotes = obj.optString("releaseNotes", "")
                    var apkUrl = obj.optString("apkUrl", "")

                    if (apkUrl.contains("localhost")) {
                        apkUrl = apkUrl.substring(apkUrl.indexOf("/public/"))
                    }
                    if (apkUrl.startsWith("/")) {
                        var bUrl = BuildConfig.API_BASE_URL
                        if (bUrl.endsWith("/")) bUrl = bUrl.substring(0, bUrl.length - 1)
                        apkUrl = bUrl + apkUrl
                    }

                    val isForceUpdate = obj.optBoolean("isForceUpdate", false)

                    var currentVersionCode = 0
                    try {
                        val pInfo = activity.packageManager.getPackageInfo(activity.packageName, 0)
                        currentVersionCode = pInfo.versionCode
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (latestVersionCode > currentVersionCode && apkUrl.isNotEmpty()) {
                        Handler(Looper.getMainLooper()).post {
                            showUpdateDialog(activity, latestVersionName, releaseNotes, apkUrl, isForceUpdate)
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error parsing update json: ${e.message}")
                }
            }
        })
    }

    private fun showUpdateDialog(
        activity: Activity,
        versionName: String,
        notes: String,
        apkUrl: String,
        isForceUpdate: Boolean
    ) {
        if (activity.isFinishing || activity.isDestroyed) return

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * activity.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF1a1a2e.toInt()) // Dark theme
        }

        val tvTitle = TextView(activity).apply {
            text = "Versi Baru: $versionName"
            textSize = 20f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            layout.addView(this)
        }

        val tvNotes = TextView(activity).apply {
            text = if (notes.isEmpty()) "Ada pembaruan baru yang tersedia." else notes
            textSize = 14f
            setTextColor(0xFFCCCCCC.toInt())
            val pad = (10 * activity.resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, pad * 2)
            layout.addView(this)
        }

        val pbProgress = ProgressBar(activity, null, android.R.attr.progressBarStyleHorizontal).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            visibility = View.GONE
            layout.addView(this)
        }

        val btnContainer = LinearLayout(activity).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = android.view.Gravity.END
            val pad = (10 * activity.resources.displayMetrics.density).toInt()
            setPadding(0, pad, 0, 0)
            layout.addView(this)
        }

        val btnLater = Button(activity).apply {
            text = "Nanti Saja"
            setBackgroundColor(0xFF333344.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            if (isForceUpdate) visibility = View.GONE
            btnContainer.addView(this)
        }

        val space = View(activity).apply {
            layoutParams = LinearLayout.LayoutParams((10 * activity.resources.displayMetrics.density).toInt(), 1)
            btnContainer.addView(this)
        }

        val btnNow = Button(activity).apply {
            text = "Unduh & Instal"
            setBackgroundColor(0xFFff6b35.toInt()) // Orange theme
            setTextColor(0xFFFFFFFF.toInt())
            btnContainer.addView(this)
        }

        val builder = AlertDialog.Builder(activity)
            .setView(layout)
            .setCancelable(!isForceUpdate)

        val dialog = builder.create()
        dialog.setCanceledOnTouchOutside(!isForceUpdate)

        if (!isForceUpdate) {
            btnLater.setOnClickListener { dialog.dismiss() }
        }

        btnNow.setOnClickListener {
            val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
            if (file.exists() && file.length() > 0) {
                installApk(activity)
            } else {
                btnNow.isEnabled = false
                btnLater.isEnabled = false
                pbProgress.visibility = View.VISIBLE
                startDownload(activity, apkUrl, pbProgress, dialog, btnNow, btnLater)
            }
        }

        dialog.show()
        btnNow.requestFocus()
    }

    private fun startDownload(
        activity: Activity,
        apkUrl: String,
        pbProgress: ProgressBar,
        dialog: AlertDialog,
        btnNow: Button,
        btnLater: Button
    ) {
        val request = DownloadManager.Request(Uri.parse(apkUrl)).apply {
            setTitle("Mengunduh Pembaruan MQLTV2")
            setDescription("Sedang mengunduh versi terbaru...")
            setDestinationInExternalFilesDir(activity, Environment.DIRECTORY_DOWNLOADS, "update.apk")
        }

        val dm = activity.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager?
        if (dm == null) {
            Toast.makeText(activity, "DownloadManager tidak tersedia.", Toast.LENGTH_SHORT).show()
            return
        }

        val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")
        if (file.exists()) file.delete()

        downloadId = dm.enqueue(request)

        Thread {
            var downloading = true
            while (downloading) {
                val q = DownloadManager.Query().setFilterById(downloadId)
                val cursor = dm.query(q)
                if (cursor != null && cursor.moveToFirst()) {
                    val statusIndex = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    val downloadedIndex = cursor.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                    val totalIndex = cursor.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                    if (statusIndex >= 0 && downloadedIndex >= 0 && totalIndex >= 0) {
                        val status = cursor.getInt(statusIndex)
                        val bytesDownloaded = cursor.getInt(downloadedIndex)
                        val bytesTotal = cursor.getInt(totalIndex)

                        if (status == DownloadManager.STATUS_SUCCESSFUL) {
                            downloading = false
                            Handler(Looper.getMainLooper()).post {
                                pbProgress.progress = 100
                                btnNow.text = "Instal Sekarang"
                                btnNow.isEnabled = true
                                btnLater.isEnabled = true
                                installApk(activity)
                            }
                        } else if (status == DownloadManager.STATUS_FAILED) {
                            downloading = false
                            Handler(Looper.getMainLooper()).post {
                                Toast.makeText(activity, "Gagal mengunduh pembaruan.", Toast.LENGTH_SHORT).show()
                                btnNow.isEnabled = true
                                btnLater.isEnabled = true
                                pbProgress.visibility = View.GONE
                            }
                        } else {
                            if (bytesTotal > 0) {
                                val progress = ((bytesDownloaded * 100L) / bytesTotal).toInt()
                                Handler(Looper.getMainLooper()).post { pbProgress.progress = progress }
                            }
                        }
                    }
                    cursor.close()
                }
                try {
                    Thread.sleep(500)
                } catch (e: InterruptedException) {
                }
            }
        }.start()
    }

    private fun installApk(activity: Activity) {
        try {
            val intent = Intent(Intent.ACTION_VIEW)
            val file = File(activity.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), "update.apk")

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                val contentUri = FileProvider.getUriForFile(activity, "${activity.packageName}.fileprovider", file)
                intent.setDataAndType(contentUri, "application/vnd.android.package-archive")
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            } else {
                intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive")
            }

            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            activity.startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "Gagal membuka instalasi: ${e.message}")
            Toast.makeText(activity, "Gagal membuka instalasi pembaruan. Silakan buka file update.apk di folder Download.", Toast.LENGTH_LONG).show()
            showPermissionDialog(activity)
        }
    }

    private fun showPermissionDialog(activity: Activity) {
        if (activity.isFinishing || activity.isDestroyed) return

        val layout = LinearLayout(activity).apply {
            orientation = LinearLayout.VERTICAL
            val pad = (20 * activity.resources.displayMetrics.density).toInt()
            setPadding(pad, pad, pad, pad)
            setBackgroundColor(0xFF1a1a2e.toInt())
        }

        TextView(activity).apply {
            text = "Peringatan Keamanan"
            textSize = 18f
            setTypeface(null, android.graphics.Typeface.BOLD)
            setTextColor(0xFFFFFFFF.toInt())
            layout.addView(this)
        }

        TextView(activity).apply {
            text = "Demi keamanan, TV Anda saat ini tidak diizinkan menginstal aplikasi yang tidak dikenal dari sumber ini."
            textSize = 14f
            setTextColor(0xFFCCCCCC.toInt())
            val pad = (20 * activity.resources.displayMetrics.density).toInt()
            setPadding(0, pad / 2, 0, pad)
            layout.addView(this)
        }

        val btnSettings = Button(activity).apply {
            text = "Buka Setelan"
            setBackgroundColor(0xFFff6b35.toInt())
            setTextColor(0xFFFFFFFF.toInt())
            layout.addView(this)
        }

        val builder = AlertDialog.Builder(activity)
        val dialog = builder.setView(layout).setCancelable(true).create()

        btnSettings.setOnClickListener {
            dialog.dismiss()
            openUnknownSourcesSettings(activity)
        }

        dialog.show()
        btnSettings.requestFocus()
    }

    private fun openUnknownSourcesSettings(activity: Activity) {
        var success = false
        val flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        val intents = mutableListOf<Intent>()

        val tvApps = Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS)
        tvApps.setPackage("com.android.tv.settings")
        intents.add(tvApps)

        intents.add(Intent(android.provider.Settings.ACTION_APPLICATION_SETTINGS))
        intents.add(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
        intents.add(Intent(android.provider.Settings.ACTION_SETTINGS))

        for (i in intents) {
            try {
                i.flags = flags
                activity.startActivity(i)
                success = true
                break
            } catch (e: Exception) {
                Log.e(TAG, "Fallback gagal: ${e.message}")
            }
        }

        if (!success) {
            Toast.makeText(activity, "Perangkat menolak membuka setelan. Silakan buka manual.", Toast.LENGTH_LONG).show()
        }
    }
}
