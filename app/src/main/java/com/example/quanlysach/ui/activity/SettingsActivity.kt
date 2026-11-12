package com.example.quanlysach.ui.activity

import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.edit
import com.example.quanlysach.R
import com.example.quanlysach.utils.setupBackButton
import com.google.android.material.button.MaterialButton
import com.google.android.material.switchmaterial.SwitchMaterial
import java.util.*

class SettingsActivity : AppCompatActivity() {

    private lateinit var tvCurrentLanguage: TextView
    private lateinit var switchDarkMode: SwitchMaterial

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Đọc cấu hình ngôn ngữ đã lưu trước khi hiển thị Layout giao diện
        loadLocale()
        setContentView(R.layout.activity_settings)

        val btnProfileInfo = findViewById<LinearLayout>(R.id.btnProfileInfo)
        val btnAddress = findViewById<LinearLayout>(R.id.btnAddress)
        val btnLanguage = findViewById<LinearLayout>(R.id.btnLanguage)
        val btnHelpCenter = findViewById<LinearLayout>(R.id.btnHelpCenter)
        val btnPolicy = findViewById<LinearLayout>(R.id.btnPolicy)
        val btnLogout = findViewById<MaterialButton>(R.id.btnLogout)

        tvCurrentLanguage = findViewById(R.id.tvCurrentLanguage)
        switchDarkMode = findViewById(R.id.switchDarkMode)

        setupBackButton(R.id.btnBack)

        // 1. Đồng bộ Trạng thái nút gạt Dark Mode
        val isNightMode = AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES
        switchDarkMode.isChecked = isNightMode

        switchDarkMode.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            } else {
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            }
        }

        // 2. Click chuyển Ngôn ngữ qua PopupMenu chuyên nghiệp
        updateLanguageTextView()
        btnLanguage.setOnClickListener { view ->
            val popup = PopupMenu(this, view)
            popup.menu.add(0, 1, 0, "Tiếng Việt 🇻🇳")
            popup.menu.add(0, 2, 1, "English 🇬🇧")

            popup.setOnMenuItemClickListener { item ->
                when (item.itemId) {
                    1 -> changeLanguage("vi")
                    2 -> changeLanguage("en")
                }
                true
            }
            popup.show()
        }

        // 3. Chuyển hướng các trang tính năng
        btnProfileInfo.setOnClickListener { startActivity(Intent(this, AccountSecurityActivity::class.java)) }
        btnAddress.setOnClickListener {
            val intent = Intent(this, AddressListActivity::class.java)
            intent.putExtra("IS_FROM_SETTINGS", true)
            startActivity(intent)
        }

        // Mở màn hình FAQ Trung tâm hỗ trợ xịn
        btnHelpCenter.setOnClickListener {
            startActivity(Intent(this, HelpCenterActivity::class.java))
        }

        // Mở màn hình Chính sách nhà sách xịn
        btnPolicy.setOnClickListener {
            startActivity(Intent(this, PolicyActivity::class.java))
        }

        // 4. Đăng xuất
        btnLogout.setOnClickListener {
            getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE).edit { clear() }
            val intent = Intent(this, LoginActivity::class.java)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(intent)
            finish()
        }
    }

    private fun changeLanguage(langCode: String) {
        val locale = Locale(langCode)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)

        // Lưu lựa chọn vào máy để khởi động lại vẫn giữ nguyên
        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        pref.edit().putString("LANG", langCode).apply()

        // Cập nhật lại tài nguyên cấu hình hệ thống
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)

        // Làm tươi lại màn hình hiện tại để ăn luôn ngôn ngữ mới
        val refresh = Intent(this, SettingsActivity::class.java)
        startActivity(refresh)
        finish()
    }

    private fun loadLocale() {
        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val lang = pref.getString("LANG", "vi") ?: "vi"
        val locale = Locale(lang)
        Locale.setDefault(locale)
        val config = Configuration()
        config.setLocale(locale)
        baseContext.resources.updateConfiguration(config, baseContext.resources.displayMetrics)
    }

    private fun updateLanguageTextView() {
        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val lang = pref.getString("LANG", "vi")
        tvCurrentLanguage.text = if (lang == "en") "English" else "Tiếng Việt"
    }
}