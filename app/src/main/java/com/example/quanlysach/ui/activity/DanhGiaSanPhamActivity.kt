package com.example.quanlysach.ui.activity

import android.app.ProgressDialog
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.CartItem
import com.example.quanlysach.ui.adapter.ReviewSelectAdapter
import com.example.quanlysach.utils.NotificationHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.util.concurrent.atomic.AtomicInteger

class DanhGiaSanPhamActivity : AppCompatActivity() {

    private var selectedBookId: String = ""
    private val reviewedBooks = mutableSetOf<String>()
    private val uploadedImagesList = mutableListOf<String>()

    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val IMBB_API_KEY = "ed361b16e0c52c67d1122f4d031bbe85"

    private lateinit var progressDialog: ProgressDialog

    private val pickImagesLauncher = registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris != null && uris.isNotEmpty()) {
            if (uris.size > 6) {
                Toast.makeText(this, "Chỉ được chọn tối đa 6 ảnh!", Toast.LENGTH_SHORT).show()
            } else {
                uploadMultipleImages(uris)
                findViewById<ImageView>(R.id.imgReviewPreview).setImageURI(uris[0])
                findViewById<ImageView>(R.id.iconCamera).visibility = View.GONE
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_danh_gia_san_pham)

        progressDialog = ProgressDialog(this).apply {
            setMessage("Đang tải ảnh lên...")
            setCancelable(false)
        }

        val listItems = intent.getSerializableExtra("LIST_ITEMS") as? ArrayList<CartItem> ?: arrayListOf()
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""

        val rv = findViewById<RecyclerView>(R.id.rvOrderItemsReview)
        val adapter = ReviewSelectAdapter(listItems, reviewedBooks) { item ->
            selectedBookId = item.bookId
            updateUI(item)
        }
        rv.adapter = adapter

        if (listItems.isNotEmpty()) {
            selectedBookId = listItems[0].bookId
            updateUI(listItems[0])
        }

        findViewById<FrameLayout>(R.id.btnPickImage).setOnClickListener { pickImagesLauncher.launch("image/*") }
        findViewById<ImageView>(R.id.btnBackReview).setOnClickListener { finish() }

        findViewById<Button>(R.id.btnSubmitReview).setOnClickListener {
            val content = findViewById<EditText>(R.id.edtReviewContent).text.toString().trim()
            val rating = findViewById<RatingBar>(R.id.ratingBarInput).rating.toInt()

            if (content.isEmpty() || rating == 0) {
                Toast.makeText(this, "Vui lòng nhập đủ nhận xét và số sao!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            sendReview(rating, content, orderId) {
                reviewedBooks.add(selectedBookId)
                adapter.notifyDataSetChanged()
                val currentItem = listItems.find { it.bookId == selectedBookId }
                if (currentItem != null) updateUI(currentItem)
            }
        }
    }

    private fun uploadMultipleImages(uris: List<Uri>) {
        progressDialog.show()
        uploadedImagesList.clear()
        val totalImages = uris.size
        val successCount = AtomicInteger(0)

        uris.forEach { uri ->
            val base64Image = encodeImageToBase64(uri)
            val client = OkHttpClient()
            val requestBody = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("key", IMBB_API_KEY)
                .addFormDataPart("image", base64Image)
                .build()

            val request = Request.Builder().url("https://api.imgbb.com/1/upload").post(requestBody).build()

            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    checkAndDismiss(successCount.incrementAndGet(), totalImages)
                }

                override fun onResponse(call: Call, response: Response) {
                    if (response.isSuccessful) {
                        val url = JSONObject(response.body?.string()!!).getJSONObject("data").getString("url")
                        uploadedImagesList.add(url)
                    }
                    checkAndDismiss(successCount.incrementAndGet(), totalImages)
                }
            })
        }
    }

    private fun checkAndDismiss(current: Int, total: Int) {
        if (current >= total) {
            runOnUiThread {
                progressDialog.dismiss()
                Toast.makeText(this@DanhGiaSanPhamActivity, "Đã tải lên ${uploadedImagesList.size} ảnh", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }

    private fun sendReview(rating: Int, content: String, orderId: String, onSuccess: () -> Unit) {
        val user = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE).getString("USERNAME", "User") ?: "User"
        val databaseRef = FirebaseDatabase.getInstance(dbUrl).getReference("Reviews").child(selectedBookId).push()

        val data = mapOf(
            "username" to user,
            "rating" to rating,
            "content" to content,
            "timestamp" to System.currentTimeMillis(),
            "images" to uploadedImagesList
        )

        databaseRef.setValue(data).addOnSuccessListener {
            // 🌟 TĂNG BADGE ĐÁNH GIÁ MỚI TRÊN FIREBASE
            FirebaseDatabase.getInstance(dbUrl).getReference("BookReviewsCount").child(selectedBookId)
                .setValue(ServerValue.increment(1))

            // Gửi thông báo chung cho Admin
            NotificationHelper.sendAdminNotification("review")

            if (orderId.isNotEmpty()) {
                FirebaseDatabase.getInstance(dbUrl).getReference("Orders").child(orderId).child("reviewed").setValue(true)
            }
            Toast.makeText(this, "🎉 Đánh giá thành công!", Toast.LENGTH_SHORT).show()
            findViewById<EditText>(R.id.edtReviewContent).setText("")
            uploadedImagesList.clear()
            onSuccess()
        }
    }

    private fun updateUI(item: CartItem) {
        findViewById<TextView>(R.id.tvCurrentBookName).text = item.tenSach
        val isDone = reviewedBooks.contains(item.bookId)
        findViewById<LinearLayout>(R.id.layoutInputReview).alpha = if (isDone) 0.5f else 1.0f
        findViewById<Button>(R.id.btnSubmitReview).apply {
            isEnabled = !isDone
            text = if (isDone) "Đã hoàn thành đánh giá" else "Gửi đánh giá"
        }
    }
}