package com.example.quanlysach.ui.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.widget.NestedScrollView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Book
import com.example.quanlysach.data.model.CartItem
import com.example.quanlysach.data.model.Review
import com.example.quanlysach.data.model.Voucher
import com.example.quanlysach.ui.adapter.BookAdapter
import com.example.quanlysach.ui.adapter.ReviewAdapter
import com.example.quanlysach.ui.adapter.VoucherTicketAdapter
import com.google.firebase.database.*
import java.text.NumberFormat
import java.util.*

class BookDetailActivity : AppCompatActivity() {

    // =====================================================
    // FIREBASE
    // =====================================================

    private val dbUrl =
        "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private lateinit var dbRef: DatabaseReference

    // =====================================================
    // ADAPTER
    // =====================================================

    private lateinit var relatedAdapter: BookAdapter

    private lateinit var reviewAdapter: ReviewAdapter

    // =====================================================
    // VIEW
    // =====================================================

    private lateinit var mainScroll: NestedScrollView

    private lateinit var imgDetail: ImageView

    private lateinit var tvName: TextView

    private lateinit var tvPrice: TextView

    private lateinit var tvOldPrice: TextView

    private lateinit var tvCategory: TextView

    private lateinit var tvAuthor: TextView

    private lateinit var tvDescription: TextView

    private lateinit var tvSelectedVoucherDetail: TextView

    private lateinit var tvSeeAllReviews: TextView


    // =====================================================
    // DATA
    // =====================================================

    private val reviewList =
        mutableListOf<Review>()

    private var selectedVoucher: Voucher? =
        null

    // =====================================================
    // ON CREATE
    // =====================================================

    override fun onCreate(
        savedInstanceState: Bundle?
    ) {

        super.onCreate(savedInstanceState)

        setContentView(
            R.layout.activity_book_detail
        )

        val bookId =
            intent.getStringExtra("BOOK_ID")
                ?: ""

        dbRef =
            FirebaseDatabase.getInstance(dbUrl)
                .getReference("Books")

        initViews()

        initRecyclerViews()

        loadBookData(bookId)

        loadReviews(bookId)
    }

    override fun onResume() {

        super.onResume()

        updateCartBadge()
    }

    // =====================================================
    // INIT VIEW
    // =====================================================

    private fun initViews() {

        mainScroll =
            findViewById(R.id.main_scroll)

        imgDetail =
            findViewById(R.id.imgDetailBook)

        tvName =
            findViewById(R.id.tvDetailName)

        tvPrice =
            findViewById(R.id.tvDetailPrice)

        tvOldPrice =
            findViewById(R.id.tvOldPrice)

        tvCategory =
            findViewById(R.id.tvDetailCategory)

        tvAuthor =
            findViewById(R.id.tvDetailAuthor)

        tvDescription =
            findViewById(R.id.tvDetailDescription)



        tvSeeAllReviews =
            findViewById(R.id.tvSeeAllReviews)

        findViewById<ImageView>(
            R.id.btnBackDetail
        ).setOnClickListener {

            finish()
        }

        findViewById<FrameLayout>(
            R.id.btnCartDetail
        ).setOnClickListener {

            startActivity(
                Intent(
                    this,
                    CartActivity::class.java
                )
            )
        }
        findViewById<LinearLayout>(R.id.btnChatShop).setOnClickListener {

            val intent = Intent(
                this,
                ConsultChatActivity::class.java
            )

            startActivity(intent)
        }

    }

    // =====================================================
    // RECYCLER VIEW
    // =====================================================

    private fun initRecyclerViews() {

        // =========================
        // RELATED BOOKS
        // =========================

        val rvRelated =
            findViewById<RecyclerView>(
                R.id.rvRelatedBooks
            )

        rvRelated.layoutManager =
            GridLayoutManager(this, 2)

        relatedAdapter =
            BookAdapter { selectedBook ->

                val intent = Intent(
                    this,
                    BookDetailActivity::class.java
                )

                intent.putExtra(
                    "BOOK_ID",
                    selectedBook.id
                )

                startActivity(intent)

                finish()
            }

        rvRelated.adapter =
            relatedAdapter

        // =========================
        // REVIEWS
        // =========================

        val rvReviews =
            findViewById<RecyclerView>(
                R.id.rvReviews
            )

        rvReviews.layoutManager =
            LinearLayoutManager(this)

        reviewAdapter =
            ReviewAdapter(reviewList)

        rvReviews.adapter =
            reviewAdapter
    }

    // =====================================================
    // LOAD BOOK
    // =====================================================

    private fun loadBookData(
        id: String
    ) {

        dbRef.child(id)
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val book =
                            snapshot.getValue(
                                Book::class.java
                            ) ?: return

                        val bookWithId =
                            book.copy(id = id)

                        tvName.text =
                            book.tenSach

                        tvAuthor.text =
                            "Tác giả: ${book.tacGia}"

                        tvCategory.text =
                            book.theLoai

                        tvDescription.text =
                            book.moTa

                        // =========================
                        // PRICE
                        // =========================

                        if (
                            book.giaGiam > 0 &&
                            book.giaGiam < book.gia
                        ) {

                            tvPrice.text =
                                formatPrice(
                                    book.giaGiam.toDouble()
                                )

                            tvOldPrice.visibility =
                                View.VISIBLE

                            tvOldPrice.text =
                                formatPrice(
                                    book.gia.toDouble()
                                )

                            tvOldPrice.paintFlags =
                                tvOldPrice.paintFlags or
                                        Paint.STRIKE_THRU_TEXT_FLAG

                        } else {

                            tvPrice.text =
                                formatPrice(
                                    book.gia.toDouble()
                                )

                            tvOldPrice.visibility =
                                View.GONE
                        }

                        // =========================
                        // IMAGE
                        // =========================

                        Glide.with(
                            this@BookDetailActivity
                        )
                            .load(book.hinhAnh)
                            .into(imgDetail)

                        // =========================
                        // OTHER
                        // =========================

                        loadAvailableVouchers(
                            bookWithId
                        )

                        handleCartAndBuyLogic(
                            bookWithId
                        )

                        loadRelatedBooks(
                            book.theLoai,
                            id
                        )
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                    }
                }
            )
    }

    // =====================================================
    // LOAD REVIEWS
    // =====================================================

    private fun loadReviews(
        bookId: String
    ) {

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Reviews")
            .child(bookId)
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val fullList =
                            mutableListOf<Review>()

                        var totalStars = 0f

                        for (ds in snapshot.children) {

                            val review =
                                ds.getValue(
                                    Review::class.java
                                )

                            if (review != null) {

                                fullList.add(review)

                                totalStars +=
                                    review.rating
                            }
                        }

                        val count =
                            fullList.size

                        if (count > 0) {

                            val average =
                                totalStars / count

                            findViewById<TextView>(
                                R.id.tvAverageRating
                            ).text =
                                String.format(
                                    "%.1f",
                                    average
                                )

                            findViewById<RatingBar>(
                                R.id.rbAverageDetail
                            ).rating =
                                average

                            findViewById<TextView>(
                                R.id.tvTotalReviewsCount
                            ).text =
                                "$count đánh giá"

                            val sortedList =
                                fullList.sortedByDescending {
                                    it.timestamp
                                }

                            val previewList =
                                if (count > 3)
                                    sortedList.take(3)
                                else
                                    sortedList

                            reviewAdapter =
                                ReviewAdapter(previewList)

                            findViewById<RecyclerView>(
                                R.id.rvReviews
                            ).adapter =
                                reviewAdapter

                            if (count > 3) {

                                tvSeeAllReviews.visibility =
                                    View.VISIBLE

                                tvSeeAllReviews.setOnClickListener {

                                    val intent =
                                        Intent(
                                            this@BookDetailActivity,
                                            AllReviewsActivity::class.java
                                        )

                                    intent.putExtra(
                                        "BOOK_ID",
                                        bookId
                                    )

                                    startActivity(intent)
                                }

                            } else {

                                tvSeeAllReviews.visibility =
                                    View.GONE
                            }

                        } else {

                            tvSeeAllReviews.visibility =
                                View.GONE

                            findViewById<TextView>(
                                R.id.tvTotalReviewsCount
                            ).text =
                                "Chưa có đánh giá"
                        }
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                    }
                }
            )
    }

    // =====================================================
    // FORMAT PRICE
    // =====================================================

    private fun formatPrice(
        price: Double
    ): String {

        return NumberFormat
            .getInstance(
                Locale("vi", "VN")
            )
            .format(price) + "đ"
    }

    // =====================================================
    // AUTO APPLY BEST VOUCHER
    // =====================================================

    private fun loadAvailableVouchers(book: Book) {
        val rvVouchers = findViewById<RecyclerView>(R.id.rvShopVouchers)
        rvVouchers.layoutManager = LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false)

        FirebaseDatabase.getInstance(dbUrl).getReference("Vouchers")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val validVouchers = mutableListOf<Voucher>()
                    val now = System.currentTimeMillis()
                    val bookPrice = if (book.giaGiam > 0) book.giaGiam.toDouble() else book.gia.toDouble()

                    for (ds in snapshot.children) {
                        val voucher = ds.getValue(Voucher::class.java) ?: continue

                        // Kiểm tra các điều kiện (Ngày, Active, Lượt dùng, Min Order, Apply To...)
                        if (!voucher.active || now < voucher.startDate || now > voucher.endDate) continue
                        if (voucher.maxUsage > 0 && voucher.usedCount >= voucher.maxUsage) continue
                        if (bookPrice < voucher.minOrderValue) continue

                        val canApply = when (voucher.applyTo) {
                            "ALL" -> true
                            "BOOK" -> voucher.applyTargetId == book.id
                            "CATEGORY" -> voucher.applyTargetId == book.theLoai
                            else -> false
                        }

                        if (canApply) {
                            validVouchers.add(voucher)
                        }
                    }

                    // Nạp data vào RecyclerView
                    if (validVouchers.isNotEmpty()) {
                        val ticketAdapter = VoucherTicketAdapter(validVouchers)
                        rvVouchers.adapter = ticketAdapter
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // =====================================================
    // CART + BUY
    // =====================================================

    private fun handleCartAndBuyLogic(
        book: Book
    ) {

        val btnAdd =
            findViewById<Button>(
                R.id.btnAddToCart
            )

        val btnBuy =
            findViewById<Button>(
                R.id.btnBuyNow
            )

        val actualPrice =
            if (book.giaGiam > 0)
                book.giaGiam
            else
                book.gia

        val pref =
            getSharedPreferences(
                "AppBanSachPrefs",
                MODE_PRIVATE
            )

        val username =
            pref.getString(
                "USERNAME",
                ""
            ) ?: ""

        val cartRef =
            FirebaseDatabase.getInstance(dbUrl)
                .getReference("Carts")
                .child(username)

        // =========================
        // ADD TO CART
        // =========================

        btnAdd.setOnClickListener {

            if (username.isEmpty()) {

                toast("Vui lòng đăng nhập")

                return@setOnClickListener
            }

            if (book.soLuong <= 0) {

                toast("Sách đã hết hàng")

                return@setOnClickListener
            }

            cartRef.child(book.id)
                .addListenerForSingleValueEvent(
                    object : ValueEventListener {

                        override fun onDataChange(
                            snapshot: DataSnapshot
                        ) {

                            val currentQty =
                                snapshot.child("soLuong")
                                    .getValue(Int::class.java)
                                    ?: 0

                            if (
                                currentQty <
                                book.soLuong
                            ) {

                                if (snapshot.exists()) {

                                    cartRef.child(book.id)
                                        .child("soLuong")
                                        .setValue(currentQty + 1)

                                } else {

                                    val item = CartItem(
                                        bookId = book.id,
                                        tenSach = book.tenSach,
                                        gia = actualPrice,
                                        hinhAnh = book.hinhAnh,
                                        soLuong = 1,
                                        theLoai = book.theLoai // ✨ DÒNG QUAN TRỌNG NHẤT ĐÂY!
                                    )

                                    cartRef.child(book.id)
                                        .setValue(item)
                                }

                                toast("Đã thêm vào giỏ hàng")

                            } else {

                                toast(
                                    "Đã đạt tối đa số lượng tồn kho"
                                )
                            }
                        }

                        override fun onCancelled(
                            error: DatabaseError
                        ) {

                            toast("Lỗi kết nối mạng")
                        }
                    }
                )
        }

        // =========================
        // BUY NOW
        // =========================

        btnBuy.setOnClickListener {

            if (username.isEmpty()) {

                toast("Vui lòng đăng nhập")

                return@setOnClickListener
            }

            if (book.soLuong <= 0) {

                toast("Sách đã hết hàng")

                return@setOnClickListener
            }

            val item = CartItem(
                bookId = book.id,
                tenSach = book.tenSach,
                gia = actualPrice,
                hinhAnh = book.hinhAnh,
                soLuong = 1,
                theLoai = book.theLoai // ✨ DÒNG QUAN TRỌNG NHẤT ĐÂY!
            )

            val intent =
                Intent(
                    this,
                    CheckoutActivity::class.java
                ).apply {

                    putExtra(
                        "USERNAME",
                        username
                    )

                    putExtra(
                        "LIST_CHECKOUT",
                        arrayListOf(item)
                    )

                    selectedVoucher?.let {

                        putExtra(
                            "SELECTED_VOUCHER",
                            it
                        )
                    }
                }

            startActivity(intent)
        }
    }

    // =====================================================
    // RELATED BOOKS
    // =====================================================

    private fun loadRelatedBooks(
        category: String,
        currentId: String
    ) {

        dbRef.orderByChild("theLoai")
            .equalTo(category)
            .limitToFirst(6)
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        val list =
                            mutableListOf<Book>()

                        for (data in snapshot.children) {

                            val book =
                                data.getValue(
                                    Book::class.java
                                )

                            if (
                                book != null &&
                                data.key != currentId
                            ) {

                                list.add(
                                    book.copy(
                                        id = data.key ?: ""
                                    )
                                )
                            }
                        }

                        relatedAdapter.setData(list)
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                    }
                }
            )
    }

    // =====================================================
    // CART BADGE
    // =====================================================

    private fun updateCartBadge() {

        val pref =
            getSharedPreferences(
                "AppBanSachPrefs",
                MODE_PRIVATE
            )

        val username =
            pref.getString(
                "USERNAME",
                ""
            ) ?: ""

        val tvBadge =
            findViewById<TextView>(
                R.id.tvCartBadgeDetail
            )

        if (username.isEmpty()) {

            tvBadge.visibility =
                View.GONE

            return
        }

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Carts")
            .child(username)
            .addValueEventListener(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        var count = 0

                        for (ds in snapshot.children) {

                            count +=
                                ds.child("soLuong")
                                    .getValue(Int::class.java)
                                    ?: 0
                        }

                        tvBadge.visibility =
                            if (count > 0)
                                View.VISIBLE
                            else
                                View.GONE

                        tvBadge.text =
                            count.toString()
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {

                    }
                }
            )
    }

    // =====================================================
    // TOAST
    // =====================================================

    private fun toast(
        message: String
    ) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }
}