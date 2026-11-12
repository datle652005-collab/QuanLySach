package com.example.quanlysach.ui.activity

import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Review
import com.example.quanlysach.ui.adapter.ReviewAdapter
import com.google.firebase.database.*

class AllReviewsActivity : AppCompatActivity() {

    private lateinit var adapter: ReviewAdapter

    // Dùng 2 list để xử lý bộ lọc
    private val fullReviewList = mutableListOf<Review>() // Chứa tất cả data gốc
    private val displayList = mutableListOf<Review>()    // Chứa data đang hiển thị lên màn hình

    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    // Danh sách các nút lọc
    private lateinit var filterButtons: List<TextView>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_all_reviews)

        val bookId = intent.getStringExtra("BOOK_ID") ?: ""

        // Xử lý nút quay lại
        findViewById<ImageView>(R.id.btnBackAllReviews).setOnClickListener { finish() }

        // Setup RecyclerView
        val rv = findViewById<RecyclerView>(R.id.rvAllReviews)
        rv.layoutManager = LinearLayoutManager(this)

        // Khởi tạo Adapter với displayList
        adapter = ReviewAdapter(displayList)
        rv.adapter = adapter

        initFilters()

        if (bookId.isNotEmpty()) {
            loadFullReviews(bookId)
        }
    }

    private fun initFilters() {
        val btnAll = findViewById<TextView>(R.id.btnFilterAll)
        val btn5 = findViewById<TextView>(R.id.btnFilter5)
        val btn4 = findViewById<TextView>(R.id.btnFilter4)
        val btn3 = findViewById<TextView>(R.id.btnFilter3)
        val btn2 = findViewById<TextView>(R.id.btnFilter2)
        val btn1 = findViewById<TextView>(R.id.btnFilter1)

        filterButtons = listOf(btnAll, btn5, btn4, btn3, btn2, btn1)

        btnAll.setOnClickListener { setActiveFilter(it as TextView); applyFilter("ALL") }
        btn5.setOnClickListener { setActiveFilter(it as TextView); applyFilter("5") }
        btn4.setOnClickListener { setActiveFilter(it as TextView); applyFilter("4") }
        btn3.setOnClickListener { setActiveFilter(it as TextView); applyFilter("3") }
        btn2.setOnClickListener { setActiveFilter(it as TextView); applyFilter("2") }
        btn1.setOnClickListener { setActiveFilter(it as TextView); applyFilter("1") }
    }

    // Đổi màu và đổi bo góc nút lọc khi click vào
    private fun setActiveFilter(selectedBtn: TextView) {
        for (btn in filterButtons) {
            // Đặt lại các nút chưa chọn thành nền hồng nhạt, chữ hồng đậm
            btn.setBackgroundResource(R.drawable.bg_filter_unselected)
            btn.setTextColor(Color.parseColor("#FF3366"))
        }
        // Nút được chọn sẽ thành nền hồng đậm, chữ trắng
        selectedBtn.setBackgroundResource(R.drawable.bg_filter_selected)
        selectedBtn.setTextColor(Color.WHITE)
    }

    // Logic lọc danh sách
    private fun applyFilter(filterType: String) {
        displayList.clear()

        when (filterType) {
            "ALL" -> displayList.addAll(fullReviewList)
            "5" -> displayList.addAll(fullReviewList.filter { it.rating == 5 })
            "4" -> displayList.addAll(fullReviewList.filter { it.rating == 4 })
            "3" -> displayList.addAll(fullReviewList.filter { it.rating == 3 })
            "2" -> displayList.addAll(fullReviewList.filter { it.rating == 2 })
            "1" -> displayList.addAll(fullReviewList.filter { it.rating == 1 })
        }

        adapter.notifyDataSetChanged()
    }

    private fun loadFullReviews(bookId: String) {
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("Reviews").child(bookId)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                fullReviewList.clear()
                for (ds in snapshot.children) {
                    val review = ds.getValue(Review::class.java)
                    if (review != null) {
                        fullReviewList.add(review)
                    }
                }
                fullReviewList.reverse() // Đảo ngược để thấy đánh giá mới nhất trên cùng

                // Mặc định gọi bộ lọc "Tất cả" sau khi load xong data
                applyFilter("ALL")

                findViewById<TextView>(R.id.tvTitleAllReviews).text = "Đánh giá (${fullReviewList.size})"
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    // HÀM HIỂN THỊ ẢNH PHÓNG TO (Gọi từ ReviewAdapter)
    fun showZoomedImage(imageUrl: String) {
        val dialog = Dialog(this)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val imageView = ImageView(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(Color.BLACK)
        }

        Glide.with(this).load(imageUrl).into(imageView)

        imageView.setOnClickListener { dialog.dismiss() }

        dialog.setContentView(imageView)
        dialog.window?.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.BLACK))
        dialog.show()
    }
}