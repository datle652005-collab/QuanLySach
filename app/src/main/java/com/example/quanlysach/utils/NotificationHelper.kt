package com.example.quanlysach.utils

import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

object NotificationHelper {

    private val database = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/").reference

    /**
     * @param type: "order" hoặc "review"
     */
    fun sendAdminNotification(type: String) {
        // 1. Tăng số đếm Badge trên Firebase
        val badgeNode = if (type == "order") "newOrders" else "newReviews"

        database.child("AdminNotifications").child(badgeNode)
            .setValue(ServerValue.increment(1))

        // 2. Ghi log chi tiết (Tùy chọn: giúp admin xem lại lịch sử)
        val logRef = database.child("AdminLogs").push()
        val logData = mapOf(
            "message" to if (type == "order") "Có đơn hàng mới chờ xử lý!" else "Có đánh giá mới cần duyệt!",
            "timestamp" to System.currentTimeMillis(),
            "isRead" to false
        )
        logRef.setValue(logData)
    }
}