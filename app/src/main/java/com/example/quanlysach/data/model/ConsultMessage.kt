package com.example.quanlysach.data.model

data class ConsultMessage(
    val messageId: String = "",
    val senderId: String = "",
    val receiverId: String = "",
    val message: String = "",       // Nếu là ảnh, trường này sẽ chứa URL ảnh
    val timestamp: Long = 0,
    val read: Boolean = false,
    val type: String = "text"       // "text" hoặc "image" để phân biệt kiểu tin nhắn
)