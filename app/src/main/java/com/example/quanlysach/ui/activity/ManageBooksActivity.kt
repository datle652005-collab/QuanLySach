package com.example.quanlysach.ui.activity

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Base64
import android.view.View
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Book
import com.example.quanlysach.ui.adapter.AdminBookAdapter

import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.tabs.TabLayout
import com.google.firebase.database.*
import com.google.firebase.storage.FirebaseStorage
import okhttp3.*
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class ManageBooksActivity : AppCompatActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var adapter: AdminBookAdapter
    private var currentTab = 0

    private val list = mutableListOf<Book>()
    private val fullList = mutableListOf<Book>()

    private val dbRef = FirebaseDatabase.getInstance(
        "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    ).getReference("Books")

    private val IMBB_API_KEY = "ed361b16e0c52c67d1122f4d031bbe85"
    private var currentEditImageUrl: String = ""
    private var currentDialogPreview: ImageView? = null

    // 🌟 1. Biến toàn cục quản lý Video được chọn trong Dialog Sửa
    private var currentEditVideoUri: Uri? = null
    private var currentEditVideoUrl: String = ""
    private var currentVideoView: VideoView? = null
    private var currentLayoutVideoPreview: FrameLayout? = null
    private var currentTvSelectedVideo: TextView? = null

    // Bộ chọn ảnh từ máy
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            currentDialogPreview?.let { img -> Glide.with(this).load(it).into(img) }
            uploadImageToImgBB(it)
        }
    }

    // 🌟 2. BỘ ĐĂNG KÝ CHỌN VIDEO TỪ MÁY CHO DIALOG SỬA
    private val selectVideoLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) {
            currentEditVideoUri = uri

            // Cập nhật giao diện xem trước ngay trên Dialog đang mở
            currentLayoutVideoPreview?.visibility = View.VISIBLE
            currentTvSelectedVideo?.text = "Đã chọn video mới"

            currentVideoView?.setVideoURI(uri)
            currentVideoView?.seekTo(1)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_manage_books)

        // Ánh xạ
        rv = findViewById(R.id.rvBooks)
        val btnAdd = findViewById<ImageView>(R.id.btnAddBook)
        val btnBack = findViewById<ImageView>(R.id.btnBack)
        val search = findViewById<EditText>(R.id.edtSearch)
        val tabFilter = findViewById<TabLayout>(R.id.tabFilter)

        adapter = AdminBookAdapter(list,
            onEdit = { showEdit(it) },
            onDelete = { deleteBook(it) },
            onVideo = { book ->
                startActivity(Intent(this, UploadVideoActivity::class.java).apply {
                    putExtra("BOOK_ID", book.id)
                    putExtra("BOOK_NAME", book.tenSach)
                    putExtra("BOOK_DESC", book.moTa)
                })
            },
            onReview = { book ->
                val intent = Intent(this, AllReviewsActivity::class.java)
                intent.putExtra("BOOK_ID", book.id)
                startActivity(intent)
            }
        )

        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        btnAdd.setOnClickListener {
            startActivity(Intent(this, AddBookActivity::class.java))
        }

        btnBack.setOnClickListener { finish() }

        loadBooks()
        setupBottomNav()

        tabFilter.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                currentTab = tab?.position ?: 0
                filterData(search.text.toString())
            }
            override fun onTabUnselected(tab: TabLayout.Tab?) {}
            override fun onTabReselected(tab: TabLayout.Tab?) {}
        })

        search.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { filterData(s.toString()) }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun filterData(text: String) {
        var result = fullList.filter {
            it.tenSach.contains(text, true) ||
                    it.tacGia.contains(text, true) ||
                    it.theLoai.contains(text, true)
        }

        result = when (currentTab) {
            1 -> result.filter { it.soLuong > 0 }
            2 -> result.filter { it.soLuong <= 0 }
            else -> result
        }

        adapter.updateList(result)
    }

    private fun setupBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigationAdmin)
        bottomNav.selectedItemId = R.id.nav_admin_books
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_admin_home -> {
                    startActivity(Intent(this, AdminActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_books -> true
                R.id.nav_admin_users -> {
                    startActivity(Intent(this, ManageUsersActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_orders -> {
                    startActivity(Intent(this, ManageOrdersActivity::class.java))
                    finish(); true
                }
                R.id.nav_admin_reports -> {
                    startActivity(Intent(this, RevenueActivity::class.java))
                    finish(); true
                }
                else -> false
            }
        }
    }

    private fun loadBooks() {
        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val tempList = mutableListOf<Book>()
                for (data in snapshot.children) {
                    try {
                        val book = data.getValue(Book::class.java)
                        book?.let { tempList.add(it.copy(id = data.key ?: "")) }
                    } catch (e: Exception) {
                        // Đọc thủ công nếu tags bị lưu sai kiểu
                        val tagsRaw = data.child("tags").value
                        val tagsList = when (tagsRaw) {
                            is String -> tagsRaw.split(",").map { it.trim() }
                            is List<*> -> tagsRaw.filterIsInstance<String>()
                            else -> emptyList()
                        }
                        val book = Book(
                            id = data.key ?: "",
                            tenSach = data.child("tenSach").getValue(String::class.java) ?: "",
                            tacGia = data.child("tacGia").getValue(String::class.java) ?: "",
                            gia = data.child("gia").getValue(Int::class.java) ?: 0,
                            giaGiam = data.child("giaGiam").getValue(Int::class.java) ?: 0,
                            moTa = data.child("moTa").getValue(String::class.java) ?: "",
                            hinhAnh = data.child("hinhAnh").getValue(String::class.java) ?: "",
                            soLuong = data.child("soLuong").getValue(Int::class.java) ?: 0,
                            theLoai = data.child("theLoai").getValue(String::class.java) ?: "",
                            tags = tagsList,
                            videoUrl = data.child("videoUrl").getValue(String::class.java) ?: "",
                            rating = data.child("rating").getValue(Float::class.java) ?: 0f,
                            daBan = data.child("daBan").getValue(Int::class.java) ?: 0
                        )
                        tempList.add(book)
                    }
                }
                fullList.clear()
                fullList.addAll(tempList)
                filterData(findViewById<EditText>(R.id.edtSearch).text.toString())
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@ManageBooksActivity, error.message, Toast.LENGTH_SHORT).show()
            }
        })
    }

    // 🌟 3. CẬP NHẬT TOÀN DIỆN HÀM HIỂN THỊ ĐIỀU CHỈNH THÔNG TIN SÁCH
    private fun showEdit(b: Book) {
        val view = layoutInflater.inflate(R.layout.dialog_book, null)
        val name = view.findViewById<EditText>(R.id.edName)
        val author = view.findViewById<EditText>(R.id.edAuthor)
        val price = view.findViewById<EditText>(R.id.edPrice)
        val soLuong = view.findViewById<EditText>(R.id.edSoLuong)
        val spTheLoai = view.findViewById<Spinner>(R.id.spTheLoai)
        val tags = view.findViewById<EditText>(R.id.edTags)
        val desc = view.findViewById<EditText>(R.id.edDesc)
        val imgBookPreview = view.findViewById<ImageView>(R.id.imgBookPreview)
        val btnSelectImage = view.findViewById<Button>(R.id.btnSelectImage)

        val layoutVideoPreview = view.findViewById<FrameLayout>(R.id.layoutVideoPreview)
        val tvSelectedVideo = view.findViewById<TextView>(R.id.tvSelectedVideo)
        val videoPreview = view.findViewById<VideoView>(R.id.videoPreview)
        val btnSelectVideo = view.findViewById<Button>(R.id.btnSelectVideo)
        val btnPlayPreview = view.findViewById<ImageView>(R.id.btnPlayPreview)
        val progressVideoUpload = view.findViewById<ProgressBar>(R.id.progressVideoUpload)
        val tvVideoProgress = view.findViewById<TextView>(R.id.tvVideoProgress)

        val categories = listOf("Chọn thể loại", "Cuộc sống", "Ngôn tình", "Kinh tế", "Thiếu nhi", "Văn học", "Kỹ năng")
        spTheLoai.adapter = ArrayAdapter(this, android.R.layout.simple_spinner_dropdown_item, categories)

        name.setText(b.tenSach)
        author.setText(b.tacGia)
        price.setText(b.gia.toString())
        soLuong.setText(b.soLuong.toString())
        desc.setText(b.moTa)
        tags.setText(b.tags.joinToString(","))

        var localVideoUri: Uri? = null
        var localVideoUrl = b.videoUrl

        if (b.videoUrl.isNotEmpty()) {
            layoutVideoPreview?.visibility = View.VISIBLE
            tvSelectedVideo?.text = "Sách đã có video cũ"
            videoPreview?.setVideoPath(b.videoUrl)
            videoPreview?.seekTo(1)
        } else {
            layoutVideoPreview?.visibility = View.GONE
            tvSelectedVideo?.text = "Chưa có video giới thiệu"
        }

        val pos = categories.indexOf(b.theLoai)
        spTheLoai.setSelection(if (pos >= 1) pos else 0)

        currentEditImageUrl = b.hinhAnh
        currentDialogPreview = imgBookPreview
        if (b.hinhAnh.isNotEmpty()) {
            com.bumptech.glide.Glide.with(this).load(b.hinhAnh).into(imgBookPreview)
        }

        btnSelectImage.setOnClickListener { pickImageLauncher.launch("image/*") }

        val dialogVideoLauncher = activityResultRegistry.register(
            "dialog_pick_video_${System.currentTimeMillis()}",
            androidx.activity.result.contract.ActivityResultContracts.GetContent()
        ) { uri: Uri? ->
            if (uri != null) {
                localVideoUri = uri
                layoutVideoPreview?.visibility = View.VISIBLE
                tvSelectedVideo?.text = "Đã chọn video mới từ máy"
                videoPreview?.setVideoURI(uri)
                videoPreview?.start()
            }
        }

        btnSelectVideo.setOnClickListener { dialogVideoLauncher.launch("video/*") }

        btnPlayPreview.setOnClickListener {
            if (videoPreview.isPlaying) videoPreview.pause() else videoPreview.start()
        }

        val builder = AlertDialog.Builder(this)
            .setTitle("Sửa sách")
            .setView(view)
            .setPositiveButton("Lưu", null)
            .setNegativeButton("Hủy", null)

        val dialog = builder.create()
        dialog.show()

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener {
            val selectedType = spTheLoai.selectedItem.toString()
            if (selectedType == "Chọn thể loại") {
                Toast.makeText(this, "Vui lòng chọn thể loại!", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val newTags = tags.text.toString().split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val finalName = name.text.toString()
            val finalAuthor = author.text.toString()
            val finalPrice = price.text.toString().toIntOrNull() ?: 0
            val finalSoLuong = soLuong.text.toString().toIntOrNull() ?: 0
            val finalDesc = desc.text.toString()

            if (localVideoUri != null) {
                dialog.setCancelable(false)
                dialog.setCanceledOnTouchOutside(false)
                progressVideoUpload.visibility = View.VISIBLE
                tvVideoProgress.visibility = View.VISIBLE

                try {
                    // Tạo file cache tạm thời từ Uri để lấy absolutePath vật lý
                    val file = java.io.File(cacheDir, "edit_temp_video.mp4")
                    contentResolver.openInputStream(localVideoUri!!)?.use { inputStream ->
                        file.outputStream().use { outputStream ->
                            inputStream.copyTo(outputStream)
                        }
                    }

                    // Gọi MediaManager chạy trực tiếp dựa trên cấu hình tổng của MyApplication
                    com.cloudinary.android.MediaManager.get().upload(file.absolutePath)
                        .option("resource_type", "video")
                        .callback(object : com.cloudinary.android.callback.UploadCallback {
                            override fun onStart(requestId: String?) {}

                            override fun onProgress(requestId: String?, bytes: Long, totalBytes: Long) {
                                runOnUiThread {
                                    val progress = (bytes * 100 / totalBytes).toInt()
                                    progressVideoUpload.progress = progress
                                    tvVideoProgress.text = "$progress%"
                                }
                            }

                            override fun onSuccess(requestId: String?, resultData: MutableMap<Any?, Any?>?) {
                                val secureUrl = resultData?.get("secure_url")?.toString() ?: ""
                                localVideoUrl = secureUrl

                                runOnUiThread {
                                    val updated = b.copy(
                                        tenSach = finalName, tacGia = finalAuthor, gia = finalPrice, soLuong = finalSoLuong,
                                        hinhAnh = currentEditImageUrl, theLoai = selectedType, moTa = finalDesc,
                                        tags = newTags, videoUrl = localVideoUrl
                                    )

                                    dbRef.child(b.id).setValue(updated).addOnSuccessListener {
                                        Toast.makeText(this@ManageBooksActivity, "Cập nhật sách thành công!", Toast.LENGTH_SHORT).show()
                                        dialog.dismiss()
                                    }
                                }
                            }

                            override fun onError(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {
                                runOnUiThread {
                                    progressVideoUpload.visibility = View.GONE
                                    tvVideoProgress.visibility = View.GONE
                                    dialog.setCancelable(true)
                                    dialog.setCanceledOnTouchOutside(true)
                                    val errorMsg = error?.description ?: "Lỗi không xác định"
                                    Toast.makeText(this@ManageBooksActivity, "Lỗi Cloudinary: $errorMsg", Toast.LENGTH_LONG).show()
                                }
                            }

                            override fun onReschedule(requestId: String?, error: com.cloudinary.android.callback.ErrorInfo?) {}
                        }).dispatch()

                } catch (e: Exception) {
                    progressVideoUpload.visibility = View.GONE
                    tvVideoProgress.visibility = View.GONE
                    dialog.setCancelable(true)
                    dialog.setCanceledOnTouchOutside(true)
                    Toast.makeText(this, "Lỗi luồng file: ${e.message}", Toast.LENGTH_SHORT).show()
                }

            } else {
                val updated = b.copy(
                    tenSach = finalName, tacGia = finalAuthor, gia = finalPrice, soLuong = finalSoLuong,
                    hinhAnh = currentEditImageUrl, theLoai = selectedType, moTa = finalDesc,
                    tags = newTags, videoUrl = localVideoUrl
                )
                dbRef.child(b.id).setValue(updated).addOnSuccessListener {
                    Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
                    dialog.dismiss()
                }
            }
        }
    }

    private fun deleteBook(b: Book) {
        AlertDialog.Builder(this)
            .setTitle("Xóa sách?")
            .setMessage("Bạn có chắc muốn xóa cuốn '${b.tenSach}' không?")
            .setPositiveButton("Xóa") { _, _ ->
                dbRef.child(b.id).removeValue().addOnSuccessListener {
                    val db = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    val videoDbRef = db.getReference("Videos")
                    val userLikesRef = db.getReference("video_user_likes")

                    videoDbRef.orderByChild("bookId").equalTo(b.id)
                        .addListenerForSingleValueEvent(object : ValueEventListener {
                            override fun onDataChange(snapshot: DataSnapshot) {
                                for (videoSnapshot in snapshot.children) {
                                    val videoId = videoSnapshot.key ?: ""
                                    videoSnapshot.ref.removeValue()
                                    if (videoId.isNotEmpty()) {
                                        userLikesRef.child(videoId).removeValue()
                                    }
                                }
                            }
                            override fun onCancelled(error: DatabaseError) {}
                        })

                    Toast.makeText(this, "Đã xóa thành công sách và video liên quan", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }

    private fun uploadImageToImgBB(uri: Uri) {
        val base64Image = encodeImageToBase64(uri)
        val client = OkHttpClient()

        val requestBody = MultipartBody.Builder()
            .setType(MultipartBody.FORM)
            .addFormDataPart("key", IMBB_API_KEY)
            .addFormDataPart("image", base64Image)
            .build()

        val request = Request.Builder().url("https://api.imgbb.com/1/upload").post(requestBody).build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {}
            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val jsonObject = JSONObject(response.body?.string()!!)
                    currentEditImageUrl = jsonObject.getJSONObject("data").getString("url")
                }
            }
        })
    }

    private fun encodeImageToBase64(uri: Uri): String {
        val inputStream = contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        val baos = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 60, baos)
        return Base64.encodeToString(baos.toByteArray(), Base64.DEFAULT)
    }
}