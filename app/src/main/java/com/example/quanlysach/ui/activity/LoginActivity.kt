package com.example.quanlysach.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class LoginActivity : AppCompatActivity() {

    private lateinit var edtUser: TextInputEditText
    private lateinit var edtPass: TextInputEditText
    private lateinit var btnLogin: MaterialButton
    private lateinit var tvRegister: TextView
    private lateinit var tvForgot: TextView
    private lateinit var btnBack: ImageView
    private lateinit var cbRemember: CheckBox

    private val databaseUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        initViews()
        loadRememberedAccount()
        setupEvents()
    }

    private fun initViews() {
        edtUser = findViewById(R.id.edtUsername)
        edtPass = findViewById(R.id.edtPassword)
        btnLogin = findViewById(R.id.btnLogin)
        tvRegister = findViewById(R.id.tvRegisterLink)
        tvForgot = findViewById(R.id.tvForgotPassword)
        btnBack = findViewById(R.id.btnBack)
        cbRemember = findViewById(R.id.cbRemember)
    }

    private fun loadRememberedAccount() {
        val sharedPref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val isRemembered = sharedPref.getBoolean("IS_REMEMBERED", false)

        if (isRemembered) {
            val savedUser = sharedPref.getString("REMEMBER_USER", "")
            val savedPass = sharedPref.getString("REMEMBER_PASS", "")
            edtUser.setText(savedUser)
            edtPass.setText(savedPass)
            cbRemember.isChecked = true
        }
    }

    private fun setupEvents() {
        // Xử lý nút Back
        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }

        // Xử lý nút Đăng nhập chính
        btnLogin.setOnClickListener {
            handleLogin()
        }

        // Chuyển sang màn hình Đăng ký
        tvRegister.setOnClickListener {
            startActivity(Intent(this, RegisterActivity::class.java))
        }

        // Chuyển sang màn hình Quên mật khẩu
        tvForgot.setOnClickListener {
            startActivity(Intent(this, ForgotPasswordActivity::class.java))
        }
    }

    private fun handleLogin() {
        val user = edtUser.text.toString().trim()
        val pass = edtPass.text.toString().trim()

        if (user.isEmpty() || pass.isEmpty()) {
            Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
            return
        }

        val invalidChars = listOf(".", "#", "$", "[", "]")
        for (char in invalidChars) {
            if (user.contains(char)) {
                Toast.makeText(this, "Tên đăng nhập không hợp lệ (không chứa dấu chấm, @gmail...)", Toast.LENGTH_LONG).show()
                return
            }
        }

        btnLogin.isEnabled = false
        btnLogin.text = "Đang xử lý..."

        val encryptedPass = hashPassword(pass)
        val db = FirebaseDatabase.getInstance(databaseUrl).getReference("Users")

        db.child(user).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val dbPass = snapshot.child("mat_khau").value.toString()
                val role = snapshot.child("vai_tro").value.toString()
                val email = snapshot.child("email").value.toString()
                val status = snapshot.child("trang_thai").value.toString()
                // 🔥 Lấy thêm trường avata từ Firebase (khớp với biến avata trong data class User)
                val avatar = snapshot.child("avata").value?.toString() ?: ""

                if (dbPass == encryptedPass) {
                    if (status == "bi_khoa") {
                        Toast.makeText(this, "Tài khoản bị khóa. Liên hệ Admin!", Toast.LENGTH_LONG).show()
                        resetLoginButton()
                        return@addOnSuccessListener
                    }

                    // 🔥 Truyền thêm avatar vào hàm lưu
                    saveUserData(user, pass, role, email, avatar)

                    Toast.makeText(this, "Đăng nhập thành công!", Toast.LENGTH_SHORT).show()

                    if (role == "admin") {
                        startActivity(Intent(this, AdminActivity::class.java))
                    } else {
                        startActivity(Intent(this, MainActivity::class.java))
                    }
                    finish()
                } else {
                    Toast.makeText(this, "Mật khẩu không chính xác!", Toast.LENGTH_SHORT).show()
                    resetLoginButton()
                }
            } else {
                Toast.makeText(this, "Tài khoản không tồn tại!", Toast.LENGTH_SHORT).show()
                resetLoginButton()
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Lỗi kết nối máy chủ!", Toast.LENGTH_SHORT).show()
            resetLoginButton()
        }
    }

    private fun saveUserData(user: String, pass: String, role: String, email: String, avatar: String) {
        val sharedPref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putString("USERNAME", user)
            putString("VAI_TRO", role)
            putString("USER_EMAIL", email)
            putString("USER_AVATAR_URL", avatar) // 🔥 Thực hiện lưu link ảnh vào SharedPreferences

            if (cbRemember.isChecked) {
                putBoolean("IS_REMEMBERED", true)
                putString("REMEMBER_USER", user)
                putString("REMEMBER_PASS", pass)
            } else {
                putBoolean("IS_REMEMBERED", false)
                remove("REMEMBER_USER")
                remove("REMEMBER_PASS")
            }
            apply()
        }
    }

    private fun resetLoginButton() {
        btnLogin.isEnabled = true
        btnLogin.text = "Đăng nhập"
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}