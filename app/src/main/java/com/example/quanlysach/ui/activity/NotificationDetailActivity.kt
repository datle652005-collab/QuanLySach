package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import com.example.quanlysach.R
import java.text.SimpleDateFormat
import java.util.*

@UnstableApi
class NotificationDetailActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_notification_detail)

        val type = intent.getStringExtra("type") ?: ""
        val reason = intent.getStringExtra("reason") ?: ""
        val productName = intent.getStringExtra("productName") ?: "N/A"
        val videoId = intent.getStringExtra("videoId") ?: ""
        val timestamp = intent.getLongExtra("timestamp", 0L)

        findViewById<ImageView>(R.id.btnBack).setOnClickListener { finish() }

        // Icon theo loại
        findViewById<ImageView>(R.id.iconDetail).setImageResource(
            when (type) {
                "Báo cáo" -> R.drawable.ic_warning
                "Chặn" -> R.drawable.ic_block
                "Không quan tâm" -> R.drawable.ic_heart_broken
                else -> R.drawable.ic_close
            }
        )

        findViewById<TextView>(R.id.tvDetailType).text = type
        findViewById<TextView>(R.id.tvDetailReason).text = reason
        findViewById<TextView>(R.id.tvDetailProduct).text = productName
        findViewById<TextView>(R.id.tvDetailVideo).text = videoId
        findViewById<TextView>(R.id.tvDetailTime).text = formatTime(timestamp)

        // Hiện link video
        val tvVideo = findViewById<TextView>(R.id.tvDetailVideo)
        if (videoId.isNotEmpty()) {
            tvVideo.text = "Xem video: mybookreview://video/$videoId"
            tvVideo.setOnClickListener {
                startActivity(Intent(this, LiveStreamActivity::class.java).apply {
                    putExtra("videoId", videoId)
                })
            }
        } else {
            tvVideo.text = "Không có video"
        }
    }

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "N/A"
        val sdf = SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
}