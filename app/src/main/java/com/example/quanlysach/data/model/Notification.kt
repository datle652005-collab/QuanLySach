package com.example.quanlysach.data.model

data class Notification(
    val title: String = "",
    val message: String = "",
    val timestamp: Long = 0,
    val receiverId: String = ""
)