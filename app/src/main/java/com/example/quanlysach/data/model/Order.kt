package com.example.quanlysach.data.model

import java.io.Serializable

data class Order(
    val orderId: String = "",
    var username: String = "",
    val receiverName: String = "",
    val phone: String = "",
    val addressDetail: String = "",
    val orderItems: List<CartItem> = listOf(),
    val totalProductPrice: Int = 0,
    val shippingFee: Int = 0,
    val discountAmount: Int = 0,
    val finalTotal: Int = 0,
    val paymentMethod: String = "",
    var status: String = "dang_cho",
    val createdAt: Long = 0,
    val completedAt: Long = 0,
    val reviewed: Boolean = false

) : Serializable