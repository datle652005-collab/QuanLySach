package com.example.quanlysach.data.model

data class User(
    var username: String = "",      // Khóa chính
    val ten_user: String = "",      // Tên hiển thị
    val email: String = "",
    val sdt: String = "",
    val gioi_tinh: String = "",
    val ngay_sinh: String = "",
    val tieu_su: String = "",
    val avata: String = "",
    val vai_tro: String = "khach_hang", // Để phân biệt Admin/Khách hàng
    val trang_thai: String = "hoat_dong" // Thêm trường này để làm Tab "Bị khóa"
)
