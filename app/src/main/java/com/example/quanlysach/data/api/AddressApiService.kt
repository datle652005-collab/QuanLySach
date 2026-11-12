package com.example.quanlysach.data.api


import com.example.quanlysach.data.model.District
import com.example.quanlysach.data.model.GHNResponse
import com.example.quanlysach.data.model.Province
import com.example.quanlysach.data.model.Ward
import com.example.quanlysach.utils.GHNConstants
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Query

interface AddressApiService {

    // 1. Lấy danh sách Tỉnh/Thành từ GHN
    @GET("shiip/public-api/master-data/province")
    fun getProvinces(
        @Header("Token") token: String = GHNConstants.TOKEN
    ): Call<GHNResponse<List<Province>>>

    // 2. Lấy Quận/Huyện dựa theo ProvinceID
    // 🔥 Đã đưa provinceId lên trước, token ra sau
    @GET("shiip/public-api/master-data/district")
    fun getDistricts(
        @Query("province_id") provinceId: Int,
        @Header("Token") token: String = GHNConstants.TOKEN
    ): Call<GHNResponse<List<District>>>

    // 3. Lấy Phường/Xã dựa theo DistrictID
    // 🔥 Đã đưa districtId lên trước, token ra sau
    @GET("shiip/public-api/master-data/ward")
    fun getWards(
        @Query("district_id") districtId: Int,
        @Header("Token") token: String = GHNConstants.TOKEN
    ): Call<GHNResponse<List<Ward>>>

    companion object {
        fun create(): AddressApiService {
            return Retrofit.Builder()
                .baseUrl(GHNConstants.BASE_URL) // Dùng link GHN đã khai báo trong Constants
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(AddressApiService::class.java)
        }
    }
}