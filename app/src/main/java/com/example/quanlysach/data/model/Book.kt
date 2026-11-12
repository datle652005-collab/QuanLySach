package com.example.quanlysach.data.model

import java.io.Serializable

data class Book(
    var timestamp: Long = 0,
    val id: String = "",
    val tenSach: String = "",
    val tacGia: String = "",
    val gia: Int = 0,             // Giá gốc
    val giaGiam: Int = 0,         // Giá sau khi giảm (Nếu = 0 là không giảm)
    val moTa: String = "",
    val hinhAnh: String = "",
    val soLuong: Int = 0,
    val theLoai: String = "",
    val tags: List<String> = emptyList(),
    val videoUrl: String = "",
    val rating: Float = 0f,       // ⭐ Số sao trung bình (Ví dụ: 4.5)
    val daBan: Int = 0// 🔥 THÊM DÒNG NÀY VÀO LÀ HẾT BỊ ĐỎ BÊN KIA NGAY!
) : Serializable