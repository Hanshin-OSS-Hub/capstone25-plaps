package com.example.plaps

import android.annotation.SuppressLint
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.plaps.data.Event
import java.time.LocalDateTime
import java.time.ZoneId

class AlarmScheduler(private val context: Context) {
    private val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    @SuppressLint("ScheduleExactAlarm")
    fun schedule(event: Event, force: Boolean = false) {
        if (!force) {
            val prefs = context.getSharedPreferences("plaps_settings", Context.MODE_PRIVATE)
            val isNotificationEnabled = prefs.getBoolean("notification_enabled", true)
            if (!isNotificationEnabled) {
                return
            }
        }

        val eventDateTime = LocalDateTime.of(event.date, event.startTime)
        val triggerTimeMs = eventDateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        if (triggerTimeMs <= System.currentTimeMillis()) {
            return
        }

        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", event.title)
            putExtra("message", "${event.startTime}에 시작하는 일정이 있습니다!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !alarmManager.canScheduleExactAlarms()) {
            alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        } else {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTimeMs, pendingIntent)
        }
    }

    // 일정을 취소하거나 삭제했을 때 알람을 해제해 주는 함수
    fun cancel(event: Event) {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            putExtra("title", event.title)
            putExtra("message", "${event.startTime}에 시작하는 일정이 있습니다!")
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            event.id,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel() // 시스템 예약 테이블에서 확실하게 증발시킴
        }
    }
}