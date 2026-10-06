package com.nehamahesh.pocketalpha

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.util.concurrent.TimeUnit
import kotlin.math.abs

class PriceAlertWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val sessions = SessionStore(applicationContext)
        if (sessions.token == null) return Result.success()

        return runCatching {
            val watchlist = ApiClient(sessions).watchlist()
            WidgetSnapshotStore(applicationContext).save(watchlist)
            PocketAlphaWidget().refresh(applicationContext)

            val movers = watchlist
                .filter { abs(it.changePercent) >= ALERT_THRESHOLD_PERCENT }
                .sortedByDescending { abs(it.changePercent) }

            if (movers.isNotEmpty()) {
                notifyPriceMovement(movers.first())
            }
            Result.success()
        }.getOrElse {
            Result.retry()
        }
    }

    private fun notifyPriceMovement(quote: Quote) {
        if (
            Build.VERSION.SDK_INT >= 33 &&
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        val manager = applicationContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                "Price alerts",
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Background watchlist movement alerts"
            }
        )

        val direction = if (quote.changePercent >= 0) "up" else "down"
        val notification = Notification.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("${quote.symbol} moved ${"%.2f".format(abs(quote.changePercent))}%")
            .setContentText("${quote.symbol} is $direction to $${"%.2f".format(quote.price)} in the simulated market.")
            .setAutoCancel(true)
            .build()

        manager.notify(quote.symbol.hashCode(), notification)
    }

    companion object {
        private const val CHANNEL_ID = "pocketalpha_price_alerts"
        private const val ALERT_THRESHOLD_PERCENT = 2.0
    }
}

object PriceAlertScheduler {
    private const val UNIQUE_WORK = "pocketalpha-price-alerts"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<PriceAlertWorker>(15, TimeUnit.MINUTES)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            UNIQUE_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK)
    }
}
