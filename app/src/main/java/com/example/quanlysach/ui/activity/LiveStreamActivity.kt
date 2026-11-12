package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2

import com.example.quanlysach.R
import com.example.quanlysach.ui.adapter.LiveStreamPagerAdapter
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
@UnstableApi
class LiveStreamActivity : AppCompatActivity() {

    private lateinit var viewPager: ViewPager2
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        setContentView(R.layout.activity_live_stream)

        viewPager = findViewById(R.id.viewPagerStreams)

        setupBottomNav()
        loadStreams()
        loadNotificationBadge() // Gọi chấm đỏ thông báo
    }

    private fun loadStreams() {
        val specificVideoId = intent.getStringExtra("videoId")

        if (specificVideoId != null) {
            // Chỉ phát 1 video cụ thể
            viewPager.adapter = LiveStreamPagerAdapter(this, listOf(specificVideoId))
            return
        }

        // load video
        val db = FirebaseDatabase.getInstance(dbUrl).getReference("Videos")

        db.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val streams = mutableListOf<String>()
                for (child in snapshot.children) {
                    child.key?.let { streams.add(it) }
                }
                if (streams.isEmpty()) streams.add("stream_test")
                viewPager.adapter =
                    LiveStreamPagerAdapter(this@LiveStreamActivity, streams.shuffled())
            }
            override fun onCancelled(error: DatabaseError) {
                viewPager.adapter =
                    LiveStreamPagerAdapter(this@LiveStreamActivity, listOf("stream_test"))
            }
        })
    }

    // =========================
    // 🔥 HÀM TẮT HIỆU ỨNG CHUYỂN TRANG
    // =========================
    private fun disableTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }

    // =========================
    // 🔥 SETUP BOTTOM NAVIGATION (TINH CHỈNH FOCUS)
    // =========================
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return

        // 1. Ép focus sáng tab Live mượt mà
        bottomNav.selectedItemId = R.id.nav_live

        // 2. CHỐNG GIẬT NHÁY (Khi đang ở Live mà bấm Live tiếp)
        bottomNav.setOnItemReselectedListener {
            // Nằm im, không làm gì cả để tránh load lại video gây lag
        }

        // 3. Lắng nghe sự kiện chuyển trang
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trang_chu -> {
                    val intent = Intent(this, MainActivity::class.java)
                    intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    startActivity(intent)
                    disableTransition()
                    finish() // Đóng LiveStream để giải phóng bộ nhớ
                    true
                }
                R.id.nav_uu_dai -> {
                    startActivity(Intent(this, PromotionActivity::class.java))
                    disableTransition()
                    finish()
                    true
                }
                R.id.nav_live -> true
                R.id.nav_notification -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    disableTransition()
                    finish()
                    true
                }
                R.id.nav_tai_khoan -> {
                    checkUserLogin()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkUserLogin() {
        val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        val username = pref.getString("USERNAME", null)

        if (username == null) {
            startActivity(Intent(this, LoginActivity::class.java))
        } else {
            val role = pref.getString("VAI_TRO", "khach_hang")
            if (role == "admin") {
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
        }

        disableTransition()
        finish()
    }

    // =========================
    // BADGE MENU DƯỚI (CHẤM ĐỎ THÔNG BÁO)
    // =========================
    private fun loadNotificationBadge() {
        val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        val username = pref.getString("USERNAME", null) ?: return
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Notifications/$username")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unread = 0
                    for (child in snapshot.children) {
                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        if (!isRead) unread++
                    }

                    if (unread > 0) {
                        val badge = bottomNav.getOrCreateBadge(R.id.nav_notification)
                        badge.isVisible = true
                        badge.number = unread
                        badge.maxCharacterCount = 3
                        badge.badgeGravity = BadgeDrawable.TOP_END
                        badge.verticalOffset = 10
                        badge.horizontalOffset = 6
                    } else {
                        bottomNav.removeBadge(R.id.nav_notification)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // hàm xóa video live khi bị chặn,...
    fun removeCurrentStream() {
        val adapter = viewPager.adapter as? LiveStreamPagerAdapter ?: return
        val currentPos = viewPager.currentItem
        val newList = adapter.streamIds.toMutableList()
        if (newList.size <= 1) return // Còn 1 video thì không xóa
        newList.removeAt(currentPos)
        viewPager.adapter = LiveStreamPagerAdapter(this, newList)
        viewPager.setCurrentItem(currentPos.coerceAtMost(newList.size - 1), false)
    }
}
