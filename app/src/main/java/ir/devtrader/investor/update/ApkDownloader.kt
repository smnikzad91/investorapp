package ir.devtrader.investor.update

import android.Manifest
import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import ir.devtrader.investor.R
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File

/**
 * Downloads the release APK, primarily via the system DownloadManager (survives the app being
 * backgrounded/killed mid-download, shows progress in the notification shade, handles retries on
 * its own) and hands the finished file to the package installer through a FileProvider content://
 * URI — a raw file:// URI throws FileUriExposedException on API 24+.
 *
 * Some OEM ROMs (MIUI in particular, also seen on other Chinese Android skins) restrict or
 * outright disable the system Download Manager component for third-party apps, so
 * DownloadManager.enqueue() either throws or silently produces a failed download regardless of
 * how the destination is specified. When that happens this falls back to a direct download via
 * the app's own OkHttp client instead of giving up — this loses DownloadManager's
 * survive-backgrounding/system-progress-notification benefits for that one download, but actually
 * completes it on devices where DownloadManager itself is the thing that's broken.
 */
class ApkDownloader(private val context: Context) {

    private val authority = "${context.packageName}.fileprovider"
    private val httpClient = OkHttpClient()
    private val mainHandler = Handler(Looper.getMainLooper())
    private var enqueuedDownloadId: Long = -1L

    fun download(
        downloadUrl: String,
        versionName: String,
        onComplete: (apkFile: File) -> Unit,
        onFailed: () -> Unit,
    ) {
        val fileName = "tradebot-update-$versionName.apk"
        val destination = File(context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS), fileName)
        if (destination.exists()) destination.delete() // stale partial/previous download

        val request = DownloadManager.Request(Uri.parse(downloadUrl))
            .setTitle("tradeBot update")
            .setDescription("Downloading version $versionName")
            .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
            // setDestinationUri(Uri.fromFile(...)) into an app-specific external-files dir fails
            // on API 29+ (scoped storage) — the download provider runs as a different process/uid
            // and a raw file:// URI there gets rejected. setDestinationInExternalFilesDir grants
            // the provider the right access instead.
            .setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, fileName)
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        enqueuedDownloadId = try {
            downloadManager.enqueue(request)
        } catch (e: Exception) {
            downloadViaHttpFallback(downloadUrl, destination, onComplete, onFailed)
            return
        }

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != enqueuedDownloadId) return
                context.unregisterReceiver(this)

                val query = DownloadManager.Query().setFilterById(enqueuedDownloadId)
                val succeeded = downloadManager.query(query).use { cursor ->
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    cursor.moveToFirst() && statusIdx >= 0 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL
                }
                if (succeeded) {
                    onComplete(destination)
                } else {
                    downloadViaHttpFallback(downloadUrl, destination, onComplete, onFailed)
                }
            }
        }

        val filter = IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED)
        } else {
            context.registerReceiver(receiver, filter)
        }
    }

    /** Blocking download on a background thread, used only when DownloadManager itself fails. */
    private fun downloadViaHttpFallback(
        downloadUrl: String,
        destination: File,
        onComplete: (apkFile: File) -> Unit,
        onFailed: () -> Unit,
    ) {
        Thread {
            val succeeded = try {
                if (destination.exists()) destination.delete()
                val httpRequest = Request.Builder().url(downloadUrl).build()
                httpClient.newCall(httpRequest).execute().use { response ->
                    val body = response.body
                    if (response.isSuccessful && body != null) {
                        destination.parentFile?.mkdirs()
                        body.byteStream().use { input ->
                            destination.outputStream().use { output -> input.copyTo(output) }
                        }
                        true
                    } else {
                        false
                    }
                }
            } catch (e: Exception) {
                false
            }
            mainHandler.post { if (succeeded) onComplete(destination) else onFailed() }
        }.start()
    }

    private fun buildInstallIntent(apkFile: File): Intent {
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
        return Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
    }

    /**
     * Called the moment the download finishes — this runs from a background broadcast/thread
     * callback (the app may well be backgrounded right now, that's the whole point of using
     * DownloadManager), where Android 10+ silently blocks any attempt to start an Activity. So
     * this only ever posts a tap-to-install notification (a notification tap is always exempt
     * from that restriction) rather than trying to launch the installer directly.
     */
    fun notifyDownloadReady(apkFile: File) {
        postInstallReadyNotification(buildInstallIntent(apkFile))
    }

    /**
     * Launches the system package installer for the downloaded APK. Only ever call this from a
     * direct user gesture (e.g. a button's onClick) — that's what makes the app unambiguously
     * foreground at the moment this runs, which is what makes the launch reliable. Calling it from
     * a background callback is exactly the unreliable path notifyDownloadReady exists to avoid.
     *
     * On API 26+, ACTION_VIEW on an APK silently does nothing at all — no crash, no dialog — if
     * this app doesn't yet have the "install unknown apps" permission for itself. Checking first
     * and sending the user to grant it (rather than firing ACTION_VIEW and hoping the OS shows
     * that prompt automatically, which isn't reliable on every OEM skin) avoids exactly that.
     */
    fun launchInstaller(apkFile: File) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !context.packageManager.canRequestPackageInstalls()) {
            val settingsIntent = Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES).apply {
                data = Uri.parse("package:${context.packageName}")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(settingsIntent)
            return
        }
        context.startActivity(buildInstallIntent(apkFile))
    }

    private fun postInstallReadyNotification(installIntent: Intent) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    UPDATE_CHANNEL_ID,
                    context.getString(R.string.update_notification_channel),
                    NotificationManager.IMPORTANCE_HIGH,
                ),
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            installIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val notification = NotificationCompat.Builder(context, UPDATE_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(context.getString(R.string.update_notification_title))
            .setContentText(context.getString(R.string.update_notification_text))
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()
        manager.notify(INSTALL_READY_NOTIFICATION_ID, notification)
    }

    private companion object {
        const val UPDATE_CHANNEL_ID = "app_update"
        const val INSTALL_READY_NOTIFICATION_ID = 2001
    }
}
