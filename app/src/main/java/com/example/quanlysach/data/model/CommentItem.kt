package com.example.quanlysach

data class CommentItem(
    val commentId: String = "",
    val username: String = "",
    val content: String = "",
    val avatarUrl: String = "",
    val timestamp: Long = 0L,
    var likeCount: Int = 0,
    val streamId: String = ""
)