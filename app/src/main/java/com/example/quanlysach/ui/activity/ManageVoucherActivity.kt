package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Voucher
import com.example.quanlysach.ui.adapter.VoucherManageAdapter
import com.google.android.material.chip.ChipGroup
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*

class ManageVoucherActivity : AppCompatActivity() {

    private lateinit var rvVouchers: RecyclerView
    private lateinit var fabAddVoucher: ExtendedFloatingActionButton
    private lateinit var tabLayout: TabLayout
    private lateinit var chipGroupType: ChipGroup
    private lateinit var ivBack: ImageView
    private lateinit var swipeRefresh: SwipeRefreshLayout
    private lateinit var layoutEmpty: LinearLayout
    private lateinit var layoutLoading: LinearLayout
    private lateinit var tvVoucherCount: TextView

    private lateinit var voucherAdapter: VoucherManageAdapter

    // FULL DATA
    private val fullList = mutableListOf<Voucher>()

    // DISPLAY DATA
    private val displayList = mutableListOf<Voucher>()

    // CHIP CURRENT
    private var currentChipCheckedId = R.id.chipAll

    // FIREBASE
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Vouchers")

    private var voucherListener: ValueEventListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_voucher)

        initViews()
        setupRecyclerView()
        setupEvents()
        loadDataFromFirebase()
    }

    private fun initViews() {
        rvVouchers = findViewById(R.id.rvVouchers)
        fabAddVoucher = findViewById(R.id.fabAddVoucher)
        tabLayout = findViewById(R.id.tabLayoutVoucher)
        chipGroupType = findViewById(R.id.chipGroupType)
        ivBack = findViewById(R.id.ivBackVoucher)
        swipeRefresh = findViewById(R.id.swipeRefresh)
        layoutEmpty = findViewById(R.id.layoutEmpty)
        layoutLoading = findViewById(R.id.layoutLoading)
        tvVoucherCount = findViewById(R.id.txtVoucherCount)
    }

    private fun setupRecyclerView() {
        voucherAdapter = VoucherManageAdapter(
            list = displayList,
            onVoucherClick = { voucher ->
                val intent = Intent(this, AddVoucherActivity::class.java)
                intent.putExtra("VOUCHER_EDIT", voucher)
                startActivity(intent)
            },
            onToggle = { voucher, active ->
                if (voucher.id.isEmpty()) {
                    Toast.makeText(this, "Voucher không hợp lệ", Toast.LENGTH_SHORT).show()
                    return@VoucherManageAdapter
                }

                // ========================================================
                // 1. CẬP NHẬT TRẠNG THÁI LÊN FIREBASE (Đã sửa thành "active")
                // ========================================================
                dbRef.child(voucher.id)
                    .child("active")
                    .setValue(active)
                    .addOnSuccessListener {
                        voucher.active = active
                        filterVouchers()
                        Toast.makeText(this, if (active) "Đã bật voucher" else "Đã tắt voucher", Toast.LENGTH_SHORT).show()
                    }
                    .addOnFailureListener {
                        Toast.makeText(this, "Cập nhật trạng thái thất bại", Toast.LENGTH_SHORT).show()
                    }

                // ========================================================
                // 2. LOGIC TẮT MÃ -> GỠ TEM GIẢM GIÁ NGOÀI TRANG CHỦ
                // ========================================================
                if (!active && voucher.loaiVoucher == "GIAM_GIA") {
                    val booksRef = FirebaseDatabase.getInstance(dbUrl).getReference("Books")

                    when (voucher.applyTo.uppercase()) {
                        "BOOK" -> {
                            if (voucher.applyTargetId.isNotEmpty()) {
                                booksRef.child(voucher.applyTargetId).child("giaGiam").setValue(0)
                            }
                        }
                        "CATEGORY" -> {
                            if (voucher.applyTargetName.isNotEmpty()) {
                                val categoryName = voucher.applyTargetName.replace("Thể loại:", "").trim()
                                booksRef.orderByChild("theLoai").equalTo(categoryName)
                                    .addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(snapshot: DataSnapshot) {
                                            for (ds in snapshot.children) {
                                                ds.ref.child("giaGiam").setValue(0)
                                            }
                                        }
                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                            }
                        }
                        "ALL" -> {
                            booksRef.addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    for (ds in snapshot.children) {
                                        ds.ref.child("giaGiam").setValue(0)
                                    }
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })
                        }
                    }
                }
            }
        )

        rvVouchers.apply {
            layoutManager = LinearLayoutManager(this@ManageVoucherActivity)
            adapter = voucherAdapter
            setHasFixedSize(true)
        }
    }

    private fun setupEvents() {
        // ADD VOUCHER
        fabAddVoucher.setOnClickListener {
            startActivity(Intent(this, AddVoucherActivity::class.java))
        }

        // BACK
        ivBack.setOnClickListener {
            finish()
        }

        // REFRESH
        swipeRefresh.setColorSchemeResources(
            android.R.color.holo_blue_bright,
            android.R.color.holo_green_dark
        )

        swipeRefresh.setOnRefreshListener {
            loadDataFromFirebase()
        }

        // TAB FILTER
        tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    filterVouchers()
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            }
        )

        // CHIP FILTER
        chipGroupType.setOnCheckedStateChangeListener { _, checkedIds ->
            if (checkedIds.isNotEmpty()) {
                currentChipCheckedId = checkedIds.first()
                filterVouchers()
            }
        }
    }

    private fun loadDataFromFirebase() {
        showLoading(true)
        swipeRefresh.isRefreshing = true

        // REMOVE OLD LISTENER
        voucherListener?.let {
            dbRef.removeEventListener(it)
        }

        voucherListener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullList.clear()

                for (ds in snapshot.children) {
                    val voucher = ds.getValue(Voucher::class.java)
                    voucher?.let {
                        // FIX ID
                        if (it.id.isEmpty()) {
                            it.id = ds.key ?: ""
                        }
                        fullList.add(it)
                    }
                }

                // SORT NEWEST
                fullList.sortByDescending { it.startDate }

                filterVouchers()
                swipeRefresh.isRefreshing = false
                showLoading(false)
            }

            override fun onCancelled(error: DatabaseError) {
                swipeRefresh.isRefreshing = false
                showLoading(false)
                Toast.makeText(this@ManageVoucherActivity, "Lỗi tải dữ liệu: ${error.message}", Toast.LENGTH_SHORT).show()
            }
        }

        dbRef.addValueEventListener(voucherListener!!)
    }

    private fun filterVouchers() {
        val now = System.currentTimeMillis()
        val tabPosition = tabLayout.selectedTabPosition

        // ===================================
        // FILTER TAB (Đã fix lỗi logic 0L - Không thời hạn)
        // ===================================
        var filteredList = when (tabPosition) {
            // 0: ĐANG HOẠT ĐỘNG
            0 -> {
                fullList.filter {
                    it.active &&
                            it.startDate <= now &&
                            (it.endDate == 0L || it.endDate >= now)
                }
            }
            // 1: SẮP TỚI
            1 -> {
                fullList.filter {
                    it.active &&
                            it.startDate > now
                }
            }
            // 2: HẾT HẠN HOẶC BỊ TẮT
            2 -> {
                fullList.filter {
                    !it.active ||
                            (it.endDate > 0L && it.endDate < now)
                }
            }
            else -> fullList
        }

        // ===================================
        // FILTER CHIP
        // ===================================
        filteredList = when (currentChipCheckedId) {
            R.id.chipDiscount -> {
                filteredList.filter { it.loaiVoucher == "GIAM_GIA" }
            }
            R.id.chipFreeship -> {
                filteredList.filter { it.loaiVoucher == "FREESHIP" }
            }
            else -> filteredList
        }

        // ===================================
        // UPDATE LIST
        // ===================================
        displayList.clear()
        displayList.addAll(filteredList)
        voucherAdapter.notifyDataSetChanged()

        updateEmptyState()
        updateVoucherCount()
    }

    private fun updateEmptyState() {
        val isEmpty = displayList.isEmpty()
        layoutEmpty.isVisible = isEmpty
        rvVouchers.isVisible = !isEmpty
    }

    private fun updateVoucherCount() {
        tvVoucherCount.text = "Tổng voucher: ${displayList.size}"
    }

    private fun showLoading(isLoading: Boolean) {
        layoutLoading.visibility = if (isLoading) View.VISIBLE else View.GONE
    }

    override fun onDestroy() {
        super.onDestroy()
        voucherListener?.let {
            dbRef.removeEventListener(it)
        }
    }
}