package com.example.quanlysach.ui.activity

import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.google.android.material.card.MaterialCardView

class HelpCenterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_help_center)

        // 1. Ánh xạ nút quay lại và xử lý sự kiện đóng màn hình
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        btnBack?.setOnClickListener {
            finish()
        }

        // 2. Ánh xạ các cụm câu hỏi FAQ từ file giao diện XML
        val layoutFaq1 = findViewById<MaterialCardView>(R.id.layoutFaq1)
        val tvAnswer1 = findViewById<TextView>(R.id.tvAnswer1)

        val layoutFaq2 = findViewById<MaterialCardView>(R.id.layoutFaq2)
        val tvAnswer2 = findViewById<TextView>(R.id.tvAnswer2)

        val layoutFaq3 = findViewById<MaterialCardView>(R.id.layoutFaq3)
        val tvAnswer3 = findViewById<TextView>(R.id.tvAnswer3)

        // 3. Cài đặt sự kiện click đóng/mở linh hoạt cho Câu hỏi 1
        layoutFaq1?.setOnClickListener {
            if (tvAnswer1.visibility == View.VISIBLE) {
                tvAnswer1.visibility = View.GONE
            } else {
                tvAnswer1.visibility = View.VISIBLE
            }
        }

        // Cài đặt sự kiện click đóng/mở linh hoạt cho Câu hỏi 2
        layoutFaq2?.setOnClickListener {
            if (tvAnswer2.visibility == View.VISIBLE) {
                tvAnswer2.visibility = View.GONE
            } else {
                tvAnswer2.visibility = View.VISIBLE
            }
        }

        // Cài đặt sự kiện click đóng/mở linh hoạt cho Câu hỏi 3
        layoutFaq3?.setOnClickListener {
            if (tvAnswer3.visibility == View.VISIBLE) {
                tvAnswer3.visibility = View.GONE
            } else {
                tvAnswer3.visibility = View.VISIBLE
            }
        }
    }
}