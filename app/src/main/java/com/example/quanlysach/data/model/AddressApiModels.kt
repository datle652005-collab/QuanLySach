package com.example.quanlysach.data.model

/**
 * 1. THÙNG CHỨA CHUNG (GHNResponse)
 * Server GHN luôn trả về dữ liệu nằm trong biến "data", kèm theo "code" và "message".
 * Việc dùng <T> giúp chúng ta dùng chung class này cho cả Tỉnh, Huyện và Xã.
 */
data class GHNResponse<T>(
    val code: Int,
    val message: String,
    val data: T
)

/**
 * 2. MODEL TỈNH / THÀNH PHỐ
 */
data class Province(
    val ProvinceID: Int,    // ID kiểu Số của GHN
    val ProvinceName: String
) {
    // Ghi đè toString để khi đưa vào Spinner, nó hiện tên Tỉnh thay vì hiện ID
    override fun toString(): String = ProvinceName
}

/**
 * 3. MODEL QUẬN / HUYỆN
 */
data class District(
    val DistrictID: Int,    // ID kiểu Số của GHN
    val ProvinceID: Int,
    val DistrictName: String
) {
    override fun toString(): String = DistrictName
}

/**
 * 4. MODEL PHƯỜNG / XÃ
 * Lưu ý: GHN dùng WardCode là kiểu String (ví dụ: "1A0311")
 */
data class Ward(
    val WardCode: String,   // Mã kiểu Chuỗi của GHN
    val DistrictID: Int,
    val WardName: String
) {
    override fun toString(): String = WardName
}