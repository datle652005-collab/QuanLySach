package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Order
import com.example.quanlysach.databinding.ActivityManageOrdersBinding
import com.example.quanlysach.ui.adapter.OrderAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.firebase.database.*

class ManageOrdersActivity : AppCompatActivity() {
    private lateinit var binding: ActivityManageOrdersBinding
    private val database = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Orders")

    private var allOrders = mutableListOf<Order>()
    private var filterStatus = "all"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityManageOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbarManage)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)
        binding.toolbarManage.setNavigationOnClickListener { finish() }

        setupFilter()
        setupSearch()
        loadOrdersFromFirebase()
        setupBottomNav()
    }

    private fun setupFilter() {
        binding.chipGroupStatus.setOnCheckedChangeListener { _, checkedId ->
            filterStatus = when (checkedId) {
                R.id.chipPending -> "dang_cho"
                R.id.chipPacked -> "da_xac_nhan"
                R.id.chipShipping -> "dang_giao"
                R.id.chipDone -> "thanh_cong"
                R.id.chipCancelled -> "da_huy"
                else -> "all"
            }
            applyFilter()
        }
    }

    private fun setupSearch() {
        binding.etSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun loadOrdersFromFirebase() {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                allOrders.clear()

                for (node in snapshot.children) {
                    val key = node.key ?: ""

                    // Nếu node chính là 1 Order (Cấu trúc phẳng)
                    if (node.hasChild("orderId")) {
                        val order = node.getValue(Order::class.java)
                        order?.let { allOrders.add(it) }
                    }
                    // Nếu node là Username (duyen, lan,...)
                    else {
                        for (subNode in node.children) {
                            val order = subNode.getValue(Order::class.java)
                            if (order != null) {
                                order.username = key
                                allOrders.add(order)
                            }
                        }
                    }
                }

                // 🌟 CHÌA KHÓA VÀNG: Sắp xếp theo thời gian tạo (createdAt) giảm dần để đơn mới lên đầu
                // Nếu không có biến createdAt, bạn có thể dùng: allOrders.reverse()
                allOrders.sortByDescending { it.createdAt }

                applyFilter()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationAdmin)
        // 🌟 SỬA: Đang ở màn hình quản lý đơn hàng thì item được chọn phải là nav_admin_orders
        bottomNav.selectedItemId = R.id.nav_admin_orders
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
                R.id.nav_admin_users -> {
                    startActivity(Intent(this, ManageUsersActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_orders -> true
                R.id.nav_admin_reports -> {
                    startActivity(Intent(this, RevenueActivity::class.java))
                    finish(); true
                }
                else -> false
            }
        }
    }

    private fun applyFilter() {
        val searchText = binding.etSearch.text.toString().lowercase()
        val filteredList = allOrders.filter { order ->
            val matchesStatus = if (filterStatus == "all") true else order.status == filterStatus
            val matchesSearch = order.receiverName.lowercase().contains(searchText) ||
                    order.orderId.lowercase().contains(searchText)
            matchesStatus && matchesSearch
        }
        displayList(filteredList)
    }

    private fun displayList(list: List<Order>) {
        binding.rvManageOrders.layoutManager = LinearLayoutManager(this)
        binding.rvManageOrders.adapter = OrderAdapter(list) { order ->
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("ORDER_DATA", order)
            startActivity(intent)
        }
    }
}