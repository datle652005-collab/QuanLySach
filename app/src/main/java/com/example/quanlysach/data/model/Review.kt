package com.example.quanlysach.data.model

data class Review(
    val username: String = "",
    val rating: Int = 0,
    val content: String = "",
    val timestamp: Long = 0,
    val images: List<String> = emptyList(),
    val reviewed: Boolean = false
)