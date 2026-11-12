package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import com.example.quanlysach.utils.setupBackButton
import com.google.firebase.database.FirebaseDatabase
import java.security.MessageDigest

class ResetPasswordActivity : AppCompatActivity() {

    private val databaseUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_password)

        setupBackButton(R.id.btnBack)

        val username = intent.getStringExtra("USER_NAME")

        val edtNewPass = findViewById<EditText>(R.id.edtNewPass)
        val edtConfirmPass = findViewById<EditText>(R.id.edtConfirmPass)
        val btnSave = findViewById<Button>(R.id.btnSavePass)

        btnSave.setOnClickListener {

            val p1 = edtNewPass.text.toString().trim()
            val p2 = edtConfirmPass.text.toString().trim()

            if (p1.isEmpty() || p2.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đầy đủ mật khẩu!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (p1 != p2) {
                Toast.makeText(this, "Mật khẩu không khớp!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (p1.length < 6) {
                Toast.makeText(this, "Mật khẩu phải >= 6 ký tự!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (username != null) {

                val dbRef = FirebaseDatabase
                    .getInstance(databaseUrl)
                    .getReference("Users")

                val hashedPass = hashPassword(p1)

                dbRef.child(username).child("mat_khau").setValue(hashedPass)
                    .addOnCompleteListener { task ->

                        if (task.isSuccessful) {

                            Toast.makeText(this, "Đổi mật khẩu thành công!", Toast.LENGTH_LONG).show()

                            val intent = Intent(this, LoginActivity::class.java)
                            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                            startActivity(intent)
                            finish()

                        } else {
                            Toast.makeText(this, "Lỗi: ${task.exception?.message}", Toast.LENGTH_SHORT).show()
                        }
                    }

            } else {
                Toast.makeText(this, "Không tìm thấy user!", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }
}