package com.example.quanlysach.data.api

import android.R
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import android.os.Handler
import android.os.Looper
import androidx.annotation.OptIn
import androidx.core.app.NotificationCompat
import androidx.media3.common.util.UnstableApi
import com.example.quanlysach.ui.activity.MainActivity
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.net.URL
import java.util.concurrent.Executors

class MyFirebaseMessagingService : FirebaseMessagingService() {

    // Hàm tự động chạy khi có thông báo FCM (Firebase Cloud Messaging) đẩy về máy
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)

        // 1. Lấy dữ liệu từ data payload gửi về
        val title = remoteMessage.data["title"] ?: remoteMessage.notification?.title ?: "Thông báo từ HEDUCI Lib"
        val body = remoteMessage.data["content"] ?: remoteMessage.data["body"] ?: remoteMessage.notification?.body ?: ""
        val imageUrl = remoteMessage.data["image"]

        // Kiểm tra loại thông báo để hiển thị giao diện phù hợp
        val isOrderSuccess = title.contains("thành công", true)

        // 2. Hiển thị thông báo lên thanh trạng thái hệ thống
        showNotification(applicationContext, title, body, imageUrl, isOrderSuccess)
    }

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        // Duyên có thể gửi token này lên MySQL/Firebase nếu cần quản lý gửi thông báo theo thiết bị cụ thể
    }

    @OptIn(UnstableApi::class) private fun showNotification(context: Context, title: String, body: String, imageUrl: String?, isOrderSuccess: Boolean) {
        val channelId = "fcm_default_channel"
        val notificationManager = context.getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        // Cấu hình Kênh thông báo (Notification Channel) cho Android 8.0 trở lên
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (isOrderSuccess) "Thông báo đơn hàng" else "Tin nhắn từ hệ thống"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                lightColor = Color.BLUE
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Tạo Intent mở MainActivity khi người dùng nhấn vào thông báo
        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            System.currentTimeMillis().toInt(),
            mainIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        // Xây dựng cấu trúc giao diện thông báo chuẩn Android
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.stat_notify_chat) // Icon nhỏ trên thanh trạng thái
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        // Nếu là đơn hàng thành công -> Thêm nút 5 sao và tải ảnh sản phẩm lớn
        if (isOrderSuccess) {
            val rateIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_RATING", true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val ratePendingIntent = PendingIntent.getActivity(
                context,
                System.currentTimeMillis().toInt() + 1,
                rateIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            // Thêm nút "Đánh giá 5 sao" trực tiếp trên thông báo
            builder.addAction(R.drawable.btn_star_big_on, "⭐ Đánh giá 5 sao", ratePendingIntent)

            // Tiến hành tải ảnh sản phẩm từ URL ngầm (luồng phụ) tránh treo app
            if (!imageUrl.isNullOrEmpty()) {
                val executor = Executors.newSingleThreadExecutor()
                executor.execute {
                    var bitmap: Bitmap? = null
                    try {
                        val url = URL(imageUrl)
                        bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                        builder.setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(null as Bitmap?)
                        )
                    }

                    // Đẩy thông báo lên màn hình sau khi tải xong ảnh
                    Handler(Looper.getMainLooper()).post {
                        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
                    }
                }
                return
            }
        }

        // Đẩy thông báo thường lên màn hình (nếu không có ảnh hoặc là tin nhắn chat)
        notificationManager.notify(System.currentTimeMillis().toInt(), builder.build())
    }
}