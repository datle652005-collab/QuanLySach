package com.example.quanlysach.data.api

import com.example.quanlysach.data.model.FeeRequest
import com.example.quanlysach.data.model.FeeResponse
import com.example.quanlysach.utils.GHNConstants
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

interface GHNApiService {
    @POST("shiip/public-api/v2/shipping-order/fee")
    fun calculateFee(
        @Header("Token") token: String = GHNConstants.TOKEN,
        @Header("ShopId") shopId: Int = GHNConstants.SHOP_ID,
        @Body request: FeeRequest
    ): Call<FeeResponse>

    companion object {
        fun create(): GHNApiService {
            return Retrofit.Builder()
                .baseUrl(GHNConstants.BASE_URL)
                .addConverterFactory(GsonConverterFactory.create())
                .build()
                .create(GHNApiService::class.java)
        }
    }
}