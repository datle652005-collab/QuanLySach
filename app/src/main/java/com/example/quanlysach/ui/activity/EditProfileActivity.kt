package com.example.quanlysach.ui.activity

import android.app.DatePickerDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.User

import com.example.quanlysach.utils.setupBackButton
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.*

class EditProfileActivity : AppCompatActivity() {

    private lateinit var edtFullName: EditText
    private lateinit var edtBio: EditText
    private lateinit var edtPhone: EditText
    private lateinit var tvGender: TextView
    private lateinit var tvDOB: TextView
    private lateinit var tvUsernameFixed: TextView
    private lateinit var tvEmailFixed: TextView
    private lateinit var imgEditAvatar: ImageView

    private var currentUsername: String = ""

    // 🌟 QUAN TRỌNG: DÁN API KEY CỦA BẠN VÀO GIỮA 2 DẤU NGOẶC KÉP DƯỚI ĐÂY
    private val IMBB_API_KEY = "ed361b16e0c52c67d1122f4d031bbe85"

    private val dbRef: DatabaseReference by lazy {
        FirebaseDatabase
            .getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("Users")
    }

    // 🌟 BỘ CHỌN ẢNH TỪ ĐIỆN THOẠI
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Glide.with(this).load(uri).into(imgEditAvatar) // Hiện ảnh lên cho khách xem trước
            uploadImageToImgBB(uri) // Bắt đầu đẩy lên mạng
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        setupBackButton(R.id.btnBack)
        initViews()
        loadCurrentData()

        findViewById<LinearLayout>(R.id.btnSelectGender).setOnClickListener { showGenderDialog() }
        findViewById<LinearLayout>(R.id.btnSelectDOB).setOnClickListener { showDatePicker() }
        findViewById<TextView>(R.id.btnSave).setOnClickListener { saveUserData() }

        // 🌟 SỰ KIỆN: BẤM VÀO ẢNH SẼ MỞ THƯ VIỆN ĐIỆN THOẠI
        imgEditAvatar.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }
    }

    private fun initViews() {
        edtFullName = findViewById(R.id.edtFullName)
        edtBio = findViewById(R.id.edtBio)
        edtPhone = findViewById(R.id.edtPhone)
        tvGender = findViewById(R.id.tvGender)
        tvDOB = findViewById(R.id.tvDOB)
        tvUsernameFixed = findViewById(R.id.tvUsernameFixed)
        tvEmailFixed = findViewById(R.id.tvEmailFixed)
        imgEditAvatar = findViewById(R.id.imgEditAvatar)
    }

    private fun loadCurrentData() {
        val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        currentUsername = pref.getString("USERNAME", "") ?: ""
        tvUsernameFixed.text = currentUsername

        dbRef.child(currentUsername).get().addOnSuccessListener { snapshot ->
            if (snapshot.exists()) {
                val user = snapshot.getValue(User::class.java)
                user?.let {
                    edtFullName.setText(it.ten_user)
                    edtBio.setText(it.tieu_su)
                    edtPhone.setText(it.sdt)
                    tvGender.text = if (it.gioi_tinh.isEmpty()) "Thiết lập ngay" else it.gioi_tinh
                    tvDOB.text = if (it.ngay_sinh.isEmpty()) "28/09/2005" else it.ngay_sinh
                    tvEmailFixed.text = it.email

                    if (it.avata.isNotEmpty()) {
                        Glide.with(this).load(it.avata).into(imgEditAvatar)
                    }
                }
            }
        }.addOnFailureListener {
            Toast.makeText(this, "Không tìm thấy Database!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserData() {
        val newName = edtFullName.text.toString().trim()
        val newBio = edtBio.text.toString().trim()
        val newPhone = edtPhone.text.toString().trim()
        val newGender = tvGender.text.toString()
        val newDOB = tvDOB.text.toString()

        if (newName.isEmpty()) {
            Toast.makeText(this, "Bạn quên nhập tên kìa!", Toast.LENGTH_SHORT).show()
            return
        }

        val updates = mapOf(
            "ten_user" to newName,
            "tieu_su" to newBio,
            "sdt" to newPhone,
            "gioi_tinh" to newGender,
            "ngay_sinh" to newDOB
        )

        dbRef.child(currentUsername).updateChildren(updates)
            .addOnSuccessListener {
                Toast.makeText(this, "Hồ sơ đã được lưu! ✨", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener {
                Toast.makeText(this, "Lỗi kết nối Firebase!", Toast.LENGTH_SHORT).show()
            }
    }

    private fun showDatePicker() {
        val c = Calendar.getInstance()
        val datePicker = DatePickerDialog(this, { _, year, month, day ->
            tvDOB.text = String.format("%02d/%02d/%d", day, month + 1, year)
        }, 2005, 8, 28)
        datePicker.show()
    }

    private fun showGenderDialog() {
        val genders = arrayOf("Nam", "Nữ", "Khác")
        AlertDialog.Builder(this)
            .setTitle("Chọn giới tính")
            .setItems(genders) { _, which ->
                tvGender.text = genders[which]
            }.show()
    }

    // ======================================================================
    // 🌟 PHẦN XỬ LÝ UPLOAD ẢNH LÊN IMGBB VÀ LƯU VÀO FIREBASE
    // ======================================================================

    private fun uploadImageToImgBB(uri: Uri) {
        Toast.makeText(this, "Đang tải ảnh lên... Vui lòng đợi nhé!", Toast.LENGTH_SHORT).show()

        val base64Image = encodeImageToBase64(uri)
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", IMBB_API_KEY)
            .addFormDataPart("image", base64Image)
            .build()

        val request = Request.Builder()
            .url("https://api.imgbb.com/1/upload")
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                runOnUiThread { Toast.makeText(this@EditProfileActivity, "Lỗi mạng khi tải ảnh!", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData!!)
                    val imageUrl = jsonObject.getJSONObject("data").getString("url")

                    saveAvatarUrlToFirebase(imageUrl) // Cất link vào Firebase
                } else {
                    runOnUiThread { Toast.makeText(this@EditProfileActivity, "API Key chưa đúng hoặc lỗi máy chủ ImgBB", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun saveAvatarUrlToFirebase(imageUrl: String) {
        dbRef.child(currentUsername).child("avata").setValue(imageUrl).addOnSuccessListener {
            runOnUiThread {
                Toast.makeText(this, "Đổi ảnh đại diện thành công! ✨", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos) // Nén 60% cho nhẹ, upload cho lẹ
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }
}
