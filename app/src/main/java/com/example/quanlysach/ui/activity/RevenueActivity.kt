package com.example.quanlysach.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Order
import com.example.quanlysach.ui.adapter.TopBookAdapter

import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.github.mikephil.charting.formatter.ValueFormatter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*
import kotlin.collections.ArrayList

// 🌟 ĐÃ SỬA: Khai báo chuẩn data class, đồng bộ kiểu dữ liệu với Adapter (Double thay vì Int)
data class TopBook(val name: String, val sales: Double, val imageUrl: String)

class RevenueActivity : AppCompatActivity() {

    private lateinit var tvRevenueValue: TextView
    private lateinit var tvOrderCount: TextView
    private lateinit var tvBooksSold: TextView
    private lateinit var barChart: BarChart
    private lateinit var btnBack: ImageView
    private lateinit var tabTime: TabLayout
    private lateinit var rvTopBooks: RecyclerView

    private val database = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
        .getReference("Orders")

    private var currentFilter = "month"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_revenue)

        initViews()
        setupEvents()
        setupBottomNav()
        loadRevenueData("month")
    }

    private fun initViews() {
        tvRevenueValue = findViewById(R.id.tvRevenueValue)
        tvOrderCount = findViewById(R.id.tvOrderCount)
        tvBooksSold = findViewById(R.id.tvBooksSold)
        barChart = findViewById(R.id.barChart)
        btnBack = findViewById(R.id.btnBack)
        tabTime = findViewById(R.id.tabTime)
        rvTopBooks = findViewById(R.id.rvTopBooks)

        rvTopBooks.layoutManager = LinearLayoutManager(this)
        tabTime.getTabAt(2)?.select()
    }

    private fun setupEvents() {
        btnBack.setOnClickListener { finish() }
        tabTime.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentFilter = when (tab?.position) {
                    0 -> "today"
                    1 -> "week"
                    2 -> "month"
                    else -> "month"
                }
                loadRevenueData(currentFilter)
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })
    }

    private fun loadRevenueData(filter: String) {
        database.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                var totalRev = 0.0
                var countOrders = 0
                var totalBooksSold = 0

                // 🌟 ĐÃ SỬA: Lưu doanh thu của từng cuốn sách theo kiểu Double để khớp Adapter
                val bookInfoMap = mutableMapOf<String, Pair<Double, String>>()
                val revenueMap = mutableMapOf<String, Float>()
                val currentTime = System.currentTimeMillis()
                val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())

                for (orderSnap in snapshot.children) {
                    val order = orderSnap.getValue(Order::class.java)

                    if (order != null) {
                        if (order.status == "thanh_cong" || order.status == "Thành công") {
                            val revenueDate = if (order.createdAt > 0L) order.createdAt else currentTime

                            if (isWithinFilter(revenueDate, currentTime, filter)) {
                                countOrders++
                                totalRev += order.finalTotal

                                order.orderItems.forEach { item ->
                                    val sl = item.soLuong
                                    val ten = item.tenSach ?: "Sách không tên"
                                    val anh = item.hinhAnh ?: ""
                                    totalBooksSold += sl

                                    val currentData = bookInfoMap[ten] ?: Pair(0.0, anh)
                                    // Cộng dồn số lượng sách đã bán (ép kiểu về Double)
                                    bookInfoMap[ten] = Pair(currentData.first + sl.toDouble(), anh)
                                }

                                val dateKey = sdf.format(Date(revenueDate))
                                revenueMap[dateKey] = (revenueMap[dateKey] ?: 0f) + order.finalTotal.toFloat()
                            }
                        }
                    } else {
                        // Xử lý trường hợp cấu trúc nhóm theo Username (Orders -> username -> orderId)
                        for (subSnap in orderSnap.children) {
                            val subOrder = subSnap.getValue(Order::class.java)
                            if (subOrder != null && (subOrder.status == "thanh_cong" || subOrder.status == "Thành công")) {
                                val revenueDate = if (subOrder.createdAt > 0L) subOrder.createdAt else currentTime

                                if (isWithinFilter(revenueDate, currentTime, filter)) {
                                    countOrders++
                                    totalRev += subOrder.finalTotal

                                    subOrder.orderItems.forEach { item ->
                                        val sl = item.soLuong
                                        val ten = item.tenSach ?: "Sách không tên"
                                        val anh = item.hinhAnh ?: ""
                                        totalBooksSold += sl

                                        val currentData = bookInfoMap[ten] ?: Pair(0.0, anh)
                                        bookInfoMap[ten] = Pair(currentData.first + sl.toDouble(), anh)
                                    }

                                    val dateKey = sdf.format(Date(revenueDate))
                                    revenueMap[dateKey] = (revenueMap[dateKey] ?: 0f) + subOrder.finalTotal.toFloat()
                                }
                            }
                        }
                    }
                }
                updateUI(totalRev, countOrders, totalBooksSold, bookInfoMap, revenueMap)
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun updateUI(rev: Double, orders: Int, books: Int, infoMap: Map<String, Pair<Double, String>>, chartMap: Map<String, Float>) {
        tvRevenueValue.text = DecimalFormat("#,### đ").format(rev)
        tvOrderCount.text = orders.toString()
        tvBooksSold.text = books.toString()

        // 🌟 ĐÃ SỬA: Ép kiểu dữ liệu tường minh, truyền đúng thuộc tính Double vào constructor của TopBook
        val top5Data: List<TopBook> = infoMap.toList()
            .sortedByDescending { it.second.first }
            .take(5)
            .map { pair -> TopBook(name = pair.first, sales = pair.second.first, imageUrl = pair.second.second) }

        rvTopBooks.adapter = TopBookAdapter(top5Data)
        updateBarChart(chartMap)
    }

    private fun getStartOfDay(time: Long): Long {
        val cal = Calendar.getInstance()
        cal.timeInMillis = time
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        return cal.timeInMillis
    }

    private fun isWithinFilter(time: Long, current: Long, filter: String): Boolean {
        val dayMillis = 24 * 60 * 60 * 1000L
        val startOfToday = getStartOfDay(current)

        return when (filter) {
            "today" -> time >= startOfToday
            "week" -> time >= (startOfToday - 6 * dayMillis)
            "month" -> time >= (startOfToday - 29 * dayMillis)
            else -> false
        }
    }

    private fun updateBarChart(realData: Map<String, Float>) {
        val entries = ArrayList<BarEntry>()
        val dateLabels = ArrayList<String>()
        val sdf = SimpleDateFormat("dd/MM", Locale.getDefault())
        val calendar = Calendar.getInstance()

        val loopCount = when (currentFilter) {
            "today" -> 1
            "week" -> 7
            "month" -> 30
            else -> 7
        }

        for (i in 0 until loopCount) {
            val dateStr = sdf.format(calendar.time)
            dateLabels.add(0, dateStr)
            calendar.add(Calendar.DAY_OF_YEAR, -1)
        }

        for (i in dateLabels.indices) {
            val label = dateLabels[i]
            val revenueAmount = realData[label] ?: 0f
            entries.add(BarEntry(i.toFloat(), revenueAmount))
        }

        if (entries.isEmpty()) {
            barChart.clear()
            return
        }

        val dataSet = BarDataSet(entries, "Doanh thu (VNĐ)")
        dataSet.color = Color.parseColor("#22C55E")
        dataSet.valueTextColor = Color.parseColor("#1E293B")
        dataSet.valueTextSize = 9f
        dataSet.valueFormatter = object : ValueFormatter() {
            override fun getFormattedValue(value: Float): String {
                return if (value >= 1000f) "${String.format(Locale.US, "%.1f", value / 1000f)}k" else value.toInt().toString()
            }
        }

        barChart.xAxis.apply {
            valueFormatter = IndexAxisValueFormatter(dateLabels)
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            isGranularityEnabled = true
            setDrawGridLines(false)
            labelRotationAngle = if (loopCount > 7) -45f else 0f
            labelCount = if (loopCount > 7) 10 else loopCount
        }

        barChart.apply {
            this.data = BarData(dataSet)
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.axisMinimum = 0f
            setFitBars(true)
            animateY(800)
            invalidate()
        }
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationAdmin)
        bottomNav.selectedItemId = R.id.nav_admin_reports
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> { startActivity(Intent(this, AdminActivity::class.java)); finish(); true }
                R.id.nav_admin_books -> { startActivity(Intent(this, ManageBooksActivity::class.java)); finish(); true }
                getItemId(bottomNav, R.id.nav_admin_users) -> { startActivity(Intent(this, ManageUsersActivity::class.java)); finish(); true }
                getItemId(bottomNav, R.id.nav_admin_orders) -> { startActivity(Intent(this, ManageOrdersActivity::class.java)); finish(); true }
                R.id.nav_admin_reports -> true
                else -> false
            }
        }
    }

    // Hàm phụ trợ tránh lỗi định danh nếu ID menu không khớp hoàn toàn
    private fun getItemId(bottomNav: BottomNavigationView, defaultId: Int): Int {
        val menu = bottomNav.menu
        for (i in 0 until menu.size()) {
            val item = menu.getItem(i)
            if (item.itemId == defaultId) return defaultId
        }
        return defaultId
    }
}