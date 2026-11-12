package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.example.quanlysach.data.model.User
import com.example.quanlysach.utils.setupBackButton
import com.google.firebase.database.FirebaseDatabase

class AccountSecurityActivity : AppCompatActivity() {

    private lateinit var tvUsernameFixed: TextView
    private lateinit var tvMaskedPhone: TextView
    private lateinit var tvMaskedEmail: TextView
    private var currentUsername: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_account_security)

        setupBackButton(R.id.btnBack)

        tvUsernameFixed = findViewById(R.id.tvUsernameFixed)
        tvMaskedPhone = findViewById(R.id.tvMaskedPhone)
        tvMaskedEmail = findViewById(R.id.tvMaskedEmail)

        // Bấm vào "Hồ sơ của tôi" -> Nhảy sang trang Sửa hồ sơ
        findViewById<LinearLayout>(R.id.btnMyProfile).setOnClickListener {
            startActivity(Intent(this, EditProfileActivity::class.java))
        }

        // Bấm vào "Đổi mật khẩu" -> Nhảy sang trang Đổi mật khẩu
        findViewById<LinearLayout>(R.id.btnChangePassword).setOnClickListener {
            val intent = Intent(this, ChangePasswordActivity::class.java)
            intent.putExtra("USER_NAME", currentUsername)
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        loadSecurityData() // Tự động load lại data mỗi khi quay lại trang này
    }

    private fun loadSecurityData() {
        val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        currentUsername = pref.getString("USERNAME", "") ?: ""
        tvUsernameFixed.text = currentUsername

        val dbRef = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("Users")

        dbRef.child(currentUsername).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    tvMaskedPhone.text = maskPhoneNumber(it.sdt)
                    tvMaskedEmail.text = maskEmailAddress(it.email)
                }
            }
        }
    }

    // THUẬT TOÁN CHE SỐ ĐIỆN THOẠI (Ví dụ: 0987654308 -> ********08)
    private fun maskPhoneNumber(phone: String): String {
        if (phone.isBlank()) return "Thiết lập ngay"
        if (phone.length <= 4) return phone // Ngắn quá không che

        // Tạo chuỗi dấu * dựa theo độ dài số điện thoại trừ đi 2 số cuối
        val stars = "*".repeat(phone.length - 2)
        val lastTwoDigits = phone.takeLast(2)
        return "$stars$lastTwoDigits"
    }

    // THUẬT TOÁN CHE EMAIL (Ví dụ: myemail123@gmail.com -> m**********3@gmail.com)
    private fun maskEmailAddress(email: String): String {
        if (email.isBlank() || !email.contains("@")) return "Chưa thiết lập"

        val parts = email.split("@")
        val namePart = parts[0]
        val domainPart = parts[1]

        if (namePart.length <= 2) return email // Ngắn quá không che

        val firstChar = namePart.first()
        val lastChar = namePart.last()
        val stars = "*".repeat(namePart.length - 2)

        return "$firstChar$stars$lastChar@$domainPart"
    }
}