package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.media3.common.util.UnstableApi

import com.example.quanlysach.R
import com.example.quanlysach.data.model.Book
import com.example.quanlysach.data.model.Video
import com.example.quanlysach.ui.adapter.BookAdapter
import com.example.quanlysach.ui.adapter.VideoThumbnailAdapter
import com.example.quanlysach.ui.bottomsheet.MoreProfileOptionsSheet

import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import dagger.hilt.android.AndroidEntryPoint
import java.util.Locale

@AndroidEntryPoint
@UnstableApi
class StreamerProfileActivity : AppCompatActivity() {

    private lateinit var bookAdapter: BookAdapter
    private lateinit var videoAdapter: VideoThumbnailAdapter
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private lateinit var tvTotalVideos: TextView
    private lateinit var tvTotalProducts: TextView
    private lateinit var tvTotalLikes: TextView
    private lateinit var rvVideos: RecyclerView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_streamer_profile)

        val btnBack = findViewById<ImageButton>(R.id.btnBack)
        val btnMore = findViewById<ImageButton>(R.id.btnMoreProfile)
        val tabLayout = findViewById<TabLayout>(R.id.tabLayout)
        val layoutEmpty = findViewById<LinearLayout>(R.id.layoutEmpty)
        val tabContent = findViewById<FrameLayout>(R.id.tabContent)
        val tvName = findViewById<TextView>(R.id.tvProfileName)

        tvTotalVideos = findViewById(R.id.tvTotalVideos)
        tvTotalProducts = findViewById(R.id.tvTotalProducts)
        tvTotalLikes = findViewById(R.id.tvTotalLikes)

        val name = intent.getStringExtra("STREAMER_NAME") ?: "BookShop Live"
        tvName.text = name

        // Khởi tạo Adapter cho tab Sản phẩm
        bookAdapter = BookAdapter { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("BOOK_ID", book.id)
            startActivity(intent)
        }

        val rvBooks = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@StreamerProfileActivity, 2)
            adapter = bookAdapter
            setPadding(16, 16, 16, 100)
            clipToPadding = false
            visibility = View.GONE
        }

        videoAdapter = VideoThumbnailAdapter(emptyList()) { video ->
            val intent = Intent(this, LiveStreamActivity::class.java).apply {
                putExtra("videoId", video.id)
            }
            startActivity(intent)
        }

        rvVideos = RecyclerView(this).apply {
            layoutManager = GridLayoutManager(this@StreamerProfileActivity, 3)
            adapter = videoAdapter
            setPadding(2, 2, 2, 100)
            clipToPadding = false
            visibility = View.VISIBLE
        }

        setupProfileText(name)
        loadRealCounterData()

        // Tab switch logic
        tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                when (tab?.position) {
                    0 -> {
                        layoutEmpty.visibility = View.GONE
                        rvVideos.visibility = View.VISIBLE
                        rvBooks.visibility = View.GONE
                    }
                    1 -> {
                        layoutEmpty.visibility = View.GONE
                        rvVideos.visibility = View.GONE
                        rvBooks.visibility = View.VISIBLE
                    }
                }
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        tabContent.addView(rvVideos)
        tabContent.addView(rvBooks)

        btnBack.setOnClickListener { finish() }
        btnMore.setOnClickListener {
            MoreProfileOptionsSheet().show(supportFragmentManager, "MoreProfileOptions")
        }
    }

    private fun setupProfileText(name: String) {
        val bios = listOf(
            "Cảm ơn các bạn đã ủng hộ mình nha ✨",
            "Sách là người bạn tốt nhất 📚",
            "Mỗi cuốn sách là một chuyến phiêu lưu 🌟",
            "Chia sẻ tình yêu sách mỗi ngày 💕"
        )
        findViewById<TextView>(R.id.tvProfileUsername).text = String.format("ID: %s", name.replace(" ", "_").lowercase(Locale.ROOT))
        findViewById<TextView>(R.id.tvProfileBio).text = bios.first()
    }

    private fun loadRealCounterData() {
        val database = FirebaseDatabase.getInstance(dbUrl)

        // 1. Đếm số lượng video và tổng like
        database.getReference("Videos").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val videoCount = snapshot.childrenCount
                var totalLikes = 0L

                for (child in snapshot.children) {
                    val likes = child.child("likeCount").getValue(Long::class.java) ?: 0L
                    totalLikes += likes
                }

                tvTotalVideos.text = formatCount(videoCount)
                tvTotalLikes.text = formatCount(totalLikes)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // 2. Đếm số sản phẩm từ node "Books"
        database.getReference("Books").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val productCount = snapshot.childrenCount
                tvTotalProducts.text = formatCount(productCount)

                val books = mutableListOf<Book>()
                for (child in snapshot.children) {
                    try {
                        val b = child.getValue(Book::class.java)
                        if (b != null) {
                            books.add(b.copy(id = child.key ?: ""))
                        }
                    } catch (e: Exception) {
                        android.util.Log.e("StreamerProfile", "Lỗi convert sách: ${e.message}")
                    }
                }
                bookAdapter.setData(books)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        loadStreamerVideos()
    }

    private fun loadStreamerVideos() {
        FirebaseDatabase.getInstance(dbUrl).getReference("Videos")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val videos = mutableListOf<Video>()
                    for (child in snapshot.children) {
                        // Sửa lỗi bóc tách bằng hàm ép kiểu Firebase chuẩn xác
                        val v = child.getValue(Video::class.java)
                        if (v != null) {
                            videos.add(v.copy(id = child.key ?: ""))
                        }
                    }

                    val layoutEmpty = findViewById<LinearLayout>(R.id.layoutEmpty)
                    if (videos.isEmpty()) {
                        layoutEmpty.visibility = View.VISIBLE
                        rvVideos.visibility = View.GONE
                    } else {
                        layoutEmpty.visibility = View.GONE
                        // Gán lại dữ liệu mới vào Adapter đã khai báo ngầm để tái sử dụng mà không cần tạo mới view rác
                        videoAdapter = VideoThumbnailAdapter(videos) { video ->
                            startActivity(Intent(this@StreamerProfileActivity, LiveStreamActivity::class.java).apply {
                                putExtra("videoId", video.id)
                            })
                        }
                        rvVideos.adapter = videoAdapter

                        // Đảm bảo hiển thị đúng Tab Video khi có dữ liệu
                        if (findViewById<TabLayout>(R.id.tabLayout).selectedTabPosition == 0) {
                            rvVideos.visibility = View.VISIBLE
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun formatCount(count: Long): String {
        return if (count >= 1000) String.format(Locale.getDefault(), "%.1fK", count / 1000.0) else count.toString()
    }
}