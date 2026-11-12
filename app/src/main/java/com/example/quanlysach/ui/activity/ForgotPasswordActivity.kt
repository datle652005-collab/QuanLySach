package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.quanlysach.R
import com.example.quanlysach.utils.MailHelper
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ForgotPasswordActivity : AppCompatActivity() {

    private val databaseUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_forgot_password)

        // 1. Ánh xạ View
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnSend = findViewById<Button>(R.id.btnSendOTP)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtForgotEmail)

        // Nút quay lại
        btnBack.setOnClickListener {
            finish()
        }

        // Nút gửi mã OTP
        btnSend.setOnClickListener {
            val emailInput = edtEmail.text.toString().trim().lowercase()

            if (emailInput.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập Email!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Hiển thị trạng thái đang xử lý
            Toast.makeText(this, "Đang kiểm tra tài khoản...", Toast.LENGTH_SHORT).show()

            val dbRef = FirebaseDatabase.getInstance(databaseUrl).getReference("Users")

            // Kiểm tra xem Email có tồn tại trong hệ thống không
            dbRef.orderByChild("email").equalTo(emailInput)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            var username = ""
                            // Lấy username từ dữ liệu tìm được
                            for (userSnapshot in snapshot.children) {
                                username = userSnapshot.key.toString()
                            }

                            // Tiến hành gửi OTP
                            guiOTP(emailInput, username)
                        } else {
                            Toast.makeText(this@ForgotPasswordActivity, "Email này chưa được đăng ký!", Toast.LENGTH_LONG).show()
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        Toast.makeText(this@ForgotPasswordActivity, "Lỗi Firebase: ${error.message}", Toast.LENGTH_SHORT).show()
                    }
                })
        }
    }

    /**
     * Hàm xử lý gửi OTP qua Email và chuyển màn hình
     */
    private fun guiOTP(email: String, username: String) {
        val otp = (100000..999999).random().toString()

        // Sử dụng Coroutine với Dispatchers.IO để xử lý tác vụ mạng ngầm
        lifecycleScope.launch(Dispatchers.IO) {
            try {
                // THỰC HIỆN GỬI MAIL (Chạy ở luồng phụ)
                MailHelper.sendOTP(email, otp)

                // Sau khi gửi xong, quay về Dispatchers.Main để cập nhật giao diện
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "Mã OTP đã được gửi vào Email của bạn!", Toast.LENGTH_SHORT).show()

                    val intent = Intent(this@ForgotPasswordActivity, VerifyOTPActivity::class.java)
                    intent.putExtra("SENT_OTP", otp)
                    intent.putExtra("USER_EMAIL", email)
                    intent.putExtra("USER_NAME", username)
                    startActivity(intent)
                }

            } catch (e: Exception) {
                // Xử lý khi gặp lỗi gửi mail (phải về luồng chính mới hiện Toast được)
                withContext(Dispatchers.Main) {
                    Toast.makeText(this@ForgotPasswordActivity, "Lỗi gửi mail: ${e.message}", Toast.LENGTH_LONG).show()
                    Log.e("MailError", "Chi tiết lỗi: ${e.stackTraceToString()}")
                }
            }
        }
    }
}