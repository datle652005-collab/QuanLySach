package com.example.quanlysach.data.model

import java.io.Serializable

data class Voucher(

    var id: String = "",

    var title: String = "",

    var code: String = "",

    // PERCENT | FIXED
    var type: String = "PERCENT",

    // Giá trị giảm
    var discountValue: Double = 0.0,

    // GIAM_GIA | FREESHIP
    var loaiVoucher: String = "GIAM_GIA",

    // Điều kiện áp dụng
    var minOrderValue: Double = 0.0,

    var maxDiscountAmount: Double = 0.0,

    // Thời gian
    var startDate: Long = 0L,

    var endDate: Long = 0L,

    var active: Boolean = true,

    // ALL | CATEGORY | BOOK
    var applyTo: String = "ALL",

    var applyTargetId: String = "",

    var applyTargetName: String = "Tất cả sản phẩm",

    // Giới hạn lượt dùng
    var maxUsage: Int = 0,

    var usedCount: Int = 0

) : Serializable