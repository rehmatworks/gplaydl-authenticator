package com.gplaydl.authenticator.sync

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.gplaydl.authenticator.data.DispenserApi
import com.gplaydl.authenticator.data.Prefs
import java.util.concurrent.TimeUnit

/**
 * Periodically touches the dispenser so the dashboard can show whether a
 * contributing device is still around, and so a rotated API key surfaces as a
 * failure the user can act on rather than silence.
 */
class ResyncWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val prefs = Prefs(applicationContext)
        val state = prefs.current()
        val apiKey = state.apiKey ?: return Result.success()

        val api = DispenserApi { state.dispenserUrl }
        return runCatching { api.accounts(apiKey) }
            .fold(onSuccess = { Result.success() }, onFailure = { Result.retry() })
    }
}

object ResyncScheduler {
    private const val WORK_NAME = "dispenser-resync"

    fun schedule(context: Context) {
        val request = PeriodicWorkRequestBuilder<ResyncWorker>(12, TimeUnit.HOURS)
            .setConstraints(
                Constraints.Builder()
                    .setRequiredNetworkType(NetworkType.CONNECTED)
                    .build(),
            )
            .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request,
        )
    }

    fun cancel(context: Context) {
        WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
    }
}
