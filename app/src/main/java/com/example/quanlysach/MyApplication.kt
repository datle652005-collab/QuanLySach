package com.example.quanlysach

import android.app.Application
import com.cloudinary.android.MediaManager
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MyApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Thay thế các giá trị bên dưới bằng thông tin từ Cloudinary Dashboard của bạn
        val config = mapOf(
            "cloud_name" to "dwqfnfett", // Lấy từ Cloud Name trong Dashboard
            "api_key" to "224223875333722", // Lấy từ API Key
            "api_secret" to "s0nuhl3-6_CmF09Nhi7dKp_RFEk" // Lấy từ API Secret
        )
        MediaManager.init(this, config)
    }
}