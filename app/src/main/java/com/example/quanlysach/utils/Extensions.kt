package com.example.quanlysach.utils

import android.app.Activity
import android.widget.ImageView

// Hàm này sẽ dùng chung cho mọi Activity có nút quay lại
fun Activity.setupBackButton(buttonId: Int) {
    val btnBack = findViewById<ImageView>(buttonId)
    btnBack?.setOnClickListener {
        finish() // Đóng trang hiện tại
    }
}