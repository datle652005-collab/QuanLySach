package com.example.quanlysach.ui.activity

import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.cloudinary.android.MediaManager
import com.cloudinary.android.callback.ErrorInfo
import com.cloudinary.android.callback.UploadCallback
import com.example.quanlysach.R
import com.google.firebase.database.FirebaseDatabase
import java.io.File
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.ValueEventListener

class UploadVideoActivity : AppCompatActivity() {


    private lateinit var videoPreview: VideoView
    private lateinit var progressBar: ProgressBar
    private lateinit var btnUpload: Button

    private var bookId: String = ""
    private var uploadedVideoUrl: String = "" // Biến tạm lưu link

    private val pickVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            videoPreview.setVideoURI(uri)
            videoPreview.start()
            findViewById<ImageView>(R.id.iconPlay).visibility = View.GONE
            uploadVideoToCloudinary(uri)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_upload_video)

        bookId = intent.getStringExtra("BOOK_ID") ?: ""
        val bookDesc = intent.getStringExtra("BOOK_DESC") ?: ""
        if (bookId.isNotEmpty()) {
            loadBookName(bookId)
        }
        videoPreview = findViewById(R.id.videoPreview)
        progressBar = findViewById(R.id.progressBar)
        btnUpload = findViewById(R.id.btnUpload)
        btnUpload.isEnabled = false // Khóa nút đến khi upload xong
        val btnBack = findViewById<android.widget.ImageButton>(R.id.btnBack)

        findViewById<EditText>(R.id.etDescription).setText(bookDesc)

        findViewById<FrameLayout>(R.id.btnSelectVideo).setOnClickListener {
            pickVideoLauncher.launch("video/*")
        }

        btnUpload.setOnClickListener {
            if (uploadedVideoUrl.isNotEmpty()) {
                saveToFirebase(uploadedVideoUrl)
            }
        }

        btnBack.setOnClickListener {
            onBackPressedDispatcher.onBackPressed()
        }
    }

    // load name sách
    private fun loadBookName(bookId: String) {
        val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

        FirebaseDatabase.getInstance(dbUrl).getReference("Books").child(bookId)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val bookName = snapshot.child("tenSach").getValue(String::class.java) ?: "Không rõ tên"

                        findViewById<TextView>(R.id.tvBookName).text = String.format("Sách: %s", bookName)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun uploadVideoToCloudinary(uri: Uri) {
        progressBar.visibility = View.VISIBLE
        btnUpload.isEnabled = false

        // Bước 1: Copy file sang cache để tạo đường dẫn vật lý (File Path)
        val file = File(cacheDir, "temp_video.mp4")
        contentResolver.openInputStream(uri)?.use { inputStream ->
            file.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }

        // Bước 2: Upload file từ đường dẫn vật lý
        MediaManager.get().upload(file.absolutePath)
            .option("resource_type", "video") // Cực kỳ quan trọng để Cloudinary biết là video
            .callback(object : UploadCallback {
                override fun onStart(requestId: String?) {}
                override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                    runOnUiThread { progressBar.progress = (bytes * 100 / totalBytes).toInt() }
                }
                override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                    uploadedVideoUrl = resultData?.get("secure_url").toString()
                    runOnUiThread {
                        btnUpload.isEnabled = true
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@UploadVideoActivity, "Video sẵn sàng!", Toast.LENGTH_SHORT).show()
                    }
                }
                override fun onError(requestId: String?, error: ErrorInfo?) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        Toast.makeText(this@UploadVideoActivity, "Lỗi: ${error?.description}", Toast.LENGTH_LONG).show()
                    }
                }
                override fun onReschedule(requestId: String?, error: ErrorInfo?) {}
            }).dispatch()
    }

    private fun saveToFirebase(url: String) {
        if (bookId.isEmpty()) {
            Toast.makeText(this, "Lỗi: Không tìm thấy ID sách!", Toast.LENGTH_SHORT).show()
            return
        }
        val db = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
        val desc = findViewById<EditText>(R.id.etDescription).text.toString().trim()

        // 1. Lưu link video vào Books (Giữ nguyên logic cũ của bạn)
        db.getReference("Books/$bookId/videoUrl").setValue(url)

        // 2. Sinh khóa ngẫu nhiên và đẩy vào node Videos khớp 100% cấu trúc ViewModel đọc
        val videoRef = db.getReference("Videos").push()
        val videoKey = videoRef.key ?: ""

        val videoData = mapOf(
            "id" to videoKey,
            "url" to url,
            "title" to desc,
            "description" to desc,
            "bookId" to bookId,
            "likeCount" to 0L
        )

        videoRef.setValue(videoData)
            .addOnSuccessListener {
                Toast.makeText(this, "Upload thành công!", Toast.LENGTH_SHORT).show()
                finish()
            }
    }
}