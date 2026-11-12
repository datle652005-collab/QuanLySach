package com.example.quanlysach.ui.activity

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.target.CustomTarget
import com.bumptech.glide.request.transition.Transition
import com.example.quanlysach.R
import com.example.quanlysach.data.api.GHNApiService
import com.example.quanlysach.data.model.*
import com.example.quanlysach.ui.adapter.VoucherUserAdapter
import com.example.quanlysach.utils.NotificationHelper
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.text.NumberFormat
import java.util.Locale

class CheckoutActivity : AppCompatActivity() {

    // =========================================================
    // DATA
    // =========================================================

    private lateinit var username: String

    private val checkoutList = mutableListOf<CartItem>()

    private var selectedVoucher: Voucher? = null
    private var selectedFreeship: Voucher? = null

    private var currentAddress: Address? = null

    private var subTotal = 0
    private var shippingFee = 30000
    private var discountAmount = 0
    private var freeshipDiscount = 0
    private var finalTotal = 0

    // =========================================================
    // MOMO
    // =========================================================

    private var isMoMoOpened = false
    private var savedMoMoOrderId = ""
    private var savedMoMoTotal = 0

    // =========================================================
    // FIREBASE
    // =========================================================

    private val dbUrl =
        "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private val db by lazy {
        FirebaseDatabase
            .getInstance(dbUrl)
            .reference
    }

    // =========================================================
    // GHN
    // =========================================================

    private val ghnApiService by lazy {
        GHNApiService.create()
    }

    // =========================================================
    // VIEW
    // =========================================================

    private lateinit var tvTotalProductPrice: TextView
    private lateinit var tvShippingFee: TextView
    private lateinit var tvFreeshipDiscountAmount: TextView
    private lateinit var tvDiscountAmount: TextView
    private lateinit var tvFinalTotal: TextView
    private lateinit var tvBottomFinalTotal: TextView

    private lateinit var tvSelectedVoucherName: TextView
    private lateinit var tvSelectedFreeshipName: TextView

    private lateinit var tvReceiverInfo: TextView
    private lateinit var tvAddressDetail: TextView

    private lateinit var btnOrder: Button

    private lateinit var rbCOD: RadioButton
    private lateinit var rbMoMo: RadioButton

    // =========================================================
    // ADDRESS RESULT
    // =========================================================

    private val addressLauncher =
        registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->

            if (result.resultCode != RESULT_OK) return@registerForActivityResult

            val address =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                    result.data?.getSerializableExtra(
                        "SELECTED_ADDRESS",
                        Address::class.java
                    )

                } else {

                    @Suppress("DEPRECATION")
                    result.data?.getSerializableExtra(
                        "SELECTED_ADDRESS"
                    ) as? Address
                }

            address?.let {

                currentAddress = it

                updateAddressUI(it)

                fetchShippingFeeFromGHN(it)
            }
        }

    // =========================================================
    // LIFECYCLE
    // =========================================================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_checkout)

        getIntentData()

        initViews()

        setupRecyclerView()

        setupEvents()

        loadDefaultAddress()

        calculateTotals()
        // Trong code xử lý đặt hàng của bạn

        NotificationHelper.sendAdminNotification("order")

    }

    override fun onResume() {
        super.onResume()

        if (
            isMoMoOpened &&
            savedMoMoOrderId.isNotEmpty()
        ) {

            isMoMoOpened = false

            db.child("Orders")
                .child(savedMoMoOrderId)
                .child("status")
                .setValue("da_thanh_toan")
                .addOnSuccessListener {

                    navigateToSuccess(
                        savedMoMoOrderId,
                        savedMoMoTotal
                    )
                }
                .addOnFailureListener {

                    navigateToSuccess(
                        savedMoMoOrderId,
                        savedMoMoTotal
                    )
                }
        }
    }

    // =========================================================
    // INIT DATA
    // =========================================================

    private fun getIntentData() {

        username =
            intent.getStringExtra("USERNAME") ?: ""

        val list =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                intent.getSerializableExtra(
                    "LIST_CHECKOUT",
                    ArrayList::class.java
                ) as? ArrayList<CartItem>

            } else {

                @Suppress("DEPRECATION")
                intent.getSerializableExtra(
                    "LIST_CHECKOUT"
                ) as? ArrayList<CartItem>
            }

        list?.let {

            checkoutList.clear()

            checkoutList.addAll(it)
        }

        selectedVoucher =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                intent.getSerializableExtra(
                    "SELECTED_VOUCHER",
                    Voucher::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                intent.getSerializableExtra(
                    "SELECTED_VOUCHER"
                ) as? Voucher
            }

        selectedFreeship =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

                intent.getSerializableExtra(
                    "SELECTED_FREESHIP",
                    Voucher::class.java
                )

            } else {

                @Suppress("DEPRECATION")
                intent.getSerializableExtra(
                    "SELECTED_FREESHIP"
                ) as? Voucher
            }
    }

    // =========================================================
    // INIT VIEW
    // =========================================================

    private fun initViews() {

        tvTotalProductPrice =
            findViewById(R.id.tvTotalProductPrice)

        tvShippingFee =
            findViewById(R.id.tvShippingFee)

        tvFreeshipDiscountAmount =
            findViewById(R.id.tvFreeshipDiscountAmount)

        tvDiscountAmount =
            findViewById(R.id.tvDiscountAmount)

        tvFinalTotal =
            findViewById(R.id.tvFinalTotal)

        tvBottomFinalTotal =
            findViewById(R.id.tvBottomFinalTotal)

        tvSelectedVoucherName =
            findViewById(R.id.tvSelectedVoucherName)

        tvSelectedFreeshipName =
            findViewById(R.id.tvSelectedFreeshipName)

        tvReceiverInfo =
            findViewById(R.id.tvReceiverInfo)

        tvAddressDetail =
            findViewById(R.id.tvAddressDetail)

        btnOrder =
            findViewById(R.id.btnOrder)

        rbCOD =
            findViewById(R.id.rbCOD)

        rbMoMo =
            findViewById(R.id.rbMoMo)
    }

    // =========================================================
    // EVENTS
    // =========================================================

    private fun setupEvents() {

        findViewById<ImageView>(R.id.ivBackCheckout)
            .setOnClickListener {
                finish()
            }

        findViewById<LinearLayout>(R.id.btnChangeAddress)
            .setOnClickListener {

                val intent =
                    Intent(
                        this,
                        AddressListActivity::class.java
                    )

                intent.putExtra(
                    "USERNAME",
                    username
                )

                addressLauncher.launch(intent)
            }

        findViewById<LinearLayout>(R.id.btnSelectVoucher)
            .setOnClickListener {

                showVoucherBottomSheet("GIAM_GIA")
            }

        findViewById<LinearLayout>(R.id.btnSelectFreeship)
            .setOnClickListener {

                showVoucherBottomSheet("FREESHIP")
            }

        btnOrder.setOnClickListener {

            when {

                checkoutList.isEmpty() -> {

                    toast("Không có sản phẩm")
                }

                currentAddress == null -> {

                    toast("Vui lòng chọn địa chỉ")
                }

                else -> {

                    handleOrderAction()
                }
            }
        }
    }

    // =========================================================
    // RECYCLER VIEW
    // =========================================================

    private fun setupRecyclerView() {

        val rv =
            findViewById<RecyclerView>(R.id.rvCheckoutItems)

        rv.layoutManager =
            LinearLayoutManager(this)

        rv.adapter =
            CheckoutProductAdapter(checkoutList)
    }

    // =========================================================
    // ADDRESS
    // =========================================================

    private fun loadDefaultAddress() {

        db.child("Addresses")
            .child(username)
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        val addressList =
                            snapshot.children.mapNotNull {

                                it.getValue(Address::class.java)
                            }

                        val address =
                            addressList.find { it.isDefault }
                                ?: addressList.firstOrNull()

                        if (address != null) {

                            currentAddress = address

                            updateAddressUI(address)

                            fetchShippingFeeFromGHN(address)

                        } else {

                            shippingFee = 30000

                            calculateTotals()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {}
                })
    }

    private fun updateAddressUI(address: Address) {

        tvReceiverInfo.text =
            "${address.receiverName} | ${address.phone}"

        tvAddressDetail.text =
            "${address.detail}, ${address.ward}, ${address.district}, ${address.cityProvince}"
    }

    // =========================================================
    // GHN
    // =========================================================

    private fun fetchShippingFeeFromGHN(address: Address) {

        if (
            address.districtId == 0 ||
            address.wardCode.isEmpty()
        ) {

            shippingFee = 30000

            calculateTotals()

            return
        }

        tvShippingFee.text = "Đang tính..."

        btnOrder.isEnabled = false

        val totalWeight =
            (checkoutList.sumOf { it.soLuong } * 300)
                .coerceAtLeast(500)

        val request =
            FeeRequest(
                to_district_id = address.districtId,
                to_ward_code = address.wardCode,
                weight = totalWeight,
                service_type_id = 2,
                insurance_value = 0
            )

        ghnApiService.calculateFee(request=request)
            .enqueue(object : Callback<FeeResponse> {

                override fun onResponse(
                    call: Call<FeeResponse>,
                    response: Response<FeeResponse>
                ) {

                    shippingFee =
                        response.body()?.data?.total ?: 30000

                    btnOrder.isEnabled = true

                    calculateTotals()
                }

                override fun onFailure(
                    call: Call<FeeResponse>,
                    t: Throwable
                ) {

                    shippingFee = 30000

                    btnOrder.isEnabled = true

                    calculateTotals()
                }
            })
    }

    // =========================================================
    // TOTAL
    // =========================================================

    private fun calculateTotals() {

        subTotal =
            checkoutList.sumOf {
                it.gia * it.soLuong
            }

        calculateDiscountVoucher()

        calculateFreeshipVoucher()

        val actualShipping =
            (shippingFee - freeshipDiscount)
                .coerceAtLeast(0)

        finalTotal =
            (subTotal + actualShipping - discountAmount)
                .coerceAtLeast(0)

        updateTotalUI()
    }

    // =========================================================
    // DISCOUNT VOUCHER
    // =========================================================

    private fun calculateDiscountVoucher() {

        discountAmount = 0

        selectedVoucher?.let { voucher ->

            val applicableItems =
                when (voucher.applyTo.uppercase()) {

                    "ALL" -> checkoutList

                    "BOOK" -> {

                        checkoutList.filter {

                            it.bookId.toString() ==
                                    voucher.applyTargetId.toString()
                        }
                    }

                    "CATEGORY" -> {

                        val voucherCategory =
                            voucher.applyTargetName
                                .replace("Thể loại:", "")
                                .trim()

                        checkoutList.filter {

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

            if (applicableItems.isEmpty()) {

                selectedVoucher = null

                resetDiscountVoucherUI()

                return
            }

            val validSubtotal =
                applicableItems.sumOf {
                    it.gia * it.soLuong
                }

            if (validSubtotal < voucher.minOrderValue) {

                selectedVoucher = null

                resetDiscountVoucherUI()

                return
            }

            discountAmount =
                if (
                    voucher.type.equals(
                        "PERCENT",
                        true
                    )
                ) {

                    (
                            validSubtotal *
                                    voucher.discountValue / 100
                            ).toInt()

                } else {

                    voucher.discountValue.toInt()
                }

            if (voucher.maxDiscountAmount > 0) {

                discountAmount =
                    discountAmount.coerceAtMost(
                        voucher.maxDiscountAmount.toInt()
                    )
            }

            discountAmount =
                discountAmount.coerceAtMost(validSubtotal)

            tvSelectedVoucherName.text =
                "Mã: ${voucher.code}"

            tvSelectedVoucherName.setTextColor(
                Color.parseColor("#1D4ED8")
            )

        } ?: resetDiscountVoucherUI()
    }

    // =========================================================
    // FREESHIP
    // =========================================================

    private fun calculateFreeshipVoucher() {

        freeshipDiscount = 0

        selectedFreeship?.let { voucher ->

            if (subTotal < voucher.minOrderValue) {

                selectedFreeship = null

                resetFreeshipUI()

                return
            }

            freeshipDiscount =
                if (
                    voucher.type.equals(
                        "PERCENT",
                        true
                    )
                ) {

                    (
                            shippingFee *
                                    voucher.discountValue / 100
                            ).toInt()

                } else {

                    voucher.discountValue.toInt()
                }

            if (voucher.maxDiscountAmount > 0) {

                freeshipDiscount =
                    freeshipDiscount.coerceAtMost(
                        voucher.maxDiscountAmount.toInt()
                    )
            }

            freeshipDiscount =
                freeshipDiscount.coerceAtMost(shippingFee)

            tvSelectedFreeshipName.text =
                "Mã: ${voucher.code}"

            tvSelectedFreeshipName.setTextColor(
                Color.parseColor("#00BFA5")
            )

        } ?: resetFreeshipUI()
    }

    // =========================================================
    // UPDATE UI
    // =========================================================

    private fun updateTotalUI() {

        tvTotalProductPrice.text =
            formatPrice(subTotal)

        tvShippingFee.text =
            formatPrice(shippingFee)

        tvFreeshipDiscountAmount.text =
            "-${formatPrice(freeshipDiscount)}"

        tvDiscountAmount.text =
            "-${formatPrice(discountAmount)}"

        tvFinalTotal.text =
            formatPrice(finalTotal)

        tvBottomFinalTotal.text =
            formatPrice(finalTotal)
    }

    private fun resetDiscountVoucherUI() {

        tvSelectedVoucherName.text =
            "Chọn mã ưu đãi >"

        tvSelectedVoucherName.setTextColor(
            Color.parseColor("#64748B")
        )
    }

    private fun resetFreeshipUI() {

        tvSelectedFreeshipName.text =
            "Chọn mã vận chuyển >"

        tvSelectedFreeshipName.setTextColor(
            Color.parseColor("#64748B")
        )
    }

    // =========================================================
    // VOUCHER
    // =========================================================

    private fun showVoucherBottomSheet(type: String) {

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

        var tempSelected: Voucher? = null

        val adapterVoucher =
            VoucherUserAdapter(
                voucherList,
                checkoutList,
                subTotal.toDouble()
            ) {

                tempSelected = it
            }

        rv.layoutManager =
            LinearLayoutManager(this)

        rv.adapter = adapterVoucher

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

                calculateTotals()
            }

            dialog.dismiss()
        }

        val now =
            System.currentTimeMillis()

        db.child("Vouchers")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        voucherList.clear()

                        for (ds in snapshot.children) {

                            val voucher =
                                ds.getValue(Voucher::class.java)
                                    ?: continue

                            if (!voucher.active)
                                continue

                            if (
                                !voucher.loaiVoucher.equals(
                                    type,
                                    true
                                )
                            ) continue

                            val endDate =
                                if (voucher.endDate > 0)
                                    voucher.endDate
                                else
                                    Long.MAX_VALUE

                            if (
                                now !in voucher.startDate..endDate
                            ) continue

                            // =================================
                            // FILTER ĐÚNG VOUCHER
                            // =================================

                            val matched =
                                when (voucher.applyTo.uppercase()) {

                                    "ALL" -> true

                                    "BOOK" -> {

                                        checkoutList.any {

                                            it.bookId.toString() ==
                                                    voucher.applyTargetId.toString()
                                        }
                                    }

                                    "CATEGORY" -> {

                                        checkoutList.any {

                                            it.theLoai
                                                ?.trim()
                                                ?.equals(
                                                    voucher.applyTargetName.trim(),
                                                    true
                                                ) == true
                                        }
                                    }

                                    else -> false
                                }

                            if (matched) {

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

                    override fun onCancelled(error: DatabaseError) {

                        toast(error.message)
                    }
                })

        dialog.setContentView(view)

        dialog.show()
    }

    // =========================================================
    // ORDER
    // =========================================================

    private fun handleOrderAction() {

        btnOrder.isEnabled = false

        btnOrder.text = "Đang đặt hàng..."

        val orderId =
            db.child("Orders")
                .push()
                .key ?: return

        val paymentMethod =
            if (rbMoMo.isChecked)
                "MOMO"
            else
                "COD"

        val order =
            Order(
                orderId = orderId,
                username = username,
                receiverName = currentAddress?.receiverName ?: "",
                phone = currentAddress?.phone ?: "",
                addressDetail = tvAddressDetail.text.toString(),
                orderItems = ArrayList(checkoutList),
                totalProductPrice = subTotal,
                shippingFee = shippingFee,
                discountAmount = discountAmount + freeshipDiscount,
                finalTotal = finalTotal,
                paymentMethod = paymentMethod,
                status = "dang_cho",
                createdAt = System.currentTimeMillis()
            )

        db.child("Orders")
            .child(orderId)
            .setValue(order)
            .addOnSuccessListener {

                clearCartAndInventory()
                updateVoucherUsage()

                if (paymentMethod == "MOMO") {

                    showMoMoPaymentDialog(
                        orderId,
                        finalTotal
                    )

                } else {

                    navigateToSuccess(
                        orderId,
                        finalTotal
                    )
                }
            }
            .addOnFailureListener {

                btnOrder.isEnabled = true

                btnOrder.text = "Đặt hàng"

                toast("Đặt hàng thất bại")
            }
    }

    private fun clearCartAndInventory() {

        checkoutList.forEach { item ->

            db.child("Books")
                .child(item.bookId)
                .child("soLuong")
                .runTransaction(object : Transaction.Handler {

                    override fun doTransaction(
                        currentData: MutableData
                    ): Transaction.Result {

                        val currentStock =
                            currentData.getValue(Int::class.java) ?: 0

                        currentData.value =
                            (currentStock - item.soLuong)
                                .coerceAtLeast(0)

                        return Transaction.success(currentData)
                    }

                    override fun onComplete(
                        error: DatabaseError?,
                        committed: Boolean,
                        currentData: DataSnapshot?
                    ) {
                    }
                })

            db.child("Carts")
                .child(username)
                .child(item.bookId)
                .removeValue()
        }
    }
    // =========================================================
    // CẬP NHẬT LƯỢT DÙNG VOUCHER
    // =========================================================

    private fun updateVoucherUsage() {
        // 1. Tăng lượt dùng cho mã Giảm giá (nếu khách có xài)
        selectedVoucher?.id?.let { id ->
            if (id.isNotEmpty()) {
                db.child("Vouchers")
                    .child(id)
                    .child("usedCount")
                    .setValue(ServerValue.increment(1))
            }
        }

        // 2. Tăng lượt dùng cho mã Freeship (nếu khách có xài)
        selectedFreeship?.id?.let { id ->
            if (id.isNotEmpty()) {
                db.child("Vouchers")
                    .child(id)
                    .child("usedCount")
                    .setValue(ServerValue.increment(1))
            }
        }
    }

    // =========================================================
    // MOMO
    // =========================================================

    private fun showMoMoPaymentDialog(
        orderId: String,
        totalAmount: Int
    ) {

        val dialog =
            BottomSheetDialog(this)

        val view =
            layoutInflater.inflate(
                R.layout.layout_momo_payment_bottom_sheet,
                null
            )

        val tvMoMoOrderId =
            view.findViewById<TextView>(
                R.id.tvMoMoOrderId
            )

        val ivMoMoQR =
            view.findViewById<ImageView>(
                R.id.ivMoMoQR
            )

        val btnOpenMoMoApp =
            view.findViewById<Button>(
                R.id.btnOpenMoMoApp
            )

        tvMoMoOrderId.text =
            "Nội dung: $orderId"

        val momoPhone = "0382773307"

        val qrUrl =
            "https://img.vietqr.io/image/momo-$momoPhone-qr_only.jpg?amount=$totalAmount&addInfo=$orderId"

        Glide.with(this)
            .load(qrUrl)
            .placeholder(R.drawable.ic_chat_gallery)
            .into(ivMoMoQR)

        btnOpenMoMoApp.setOnClickListener {

            copyOrderId(orderId)

            dialog.dismiss()

            Glide.with(this)
                .asBitmap()
                .load(qrUrl)
                .into(object : CustomTarget<Bitmap>() {

                    override fun onResourceReady(
                        resource: Bitmap,
                        transition: Transition<in Bitmap>?
                    ) {

                        try {

                            MediaStore.Images.Media.insertImage(
                                contentResolver,
                                resource,
                                "QR_MOMO_$orderId",
                                "Thanh toán"
                            )

                        } catch (_: Exception) {
                        }

                        openMoMo(
                            orderId,
                            totalAmount
                        )
                    }

                    override fun onLoadCleared(
                        placeholder: Drawable?
                    ) {
                    }
                })
        }

        dialog.setContentView(view)

        dialog.show()
    }

    private fun openMoMo(
        orderId: String,
        totalAmount: Int
    ) {

        isMoMoOpened = true

        savedMoMoOrderId = orderId

        savedMoMoTotal = totalAmount

        val momoIntent =
            packageManager.getLaunchIntentForPackage(
                "vn.momo.platform"
            )

        if (momoIntent != null) {

            startActivity(momoIntent)

        } else {

            try {

                startActivity(
                    Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("momo://")
                    )
                )

            } catch (e: Exception) {

                toast("Không mở được MoMo")
            }
        }
    }

    private fun copyOrderId(orderId: String) {

        val clipboard =
            getSystemService(
                Context.CLIPBOARD_SERVICE
            ) as ClipboardManager

        val clip =
            ClipData.newPlainText(
                "ORDER_ID",
                orderId
            )

        clipboard.setPrimaryClip(clip)
    }

    // =========================================================
    // SUCCESS
    // =========================================================

    private fun navigateToSuccess(
        orderId: String,
        total: Int
    ) {

        val intent =
            Intent(
                this,
                ActivityOrderSuccess::class.java
            )

        intent.putExtra(
            "ORDER_ID",
            orderId
        )

        intent.putExtra(
            "TOTAL",
            total
        )

        intent.putExtra(
            "ADDRESS",
            tvAddressDetail.text.toString()
        )

        intent.putExtra(
            "PAYMENT_METHOD",
            if (rbMoMo.isChecked)
                "MOMO"
            else
                "COD"
        )

        startActivity(intent)

        finish()
    }

    // =========================================================
    // UTIL
    // =========================================================

    private fun formatPrice(price: Int): String {

        return NumberFormat
            .getInstance(Locale("vi", "VN"))
            .format(price) + "đ"
    }

    private fun toast(message: String) {

        Toast.makeText(
            this,
            message,
            Toast.LENGTH_SHORT
        ).show()
    }

    // =========================================================
    // CHECKOUT PRODUCT ADAPTER
    // =========================================================

    private inner class CheckoutProductAdapter(
        private val list: List<CartItem>
    ) : RecyclerView.Adapter<CheckoutProductAdapter.VH>() {

        inner class VH(view: View) :
            RecyclerView.ViewHolder(view) {

            val img =
                view.findViewById<ImageView>(
                    R.id.imgProduct
                )

            val name =
                view.findViewById<TextView>(
                    R.id.tvProductName
                )

            val price =
                view.findViewById<TextView>(
                    R.id.tvProductPrice
                )

            val qty =
                view.findViewById<TextView>(
                    R.id.tvQuantity
                )
        }

        override fun onCreateViewHolder(
            parent: ViewGroup,
            viewType: Int
        ): VH {

            return VH(
                LayoutInflater.from(parent.context)
                    .inflate(
                        R.layout.item_checkout_product,
                        parent,
                        false
                    )
            )
        }

        override fun onBindViewHolder(
            holder: VH,
            position: Int
        ) {

            val item =
                list[position]

            holder.name.text =
                item.tenSach

            holder.price.text =
                formatPrice(item.gia)

            holder.qty.text =
                "x${item.soLuong}"

            Glide.with(this@CheckoutActivity)
                .load(item.hinhAnh)
                .placeholder(R.drawable.ic_chat_gallery)
                .into(holder.img)
        }

        override fun getItemCount(): Int {

            return list.size
        }
    }
}