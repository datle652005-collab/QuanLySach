package com.example.quanlysach.ui.activity

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Book
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class AddBookActivity : AppCompatActivity() {

    private lateinit var imgBookPreview: ImageView
    private var uploadedImageUrl: String = ""
    private var uploadedVideoUrl: String = ""

    private val IMBB_API_KEY = "ed361b16e0c52c67d1122f4d031bbe85"

    // 🌟 BỘ CHỌN ẢNH TỪ ĐIỆN THOẠI
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            Glide.with(this).load(uri).into(imgBookPreview)
            uploadImageToImgBB(uri)
        }
    }

    private var videoUri: Uri? = null

    // 🌟 BỘ CHỌN VIDEO TỪ ĐIỆN THOẠI
    private val selectVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            videoUri = uri

            val layoutVideoPreview = findViewById<FrameLayout>(R.id.layoutVideoPreview)
            val tvSelectedVideo = findViewById<TextView>(R.id.tvSelectedVideo)
            val videoPreview = findViewById<VideoView>(R.id.videoPreview)

            layoutVideoPreview.visibility = View.VISIBLE
            tvSelectedVideo.text = "Đã chọn 1 video"

            videoPreview.setVideoURI(uri)
            videoPreview.seekTo(1) // Xem trước frame đầu tiên của video
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_add_book)

        val edtTen = findViewById<EditText>(R.id.edtTenSach)
        val edtTacGia = findViewById<EditText>(R.id.edtTacGia)
        val edtGia = findViewById<EditText>(R.id.edtGia)
        val edtSoLuong = findViewById<EditText>(R.id.edtSoLuong)
        val spinnerTheLoai = findViewById<Spinner>(R.id.spinnerTheLoai)
        val edtTags = findViewById<EditText>(R.id.edtTags)
        val edtMoTa = findViewById<EditText>(R.id.edtMoTa)

        val btnSelectImage = findViewById<Button>(R.id.btnSelectImage)
        imgBookPreview = findViewById(R.id.imgBookPreview)

        val btnAdd = findViewById<Button>(R.id.btnAddBook)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        btnBack.setOnClickListener { finish() }

        btnSelectImage.setOnClickListener {
            pickImageLauncher.launch("image/*")
        }

        // Cấu hình danh sách Thể loại cho Spinner
        val categories = listOf("Thể loại", "Cuộc sống", "Ngôn tình", "Kinh tế", "Thiếu nhi", "Văn học", "Kỹ năng")
        val adapterSpinner = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)
        spinnerTheLoai.adapter = adapterSpinner
        spinnerTheLoai.setSelection(0)

        // Bắt sự kiện chọn video từ máy
        findViewById<com.google.android.material.button.MaterialButton>(R.id.btnSelectVideo).setOnClickListener {
            selectVideoLauncher.launch("video/*")
        }

        // Phát/Tạm dừng video preview khi bấm nút Play chính giữa
        val videoPreview = findViewById<VideoView>(R.id.videoPreview)
        findViewById<ImageView>(R.id.btnPlayPreview).setOnClickListener {
            if (videoPreview.isPlaying) {
                videoPreview.pause()
            } else {
                videoPreview.start()
            }
        }

        // KHI BẤM NÚT "LƯU SÁCH MỚI"
        btnAdd.setOnClickListener {
            val ten = edtTen.text.toString().trim()
            val tacGia = edtTacGia.text.toString().trim()
            val giaStr = edtGia.text.toString().trim()
            val soLuongStr = edtSoLuong.text.toString().trim()
            val theLoai = spinnerTheLoai.selectedItem.toString()

            val tagsInput = edtTags.text.toString().trim()
            val tags = if (tagsInput.isNotEmpty()) tagsInput.split(",").map { it.trim() } else emptyList()
            val moTa = edtMoTa.text.toString().trim()

            // Kiểm tra thông tin bắt buộc
            if (ten.isEmpty() || giaStr.isEmpty()) {
                Toast.makeText(this, "Vui lòng nhập đủ thông tin!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (uploadedImageUrl.isEmpty()) {
                Toast.makeText(this, "Vui lòng chọn ảnh bìa và chờ tải lên!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (theLoai == "Thể loại") {
                Toast.makeText(this, "Vui lòng chọn thể loại!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val gia = giaStr.toIntOrNull() ?: 0
            val soLuong = soLuongStr.toIntOrNull() ?: 0
            val id = "BOOK_" + System.currentTimeMillis()

            // Rẽ nhánh xử lý dựa vào việc có chọn video từ máy hay không
            if (videoUri != null) {
                // Có video -> Chạy tiến trình tải video lên trước, rồi tự động lưu sách sau
                uploadVideoAndSaveBook(id, ten, tacGia, gia, soLuong, moTa, theLoai, tags)
            } else {
                // Không có video -> Lưu trực tiếp thông tin sách lên Realtime Database với videoUrl rỗng
                saveBookToFirebase(id, ten, tacGia, gia, soLuong, moTa, theLoai, tags, "")
            }
        }
    }

    // 🌟 HÀM TẢI VIDEO TRỰC TIẾP LÊN FIREBASE STORAGE
    private fun uploadVideoAndSaveBook(
        id: String, ten: String, tacGia: String, gia: Int, soLuong: Int,
        moTa: String, theLoai: String, tags: List<String>
    ) {
        val progressVideoUpload = findViewById<ProgressBar>(R.id.progressVideoUpload)
        val tvVideoProgress = findViewById<TextView>(R.id.tvVideoProgress)

        progressVideoUpload.visibility = View.VISIBLE
        tvVideoProgress.visibility = View.VISIBLE

        // Tạo đường dẫn lưu file trên Firebase Storage dựa theo thời gian thực
        val storageRef = FirebaseStorage.getInstance()
            .getReference("book_videos/${System.currentTimeMillis()}.mp4")

        storageRef.putFile(videoUri!!)
            .addOnProgressListener { taskSnapshot ->
                // Cập nhật phần trăm tiến trình tải lên
                val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount).toInt()
                progressVideoUpload.progress = progress
                tvVideoProgress.text = String.format("%d%%", progress)
            }
            .addOnSuccessListener {
                // Tải lên Storage thành công -> Lấy URL phân phối trực tiếp công khai
                storageRef.downloadUrl.addOnSuccessListener { uri ->
                    uploadedVideoUrl = uri.toString()
                    // Gọi hàm ghi thông tin vào Realtime Database
                    saveBookToFirebase(id, ten, tacGia, gia, soLuong, moTa, theLoai, tags, uploadedVideoUrl)
                }
            }
            .addOnFailureListener { e ->
                progressVideoUpload.visibility = View.GONE
                tvVideoProgress.visibility = View.GONE
                Toast.makeText(this, "❌ Lỗi tải video: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // 🌟 HÀM GHI DỮ LIỆU SÁCH LÊN FIREBASE REALTIME DATABASE
    private fun saveBookToFirebase(
        id: String, ten: String, tacGia: String, gia: Int, soLuong: Int,
        moTa: String, theLoai: String, tags: List<String>, videoUrl: String
    ) {
        val db = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/").getReference("Books")

        val book = Book(
            id = id,
            tenSach = ten,
            tacGia = tacGia,
            gia = gia,
            moTa = moTa,
            hinhAnh = uploadedImageUrl, // Đường dẫn ảnh từ ImgBB
            soLuong = soLuong,
            theLoai = theLoai,
            tags = tags,
            timestamp = System.currentTimeMillis(),
            videoUrl = videoUrl // Đường dẫn video từ Firebase Storage
        )

        db.child(id).setValue(book)
            .addOnSuccessListener {
                Toast.makeText(this, "🎉 Thêm sách thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
            .addOnFailureListener { e ->
                Toast.makeText(this, "❌ Lỗi: ${e.message}", Toast.LENGTH_SHORT).show()
            }
    }

    // =========================================================
    // HÀM XỬ LÝ ẢNH VÀ TẢI LÊN IMGBB
    // =========================================================
    private fun uploadImageToImgBB(uri: Uri) {
        Toast.makeText(this, "Đang tải ảnh bìa lên...", Toast.LENGTH_SHORT).show()
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
                runOnUiThread { Toast.makeText(this@AddBookActivity, "Lỗi mạng khi tải ảnh!", Toast.LENGTH_SHORT).show() }
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData!!)
                    uploadedImageUrl = jsonObject.getJSONObject("data").getString("url")

                    runOnUiThread {
                        Toast.makeText(this@AddBookActivity, "Tải ảnh hoàn tất! Có thể thêm sách.", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    runOnUiThread { Toast.makeText(this@AddBookActivity, "Lỗi API ImgBB", Toast.LENGTH_SHORT).show() }
                }
            }
        })
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos)
        val b = baos.toByteArray()
        return Base64.encodeToString(b, Base64.DEFAULT)
    }
}