package com.example.quanlysach.ui.activity

import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.example.quanlysach.utils.setupBackButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_register)

        setupBackButton(R.id.btnBack)

        val edtUser = findViewById<TextInputEditText>(R.id.edtRegUser)
        val edtEmail = findViewById<TextInputEditText>(R.id.edtRegEmail)
        val edtPass = findViewById<TextInputEditText>(R.id.edtRegPass)
        val btnReg = findViewById<Button>(R.id.btnRegister)

        val db = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Users")
        // 1. Ánh xạ đúng ID mới từ file XML
        val tvLoginLink = findViewById<TextView>(R.id.tvLoginLink)

        // 2. Xử lý khi bấm vào "Đã có tài khoản? Đăng nhập"
        tvLoginLink.setOnClickListener {
            // Thay vì dùng Intent mở lại Login (làm nặng máy),
            // mình dùng finish() để đóng trang Đăng ký và quay về trang Đăng nhập có sẵn ở bên dưới.
            finish()
        }
        btnReg.setOnClickListener {
            Log.d("DEBUG_HUY", "Đã nhận lệnh Click nút Đăng ký!")
            val user = edtUser.text.toString().trim()
            val email = edtEmail.text.toString().trim()
            val pass = edtPass.text.toString().trim()

            if (user.isEmpty() || email.isEmpty() || pass.isEmpty()) {
                Toast.makeText(this, "Vui lòng không để trống!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // 1. Kiểm tra tài khoản đã tồn tại chưa
            db.child(user).get().addOnSuccessListener { snapshot ->
                if (snapshot.exists()) {
                    Toast.makeText(this, "Tên đăng nhập đã tồn tại!", Toast.LENGTH_SHORT).show()
                } else {
                    // 2. Tạo object user mới (mặc định vai trò là khách hàng)
                    val newUser = mapOf(
                        "email" to email,
                        "mat_khau" to hashPassword(pass),
                        "vai_tro" to "khach_hang"
                    )

                    // 3. Lưu vào Firebase
                    db.child(user).setValue(newUser)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Đăng ký thành công!", Toast.LENGTH_SHORT).show()
                            finish()
                        }
                        .addOnFailureListener { e ->
                            // NẾU LỖI, NÓ SẼ BÁO Ở ĐÂY
                            Toast.makeText(this, "Lỗi: ${e.message}", Toast.LENGTH_LONG).show()
                            Log.e("FIREBASE_ERROR", "Lỗi lưu: ${e.message}")
                        }
                }
            }
        }
    }

    // Hàm mã hóa mật khẩu giống LoginActivity để so khớp được
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}