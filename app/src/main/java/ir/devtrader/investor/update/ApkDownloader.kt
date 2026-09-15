package ir.devtrader.investor.update

import android.app.DownloadManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import androidx.core.content.FileProvider
import java.io.File

/**
 * Downloads the release APK via the system DownloadManager (survives the app being
 * backgrounded/killed mid-download, shows progress in the notification shade, handles retries
 * on its own) and hands the finished file to the package installer through a FileProvider
 * content:// URI — a raw file:// URI throws FileUriExposedException on API 24+.
 */
class ApkDownloader(private val context: Context) {

    private val authority = "${context.packageName}.fileprovider"
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
            .setDestinationUri(Uri.fromFile(destination))
            .setAllowedOverMetered(true)
            .setAllowedOverRoaming(true)

        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        enqueuedDownloadId = downloadManager.enqueue(request)

        val receiver = object : BroadcastReceiver() {
            override fun onReceive(ctx: Context, intent: Intent) {
                val finishedId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1L)
                if (finishedId != enqueuedDownloadId) return
                context.unregisterReceiver(this)

                val query = DownloadManager.Query().setFilterById(enqueuedDownloadId)
                downloadManager.query(query).use { cursor ->
                    val statusIdx = cursor.getColumnIndex(DownloadManager.COLUMN_STATUS)
                    if (cursor.moveToFirst() && statusIdx >= 0 && cursor.getInt(statusIdx) == DownloadManager.STATUS_SUCCESSFUL) {
                        onComplete(destination)
                    } else {
                        onFailed()
                    }
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

    /** Launches the system package installer for the downloaded APK. */
    fun promptInstall(apkFile: File) {
        val apkUri: Uri = FileProvider.getUriForFile(context, authority, apkFile)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(apkUri, "application/vnd.android.package-archive")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}
