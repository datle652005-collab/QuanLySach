package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.example.quanlysach.utils.setupBackButton

class VerifyOTPActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_verify_otp)

        setupBackButton(R.id.btnBack)

        // Nhận dữ liệu từ ForgotPassword
        val otpGoc = intent.getStringExtra("SENT_OTP")
        val email = intent.getStringExtra("USER_EMAIL")
        val username = intent.getStringExtra("USER_NAME") // 🔥 THÊM

        val edtOTP = findViewById<EditText>(R.id.edtOTP)
        val btnVerify = findViewById<Button>(R.id.btnVerify)

        btnVerify.setOnClickListener {

            val otpNhap = edtOTP.text.toString().trim()

            if (otpNhap.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập OTP!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (otpNhap == otpGoc) {

                Toast.makeText(this, "Xác minh thành công!", Toast.LENGTH_SHORT).show()

                val intent = Intent(this, ResetPasswordActivity::class.java)
                intent.putExtra("USER_EMAIL", email)
                intent.putExtra("USER_NAME", username) // 🔥 QUAN TRỌNG

                startActivity(intent)
                finish()

            } else {
                Toast.makeText(this, "Mã OTP không đúng!", Toast.LENGTH_SHORT).show()
            }
        }
    }
}