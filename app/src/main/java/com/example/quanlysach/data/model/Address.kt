package com.example.quanlysach.data.model

import java.io.Serializable

data class Address(
    // 1. THUỘC TÍNH ĐỊNH DANH (Firebase & User)
    val id: String = "",           // Mã ID duy nhất của địa chỉ trên Firebase
    val username: String = "",     // Tài khoản sở hữu địa chỉ này

    // 2. THÔNG TIN NGƯỜI NHẬN
    val receiverName: String = "",
    val phone: String = "",
    val detail: String = "",       // Số nhà, tên đường

    // 3. THÔNG TIN ĐỊA CHỈ (DÙNG ĐỂ HIỂN THỊ)
    val cityProvince: String = "",
    val district: String = "",
    val ward: String = "",

    // 4. 🔥 CÁC MÃ ID (BẮT BUỘC ĐỂ GỌI API GHN TÍNH SHIP)
    val provinceId: Int = 0,      // ID Tỉnh/Thành của GHN
    val districtId: Int = 0,      // ID Quận/Huyện của GHN
    val wardCode: String = "",    // Mã Phường/Xã của GHN (GHN dùng kiểu String cho Ward)

    // 5. CÁC THUỘC TÍNH PHỤ
    val addressType: String = "Nhà riêng", // "Nhà riêng" hoặc "Văn phòng"
    var isDefault: Boolean = false         // Địa chỉ mặc định
) : Serializable