package com.example.quanlysach.ui.activity

import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.CartItem
import com.example.quanlysach.data.model.Voucher
import com.example.quanlysach.ui.adapter.CartAdapter
import com.example.quanlysach.ui.adapter.VoucherUserAdapter
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*

class CartActivity : AppCompatActivity() {

    // =====================================================
    // VIEW
    // =====================================================

    private lateinit var adapter: CartAdapter

    private lateinit var tvTotal: TextView
    private lateinit var btnCheckout: Button

    private lateinit var tvVoucherName: TextView
    private lateinit var tvDiscountAmount: TextView
    private lateinit var layoutSelectVoucher: LinearLayout

    private lateinit var tvFreeshipName: TextView
    private lateinit var tvFreeshipAmount: TextView
    private lateinit var layoutSelectFreeship: LinearLayout

    // =====================================================
    // DATA
    // =====================================================

    private val cartList = mutableListOf<CartItem>()

    private var selectedVoucher: Voucher? = null
    private var selectedFreeship: Voucher? = null

    private var currentSubtotal = 0.0

    private lateinit var username: String
    private lateinit var cartRef: DatabaseReference

    private val dbUrl =
        "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    // =====================================================
    // LIFECYCLE
    // =====================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_cart)

        initViews()

        setupUser()

        setupRecyclerView()

        setupEvents()

        loadCartData()
    }

    // =====================================================
    // INIT VIEW
    // =====================================================

    private fun initViews() {

        tvTotal =
            findViewById(R.id.tvTotalAmount)

        btnCheckout =
            findViewById(R.id.btnCheckout)

        tvVoucherName =
            findViewById(R.id.tvVoucherName)

        tvDiscountAmount =
            findViewById(R.id.tvDiscountAmount)

        layoutSelectVoucher =
            findViewById(R.id.layoutSelectVoucher)

        tvFreeshipName =
            findViewById(R.id.tvFreeshipName)

        tvFreeshipAmount =
            findViewById(R.id.tvFreeshipAmount)

        layoutSelectFreeship =
            findViewById(R.id.layoutSelectFreeship)

        findViewById<ImageView>(R.id.btnBackCart)
            .setOnClickListener {
                finish()
            }
    }

    // =====================================================
    // SETUP USER
    // =====================================================

    private fun setupUser() {

        val pref =
            getSharedPreferences(
                "AppBanSachPrefs",
                MODE_PRIVATE
            )

        username =
            pref.getString(
                "USERNAME",
                ""
            ) ?: ""

        cartRef =
            FirebaseDatabase
                .getInstance(dbUrl)
                .getReference("Carts")
                .child(username)
    }

    // =====================================================
    // RECYCLERVIEW
    // =====================================================

    private fun setupRecyclerView() {

        val rv =
            findViewById<RecyclerView>(R.id.rvCart)

        rv.layoutManager =
            LinearLayoutManager(this)

        adapter =
            CartAdapter(
                cartList,
                username
            ) {

                calculateTotal()
            }

        rv.adapter = adapter
    }

    // =====================================================
    // EVENTS
    // =====================================================

    private fun setupEvents() {

        layoutSelectVoucher.setOnClickListener {

            handleVoucherClick("GIAM_GIA")
        }

        layoutSelectFreeship.setOnClickListener {

            handleVoucherClick("FREESHIP")
        }

        btnCheckout.setOnClickListener {

            handleCheckout()
        }
    }

    // =====================================================
    // FORMAT
    // =====================================================

    private fun formatPrice(value: Double): String {

        return String.format("%,.0fđ", value)
    }

    // =====================================================
    // CLICK VOUCHER
    // =====================================================

    private fun handleVoucherClick(type: String) {

        val selectedItems =
            cartList.filter { it.isSelected }

        if (selectedItems.isEmpty()) {

            Toast.makeText(
                this,
                "Vui lòng chọn ít nhất 1 sản phẩm",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        showVoucherBottomSheet(type)
    }

    // =====================================================
    // TÍNH TIỀN
    // =====================================================

    private fun calculateTotal() {

        val selectedItems =
            cartList.filter { it.isSelected }

        currentSubtotal =
            selectedItems.sumOf {
                it.gia.toDouble() * it.soLuong
            }

        // =====================================================
        // KHÔNG CÓ ITEM
        // =====================================================

        if (selectedItems.isEmpty()) {

            selectedVoucher = null

            selectedFreeship = null

            resetVoucherUI()

            tvTotal.text = "0đ"

            btnCheckout.text = "Mua hàng"

            btnCheckout.isEnabled = false

            btnCheckout.setBackgroundColor(
                Color.GRAY
            )

            return
        }

        btnCheckout.isEnabled = true

        btnCheckout.setBackgroundColor(
            Color.parseColor("#004C8C")
        )

        var discount = 0.0

        // =====================================================
        // VOUCHER GIẢM GIÁ
        // =====================================================

        selectedVoucher?.let { voucher ->

            val validItems =
                when (voucher.applyTo.uppercase()) {

                    "ALL" -> {

                        selectedItems
                    }

                    "BOOK" -> {

                        selectedItems.filter {

                            it.bookId.toString()
                                .trim() ==
                                    voucher.applyTargetId
                                        .toString()
                                        .trim()
                        }
                    }

                    "CATEGORY" -> {

                        selectedItems.filter {

                            val voucherCategory =
                                voucher.applyTargetName
                                    .replace("Thể loại:", "")
                                    .trim()

                            it.theLoai
                                ?.trim()
                                ?.equals(
                                    voucherCategory,
                                    true
                                ) == true
                        }
                    }

                    else -> emptyList()
                }

            // =====================================================
            // KHÔNG CÒN HỢP LỆ
            // =====================================================

            if (
                validItems.isEmpty() ||
                currentSubtotal < voucher.minOrderValue
            ) {

                selectedVoucher = null

                resetDiscountVoucherUI()

            } else {

                val validAmount =
                    validItems.sumOf {
                        it.gia.toDouble() * it.soLuong
                    }

                discount =
                    if (
                        voucher.type.equals(
                            "PERCENT",
                            true
                        )
                    ) {

                        (
                                validAmount *
                                        voucher.discountValue / 100
                                ).coerceAtMost(
                                if (voucher.maxDiscountAmount > 0)
                                    voucher.maxDiscountAmount
                                else
                                    Double.MAX_VALUE
                            )

                    } else {

                        voucher.discountValue
                    }

                discount =
                    discount.coerceAtMost(validAmount)

                tvVoucherName.text =
                    "Mã: ${voucher.code}"

                tvVoucherName.setTextColor(
                    Color.parseColor("#EE4D2D")
                )

                tvDiscountAmount.text =
                    "-${formatPrice(discount)}"
            }

        } ?: resetDiscountVoucherUI()

        // =====================================================
        // FREESHIP
        // =====================================================

        selectedFreeship?.let { voucher ->

            if (
                currentSubtotal <
                voucher.minOrderValue
            ) {

                selectedFreeship = null

                resetFreeshipUI()

            } else {

                tvFreeshipName.text =
                    "Mã ship: ${voucher.code}"

                tvFreeshipName.setTextColor(
                    Color.parseColor("#00BFA5")
                )

                tvFreeshipAmount.text =
                    "Đã chọn"
            }

        } ?: resetFreeshipUI()

        // =====================================================
        // TOTAL
        // =====================================================

        val finalTotal =
            (currentSubtotal - discount)
                .coerceAtLeast(0.0)

        tvTotal.text =
            formatPrice(finalTotal)

        btnCheckout.text =
            "Mua hàng (${selectedItems.size})"
    }

    // =====================================================
    // RESET UI
    // =====================================================

    private fun resetVoucherUI() {

        resetDiscountVoucherUI()

        resetFreeshipUI()
    }

    private fun resetDiscountVoucherUI() {

        tvVoucherName.text =
            "Chọn mã giảm giá"

        tvVoucherName.setTextColor(
            Color.parseColor("#64748B")
        )

        tvDiscountAmount.text =
            "0đ"
    }

    private fun resetFreeshipUI() {

        tvFreeshipName.text =
            "Chọn mã Freeship"

        tvFreeshipName.setTextColor(
            Color.parseColor("#64748B")
        )

        tvFreeshipAmount.text =
            "0đ"
    }

    // =====================================================
    // BOTTOM SHEET
    // =====================================================

    private fun showVoucherBottomSheet(
        loaiCanTim: String
    ) {

        val dialog =
            BottomSheetDialog(this)

        val view =
            layoutInflater.inflate(
                R.layout.layout_voucher_bottom_sheet,
                null
            )

        val rv =
            view.findViewById<RecyclerView>(
                R.id.rvVoucherList
            )

        val layoutEmpty =
            view.findViewById<LinearLayout>(
                R.id.layoutEmpty
            )

        val btnConfirm =
            view.findViewById<Button>(
                R.id.btnConfirmVoucher
            )

        val voucherList =
            mutableListOf<Voucher>()

        val selectedItems =
            cartList.filter { it.isSelected }

        var tempSelected: Voucher? = null

        // =====================================================
        // ADAPTER
        // =====================================================

        val adapterVoucher =
            VoucherUserAdapter(
                voucherList,
                selectedItems,
                currentSubtotal
            ) {

                tempSelected = it
            }

        rv.layoutManager =
            LinearLayoutManager(this)

        rv.adapter =
            adapterVoucher

        // =====================================================
        // CONFIRM
        // =====================================================

        btnConfirm.setOnClickListener {

            tempSelected?.let {

                if (
                    it.loaiVoucher.equals(
                        "FREESHIP",
                        true
                    )
                ) {

                    selectedFreeship = it

                } else {

                    selectedVoucher = it
                }

                calculateTotal()
            }

            dialog.dismiss()
        }

        // =====================================================
        // LOAD VOUCHER
        // =====================================================

        val now =
            System.currentTimeMillis()

        FirebaseDatabase
            .getInstance(dbUrl)
            .getReference("Vouchers")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(
                        snapshot: DataSnapshot
                    ) {

                        voucherList.clear()

                        for (ds in snapshot.children) {

                            val voucher =
                                ds.getValue(
                                    Voucher::class.java
                                ) ?: continue

                            // inactive
                            if (!voucher.active)
                                continue

                            // sai loại
                            if (
                                !voucher.loaiVoucher.equals(
                                    loaiCanTim,
                                    true
                                )
                            ) continue

                            // hết hạn
                            val endDate =
                                if (voucher.endDate > 0)
                                    voucher.endDate
                                else
                                    Long.MAX_VALUE

                            if (
                                now !in voucher.startDate..endDate
                            ) continue

                            // =====================================================
                            // FILTER ĐÚNG VOUCHER
                            // =====================================================

                            val isMatch =
                                when (voucher.applyTo.uppercase()) {

                                    // TOÀN SHOP
                                    "ALL" -> true

                                    // VOUCHER THEO SÁCH
                                    "BOOK" -> {

                                        selectedItems.any {

                                            it.bookId.toString()
                                                .trim() ==
                                                    voucher.applyTargetId
                                                        .toString()
                                                        .trim()
                                        }
                                    }

                                    // VOUCHER THEO THỂ LOẠI
                                    "CATEGORY" -> {

                                        val voucherCategory =
                                            voucher.applyTargetName
                                                .replace("Thể loại:", "")
                                                .trim()

                                        selectedItems.any {

                                            it.theLoai
                                                ?.trim()
                                                ?.equals(
                                                    voucherCategory,
                                                    true
                                                ) == true
                                        }
                                    }

                                    else -> false
                                }

                            if (isMatch) {

                                voucherList.add(voucher)
                            }
                        }

                        adapterVoucher.notifyDataSetChanged()

                        val isEmpty =
                            voucherList.isEmpty()

                        layoutEmpty.visibility =
                            if (isEmpty)
                                View.VISIBLE
                            else
                                View.GONE

                        rv.visibility =
                            if (isEmpty)
                                View.GONE
                            else
                                View.VISIBLE
                    }

                    override fun onCancelled(
                        error: DatabaseError
                    ) {
                    }
                })

        dialog.setContentView(view)

        dialog.show()
    }

    // =====================================================
    // CHECKOUT
    // =====================================================

    private fun handleCheckout() {

        val selectedItems =
            cartList.filter { it.isSelected }

        if (selectedItems.isEmpty()) {

            Toast.makeText(
                this,
                "Vui lòng chọn sản phẩm",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

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
                    ArrayList(selectedItems)
                )

                selectedVoucher?.let {

                    putExtra(
                        "SELECTED_VOUCHER",
                        it
                    )
                }

                selectedFreeship?.let {

                    putExtra(
                        "SELECTED_FREESHIP",
                        it
                    )
                }
            }

        startActivity(intent)
    }

    // =====================================================
    // LOAD CART
    // =====================================================

    private fun loadCartData() {

        cartRef.addValueEventListener(
            object : ValueEventListener {

                override fun onDataChange(
                    snapshot: DataSnapshot
                ) {

                    val tempList =
                        mutableListOf<CartItem>()

                    for (data in snapshot.children) {

                        val item =
                            data.getValue(
                                CartItem::class.java
                            ) ?: continue

                        val oldItem =
                            cartList.find {

                                it.bookId == item.bookId
                            }

                        if (oldItem != null) {

                            item.isSelected =
                                oldItem.isSelected
                        }

                        tempList.add(item)
                    }

                    cartList.clear()

                    cartList.addAll(tempList)

                    adapter.notifyDataSetChanged()

                    calculateTotal()
                }

                override fun onCancelled(
                    error: DatabaseError
                ) {
                }
            })
    }
}