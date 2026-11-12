package com.example.quanlysach.data.model

data class AdminChatRoom(
    var username: String = "",       // Tài khoản user chat (dùng làm key map)
    var displayName: String = "",    // Tên hiển thị thật (ten_user)
    var avatarUrl: String = "",      // Đường dẫn ảnh (avata)
    var lastMessage: String = "",
    var lastTimestamp: Long = 0,
    var unreadCount: Int = 0
)