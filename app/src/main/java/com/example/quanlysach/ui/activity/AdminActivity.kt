package com.example.quanlysach.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Order
import com.example.quanlysach.ui.adapter.OrderAdapter
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.card.MaterialCardView
import com.google.firebase.database.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class AdminActivity : AppCompatActivity() {

    private lateinit var tvTotalRevenue: TextView
    private lateinit var tvTotalOrders: TextView
    private lateinit var tvTotalUsers: TextView
    private lateinit var tvTotalBooks: TextView
    private lateinit var lineChart: LineChart

    private lateinit var tvAdminChatBadge: TextView
    private lateinit var tvOrderBadge: TextView
    private lateinit var tvReviewBadge: TextView

    private lateinit var database: DatabaseReference
    private lateinit var rvRecentOrders: RecyclerView
    private var recentOrdersList = mutableListOf<Order>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin)

        val firebaseUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
        database = FirebaseDatabase.getInstance(firebaseUrl).reference

        initViews()
        setupRecyclerView()
        loadDataFromFirebase()
        loadRecentOrders()
        setupNavigation()

        // Load các loại thông báo
        loadAdminChatBadge()
        loadAdminNotifications()
    }

    private fun initViews() {
        tvTotalRevenue = findViewById(R.id.tvTotalRevenue)
        tvTotalOrders = findViewById(R.id.tvTotalOrders)
        tvTotalUsers = findViewById(R.id.tvTotalUsers)
        tvTotalBooks = findViewById(R.id.tvTotalBooks)
        lineChart = findViewById(R.id.lineChartRevenue)
        rvRecentOrders = findViewById(R.id.rvRecentOrders)

        // Các Badge
        tvAdminChatBadge = findViewById(R.id.tvAdminChatBadge)
        tvOrderBadge = findViewById(R.id.tvOrderBadge)
        tvReviewBadge = findViewById(R.id.tvReviewBadge)
    }

    private fun loadAdminNotifications() {
        val notiRef = database.child("AdminNotifications")

        // Lắng nghe đơn hàng mới
        notiRef.child("newOrders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                updateBadge(tvOrderBadge, count)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Lắng nghe đánh giá mới
        notiRef.child("newReviews").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                updateBadge(tvReviewBadge, count)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateBadge(view: TextView, count: Int) {
        if (count > 0) {
            view.visibility = View.VISIBLE
            view.text = if (count > 99) "99+" else count.toString()
        } else {
            view.visibility = View.GONE
        }
    }

    private fun loadAdminChatBadge() {
        val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        val adminUsername = pref.getString("USERNAME", "admin") ?: "admin"

        database.child("UnreadChatCounters").child(adminUsername)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val totalUnread = snapshot.getValue(Int::class.java) ?: 0
                    if (totalUnread > 0) {
                        tvAdminChatBadge.visibility = View.VISIBLE
                        tvAdminChatBadge.text = if (totalUnread > 99) "99+" else totalUnread.toString()
                    } else {
                        tvAdminChatBadge.visibility = View.GONE
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun setupRecyclerView() {
        rvRecentOrders.layoutManager = LinearLayoutManager(this)
        rvRecentOrders.adapter = OrderAdapter(recentOrdersList) { order ->
            val intent = Intent(this, OrderDetailActivity::class.java)
            intent.putExtra("ORDER_DATA", order)
            startActivity(intent)
        }
    }

    private fun loadRecentOrders() {
        database.child("Orders").limitToLast(5)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    recentOrdersList.clear()
                    for (node in snapshot.children) {
                        val order = node.getValue(Order::class.java)
                        order?.let { recentOrdersList.add(0, it) }
                    }
                    rvRecentOrders.adapter?.notifyDataSetChanged()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun loadDataFromFirebase() {
        database.child("Books").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tvTotalBooks.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("Users").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                tvTotalUsers.text = snapshot.childrenCount.toString()
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        database.child("Orders").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRev = 0.0
                tvTotalOrders.text = snapshot.childrenCount.toString()

                for (order in snapshot.children) {
                    val status = order.child("status").getValue(String::class.java) ?: ""
                    if (status == "Thành công" || status == "thanh_cong") {
                        val price = order.child("finalTotal").getValue(Double::class.java) ?: 0.0
                        totalRev += price
                    }
                }
                val formatter = DecimalFormat("#,### đ")
                tvTotalRevenue.text = formatter.format(totalRev)
                updateChartWithRealData(snapshot)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateChartWithRealData(snapshot: DataSnapshot) {
        val entries = ArrayList<Entry>()
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val last7DaysList = ArrayList<String>()
        val revenueMap = mutableMapOf<String, Float>()
        val calendar = Calendar.getInstance()

        for (i in 0 until 7) {
            val dateStr = sdf.format(calendar.time)
            last7DaysList.add(0, dateStr)
            revenueMap[dateStr] = 0f
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        for (orderNode in snapshot.children) {
            val status = orderNode.child("status").getValue(String::class.java) ?: ""
            if (status == "Thành công" || status == "thanh_cong") {
                val price = orderNode.child("finalTotal").getValue(Double::class.java) ?: 0.0
                val timestamp = orderNode.child("createdAt").getValue(Long::class.java) ?: 0L
                if (timestamp > 0) {
                    val orderDateStr = sdf.format(Date(timestamp))
                    if (revenueMap.containsKey(orderDateStr)) {
                        revenueMap[orderDateStr] = revenueMap[orderDateStr]!! + price.toFloat()
                    }
                }
            }
        }

        for (i in last7DaysList.indices) {
            val dayStr = last7DaysList[i]
            val amount = revenueMap[dayStr] ?: 0f
            entries.add(Entry(i.toFloat(), amount))
        }

        val dataSet = LineDataSet(entries, "Doanh thu 7 ngày")
        dataSet.color = Color.parseColor("#3B82F6")
        dataSet.setCircleColor(Color.parseColor("#3B82F6"))
        dataSet.circleRadius = 4f
        dataSet.lineWidth = 3f
        dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
        dataSet.setDrawFilled(true)
        dataSet.fillColor = Color.parseColor("#3B82F6")
        dataSet.fillAlpha = 30
        lineChart.data = LineData(dataSet)
        lineChart.xAxis.position = XAxis.XAxisPosition.BOTTOM
        lineChart.xAxis.setDrawGridLines(false)
        lineChart.axisRight.isEnabled = false
        lineChart.description.isEnabled = false
        lineChart.invalidate()
    }

    private fun setupNavigation() {
        findViewById<TextView>(R.id.tvSeeAllOrders).setOnClickListener {
            // Khi nhấn vào xem đơn hàng, reset badge đơn hàng
            database.child("AdminNotifications").child("newOrders").setValue(0)
            startActivity(Intent(this, ManageOrdersActivity::class.java))
        }

        findViewById<MaterialCardView>(R.id.cardBooks).setOnClickListener {
            // Khi nhấn vào quản lý sách, reset badge đánh giá
            database.child("AdminNotifications").child("newReviews").setValue(0)
            startActivity(Intent(this, ManageBooksActivity::class.java))
        }

        findViewById<ImageView>(R.id.btnAdminMenu).setOnClickListener { performLogout() }
        findViewById<MaterialCardView>(R.id.cardUsers).setOnClickListener { startActivity(Intent(this, ManageUsersActivity::class.java)) }
        findViewById<MaterialCardView>(R.id.cardOrders).setOnClickListener {
            database.child("AdminNotifications").child("newOrders").setValue(0)
            startActivity(Intent(this, ManageOrdersActivity::class.java))
        }
        findViewById<MaterialCardView>(R.id.cardManageOffers).setOnClickListener { startActivity(Intent(this, ManageVoucherActivity::class.java)) }
        findViewById<MaterialCardView>(R.id.cardRevenue).setOnClickListener { startActivity(Intent(this, RevenueActivity::class.java)) }
        findViewById<ImageView>(R.id.btnAdminChatList).setOnClickListener { startActivity(Intent(this, AdminChatListActivity::class.java)) }

        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationAdmin)
        bottomNav.selectedItemId = R.id.nav_admin_home
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> true
                R.id.nav_admin_books -> {
                    startActivity(Intent(this, ManageBooksActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_orders -> {
                    startActivity(Intent(this, ManageOrdersActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_users -> {
                    startActivity(Intent(this, ManageUsersActivity::class.java))
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

    private fun performLogout() {
        getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE).edit().clear().apply()
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}