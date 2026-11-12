package com.example.quanlysach.ui.activity

import android.app.DatePickerDialog
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.AutoCompleteTextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Voucher
import com.google.android.material.appbar.MaterialToolbar
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import com.google.android.material.textfield.TextInputEditText
import com.google.android.material.textfield.TextInputLayout
import com.google.firebase.database.*
import java.text.SimpleDateFormat
import java.util.*

class AddVoucherActivity : AppCompatActivity() {

    // ================= VIEW =================

    private lateinit var edtTitle: TextInputEditText
    private lateinit var edtCode: TextInputEditText
    private lateinit var edtDiscount: TextInputEditText
    private lateinit var edtMinOrder: TextInputEditText
    private lateinit var edtMaxDiscount: TextInputEditText
    private lateinit var edtMaxUsage: TextInputEditText
    private lateinit var edtStartDate: TextInputEditText
    private lateinit var edtEndDate: TextInputEditText

    private lateinit var toggleGroupLoai: MaterialButtonToggleGroup
    private lateinit var toggleGroupDonVi: MaterialButtonToggleGroup

    private lateinit var spApplyToMenu: AutoCompleteTextView
    private lateinit var tipTarget: TextInputLayout
    private lateinit var autoCompleteTarget: AutoCompleteTextView

    private lateinit var btnSave: MaterialButton

    // ================= DATA =================

    private var startDateStamp = 0L
    private var endDateStamp = 0L

    private var currentApplyPos = 0

    private var voucherToEdit: Voucher? = null

    private val bookMap = mutableMapOf<String, String>()

    private val database by lazy {
        FirebaseDatabase.getInstance(
            "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).reference
    }

    // ================= ON CREATE =================

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_add_voucher)

        initViews()

        setupToolbar()

        setupDatePicker()

        setupApplyDropdown()

        getEditData()

        btnSave.setOnClickListener {
            saveVoucher()
        }
    }

    // ================= INIT =================

    private fun initViews() {

        edtTitle = findViewById(R.id.edtTitle)
        edtCode = findViewById(R.id.edtCode)
        edtDiscount = findViewById(R.id.edtDiscountValue)
        edtMinOrder = findViewById(R.id.edtMinOrder)
        edtMaxDiscount = findViewById(R.id.edtMaxDiscount)
        edtMaxUsage = findViewById(R.id.edtMaxUsage)

        edtStartDate = findViewById(R.id.edtStartDate)
        edtEndDate = findViewById(R.id.edtEndDate)

        toggleGroupLoai = findViewById(R.id.toggleGroupLoai)
        toggleGroupDonVi = findViewById(R.id.toggleGroupDonVi)

        spApplyToMenu = findViewById(R.id.spApplyToMenu)

        tipTarget = findViewById(R.id.tipTarget)

        autoCompleteTarget = findViewById(R.id.autoCompleteTarget)

        btnSave = findViewById(R.id.btnSave)

        edtCode.setOnLongClickListener {

            edtCode.setText(generateCode())

            toast("Đã tạo mã ngẫu nhiên")

            true
        }
    }

    // ================= TOOLBAR =================

    private fun setupToolbar() {

        val toolbar =
            findViewById<MaterialToolbar>(R.id.toolbar)

        toolbar.setNavigationOnClickListener {
            finish()
        }

        toolbar.title =
            if (intent.hasExtra("VOUCHER_EDIT")) {
                "Sửa Voucher"
            } else {
                "Tạo Voucher"
            }
    }

    // ================= RANDOM CODE =================

    private fun generateCode(): String {

        val chars =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789"

        return (1..8)
            .map { chars.random() }
            .joinToString("")
    }

    // ================= DATE PICKER =================

    private fun setupDatePicker() {

        edtStartDate.setOnClickListener {
            showDatePicker(true)
        }

        edtEndDate.setOnClickListener {
            showDatePicker(false)
        }
    }

    private fun showDatePicker(isStart: Boolean) {

        val calendar = Calendar.getInstance()

        val dialog = DatePickerDialog(
            this,
            { _, year, month, day ->

                calendar.set(
                    year,
                    month,
                    day,
                    if (isStart) 0 else 23,
                    if (isStart) 0 else 59,
                    if (isStart) 0 else 59
                )

                val time = calendar.timeInMillis

                val sdf = SimpleDateFormat(
                    "dd/MM/yyyy",
                    Locale.getDefault()
                )

                if (isStart) {

                    startDateStamp = time

                    edtStartDate.setText(
                        sdf.format(Date(time))
                    )

                } else {

                    endDateStamp = time

                    edtEndDate.setText(
                        sdf.format(Date(time))
                    )
                }
            },
            calendar.get(Calendar.YEAR),
            calendar.get(Calendar.MONTH),
            calendar.get(Calendar.DAY_OF_MONTH)
        )

        dialog.show()
    }

    // ================= APPLY TO =================

    private fun setupApplyDropdown() {

        val options = arrayOf(
            "Tất cả sản phẩm",
            "Theo sách",
            "Theo thể loại"
        )

        spApplyToMenu.setAdapter(
            ArrayAdapter(
                this,
                android.R.layout.simple_list_item_1,
                options
            )
        )

        spApplyToMenu.setText(options[0], false)

        tipTarget.visibility = View.GONE

        spApplyToMenu.setOnItemClickListener { _, _, position, _ ->

            currentApplyPos = position

            autoCompleteTarget.setText("")

            when (position) {

                0 -> {
                    tipTarget.visibility = View.GONE
                }

                1 -> {

                    tipTarget.visibility = View.VISIBLE

                    tipTarget.hint = "Chọn sách"

                    loadBooks()
                }

                2 -> {

                    tipTarget.visibility = View.VISIBLE

                    tipTarget.hint = "Chọn thể loại"

                    loadCategories()
                }
            }
        }
    }

    // ================= LOAD BOOK =================

    private fun loadBooks() {

        database.child("Books")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        val names = mutableListOf<String>()

                        bookMap.clear()

                        for (ds in snapshot.children) {

                            val id = ds.key ?: continue

                            val name =
                                ds.child("tenSach")
                                    .getValue(String::class.java)
                                    ?: continue

                            bookMap[name] = id

                            names.add(name)
                        }

                        autoCompleteTarget.setAdapter(
                            ArrayAdapter(
                                this@AddVoucherActivity,
                                android.R.layout.simple_list_item_1,
                                names
                            )
                        )
                    }

                    override fun onCancelled(error: DatabaseError) {
                        toast(error.message)
                    }
                }
            )
    }

    // ================= LOAD CATEGORY =================

    private fun loadCategories() {

        database.child("Books")
            .addListenerForSingleValueEvent(
                object : ValueEventListener {

                    override fun onDataChange(snapshot: DataSnapshot) {

                        val categories = mutableSetOf<String>()

                        for (ds in snapshot.children) {

                            val category =
                                ds.child("theLoai")
                                    .getValue(String::class.java)

                            if (!category.isNullOrEmpty()) {
                                categories.add(category)
                            }
                        }

                        autoCompleteTarget.setAdapter(
                            ArrayAdapter(
                                this@AddVoucherActivity,
                                android.R.layout.simple_list_item_1,
                                categories.toList()
                            )
                        )
                    }

                    override fun onCancelled(error: DatabaseError) {
                        toast(error.message)
                    }
                }
            )
    }

    // ================= EDIT =================

    private fun getEditData() {

        voucherToEdit =
            intent.getSerializableExtra(
                "VOUCHER_EDIT"
            ) as? Voucher

        voucherToEdit?.let {

            btnSave.text = "CẬP NHẬT"

            fillData(it)
        }
    }

    private fun fillData(v: Voucher) {

        val sdf = SimpleDateFormat(
            "dd/MM/yyyy",
            Locale.getDefault()
        )

        edtTitle.setText(v.title)
        edtCode.setText(v.code)

        edtDiscount.setText(v.discountValue.toString())
        edtMinOrder.setText(v.minOrderValue.toString())
        edtMaxDiscount.setText(v.maxDiscountAmount.toString())
        edtMaxUsage.setText(v.maxUsage.toString())

        v.startDate?.let {

            startDateStamp = it

            edtStartDate.setText(
                sdf.format(Date(it))
            )
        }

        v.endDate?.let {

            endDateStamp = it

            edtEndDate.setText(
                sdf.format(Date(it))
            )
        }

        if (v.loaiVoucher == "FREESHIP") {

            toggleGroupLoai.check(
                R.id.btnTypeFreeship
            )

        } else {

            toggleGroupLoai.check(
                R.id.btnTypeGiamGia
            )
        }

        if (v.type == "PERCENT") {

            toggleGroupDonVi.check(
                R.id.btnTypePercent
            )

        } else {

            toggleGroupDonVi.check(
                R.id.btnTypeFixed
            )
        }
    }

    // ================= SAVE =================

    private fun saveVoucher() {

        val title =
            edtTitle.text.toString().trim()

        val code =
            edtCode.text.toString()
                .trim()
                .uppercase()

        val discountValue =
            edtDiscount.text.toString()
                .toDoubleOrNull() ?: 0.0

        val minOrder =
            edtMinOrder.text.toString()
                .toDoubleOrNull() ?: 0.0

        val maxDiscount =
            edtMaxDiscount.text.toString()
                .toDoubleOrNull() ?: 0.0

        val maxUsage =
            edtMaxUsage.text.toString()
                .toIntOrNull() ?: 0

        // VALIDATE

        if (title.isEmpty()) {
            toast("Nhập tiêu đề voucher")
            return
        }

        if (code.isEmpty()) {
            toast("Nhập mã voucher")
            return
        }

        if (discountValue <= 0) {
            toast("Giá trị giảm không hợp lệ")
            return
        }

        if (startDateStamp == 0L ||
            endDateStamp == 0L
        ) {
            toast("Chọn thời gian")
            return
        }

        if (endDateStamp <= startDateStamp) {
            toast("Ngày kết thúc không hợp lệ")
            return
        }

        val loaiVoucher =
            if (
                toggleGroupLoai.checkedButtonId
                == R.id.btnTypeFreeship
            ) {
                "FREESHIP"
            } else {
                "GIAM_GIA"
            }

        val type =
            if (
                toggleGroupDonVi.checkedButtonId
                == R.id.btnTypePercent
            ) {
                "PERCENT"
            } else {
                "FIXED"
            }

        var applyTo = "ALL"

        var applyTargetId = ""

        var applyTargetName = "Tất cả sản phẩm"

        when (currentApplyPos) {

            1 -> {

                val bookName =
                    autoCompleteTarget.text.toString().trim()

                if (bookName.isEmpty()) {
                    toast("Chọn sách")
                    return
                }

                applyTo = "BOOK"

                applyTargetName = bookName

                applyTargetId =
                    bookMap[bookName] ?: ""
            }

            2 -> {

                val category =
                    autoCompleteTarget.text.toString().trim()

                if (category.isEmpty()) {
                    toast("Chọn thể loại")
                    return
                }

                applyTo = "CATEGORY"

                applyTargetName = category

                applyTargetId = category
            }
        }

        val id =
            voucherToEdit?.id
                ?: database.child("Vouchers")
                    .push()
                    .key
                ?: return

        val voucher = Voucher(

            id = id,

            title = title,

            code = code,

            type = type,

            discountValue = discountValue,

            loaiVoucher = loaiVoucher,

            minOrderValue = minOrder,

            maxDiscountAmount = maxDiscount,

            startDate = startDateStamp,

            endDate = endDateStamp,

            active = true,

            applyTo = applyTo,

            applyTargetId = applyTargetId,

            applyTargetName = applyTargetName,

            maxUsage = maxUsage,

            usedCount = voucherToEdit?.usedCount ?: 0
        )

        btnSave.isEnabled = false

        btnSave.text = "Đang lưu..."

        database.child("Vouchers")
            .child(id)
            .setValue(voucher)
            .addOnSuccessListener {

                // 🔥 LOGIC TỰ ĐỘNG CẬP NHẬT GIÁ SÁCH:
                // Nếu voucher tạo ra là mã GIẢM GIÁ (không phải FREESHIP) và áp dụng riêng cho 1 cuốn SÁCH
                if (loaiVoucher == "GIAM_GIA" && applyTo == "BOOK" && applyTargetId.isNotEmpty()) {
                    updateBookSalePrice(applyTargetId, type, discountValue)
                }

                toast("Lưu thành công")

                finish()
            }
            .addOnFailureListener {

                btnSave.isEnabled = true

                btnSave.text = "LƯU VOUCHER"

                toast(it.message ?: "Có lỗi")
            }
    }

    // ================= TỰ ĐỘNG CẬP NHẬT GIÁ SÁCH =================
    private fun updateBookSalePrice(bookId: String, voucherType: String, discountValue: Double) {
        val bookRef = database.child("Books").child(bookId)

        bookRef.get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val giaGoc = snapshot.child("gia").getValue(Int::class.java) ?: 0

                if (giaGoc > 0) {
                    var giaSauKhiGiam = 0

                    if (voucherType == "PERCENT") {
                        // Nếu giảm theo %: Ví dụ 100k giảm 20% -> Còn 80k
                        giaSauKhiGiam = (giaGoc - (giaGoc * (discountValue / 100))).toInt()
                    } else {
                        // Nếu giảm tiền mặt: Ví dụ 100k giảm 20k -> Còn 80k
                        giaSauKhiGiam = (giaGoc - discountValue).toInt()
                    }

                    // Đảm bảo giá giảm không bị âm
                    if (giaSauKhiGiam < 0) giaSauKhiGiam = 0

                    // Cập nhật thuộc tính giaGiam vào đúng cuốn sách đó trên Firebase
                    bookRef.child("giaGiam").setValue(giaSauKhiGiam)
                }
            }
        }
    }

    // ================= TOAST =================

    private fun toast(msg: String) {

        Toast.makeText(
            this,
            msg,
            Toast.LENGTH_SHORT
        ).show()
    }
}