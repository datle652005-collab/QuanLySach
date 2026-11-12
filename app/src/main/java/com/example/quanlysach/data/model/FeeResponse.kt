package com.example.quanlysach.data.model

data class FeeResponse(
    val code: Int,
    val data: FeeData
)

data class FeeData(
    val total: Int
)