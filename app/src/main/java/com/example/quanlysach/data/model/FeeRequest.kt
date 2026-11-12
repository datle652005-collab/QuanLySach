package com.example.quanlysach.data.model

data class FeeRequest(
    val service_type_id: Int = 2, // 2 là gói giao hàng chuẩn
    val insurance_value: Int = 0, // Giá trị bảo hiểm (đồ án thì để 0)
    val coupon: String? = null,
    val from_district_id: Int = 3440, // Mã quận của shop (3440 là Nam Từ Liêm, HN)
    val to_district_id: Int,          // Mã quận của khách (lấy từ API Địa chỉ)
    val to_ward_code: String,         // Mã phường của khách
    val weight: Int = 500,            // Trọng lượng sách (gram)
    val length: Int = 20,             // Kích thước (cm)
    val width: Int = 15,
    val height: Int = 5
)