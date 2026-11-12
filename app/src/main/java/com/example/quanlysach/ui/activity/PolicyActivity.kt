package com.example.quanlysach.ui.activity

import android.os.Bundle
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R

class PolicyActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_policy)

        // Ánh xạ nút quay lại từ toolbar XML
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        // Sự kiện click kết thúc màn hình để quay về trang cài đặt trước đó
        btnBack?.setOnClickListener {
            finish()
        }
    }
}