package com.caladapt.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

abstract class BaseNotificationWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    protected fun sendNotification(title: String, message: String, notificationId: Int) {
        val notificationManager = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        val channel = NotificationChannel(
            "caladapt_reminders",
            "Reminders",
            NotificationManager.IMPORTANCE_DEFAULT
        )
        notificationManager.createNotificationChannel(channel)

        val notification = NotificationCompat.Builder(applicationContext, "caladapt_reminders")
            .setSmallIcon(android.R.drawable.ic_dialog_info) // TODO: replace with app icon
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(notificationId, notification)
    }
}

class MorningWeightReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : BaseNotificationWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        sendNotification(
            title = "Morning Weigh-In",
            message = "Don't forget to log your weight this morning!",
            notificationId = 1001
        )
        return Result.success()
    }
}

class EveningCalorieReminderWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : BaseNotificationWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        sendNotification(
            title = "Log Your Meals",
            message = "Have you logged everything you ate today?",
            notificationId = 1002
        )
        return Result.success()
    }
}

class WeeklyReviewReadyWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : BaseNotificationWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        sendNotification(
            title = "Weekly Review Ready",
            message = "Your metabolism data has been crunched. Tap to see your new plan!",
            notificationId = 1003
        )
        return Result.success()
    }
}
