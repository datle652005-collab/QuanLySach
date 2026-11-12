package com.example.quanlysach.ui.activity

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysach.data.model.Order

import com.example.quanlysach.databinding.ActivityMyOrdersBinding
import com.example.quanlysach.ui.adapter.UserOrderAdapter

import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMyOrdersBinding
    private var allOrders = mutableListOf<Order>()
    private var filteredList = mutableListOf<Order>()
    private lateinit var adapter: UserOrderAdapter
    private val databaseUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMyOrdersBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setupTabs()
        setupRecyclerView()
        setupSearch()
        loadOrdersFromFirebase()

        binding.btnBack.setOnClickListener { finish() }

        val statusFromIntent = intent.getStringExtra("ORDER_STATUS") ?: "all"
        selectTabByTag(statusFromIntent)
    }

    private fun setupTabs() {
        val labels = listOf("Tất cả", "Chờ xác nhận", "Đóng gói", "Đang giao", "Thành công", "Đã hủy")
        val tags = listOf("all", "dang_cho", "da_xac_nhan", "dang_giao", "thanh_cong", "da_huy")

        for (i in tags.indices) {
            val tab = binding.tabLayoutOrders.newTab().setText(labels[i])
            tab.tag = tags[i]
            binding.tabLayoutOrders.addTab(tab)
        }

        binding.tabLayoutOrders.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) { applyFilter() }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun setupRecyclerView() {
        adapter = UserOrderAdapter(filteredList) { _, _ -> }
        binding.rvUserOrders.layoutManager = LinearLayoutManager(this)
        binding.rvUserOrders.adapter = adapter
    }

    private fun setupSearch() {
        binding.etSearchOrders.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { applyFilter() }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun selectTabByTag(tag: String) {
        for (i in 0 until binding.tabLayoutOrders.tabCount) {
            if (binding.tabLayoutOrders.getTabAt(i)?.tag == tag) {
                binding.tabLayoutOrders.getTabAt(i)?.select()
                break
            }
        }
    }

    private fun loadOrdersFromFirebase() {
        val currentUser = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
            .getString("USERNAME", "") ?: ""

        if (currentUser.isEmpty()) return

        // CHỈNH SỬA TẠI ĐÂY: Truy vấn từ danh sách phẳng dựa vào trường "username"
        FirebaseDatabase.getInstance(databaseUrl).getReference("Orders")
            .orderByChild("username").equalTo(currentUser)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    allOrders.clear()
                    for (ds in snapshot.children) {
                        val order = ds.getValue(Order::class.java)
                        order?.let { allOrders.add(it) }
                    }
                    allOrders.sortByDescending { it.createdAt }
                    applyFilter()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun applyFilter() {
        val selectedTag = binding.tabLayoutOrders.getTabAt(binding.tabLayoutOrders.selectedTabPosition)?.tag?.toString() ?: "all"
        val query = binding.etSearchOrders.text.toString().trim().lowercase()

        filteredList.clear()
        for (order in allOrders) {
            val matchesTab = selectedTag == "all" || order.status == selectedTag
            val matchesSearch = order.orderId.lowercase().contains(query) ||
                    order.orderItems.any { it.tenSach.lowercase().contains(query) }

            if (matchesTab && matchesSearch) {
                filteredList.add(order)
            }
        }
        adapter.notifyDataSetChanged()
    }
}