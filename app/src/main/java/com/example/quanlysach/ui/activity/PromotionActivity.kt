package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Voucher
import com.example.quanlysach.ui.adapter.PromotionAdapter
import com.google.android.material.badge.BadgeDrawable
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.chip.ChipGroup
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*

class PromotionActivity : AppCompatActivity() {

    private lateinit var adapter: PromotionAdapter
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var tvEmptyMessage: TextView
    private lateinit var tvPromoCount: TextView
    private lateinit var tabLayout: TabLayout
    private lateinit var chipGroupPromoType: ChipGroup
    private lateinit var bottomNav: BottomNavigationView // Thêm BottomNav

    private val displayedVouchers = mutableListOf<Voucher>()
    private val fullVoucherList = mutableListOf<Voucher>()
    private val savedVoucherIds = mutableSetOf<String>()

    private var currentTabFilter = "ALL"
    private var currentChipFilter = "ALL"
    private var username: String = "" // Thêm biến lưu username

    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_promotion)

        // Lấy username từ SharedPreferences
        val sharedPref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        username = sharedPref.getString("USERNAME", "") ?: ""

        initViews()
        initBottomNav()
        setupRecyclerView()
        setupTabsAndChips()
        loadSavedVouchers()
        loadVouchers()
        loadNotificationBadge() // Tải số lượng thông báo cho menu

        findViewById<ImageView>(R.id.ivBackPromotion).setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        // Đảm bảo tab "Ưu đãi" luôn được highlight khi quay lại màn hình này
        bottomNav.selectedItemId = R.id.nav_uu_dai
    }

    private fun initViews() {
        layoutEmpty = findViewById(R.id.layoutEmpty)
        tvEmptyMessage = findViewById(R.id.tvEmptyMessage)
        tvPromoCount = findViewById(R.id.tvPromoCount)
        tabLayout = findViewById(R.id.tabLayoutPromotion)
        chipGroupPromoType = findViewById(R.id.chipGroupPromoType)
        bottomNav = findViewById(R.id.bottomNavigation)
    }

    // ===================================================
    // XỬ LÝ BOTTOM NAVIGATION (Giống hệt ProfileActivity)
    // ===================================================
    private fun initBottomNav() {
        bottomNav.selectedItemId = R.id.nav_uu_dai
        bottomNav.setOnItemReselectedListener { }
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trang_chu -> navigateTo(MainActivity::class.java, true)
                R.id.nav_uu_dai -> true // Đang ở màn hình này rồi thì không làm gì
                R.id.nav_live -> navigateTo(LiveStreamActivity::class.java)
                R.id.nav_notification -> navigateTo(NotificationsActivity::class.java)
                R.id.nav_tai_khoan -> navigateTo(ProfileActivity::class.java)
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

    private fun disableTransition() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(OVERRIDE_TRANSITION_OPEN, 0, 0)
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(0, 0)
        }
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

    // ===================================================
    // XỬ LÝ VOUCHER (Giữ nguyên logic cực chuẩn của ông giáo)
    // ===================================================
    private fun setupRecyclerView() {
        val rv = findViewById<RecyclerView>(R.id.rvPromotionList)
        rv.layoutManager = LinearLayoutManager(this)

        adapter = PromotionAdapter(
            list = displayedVouchers,
            onSaveClick = { voucher ->
                if (username.isEmpty()) {
                    startActivity(Intent(this, LoginActivity::class.java))
                    return@PromotionAdapter
                }

                if (savedVoucherIds.contains(voucher.id)) {
                    Toast.makeText(this, "Voucher đã được lưu", Toast.LENGTH_SHORT).show()
                    return@PromotionAdapter
                }

                FirebaseDatabase.getInstance(dbUrl)
                    .getReference("MyVouchers/$username/${voucher.id}")
                    .setValue(voucher)
                    .addOnSuccessListener {
                        savedVoucherIds.add(voucher.id)
                        adapter.notifyDataSetChanged()
                        Toast.makeText(this, "Đã lưu voucher", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Lưu voucher thất bại", Toast.LENGTH_SHORT).show()
                    }
            },
            onUseClick = { voucher ->
                // Thêm .uppercase() để đề phòng trên Firebase lưu chữ thường
                when (voucher.applyTo.uppercase()) {
                    "BOOK" -> {
                        val intent = Intent(this, BookDetailActivity::class.java)
                        intent.putExtra("BOOK_ID", voucher.applyTargetId)
                        startActivity(intent)
                    }
                    "CATEGORY" -> {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.putExtra("TARGET_CATEGORY", voucher.applyTargetId)
                        startActivity(intent)
                        // 🔥 Đã xóa finish() và Flag ở đây để MainActivity nhận được Extra
                    }
                    else -> {
                        val intent = Intent(this, MainActivity::class.java)
                        intent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP
                        startActivity(intent)
                        finish()
                    }
                }
            },
            isVoucherSaved = { voucher ->
                savedVoucherIds.contains(voucher.id)
            }
        )
        rv.adapter = adapter
    }

    private fun setupTabsAndChips() {
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTabFilter = when (tab?.position) {
                    1 -> "BOOK"
                    2 -> "CATEGORY"
                    else -> "ALL"
                }
                applyFilters()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        chipGroupPromoType.setOnCheckedStateChangeListener { group, checkedIds ->
            if (checkedIds.isEmpty()) return@setOnCheckedStateChangeListener
            currentChipFilter = when (checkedIds[0]) {
                R.id.chipPromoDiscount -> "GIAM_GIA"
                R.id.chipPromoFreeship -> "FREESHIP"
                else -> "ALL"
            }
            applyFilters()
        }
    }

    private fun loadSavedVouchers() {
        if (username.isEmpty()) return

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("MyVouchers/$username")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    savedVoucherIds.clear()
                    for (ds in snapshot.children) {
                        ds.key?.let { savedVoucherIds.add(it) }
                    }
                    adapter.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadVouchers() {
        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Vouchers")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    fullVoucherList.clear()
                    val now = System.currentTimeMillis()

                    for (ds in snapshot.children) {
                        val voucher = ds.getValue(Voucher::class.java)
                        voucher?.let {
                            val start = it.startDate
                            val end = if (it.endDate > 0) it.endDate else Long.MAX_VALUE

                            val valid = it.active && start <= now && end >= now
                            if (valid) {
                                fullVoucherList.add(it)
                            }
                        }
                    }

                    fullVoucherList.sortByDescending { it.startDate }
                    applyFilters()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@PromotionActivity, error.message, Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun applyFilters() {
        var filtered = when (currentTabFilter) {
            "BOOK" -> fullVoucherList.filter { it.applyTo == "BOOK" }
            "CATEGORY" -> fullVoucherList.filter { it.applyTo == "CATEGORY" }
            else -> fullVoucherList.toList()
        }

        if (currentChipFilter != "ALL") {
            filtered = filtered.filter { it.loaiVoucher == currentChipFilter }
        }

        displayedVouchers.clear()
        displayedVouchers.addAll(filtered)
        adapter.notifyDataSetChanged()

        updateVoucherCount()
        updateEmptyState(currentTabFilter)
    }

    private fun updateEmptyState(type: String) {
        if (displayedVouchers.isEmpty()) {
            layoutEmpty.visibility = View.VISIBLE
            tvEmptyMessage.text = when (type) {
                "BOOK" -> "Chưa có ưu đãi sách"
                "CATEGORY" -> "Chưa có ưu đãi chủ đề"
                else -> "Hiện chưa có ưu đãi"
            }
        } else {
            layoutEmpty.visibility = View.GONE
        }
    }

    private fun updateVoucherCount() {
        tvPromoCount.text = "${displayedVouchers.size} ưu đãi"
    }
}