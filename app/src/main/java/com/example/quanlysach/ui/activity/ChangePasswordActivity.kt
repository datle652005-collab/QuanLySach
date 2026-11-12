package com.example.quanlysach.ui.activity

import android.os.Bundle
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class ChangePasswordActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_change_password)

        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val btnUpdate = findViewById<Button>(R.id.btnUpdatePass)
        val edtOld = findViewById<TextInputEditText>(R.id.edtOldPass)
        val edtNew = findViewById<TextInputEditText>(R.id.edtNewPass)
        val edtConfirm = findViewById<TextInputEditText>(R.id.edtConfirmPass)

        btnBack.setOnClickListener { finish() }

        // ✅ Lấy username từ SharedPreferences (chuẩn hệ thống của bạn)
        val sharedPref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        val username = sharedPref.getString("USERNAME", null)

        if (username == null) {
            Toast.makeText(this, "Chưa đăng nhập!", Toast.LENGTH_SHORT).show()
            finish()
            return
        }

        val db = FirebaseDatabase
            .getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("Users")

        btnUpdate.setOnClickListener {

            val oldPass = edtOld.text.toString().trim()
            val newPass = edtNew.text.toString().trim()
            val confirmPass = edtConfirm.text.toString().trim()

            // 🔹 Validate
            if (oldPass.isEmpty() || newPass.isEmpty() || confirmPass.isEmpty()) {
                Toast.makeText(this, "Nhập đầy đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass != confirmPass) {
                Toast.makeText(this, "Mật khẩu xác nhận không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (newPass.length < 6) {
                Toast.makeText(this, "Mật khẩu >= 6 ký tự!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val oldHash = hashPassword(oldPass)
            val newHash = hashPassword(newPass)

            // 🔥 Kiểm tra mật khẩu cũ
            db.child(username).get().addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {
                    Toast.makeText(this, "User không tồn tại!", Toast.LENGTH_SHORT).show()
                    return@addOnSuccessListener
                }

                val passDB = snapshot.child("mat_khau").value.toString()

                if (passDB == oldHash) {

                    // ✅ Update mật khẩu mới
                    db.child(username).child("mat_khau").setValue(newHash)
                        .addOnSuccessListener {
                            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show()
                            finish()
                        }
                        .addOnFailureListener {
                            Toast.makeText(this, "Lỗi cập nhật!", Toast.LENGTH_SHORT).show()
                        }

                } else {
                    Toast.makeText(this, "Mật khẩu cũ không đúng!", Toast.LENGTH_SHORT).show()
                }

            }.addOnFailureListener {
                Toast.makeText(this, "Lỗi kết nối Firebase!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // ✅ Hàm hash giống LoginActivity
    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}