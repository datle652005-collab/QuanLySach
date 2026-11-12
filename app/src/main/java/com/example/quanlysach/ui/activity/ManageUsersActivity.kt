package com.example.quanlysach.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.User
import com.example.quanlysach.ui.adapter.UserAdapter

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.imageview.ShapeableImageView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*

class ManageUsersActivity : AppCompatActivity() {

    private lateinit var userAdapter: UserAdapter
    private var fullList = mutableListOf<User>()
    private var currentTab = "Tất cả"
    private var searchText = ""
    private lateinit var dbRef: DatabaseReference

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_users)
        val btnBack = findViewById<View>(R.id.btnBackUser)
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed() // Hoặc dùng finish()
        }

        dbRef = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users")

        setupRecyclerView()
        setupBottomNav()
        setupUI()
        loadData()
    }

    private fun setupRecyclerView() {
        val rvUsers = findViewById<RecyclerView>(R.id.rvUsers)
        userAdapter = UserAdapter(mutableListOf()) { showDetail(it) }
        rvUsers.layoutManager = LinearLayoutManager(this)
        rvUsers.adapter = userAdapter
    }

    private fun setupUI() {
        val edtSearch = findViewById<EditText>(R.id.edtSearchUser)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayoutUsers)

        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                searchText = s.toString().trim()
                applyFilter()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.text.toString()
                applyFilter()
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadData() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullList.clear()
                for (ds in snapshot.children) {
                    // CHỐT CHẶN: Bỏ qua các node lỗi nếu Firebase của bạn chưa xóa sạch rác (ví dụ node "trang_thai" bị rớt ra ngoài)
                    if (ds.key == "trang_thai") continue

                    val user = ds.getValue(User::class.java)
                    if (user != null) {
                        // ÉP CỨNG GÁN KEY VÀO MODEL
                        user.username = ds.key ?: ""
                        fullList.add(user)
                    }
                }
                applyFilter() // Dữ liệu tải xong thì gọi bộ lọc ngay
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun applyFilter() {
        val filtered = fullList.filter {
            val matchesTab = when (currentTab) {
                "Khách hàng" -> it.vai_tro == "khach_hang" && it.trang_thai != "bi_khoa"
                "Admin" -> it.vai_tro == "admin"
                "Bị khóa" -> it.trang_thai == "bi_khoa"
                else -> true
            }
            // Tìm theo tên hoặc email
            val matchesSearch = it.ten_user.contains(searchText, ignoreCase = true) ||
                    it.email.contains(searchText, ignoreCase = true)
            matchesTab && matchesSearch
        }
        userAdapter.updateList(filtered)
    }

    private fun showDetail(user: User) {
        val dialogView = layoutInflater.inflate(R.layout.dialog_user_detail, null)

        // Ánh xạ View
        val imgAvatar = dialogView.findViewById<ShapeableImageView>(R.id.imgDetailAvatar)
        val tvName = dialogView.findViewById<TextView>(R.id.tvDetailName)
        val tvUsername = dialogView.findViewById<TextView>(R.id.tvDetailUsername)
        val badgeRole = dialogView.findViewById<TextView>(R.id.badgeDetailRole)
        val badgeStatus = dialogView.findViewById<TextView>(R.id.badgeDetailStatus)
        val tvEmail = dialogView.findViewById<TextView>(R.id.tvDetailEmail)
        val tvPhone = dialogView.findViewById<TextView>(R.id.tvDetailPhone)
        val tvGender = dialogView.findViewById<TextView>(R.id.tvDetailGender)
        val tvBirthday = dialogView.findViewById<TextView>(R.id.tvDetailBirthday)
        val tvBio = dialogView.findViewById<TextView>(R.id.tvDetailBio)

        // Đổ dữ liệu
        Glide.with(this).load(user.avata).placeholder(R.drawable.ic_launcher_background).into(imgAvatar)
        tvName.text = user.ten_user
        tvUsername.text = "@${user.username}"
        tvEmail.text = if (user.email.isNotEmpty()) user.email else "Chưa cập nhật email"
        tvPhone.text = if (user.sdt.isNotEmpty()) user.sdt else "Chưa có số điện thoại"
        tvGender.text = "Giới tính: ${user.gioi_tinh}"
        tvBirthday.text = "Ngày sinh: ${user.ngay_sinh}"
        tvBio.text = if (user.tieu_su.isNotEmpty()) "“${user.tieu_su}”" else "Chưa có tiểu sử."

        // Xử lý Badge Vai trò
        badgeRole.text = if (user.vai_tro == "admin") "Quản trị viên" else "Khách hàng"
        badgeRole.setBackgroundResource(if (user.vai_tro == "admin") R.drawable.bg_badge_role else R.drawable.bg_badge_role)

        // Xử lý Badge Trạng thái
        if (user.trang_thai == "bi_khoa") {
            badgeStatus.text = "Đang bị khóa"
            badgeStatus.visibility = View.VISIBLE
        } else {
            badgeStatus.text = "Đang hoạt động"
            badgeStatus.setTextColor(Color.parseColor("#16A34A"))
            badgeStatus.setBackgroundResource(R.drawable.bg_badge_status)
        }

        // Hiển thị Dialog
        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("Đóng", null)
            .show()
    }
    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationAdmin)
        bottomNav.selectedItemId = R.id.nav_admin_users
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_books -> {
                    startActivity(Intent(this, ManageBooksActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_users -> true
                R.id.nav_admin_orders -> {
                    startActivity(Intent(this, ManageOrdersActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_reports -> {
                    startActivity(Intent(this, RevenueActivity::class.java))
                    finish(); true
                }
                else -> false
            }
        }
    }
}