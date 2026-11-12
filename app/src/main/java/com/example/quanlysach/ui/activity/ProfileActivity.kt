package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

class ProfileActivity : AppCompatActivity() {

    private lateinit var tvName: TextView
    private lateinit var tvEmail: TextView
    private lateinit var imgAvatar: ImageView
    private lateinit var tvCartBadgeProfile: TextView
    private lateinit var bottomNav: BottomNavigationView

    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private var username: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        val sharedPref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        username = sharedPref.getString("USERNAME", "") ?: ""

        initViews()
        loadUserInfo()
        setupClickListeners()
        initBottomNav()
        loadNotificationBadge()
    }

    override fun onResume() {
        super.onResume()
        bottomNav.selectedItemId = R.id.nav_tai_khoan
        updateCartBadge()
    }

    private fun initViews() {
        tvName = findViewById(R.id.tvName)
        tvEmail = findViewById(R.id.tvUserEmail)
        imgAvatar = findViewById(R.id.imgAvatar)
        tvCartBadgeProfile = findViewById(R.id.tvCartBadgeProfile)
        bottomNav = findViewById(R.id.bottomNavigation)
    }

    private fun setupClickListeners() {
        // 1. Header: Cài đặt và Giỏ hàng
        findViewById<ImageView>(R.id.btnSettingsGear)?.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        findViewById<FrameLayout>(R.id.btnCartProfile)?.setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        // 2. Avatar & Tên (Chỉnh sửa hồ sơ)
        val openEdit = View.OnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }
        imgAvatar.setOnClickListener(openEdit)
        tvName.setOnClickListener(openEdit)

        // 3. Trạng thái đơn hàng
        findViewById<LinearLayout>(R.id.layoutOrderPending)?.setOnClickListener { openMyOrders("dang_cho") }
        findViewById<LinearLayout>(R.id.layoutOrderAccepted)?.setOnClickListener { openMyOrders("da_xac_nhan") }
        findViewById<LinearLayout>(R.id.layoutOrderShipping)?.setOnClickListener { openMyOrders("dang_giao") }
        findViewById<LinearLayout>(R.id.layoutOrderDone)?.setOnClickListener { openMyOrders("thanh_cong") }
        findViewById<TextView>(R.id.tvViewOrderHistory)?.setOnClickListener { openMyOrders("all") }

        // 4. Các mục Tài khoản của tôi (Tương ứng với các ID trong XML mới)

        // Hồ sơ & Bảo mật
        findViewById<LinearLayout>(R.id.btnProfileInfo)?.setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Địa chỉ nhận sách
        findViewById<LinearLayout>(R.id.btnAddress)?.setOnClickListener {
            val intent = Intent(this, AddressListActivity::class.java)
            intent.putExtra("IS_FROM_SETTINGS", true)
            startActivity(intent)
        }

        // Trung tâm hỗ trợ (FAQ)
        findViewById<LinearLayout>(R.id.btnHelpCenter)?.setOnClickListener {
            startActivity(Intent(this, HelpCenterActivity::class.java))
        }

        // Chính sách nhà sách
        findViewById<LinearLayout>(R.id.btnPolicy)?.setOnClickListener {
            startActivity(Intent(this, PolicyActivity::class.java))
        }
    }

    private fun openMyOrders(status: String) {
        val intent = Intent(this, MyOrdersActivity::class.java)
        intent.putExtra("ORDER_STATUS", status)
        startActivity(intent)
    }

    private fun loadUserInfo() {
        if (username.isEmpty()) return

        val dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(username)
        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                if (snapshot.exists()) {
                    tvName.text = snapshot.child("ten_user").value?.toString() ?: username
                    tvEmail.text = snapshot.child("email").value?.toString() ?: "Chưa có email"
                    val avatar = snapshot.child("avata").value?.toString() ?: ""
                    if (avatar.isNotEmpty()) {
                        Glide.with(this@ProfileActivity)
                            .load(avatar)
                            .placeholder(R.drawable.ic_launcher_background)
                            .circleCrop()
                            .into(imgAvatar)
                    }
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun initBottomNav() {
        bottomNav.selectedItemId = R.id.nav_tai_khoan
        bottomNav.setOnItemReselectedListener { }
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trang_chu -> navigateTo(MainActivity::class.java, true)
                R.id.nav_uu_dai -> navigateTo(PromotionActivity::class.java)
                R.id.nav_live -> navigateTo(LiveStreamActivity::class.java)
                R.id.nav_notification -> navigateTo(NotificationsActivity::class.java)
                R.id.nav_tai_khoan -> true
                else -> false
            }
        }
    }

    private fun navigateTo(cls: Class<*>, isMain: Boolean = false): Boolean {
        val intent = Intent(this, cls)
        if (isMain) {
            intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        startActivity(intent)
        disableTransition()
        finish()
        return true
    }

    private fun updateCartBadge() {
        if (username.isEmpty()) {
            tvCartBadgeProfile.visibility = View.GONE
            return
        }

        FirebaseDatabase.getInstance(dbUrl).getReference("Carts").child(username)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalCount = 0
                    for (ds in snapshot.children) {
                        totalCount += ds.child("soLuong").getValue(Int::class.java) ?: 0
                    }

                    if (totalCount > 0) {
                        tvCartBadgeProfile.visibility = View.VISIBLE
                        tvCartBadgeProfile.text = if (totalCount > 99) "99+" else totalCount.toString()
                    } else {
                        tvCartBadgeProfile.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadNotificationBadge() {
        if (username.isEmpty()) return

        FirebaseDatabase.getInstance(dbUrl).getReference("Notifications/$username")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unread = 0
                    for (child in snapshot.children) {
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

    private fun disableTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
    }
}
