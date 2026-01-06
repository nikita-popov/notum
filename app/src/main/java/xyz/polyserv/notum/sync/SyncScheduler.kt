package xyz.polyserv.notum.sync

import android.content.Context
import androidx.work.*
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class SyncScheduler@Inject constructor(
    //@ApplicationContext private val context: Context
) {

    fun scheduleSyncWork(context: Context) {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(
            15, TimeUnit.MINUTES
        )
            .setConstraints(
                Constraints.Builder()
                    .setRequiresBatteryNotLow(true)
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build()
            )
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                15,
                TimeUnit.MINUTES
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            "memo_sync_work",
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    fun syncNow(context: Context) {
        val oneTimeRequest = OneTimeWorkRequestBuilder<SyncWorker>().build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "memo_sync_now",
            ExistingWorkPolicy.REPLACE,
            oneTimeRequest
        )
    }
}
