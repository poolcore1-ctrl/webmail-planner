package com.example.services

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val TAG = "AlarmReceiver"
        const val CHANNEL_ID = "WORKMAIL_ALARMS"
        
        fun scheduleAlarm(context: Context, taskId: Int, title: String, timeInMillis: Long) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            val intent = Intent(context, AlarmReceiver::class.java).apply {
                putExtra("TASK_ID", taskId)
                putExtra("TASK_TITLE", title)
            }
            
            val pendingIntent = PendingIntent.getBroadcast(
                context, 
                taskId, 
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            
            val alarmClockInfo = AlarmManager.AlarmClockInfo(timeInMillis, pendingIntent)
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
            Log.d(TAG, "Scheduled Exact Alarm Clock for task $taskId")
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val taskId = intent.getIntExtra("TASK_ID", -1)
        val title = intent.getStringExtra("TASK_TITLE") ?: "WorkMail Task Due"
        
        Log.d(TAG, "Alarm triggered for task $taskId: $title")
        
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "WorkMail Exact Alarms",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "High priority alarms for deadlines"
            }
            notificationManager.createNotificationChannel(channel)
        }
        
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(context, taskId, mainIntent, PendingIntent.FLAG_IMMUTABLE)

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher) // Use default icon for now
            .setContentTitle("Task Deadline: $title")
            .setContentText("Tap to open WorkMail Planner")
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setFullScreenIntent(pendingIntent, true)
            .setAutoCancel(true)

        notificationManager.notify(taskId, builder.build())
    }
}
