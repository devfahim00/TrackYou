package com.devfahim00.trackyou.util

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.devfahim00.trackyou.MainActivity
import com.devfahim00.trackyou.R
import com.devfahim00.trackyou.data.AppDatabase
import com.devfahim00.trackyou.data.Prefs
import com.devfahim00.trackyou.data.currencies
import java.util.Calendar
import java.util.concurrent.TimeUnit

/**
 * Reminder engine:
 * - Nightly "Aj er hishab likhecho?" notification at ~21:00 (only when nothing was
 *   recorded that day).
 * - Morning (~09:00) notification for dena/paona records that are due today or overdue.
 *
 * Uses periodic WorkManager jobs (battery friendly, survives reboot, no exact-alarm
 * permission needed).
 */
object Reminders {

    const val CHANNEL_REMINDERS = "reminders"
    private const val WORK_HISAB = "daily_hisab_reminder"
    private const val WORK_DUE = "daily_due_reminder"

    fun ensureChannels(context: Context) {
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (nm.getNotificationChannel(CHANNEL_REMINDERS) == null) {
            nm.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_REMINDERS,
                    "Reminders",
                    NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Daily hishab reminder and debt due-date alerts"
                }
            )
        }
    }

    fun canPostNotifications(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(
                context, Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    /** Millis from now until the next local occurrence of [hour]:00. */
    private fun delayUntil(hour: Int): Long {
        val now = Calendar.getInstance()
        val next = Calendar.getInstance()
        next.set(Calendar.HOUR_OF_DAY, hour)
        next.set(Calendar.MINUTE, 0)
        next.set(Calendar.SECOND, 0)
        next.set(Calendar.MILLISECOND, 0)
        if (next.timeInMillis <= now.timeInMillis) next.add(Calendar.DAY_OF_YEAR, 1)
        return next.timeInMillis - now.timeInMillis
    }

    /** Schedules both reminder jobs according to current prefs (idempotent). */
    fun sync(context: Context) {
        val prefs = Prefs(context)
        val wm = WorkManager.getInstance(context)
        if (prefs.dailyReminder) {
            wm.enqueueUniquePeriodicWork(
                WORK_HISAB,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<HisabReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayUntil(21), TimeUnit.MILLISECONDS)
                    .build()
            )
        } else {
            wm.cancelUniqueWork(WORK_HISAB)
        }
        if (prefs.dueReminder) {
            wm.enqueueUniquePeriodicWork(
                WORK_DUE,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<DueReminderWorker>(24, TimeUnit.HOURS)
                    .setInitialDelay(delayUntil(9), TimeUnit.MILLISECONDS)
                    .build()
            )
        } else {
            wm.cancelUniqueWork(WORK_DUE)
        }
    }

    fun cancelAll(context: Context) {
        val wm = WorkManager.getInstance(context)
        wm.cancelUniqueWork(WORK_HISAB)
        wm.cancelUniqueWork(WORK_DUE)
    }

    // ---------------- notification builders ----------------

    private fun contentIntent(context: Context): PendingIntent =
        PendingIntent.getActivity(
            context, 0,
            Intent(context, MainActivity::class.java).setFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    fun notifyHisab(context: Context) {
        val n = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle("Aj er hishab likhecho?")
            .setContentText("You haven't recorded any transaction today. Note it down before the day ends.")
            .setStyle(
                NotificationCompat.BigTextStyle()
                    .bigText("You haven't recorded any transaction today. Note it down before the day ends.")
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        post(context, 1001, n)
    }

    fun notifyDue(context: Context, name: String, amount: String, dueOn: String, overdue: Boolean) {
        val text = if (overdue) {
            "Was due on $dueOn and is still unsettled."
        } else {
            "Due today ($dueOn)."
        }
        val n = NotificationCompat.Builder(context, CHANNEL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_monochrome)
            .setContentTitle(if (overdue) "Overdue: $name - $amount" else "Due today: $name - $amount")
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(contentIntent(context))
            .setAutoCancel(true)
            .build()
        // Unique id per notification so parallel alerts stack up.
        post(context, (System.currentTimeMillis() % 100000).toInt() + 2000, n)
    }

    private fun post(context: Context, id: Int, n: android.app.Notification) {
        runCatching {
            if (canPostNotifications(context)) {
                NotificationManagerCompat.from(context).notify(id, n)
            }
        }
    }
}

/** ~21:00 daily: asks the user to record today's transactions. */
class HisabReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = Prefs(ctx)
        if (!prefs.dailyReminder) return Result.success()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        val count = AppDatabase.get(ctx).txDao()
            .countBetween(today.timeInMillis, tomorrow.timeInMillis)
        if (count == 0) {
            Reminders.ensureChannels(ctx)
            Reminders.notifyHisab(ctx)
        }
        return Result.success()
    }
}

/** ~09:00 daily: alerts about debts due today or overdue. */
class DueReminderWorker(context: Context, params: WorkerParameters) :
    CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val ctx = applicationContext
        val prefs = Prefs(ctx)
        if (!prefs.dueReminder) return Result.success()

        val today = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val tomorrow = (today.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, 1) }

        val cur = currencies.firstOrNull { it.code == prefs.currencyCode }
        val sym = cur?.symbol ?: ""
        val fmtDate = java.text.SimpleDateFormat("dd MMM", java.util.Locale.getDefault())

        AppDatabase.get(ctx).debtDao().allOnce()
            .filter { d ->
                val remaining = d.amount - d.paid
                remaining >= 0.005 && d.dueDate != null && d.dueDate < tomorrow.timeInMillis
            }
            .sortedBy { it.dueDate }
            .take(8)
            .forEach { d ->
                val remaining = d.amount - d.paid
                val overdue = d.dueDate!! < today.timeInMillis
                val what = if (d.type == com.devfahim00.trackyou.data.DebtType.LENT) "paona" else "dena"
                Reminders.notifyDue(
                    ctx,
                    "${d.person} (${what})",
                    sym + String.format(java.util.Locale.US, "%,.0f", remaining),
                    fmtDate.format(java.util.Date(d.dueDate!!)),
                    overdue
                )
            }
        return Result.success()
    }
}
