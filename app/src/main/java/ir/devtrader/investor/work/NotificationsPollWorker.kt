package ir.devtrader.investor.work

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import ir.devtrader.investor.R
import ir.devtrader.investor.TradeBotApplication

/**
 * There is no FCM/push wiring on the backend yet (see project brief) — this is the v1
 * stand-in: poll /notifications periodically and surface unseenCount as a local notification.
 * Fast-follow: replace with real push once the backend grows a Firebase sender.
 */
class NotificationsPollWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as TradeBotApplication).appContainer
        if (!container.sessionManager.isLoggedIn.value) return Result.success()

        container.notificationsCenter.refresh()
        if (container.notificationsCenter.error.value != null) return Result.retry()

        val unseenCount = container.notificationsCenter.unseenCount.value
        if (unseenCount > 0) showBadgeNotification(unseenCount)
        return Result.success()
    }

    private fun showBadgeNotification(unseenCount: Int) {
        val hasPermission = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) ==
            PackageManager.PERMISSION_GRANTED
        if (!hasPermission) return

        val manager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Account notifications",
                NotificationManager.IMPORTANCE_DEFAULT,
            )
            manager.createNotificationChannel(channel)
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("tradeBot Investor")
            .setContentText("You have $unseenCount unread notification${if (unseenCount == 1) "" else "s"}")
            .setNumber(unseenCount)
            .setAutoCancel(true)
            .build()

        manager.notify(BADGE_NOTIFICATION_ID, notification)
    }

    companion object {
        const val UNIQUE_WORK_NAME = "notifications_poll"
        private const val CHANNEL_ID = "account_notifications"
        private const val BADGE_NOTIFICATION_ID = 1001
    }
}
