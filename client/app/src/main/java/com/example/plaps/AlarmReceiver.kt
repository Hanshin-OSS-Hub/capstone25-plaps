package com.example.plaps

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat

class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        // 알람이 정시에 깨어났을 때, 마이페이지 알림 스위치 상태를 즉시 확인!
        val prefs = context.getSharedPreferences("plaps_settings", Context.MODE_PRIVATE)
        val isNotificationEnabled = prefs.getBoolean("notification_enabled", true)

        // 유저가 마이페이지에서 설정을 꺼둔 상태라면, 아래의 노티 발송 로직을 타지 않고 즉시 폭파(종료)!
        if (!isNotificationEnabled) {
            return
        }

        // 알람 예약 시 담아둔 일정 제목과 메시지 가져오기
        val title = intent.getStringExtra("title") ?: "PLAPS 일정 알림"
        val message = intent.getStringExtra("message") ?: "일정 시작 시간이 되었습니다!"

        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val channelId = "plaps_alarm_channel"

        // 1. Android 8.0 (Oreo) 이상은 반드시 '알림 채널'을 만들어야 팝업이 뜸
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "플래너 일정 알림",
                NotificationManager.IMPORTANCE_HIGH // ✨ IMPORTANCE_HIGH여야 폰 상단에 팝업(헤드업)이 빡 뜹니다!
            ).apply {
                description = "PLAPS 플래너의 일정 시작 시간에 울리는 알림입니다."
            }
            notificationManager.createNotificationChannel(channel)
        }

        // 2. 팝업 알림을 클릭했을 때 앱 메인 화면(MainActivity)으로 유저를 보내주는 인텐트 설정
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            mainIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 3. 팝업 알림의 디자인 및 속성 구성
        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.ic_lock_idle_alarm) // ⏰ 일단 기본 시계 아이콘 (나중에 앱 로고로 교체 가능)
            .setContentTitle(title)
            .setContentText(message)
            .setPriority(NotificationCompat.PRIORITY_HIGH) // ✨ 옛날 버전을 위한 팝업 노출 필수 설정
            .setDefaults(NotificationCompat.DEFAULT_ALL)   // 진동, 소리 기본값 세팅
            .setContentIntent(pendingIntent)
            .setAutoCancel(true) // 누르면 상단바에서 자동으로 사라지게 설정
            .build()

        // 4. 진짜 폰 화면에 알림 발송 (고유 ID는 겹치지 않게 타임스탬프 활용)
        notificationManager.notify(System.currentTimeMillis().toInt(), notification)
    }
}