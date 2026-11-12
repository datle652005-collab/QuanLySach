package com.example.quanlysach.data.model

import java.io.Serializable

data class CartItem(


    var bookId: String = "",
    var tenSach: String = "",
    var gia: Int = 0,
    var hinhAnh: String = "",

    var soLuong: Int = 1,

    var isSelected: Boolean = true,

    var theLoai: String = "",

    var soLuongTon: Int = 0,

    var giaGoc: Int = 0,

    var giaGiam: Int = 0

) : Serializable