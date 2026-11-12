package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.annotation.OptIn
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.util.UnstableApi
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysach.R
import com.example.quanlysach.data.model.NotificationModel

import com.example.quanlysach.databinding.ActivityNotificationsBinding
import com.example.quanlysach.ui.adapter.NotificationAdapter

import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

class NotificationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityNotificationsBinding
    private lateinit var adapter: NotificationAdapter
    private val notiList = mutableListOf<NotificationModel>()

    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityNotificationsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Lấy username ngay từ đầu để dùng chung
        val sharedPref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        username = sharedPref.getString("USERNAME", "") ?: ""

        if (username.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        setupToolbar()
        setupRecyclerView()
        initBottomNav()
        loadNotifications()
        loadNotificationBadge()

        // 🔔 SỬA QUAN TRỌNG: Chỉ chạy cập nhật đã đọc DUY NHẤT 1 LẦN khi vừa mở màn hình lên
        markAllAsReadOnce()
    }

    // =========================
    // TOOLBAR
    // =========================
    private fun setupToolbar() {
        setSupportActionBar(binding.toolbarNoti)
        supportActionBar?.setDisplayShowTitleEnabled(false)
        binding.toolbarNoti.setNavigationIcon(R.drawable.ic_back_white)
        binding.toolbarNoti.setNavigationOnClickListener { finish() }
    }

    // =========================
    // RECYCLERVIEW
    // =========================
    private fun setupRecyclerView() {
        adapter = NotificationAdapter(notiList)
        binding.rvNotifications.apply {
            layoutManager = LinearLayoutManager(this@NotificationsActivity)
            this.adapter = this@NotificationsActivity.adapter
            if (itemDecorationCount == 0) {
                addItemDecoration(DividerItemDecoration(this@NotificationsActivity, DividerItemDecoration.VERTICAL))
            }
        }
    }

    // =========================
    // LOAD NOTIFICATIONS
    // =========================
    private fun loadNotifications() {
        val dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Notifications").child(username)

        // Lắng nghe dữ liệu đổ về danh sách
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                notiList.clear()
                for (node in snapshot.children) {
                    val title = node.child("title").value as? String ?: ""

                    // 🌟 SỬA ĐỔI QUAN TRỌNG: Lọc sạch và tự động dọn rác tin nhắn chat hỗ trợ khỏi nhánh Notifications
                    if (title.contains("Phản hồi từ Admin", true) || title.contains("💬", true) || title.contains("tin nhắn", true)) {
                        node.ref.removeValue() // Tự động xóa luôn node rác này trên Firebase Console
                        continue              // Bỏ qua không thêm vào danh sách hiển thị trên giao diện người dùng
                    }

                    node.getValue(NotificationModel::class.java)?.let { notiList.add(it) }
                }

                // Sắp xếp thông báo mới nhất lên đầu
                notiList.sortByDescending { it.timestamp }
                adapter.notifyDataSetChanged()

                // Cập nhật giao diện (Số lượng, Empty state)
                updateNotificationUI()
            }

            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@NotificationsActivity, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun updateNotificationUI() {
        val total = notiList.size
        binding.txtNotiCount.text = "$total thông báo"

        if (total > 0) {
            binding.txtBadge.visibility = View.VISIBLE
            binding.txtBadge.text = if (total > 99) "99+" else total.toString()
            binding.rvNotifications.visibility = View.VISIBLE
            binding.txtEmpty.visibility = View.GONE
        } else {
            binding.txtBadge.visibility = View.GONE
            binding.rvNotifications.visibility = View.GONE
            binding.txtEmpty.visibility = View.VISIBLE
        }
    }

    // =========================
    // BADGE MENU DƯỚI (CHẤM ĐỎ)
    // =========================
    private fun loadNotificationBadge() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Notifications/$username")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unread = 0
                    for (child in snapshot.children) {
                        val title = child.child("title").value as? String ?: ""

                        // 🌟 Bỏ qua tin chat hỗ trợ, không đếm vào tổng số thông báo hệ thống chưa đọc
                        if (title.contains("Phản hồi từ Admin", true) || title.contains("💬", true) || title.contains("tin nhắn", true)) {
                            continue
                        }

                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        if (!isRead) unread++
                    }

                    if (unread > 0) {
                        bottomNav.getOrCreateBadge(R.id.nav_notification).apply {
                            isVisible = true
                            number = unread
                            badgeGravity = BadgeDrawable.TOP_END
                        }
                    } else {
                        bottomNav.removeBadge(R.id.nav_notification)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // 🔔 HÀM SỬA CHUẨN: Đánh dấu tất cả là đã đọc an toàn, tách biệt hoàn toàn luồng dữ liệu
    private fun markAllAsReadOnce() {
        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Notifications/$username")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    snapshot.children.forEach { child ->
                        val title = child.child("title").value as? String ?: ""

                        // 🌟 Bỏ qua không cập nhật trạng thái đọc của các node chat hỗ trợ (vì sẽ bị xóa ở luồng load)
                        if (title.contains("Phản hồi từ Admin", true) || title.contains("💬", true) || title.contains("tin nhắn", true)) {
                            return@forEach
                        }

                        val isRead = child.child("isRead").getValue(Boolean::class.java) ?: false
                        if (!isRead) {
                            child.ref.child("isRead").setValue(true)
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // =========================
    // MENU DƯỚI (BOTTOM NAV)
    // =========================
    @OptIn(UnstableApi::class) private fun initBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation) ?: return
        bottomNav.selectedItemId = R.id.nav_notification

        bottomNav.setOnItemReselectedListener { /* Chống giật khi nhấn lại chính nó */ }

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trang_chu -> {
                    startActivity(Intent(this, MainActivity::class.java).apply {
                        flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
                    })
                    finishAndDisableTrans()
                    true
                }
                R.id.nav_uu_dai -> {
                    startActivity(Intent(this, PromotionActivity::class.java))
                    finishAndDisableTrans()
                    true
                }
                R.id.nav_live -> {
                    startActivity(Intent(this, LiveStreamActivity::class.java))
                    finishAndDisableTrans()
                    true
                }
                R.id.nav_notification -> true
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
        val role = pref.getString("VAI_TRO", "khach_hang")

        val intent = if (role == "admin") {
            Intent(this, AdminActivity::class.java)
        } else {
            Intent(this, ProfileActivity::class.java)
        }
        startActivity(intent)
        finishAndDisableTrans()
    }

    private fun finishAndDisableTrans() {
        disableTransition()
        finish()
    }

    private fun disableTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}