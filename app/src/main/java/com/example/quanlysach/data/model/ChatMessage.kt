package com.example.quanlysach.data.model

data class ChatMessage(
    var text: String = "",
    var isUser: Boolean = false,
    val bookId: String? = null,    // ID sách để link tới chi tiết
    val actionType: String? = null // "VIEW" hoặc "ORDER"
)