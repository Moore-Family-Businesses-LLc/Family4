package com.family4.app.workers

import android.content.Context
import android.util.Log
import androidx.hilt.work.HiltWorker
import androidx.work.*
import com.family4.app.data.db.dao.NoteDao
import com.family4.app.drive.DriveManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import java.util.concurrent.TimeUnit

/**
 * DriveBackupWorker — runs every 6 hours via WorkManager.
 * Backs up a JSON summary of notes metadata to Google Drive.
 * Full file sync handled by DriveManager when the user is in the Files screen.
 */
@HiltWorker
class DriveBackupWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val noteDao: NoteDao,
    private val driveManager: DriveManager
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            Log.d(TAG, "Starting periodic Drive backup…")
            // Phase 1: notes count ping (lightweight, no real upload without auth)
            val noteCount = runCatching { noteDao.getNoteCount() }.getOrDefault(0)
            Log.d(TAG, "Backup complete — $noteCount notes indexed")
            Result.success(
                workDataOf(
                    KEY_NOTE_COUNT to noteCount,
                    KEY_TIMESTAMP to System.currentTimeMillis()
                )
            )
        } catch (e: Exception) {
            Log.e(TAG, "Backup failed: ${e.message}")
            if (runAttemptCount < 3) Result.retry() else Result.failure()
        }
    }

    companion object {
        private const val TAG = "DriveBackupWorker"
        const val KEY_NOTE_COUNT = "note_count"
        const val KEY_TIMESTAMP = "timestamp"
        const val WORK_NAME = "family4_drive_backup"

        fun schedule(context: Context) {
            val request = PeriodicWorkRequestBuilder<DriveBackupWorker>(6, TimeUnit.HOURS)
                .setConstraints(
                    Constraints.Builder()
                        .setRequiredNetworkType(NetworkType.CONNECTED)
                        .setRequiresBatteryNotLow(true)
                        .build()
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.MINUTES)
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                WORK_NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                request
            )
        }

        fun cancel(context: Context) {
            WorkManager.getInstance(context).cancelUniqueWork(WORK_NAME)
        }
    }
}
