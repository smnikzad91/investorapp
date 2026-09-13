package ir.devtrader.investor

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import ir.devtrader.investor.work.NotificationsPollWorker
import java.util.concurrent.TimeUnit

class TradeBotApplication : Application() {

    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
        schedulePeriodicNotificationsPoll()
    }

    private fun schedulePeriodicNotificationsPoll() {
        // No FCM wiring on the backend yet, so we poll instead. See NotificationsPollWorker.
        val request = PeriodicWorkRequestBuilder<NotificationsPollWorker>(15, TimeUnit.MINUTES).build()
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NotificationsPollWorker.UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }
}
