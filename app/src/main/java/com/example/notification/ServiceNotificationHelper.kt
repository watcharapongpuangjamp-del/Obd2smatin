package com.example.notification

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
import com.example.MainActivity
import com.example.model.ServiceIntervalUiModel
import com.example.model.ServiceUrgencyStatus

object ServiceNotificationHelper {

    const val CHANNEL_ID = "thai_car_obd_service_interval_channel"
    private const val CHANNEL_NAME = "แจ้งเตือนเช็กระยะและบำรุงรักษารถยนต์"
    private const val CHANNEL_DESC = "การแจ้งเตือนเมื่อรถถึงกำหนดเช็กระยะ บำรุงรักษา หรือครบกำหนดหลังลบโค้ด DTC"

    fun createNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val importance = NotificationManager.IMPORTANCE_HIGH
            val channel = NotificationChannel(CHANNEL_ID, CHANNEL_NAME, importance).apply {
                description = CHANNEL_DESC
                enableVibration(true)
                setShowBadge(true)
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun hasNotificationPermission(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            NotificationManagerCompat.from(context).areNotificationsEnabled()
        }
    }

    fun sendServiceAlertNotification(
        context: Context,
        interval: ServiceIntervalUiModel,
        vehicleName: String = "รถของคุณ"
    ) {
        createNotificationChannel(context)

        if (!hasNotificationPermission(context)) {
            return
        }

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            interval.id.toInt(),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = when (interval.urgencyStatus) {
            ServiceUrgencyStatus.OVERDUE -> "⚠️ ถึงกำหนดเกินเวลา: ${interval.titleTh}"
            ServiceUrgencyStatus.DUE_SOON -> "🔔 ใกล้ถึงกำหนดเช็กระยะ: ${interval.titleTh}"
            ServiceUrgencyStatus.NORMAL -> "ℹ️ บันทึกการเช็กระยะ: ${interval.titleTh}"
        }

        val contentText = "$vehicleName - ${interval.statusDescriptionTh}"
        val bigText = buildString {
            append("$vehicleName\n")
            append("รายการ: ${interval.titleTh} (${interval.titleEn})\n")
            append("สถานะ: ${interval.statusDescriptionTh}\n")
            append("รอบกำหนด: ทุก ${interval.targetIntervalKm} กม. หรือ ${interval.targetIntervalDays} วัน\n")
            if (interval.trackDtcClear) {
                append("หมายเหตุ: รอบการขับขี่ตรวจสอบระบบหลังลบโค้ดความผิดปกติ (DTC Drive Cycle)\n")
            }
            append("กรุณาตรวจสอบหรือนำรถเข้ารับบริการเพื่อความปลอดภัยในการขับขี่")
        }

        val iconRes = android.R.drawable.ic_dialog_alert

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(iconRes)
            .setContentTitle(title)
            .setContentText(contentText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(bigText))
            .setPriority(
                if (interval.urgencyStatus == ServiceUrgencyStatus.OVERDUE)
                    NotificationCompat.PRIORITY_MAX
                else
                    NotificationCompat.PRIORITY_HIGH
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify((1000 + interval.id).toInt(), builder.build())
        } catch (e: SecurityException) {
            // Permission revoked at runtime
        }
    }

    fun sendDtcClearVerificationAlert(
        context: Context,
        vehicleName: String,
        distanceKm: Int,
        clearedCount: Int
    ) {
        createNotificationChannel(context)
        if (!hasNotificationPermission(context)) return

        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            9999,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val title = "🚗 สรุปการขับขี่หลังลบโค้ด DTC ($vehicleName)"
        val message = "ขับขี่มาแล้ว $distanceKm กม. จาก $clearedCount รหัสที่ถูกลบ — แนะนำทำการสแกน ECU ซ้ำเพื่อยืนยันว่าไม่มีโค้ดข้อผิดพลาดเกิดขึ้นใหม่อีก"

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle(title)
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)

        try {
            NotificationManagerCompat.from(context).notify(9999, builder.build())
        } catch (e: SecurityException) {
            // Permission revoked
        }
    }
}
