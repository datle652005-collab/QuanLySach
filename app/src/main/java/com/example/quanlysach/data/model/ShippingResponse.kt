package com.example.quanlysach.data.model

// Lưu ý: Không import android.telecom.Call
data class ShippingResponse(
    val fee: Int = 0,
    val provider: String = "",
    val delivery_date: String = "",
    val tracking_code: String = ""
)