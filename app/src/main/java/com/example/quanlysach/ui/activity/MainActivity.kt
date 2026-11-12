package com.example.quanlysach.ui.activity

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.os.*
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.app.NotificationCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Book
import com.example.quanlysach.ui.adapter.BannerAdapter
import com.example.quanlysach.ui.adapter.BookAdapter
import com.example.quanlysach.ui.adapter.BookSuggestAdapter
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.firebase.database.*
import androidx.media3.common.util.UnstableApi
import dagger.hilt.android.AndroidEntryPoint
import java.net.URL
import java.util.concurrent.Executors
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody

@AndroidEntryPoint
@UnstableApi
class MainActivity : AppCompatActivity() {

    // UI
    private lateinit var edtSearch: AutoCompleteTextView
    private lateinit var rvBooks: RecyclerView
    private lateinit var viewPagerBanner: ViewPager2
    private lateinit var tvListTitle: TextView
    private lateinit var tvAppName: TextView
    private lateinit var btnCameraSearch: ImageView
    private var imageUri: android.net.Uri? = null

    // Adapter
    private lateinit var bookAdapter: BookAdapter
    private lateinit var suggestAdapter: BookSuggestAdapter
    private var aiSuggestLoaded = false

    // Data
    private val fullList = mutableListOf<Book>()
    private val displayList = mutableListOf<Book>()

    // 🔥 FIX LỖI NHẢY TAB: Biến lưu trữ tab hiện tại khách đang xem
    private var currentCategoryFilter = "tat_ca"


    // Slider Banner
    private val sliderHandler = Handler(Looper.getMainLooper())
    private var sliderRunnable: Runnable? = null

    // Bộ nhớ đệm chặn trùng thông báo
    private val processedNotiIds = HashSet<String>()
    private lateinit var tvChatBadge: TextView

    // 1. Biến lưu Uri ảnh tạm thời
    private var tempImageUri: android.net.Uri? = null

    // 2. Launcher để chụp ảnh
    private val takePictureLauncher = registerForActivityResult(androidx.activity.result.contract.ActivityResultContracts.TakePicture()) { success ->
        if (success && tempImageUri != null) {
            recognizeTextFromImage(tempImageUri!!)
        }
    }
    // --- XÓA CÁC DÒNG KHAI BÁO TABS CŨ (kiểu TextView) ---
// --- THAY BẰNG DÒNG NÀY ---
    private lateinit var tabTatCa: LinearLayout
    private lateinit var tabCuocSong: LinearLayout
    private lateinit var tabNgonTinh: LinearLayout
    private lateinit var tabKinhTe: LinearLayout
    private lateinit var tabThieuNhi: LinearLayout
    private lateinit var tabVanHoc: LinearLayout
    private lateinit var tabKyNang: LinearLayout
    private lateinit var tabs: List<LinearLayout>

    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Books")

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        edtSearch = findViewById(R.id.edtSearch)
        tvListTitle = findViewById(R.id.tvListTitle)
        tvAppName = findViewById(R.id.tvAppName)
        tvChatBadge = findViewById(R.id.tvChatBadge)

        findViewById<FrameLayout>(R.id.btnCart).setOnClickListener {
            startActivity(Intent(this, CartActivity::class.java))
        }

        findViewById<FrameLayout>(R.id.btnPhone).setOnClickListener {
            val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
            val currentUsername = pref.getString("USERNAME", "") ?: ""

            if (currentUsername.isNotEmpty()) {
                val intent = Intent(this, ConsultChatActivity::class.java)
                intent.putExtra("RECEIVER_USERNAME", currentUsername)
                intent.putExtra("ORDER_ID", "SUPPORT")
                startActivity(intent)
            } else {
                startActivity(Intent(this, LoginActivity::class.java))
            }
        }
        btnCameraSearch = findViewById(R.id.btnCameraSearch)

        btnCameraSearch.setOnClickListener {
            // Kiểm tra quyền Camera trước khi mở menu
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
                showImageSourceDialog()
            } else {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.CAMERA), 200)
            }
        }

        initBanner()
        initRecyclerView()
        initTabs()
        initSearch()
        initBottomNav()
        initChatAI()

        loadBooks()
        loadNotificationBadge()
        autoManagerVouchersAndPrices()
        loadChatBadge()

        // --- LẮNG NGHE THÔNG BÁO ---
        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val myUsername = pref.getString("USERNAME", "") ?: ""
        val myRole = pref.getString("VAI_TRO", "khach_hang")

        if (myRole == "khach_hang" && myUsername.isNotEmpty()) {
            listenForNewAdminNotifications(this, myUsername)
        }

        // 🔥 Tinh chỉnh xin quyền thông báo an toàn hơn cho Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 101)
            }
        }
    }

    private fun showImageSourceDialog() {
        val options = arrayOf("Chụp ảnh trực tiếp", "Chọn từ thư viện")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("Tìm kiếm bằng ảnh")
            .setItems(options) { _, which ->
                if (which == 0) {
                    // Chụp ảnh: Tạo file tạm trước
                    val photoFile = java.io.File(externalCacheDir, "temp_image.jpg")
                    tempImageUri = androidx.core.content.FileProvider.getUriForFile(
                        this, "${packageName}.provider", photoFile
                    )
                    takePictureLauncher.launch(tempImageUri!!)
                } else {
                    // Chọn từ thư viện (code cũ của bạn)
                    pickImageLauncher.launch("image/*")
                }
            }.show()
    }

    private val pickImageLauncher = registerForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            imageUri = uri
            recognizeTextFromImage(uri)
        }
    }
    private fun recognizeTextFromImage(uri: android.net.Uri) {
        try {
            val image = com.google.mlkit.vision.common.InputImage.fromFilePath(this, uri)
            val recognizer = com.google.mlkit.vision.text.TextRecognition.getClient(
                com.google.mlkit.vision.text.latin.TextRecognizerOptions.DEFAULT_OPTIONS
            )

            Toast.makeText(this, "Đang quét ảnh...", Toast.LENGTH_SHORT).show()

            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val fullText = visionText.text
                    if (fullText.isNullOrEmpty()) {
                        Toast.makeText(this, "Không quét được chữ!", Toast.LENGTH_SHORT).show()
                        return@addOnSuccessListener
                    }

                    val suggestions = getSuggestedBooksFromImage(fullText)

                    if (suggestions.isNotEmpty()) {

                        bookAdapter.setData(suggestions)

                        tvListTitle.text = "📸 Kết quả nhận diện"

                    } else {

                        bookAdapter.setData(emptyList())

                        tvListTitle.text = "❌ Không tìm thấy sản phẩm phù hợp"

                        Toast.makeText(
                            this,
                            "Không tìm thấy sách phù hợp!",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
        } catch (e: Exception) { e.printStackTrace() }
    }
    private fun getSuggestedBooksFromImage(scannedText: String): List<Book> {

        if (fullList.isEmpty()) return emptyList()

        fun normalize(text: String): String {

            val temp = java.text.Normalizer.normalize(
                text,
                java.text.Normalizer.Form.NFD
            )

            return "\\p{InCombiningDiacriticalMarks}+".toRegex()
                .replace(temp, "")
                .replace("đ", "d")
                .replace("Đ", "D")
                .lowercase()
                .replace("[^a-z0-9 ]".toRegex(), " ")
                .replace("\\s+".toRegex(), " ")
                .trim()
        }

        // =========================
        // TỪ KHÔNG QUAN TRỌNG
        // =========================
        val stopWords = listOf(
            "va", "la", "cua", "mot", "nhung",
            "tap", "quyen", "book", "sach", "truyen"
        )

        val scanned = normalize(scannedText)

        if (scanned.length < 3) {
            return emptyList()
        }

        val scannedWords = scanned.split(" ")
            .filter {
                it.length >= 2 &&
                        !stopWords.contains(it)
            }

        data class MatchResult(
            val book: Book,
            val score: Double
        )

        val results = mutableListOf<MatchResult>()

        for (book in fullList) {

            val name = normalize(book.tenSach)

            val bookWords = name.split(" ")
                .filter {
                    it.length >= 2 &&
                            !stopWords.contains(it)
                }

            if (bookWords.isEmpty()) continue

            var exactMatch = 0
            var nearMatch = 0

            // =========================
            // SO KHỚP TỪ
            // =========================
            for (bw in bookWords) {

                if (scannedWords.contains(bw)) {

                    exactMatch++

                } else {

                    for (sw in scannedWords) {

                        val similarity =
                            calculateStringSimilarity(bw, sw)

                        // OCR sai nhẹ vẫn chấp nhận
                        if (similarity >= 0.82) {
                            nearMatch++
                            break
                        }
                    }
                }
            }

            // =========================
            // TÍNH ĐIỂM
            // =========================
            var score = 0.0

            score += exactMatch * 2.5
            score += nearMatch * 1.0

            // BONUS nếu chứa nguyên cụm tên
            if (scanned.contains(name)) {
                score += 8
            }

            // BONUS nếu match gần hết tên sách
            val matchRate =
                exactMatch.toDouble() / bookWords.size

            score += matchRate * 5

            // =========================
            // CHẶN MATCH RÁC
            // =========================
            if (
                exactMatch >= 2 ||
                (exactMatch >= 1 && nearMatch >= 2)
            ) {
                results.add(MatchResult(book, score))
            }
        }

        // =========================
        // KHÔNG CÓ KẾT QUẢ
        // =========================
        if (results.isEmpty()) {
            return emptyList()
        }

        // =========================
        // SẮP XẾP ĐIỂM CAO NHẤT
        // =========================
        val sorted = results.sortedByDescending { it.score }

        val best = sorted.first()

        // =========================
        // NẾU QUÁ CHÍNH XÁC
        // CHỈ HIỆN 1 QUYỂN
        // =========================
        if (best.score >= 12) {
            return listOf(best.book)
        }

        // =========================
        // NẾU KHÔNG CHẮC
        // HIỆN NHIỀU QUYỂN GẦN GIỐNG
        // =========================
        val topResults = sorted.filter {

            // Chỉ lấy quyển gần điểm nhất
            it.score >= best.score - 2

        }.take(5)

        return topResults.map { it.book }
    }
    private fun calculateStringSimilarity(a: String, b: String): Double {

        val longer = if (a.length > b.length) a else b
        val shorter = if (a.length > b.length) b else a

        if (longer.isEmpty()) return 1.0

        val distance = levenshteinDistance(longer, shorter)

        return (longer.length - distance).toDouble() / longer.length
    }
    private fun levenshteinDistance(lhs: String, rhs: String): Int {

        val dp = Array(lhs.length + 1) {
            IntArray(rhs.length + 1)
        }

        for (i in 0..lhs.length) {
            dp[i][0] = i
        }

        for (j in 0..rhs.length) {
            dp[0][j] = j
        }

        for (i in 1..lhs.length) {
            for (j in 1..rhs.length) {

                val cost =
                    if (lhs[i - 1] == rhs[j - 1]) 0 else 1

                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }

        return dp[lhs.length][rhs.length]
    }
    private fun findBestMatchingBook(scannedText: String) {
        if (fullList.isEmpty()) return

        // 1. Dọn dẹp chuỗi quét được: viết thường, bỏ khoảng trắng dư thừa
        val cleanScanned = scannedText.lowercase().replace("\\s+".toRegex(), "").trim()

        var bestMatch: Book? = null
        var maxSimilarity = 0.0

        for (book in fullList) {
            val cleanBookName = book.tenSach.lowercase().replace("\\s+".toRegex(), "").trim()

            // 2. Tính điểm tương đồng (Similarity Score)
            // Cách đơn giản: Tính xem tên sách xuất hiện bao nhiêu lần trong đống text đã quét
            // Hoặc kiểm tra xem có bao nhiêu ký tự của tên sách nằm trong đống text đó
            val similarity = calculateSimilarity(cleanScanned, cleanBookName)

            if (similarity > maxSimilarity) {
                maxSimilarity = similarity
                bestMatch = book
            }
        }

        // 3. Ngưỡng chấp nhận (Ví dụ: phải giống nhau ít nhất 60%)
        if (bestMatch != null && maxSimilarity > 0.6) {
            edtSearch.setText(bestMatch.tenSach)
            Toast.makeText(this, "Đã tìm thấy: ${bestMatch.tenSach}", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "Không tìm thấy sách. (Độ khớp thấp: $maxSimilarity)", Toast.LENGTH_SHORT).show()
        }
    }

    // Hàm tính tỉ lệ tương đồng giữa 2 chuỗi
    private fun calculateSimilarity(source: String, target: String): Double {
        if (source.contains(target) || target.contains(source)) return 1.0

        // Tính toán số ký tự trùng khớp (đơn giản hóa)
        var matchCount = 0
        for (char in target) {
            if (source.contains(char.toString())) matchCount++
        }
        return matchCount.toDouble() / target.length
    }

    private fun openGallery() {
        pickImageLauncher.launch("image/*")
    }

    private fun loadChatBadge() {
        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val username = pref.getString("USERNAME", null) ?: return
        val role = pref.getString("VAI_TRO", "khach_hang") ?: "khach_hang"

        // Chỉ đếm tin nhắn cho tài khoản khách hàng (nếu là Admin thì cấu trúc logic đếm sẽ khác, ở đây ưu tiên cho User)
        if (role == "khach_hang") {
            val roomId = "admin_${username}_SUPPORT"

            FirebaseDatabase.getInstance(dbUrl)
                .getReference("Consultations/$roomId")
                .addValueEventListener(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        var unreadChatCount = 0

                        for (node in snapshot.children) {
                            val receiverId = node.child("receiverId").getValue(String::class.java)
                            val isRead = node.child("read").getValue(Boolean::class.java) ?: true

                            // Nếu người nhận là chính mình và tin nhắn chưa đọc
                            if (receiverId == username && !isRead) {
                                unreadChatCount++
                            }
                        }

                        // Hiển thị hoặc ẩn badge dựa trên số lượng tin nhắn chưa đọc
                        if (unreadChatCount > 0) {
                            tvChatBadge.visibility = View.VISIBLE
                            tvChatBadge.text = if (unreadChatCount > 99) "99+" else unreadChatCount.toString()
                        } else {
                            tvChatBadge.visibility = View.GONE
                        }
                    }

                    override fun onCancelled(error: DatabaseError) {
                        // Xử lý lỗi nếu cần
                    }
                })
        }
    }

    private fun initRecyclerView() {
        rvBooks = findViewById(R.id.rvBooks)
        rvBooks.layoutManager = GridLayoutManager(this, 2)
        rvBooks.isNestedScrollingEnabled = false

        bookAdapter = BookAdapter { book ->
            val intent = Intent(this, BookDetailActivity::class.java)
            intent.putExtra("BOOK_ID", book.id)
            startActivity(intent)
        }
        rvBooks.adapter = bookAdapter
    }

    private fun loadBooks() {
        dbRef.orderByChild("timestamp")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    val temp = mutableListOf<Book>()

                    for (data in snapshot.children) {
                        try {
                            val book = data.getValue(Book::class.java)

                            if (book != null) {
                                temp.add(book.copy(id = data.key ?: ""))
                            }

                        } catch (e: Exception) {
                            android.util.Log.e("MainActivity", "Lỗi convert sách: ${e.message}")
                        }
                    }

                    // 🔥 Đảo ngược list để mới nhất lên đầu
                    temp.reverse()

                    fullList.clear()
                    fullList.addAll(temp)

                    val targetCategory = intent.getStringExtra("TARGET_CATEGORY")

                    if (!targetCategory.isNullOrEmpty()) {
                        checkAndOpenCategoryFromIntent()
                    } else {
                        filterByCategory(currentCategoryFilter)
                    }

                    setupSuggestions()
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(
                        this@MainActivity,
                        "Lỗi: ${error.message}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            })
    }

    private fun initTabs() {
        tabTatCa = findViewById(R.id.tabTatCa)
        tabCuocSong = findViewById(R.id.tabCuocSong)
        tabNgonTinh = findViewById(R.id.tabNgonTinh)
        tabKinhTe = findViewById(R.id.tabKinhTe)
        tabThieuNhi = findViewById(R.id.tabThieuNhi)
        tabVanHoc = findViewById(R.id.tabVanHoc)
        tabKyNang = findViewById(R.id.tabKyNang)

        tabs = listOf(tabTatCa, tabCuocSong, tabNgonTinh, tabKinhTe, tabThieuNhi, tabVanHoc, tabKyNang)

        fun setActive(selected: LinearLayout) {
            tabs.forEach { layout ->
                // Đặt lại trạng thái không chọn cho toàn bộ khối
                layout.setBackgroundResource(R.drawable.bg_tab_inactive)

                // Tìm TextView (nằm ở vị trí 1 trong layout) để đổi màu
                val tv = layout.getChildAt(1) as TextView
                tv.setTextColor(Color.parseColor("#888888")) // Màu xám lúc không chọn
                tv.typeface = Typeface.DEFAULT
            }

            // Cấu hình trạng thái đang chọn
            selected.setBackgroundResource(R.drawable.bg_tab_active)
            val tvSelected = selected.getChildAt(1) as TextView
            tvSelected.setTextColor(Color.WHITE) // Hoặc màu bạn muốn khi chọn
            tvSelected.typeface = Typeface.DEFAULT_BOLD
        }

        tabTatCa.setOnClickListener { setActive(tabTatCa); filterByCategory("tat_ca") }
        tabCuocSong.setOnClickListener { setActive(tabCuocSong); filterByCategory("cuoc_song") }
        tabNgonTinh.setOnClickListener { setActive(tabNgonTinh); filterByCategory("ngon_tinh") }
        tabKinhTe.setOnClickListener { setActive(tabKinhTe); filterByCategory("kinh_te") }
        tabThieuNhi.setOnClickListener { setActive(tabThieuNhi); filterByCategory("thieu_nhi") }
        tabVanHoc.setOnClickListener { setActive(tabVanHoc); filterByCategory("van_hoc") }
        tabKyNang.setOnClickListener { setActive(tabKyNang); filterByCategory("ky_nang") }
    }
    private fun setActive(selected: LinearLayout) {
        tabs.forEach { layout ->
            layout.setBackgroundResource(R.drawable.bg_tab_inactive)
            layout.elevation = 0f // Không đổ bóng khi không chọn
            val tv = layout.getChildAt(1) as TextView
            tv.setTextColor(Color.parseColor("#64748B"))
            tv.typeface = Typeface.DEFAULT
        }

        // Khi chọn: đổi background và đổ bóng nhẹ
        selected.setBackgroundResource(R.drawable.bg_tab_active)
        selected.elevation = 4f // Tạo độ nổi so với màn hình

        val tvSelected = selected.getChildAt(1) as TextView
        tvSelected.setTextColor(Color.WHITE)
        tvSelected.typeface = Typeface.DEFAULT_BOLD
    }

    private fun filterByCategory(category: String) {
        currentCategoryFilter = category // Lưu lại trạng thái tab hiện hành


        val result = when (category) {
            "tat_ca" -> {
                tvListTitle.text = "📚 Sách mới nhất"
                fullList
            }
            "cuoc_song" -> {
                tvListTitle.text = "🌱 Sách Cuộc sống"
                fullList.filter { it.theLoai.equals("Cuộc sống", true) }
            }
            "ngon_tinh" -> {
                tvListTitle.text = "💖 Sách Ngôn tình"
                fullList.filter { it.theLoai.equals("Ngôn tình", true) }
            }
            "kinh_te" -> {
                tvListTitle.text = "📈 Sách Kinh tế"
                fullList.filter { it.theLoai.equals("Kinh tế", true) }
            }
            "thieu_nhi" -> {
                tvListTitle.text = "🎈 Sách Thiếu nhi"
                fullList.filter { it.theLoai.equals("Thiếu nhi", true) }
            }
            "van_hoc" -> {
                tvListTitle.text = "✍️ Sách Văn học"
                fullList.filter { it.theLoai.equals("Văn học", true) }
            }
            "ky_nang" -> {
                tvListTitle.text = "💡 Sách Kỹ năng"
                fullList.filter { it.theLoai.equals("Kỹ năng", true) }
            }
            else -> emptyList()
        }
        displayList.clear()
        displayList.addAll(result)
        bookAdapter.setData(result)
    }

    private fun initSearch() {
        edtSearch.threshold = 1
        edtSearch.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                if (s.toString().isNotEmpty()) edtSearch.showDropDown()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })
    }

    private fun setupSuggestions() {
        suggestAdapter = BookSuggestAdapter(this, fullList)
        edtSearch.setAdapter(suggestAdapter)
        edtSearch.dropDownWidth = resources.displayMetrics.widthPixels
        edtSearch.setOnItemClickListener { parent, _, position, _ ->
            val selectedBook = parent.adapter.getItem(position) as? Book
            selectedBook?.let {
                val intent = Intent(this, BookDetailActivity::class.java)
                intent.putExtra("BOOK_ID", it.id)
                startActivity(intent)
            }
        }
    }

    private fun initBanner() {
        viewPagerBanner = findViewById(R.id.viewPagerBanner)
        val images = listOf(R.drawable.banner1, R.drawable.banner2, R.drawable.banner3)
        viewPagerBanner.adapter = BannerAdapter(images)

        sliderRunnable = Runnable {
            var next = viewPagerBanner.currentItem + 1
            if (next >= images.size) next = 0
            viewPagerBanner.currentItem = next
            sliderHandler.postDelayed(sliderRunnable!!, 4000)
        }
        sliderHandler.postDelayed(sliderRunnable!!, 4000)
    }

    private fun initBottomNav() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_trang_chu -> true
                R.id.nav_uu_dai -> {
                    startActivity(Intent(this, PromotionActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_live -> {
                    startActivity(Intent(this, LiveStreamActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_notification -> {
                    startActivity(Intent(this, NotificationsActivity::class.java))
                    overridePendingTransition(0, 0)
                    true
                }
                R.id.nav_tai_khoan -> {
                    checkUserLogin()
                    true
                }
                else -> false
            }
        }
    }

    private fun checkUserLogin() {
        val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        val username = pref.getString("USERNAME", null)
        if (username == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            overridePendingTransition(0, 0)
        } else {
            val role = pref.getString("VAI_TRO", "khach_hang")
            if (role == "admin") {
                startActivity(Intent(this, AdminActivity::class.java))
            } else {
                startActivity(Intent(this, ProfileActivity::class.java))
            }
            overridePendingTransition(0, 0)
        }
    }

    private fun initChatAI() {
        findViewById<FloatingActionButton>(R.id.fabChat).setOnClickListener {
            startActivity(Intent(this, ChatActivity::class.java))
        }
    }

    override fun onPause() {
        super.onPause()
        sliderRunnable?.let { sliderHandler.removeCallbacks(it) }
    }

    override fun onResume() {
        super.onResume()
        findViewById<BottomNavigationView>(R.id.bottomNavigation).menu.findItem(R.id.nav_trang_chu).isChecked = true
        sliderRunnable?.let { sliderHandler.postDelayed(it, 4000) }
        updateCartBadge()
    }

    override fun onDestroy() {
        super.onDestroy()
        sliderHandler.removeCallbacksAndMessages(null)
    }

    private fun autoManagerVouchersAndPrices() {
        val db = FirebaseDatabase.getInstance(dbUrl).reference
        val now = System.currentTimeMillis()

        // Quét bảng Vouchers để xử lý mã hết hạn
        db.child("Vouchers").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                for (ds in snapshot.children) {
                    val endDate = ds.child("endDate").getValue(Long::class.java) ?: 0L
                    val active = ds.child("active").getValue(Boolean::class.java) ?: true
                    val loaiVoucher = ds.child("loaiVoucher").getValue(String::class.java) ?: ""
                    val applyTo = ds.child("applyTo").getValue(String::class.java) ?: "ALL"
                    val applyTargetId = ds.child("applyTargetId").getValue(String::class.java) ?: ""

                    // Kiểm tra xem mã đã thiết lập hạn chưa (>0), đã quá hạn chưa (<now) và có đang bật không
                    if (endDate in 1..<now && active) {

                        // 1. NGAY LẬP TỨC KHÓA MÃ VOUCHER LẠI
                        ds.ref.child("active").setValue(false)

                        // 2. NẾU LÀ MÃ GIẢM GIÁ -> TIẾN HÀNH RESET GIÁ SÁCH VỀ 0
                        if (loaiVoucher == "GIAM_GIA") {
                            when (applyTo.uppercase()) {

                                // Trường hợp 1: Mã áp dụng cho 1 cuốn sách cụ thể
                                "BOOK" -> {
                                    if (applyTargetId.isNotEmpty()) {
                                        db.child("Books").child(applyTargetId).child("giaGiam").setValue(0)
                                    }
                                }

                                // Trường hợp 2: Mã áp dụng cho cả 1 Thể loại
                                "CATEGORY" -> {
                                    if (applyTargetId.isNotEmpty()) {
                                        db.child("Books").orderByChild("theLoai").equalTo(applyTargetId)
                                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                                override fun onDataChange(bookSnap: DataSnapshot) {
                                                    for (bookDs in bookSnap.children) {
                                                        bookDs.ref.child("giaGiam").setValue(0)
                                                    }
                                                }
                                                override fun onCancelled(error: DatabaseError) {}
                                            })
                                    }
                                }

                                // Trường hợp 3: Mã áp dụng cho Toàn bộ cửa hàng
                                "ALL" -> {
                                    db.child("Books").addListenerForSingleValueEvent(object : ValueEventListener {
                                        override fun onDataChange(bookSnap: DataSnapshot) {
                                            for (bookDs in bookSnap.children) {
                                                bookDs.ref.child("giaGiam").setValue(0)
                                            }
                                        }
                                        override fun onCancelled(error: DatabaseError) {}
                                    })
                                }
                            }
                        }
                        // Ghi chú: Nếu loaiVoucher == "FREESHIP", nó chỉ chạy bước 1 (Khóa mã)
                        // và bỏ qua bước 2, đảm bảo không làm mất giá gốc của sách.
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {
                // Có thể thêm Toast hoặc Log để báo lỗi mạng nếu cần
            }
        })
    }

    private fun checkAndOpenCategoryFromIntent() {
        val targetCategory = intent.getStringExtra("TARGET_CATEGORY")

        if (!targetCategory.isNullOrEmpty()) {
            val categoryKey = when (targetCategory.lowercase()) {
                "cuộc sống", "cuoc_song" -> "cuoc_song"
                "ngôn tình", "ngon_tinh" -> "ngon_tinh"
                "kinh tế", "kinh_te" -> "kinh_te"
                "thiếu nhi", "thieu_nhi" -> "thieu_nhi"
                "văn học", "van_hoc" -> "van_hoc"
                "kỹ năng", "ky_nang" -> "ky_nang"
                else -> "tat_ca"
            }

            when (categoryKey) {
                "cuoc_song" -> tabs[1].performClick()
                "ngon_tinh" -> tabs[2].performClick()
                "kinh_te" -> tabs[3].performClick()
                "thieu_nhi" -> tabs[4].performClick()
                "van_hoc" -> tabs[5].performClick()
                "ky_nang" -> tabs[6].performClick()
                else -> tabs[0].performClick()
            }

            // 🔥 Xóa lệnh chuyển tab sau khi thực hiện xong, chống kẹt vòng lặp cập nhật
            intent.removeExtra("TARGET_CATEGORY")
        }
    }

    private fun updateCartBadge() {
        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val username = pref.getString("USERNAME", "") ?: ""

        val tvCartBadge = findViewById<TextView>(R.id.tvCartBadge)

        // Nếu chưa đăng nhập thì ẩn luôn badge
        if (username.isEmpty()) {
            tvCartBadge.visibility = View.GONE
            return
        }

        // Chọc vào Firebase lấy số lượng sách trong Carts của user này
        FirebaseDatabase.getInstance(dbUrl).getReference("Carts").child(username)
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var totalCount = 0

                    // Cộng dồn 'soLuong' của từng món trong giỏ
                    for (ds in snapshot.children) {
                        totalCount += ds.child("soLuong").getValue(Int::class.java) ?: 0
                    }

                    // Cập nhật UI
                    if (totalCount > 0) {
                        tvCartBadge.visibility = View.VISIBLE
                        // Nếu nhiều quá thì hiển thị 99+ cho chuyên nghiệp
                        tvCartBadge.text = if (totalCount > 99) "99+" else totalCount.toString()
                    } else {
                        tvCartBadge.visibility = View.GONE
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    // Lỗi mạng hoặc bị chặn thì cứ kệ nó, không làm crash app
                }
            })
    }

    private fun loadNotificationBadge() {
        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val username = pref.getString("USERNAME", null) ?: return

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Notifications/$username")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    var unread = 0
                    for (child in snapshot.children) {
                        if (!(child.child("isRead").getValue(Boolean::class.java) ?: false)) unread++
                    }
                    val bottomNav = findViewById<BottomNavigationView>(R.id.bottomNavigation)
                    if (unread > 0) {
                        bottomNav.getOrCreateBadge(R.id.nav_notification).apply {
                            isVisible = true
                            number = unread
                        }
                    } else {
                        bottomNav.removeBadge(R.id.nav_notification)
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun listenForNewAdminNotifications(context: Context, myUsername: String) {
        val notiRef = FirebaseDatabase.getInstance(dbUrl).getReference("Notifications").child(myUsername)

        notiRef.addChildEventListener(object : ChildEventListener {
            override fun onChildAdded(snapshot: DataSnapshot, previousChildName: String?) {
                val notiId = snapshot.key ?: return
                if (processedNotiIds.contains(notiId)) return

                val isProcessed = snapshot.child("isProcessed").getValue(Boolean::class.java) ?: false

                if (!isProcessed) {
                    val title = snapshot.child("title").value as? String ?: "Thông báo từ cửa hàng"
                    val body = snapshot.child("content").value as? String
                        ?: snapshot.child("body").value as? String
                        ?: "Trạng thái đơn hàng của bạn đã thay đổi."
                    val imageUrl = snapshot.child("image").value as? String

                    val isOrderSuccess = title.contains("thành công", true)
                    processedNotiIds.add(notiId)

                    showRealNotification(context, title, body, imageUrl, isOrderSuccess)
                    snapshot.ref.child("isProcessed").setValue(true)
                } else {
                    processedNotiIds.add(notiId)
                }
            }

            override fun onChildChanged(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onChildRemoved(snapshot: DataSnapshot) {
                processedNotiIds.remove(snapshot.key)
            }
            override fun onChildMoved(snapshot: DataSnapshot, previousChildName: String?) {}
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun showRealNotification(context: Context, title: String, body: String, imageUrl: String?, isOrderSuccess: Boolean) {
        val channelId = "real_chat_channel"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notificationId = System.currentTimeMillis().toInt()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channelName = if (isOrderSuccess) "Thông báo đơn hàng" else "Tin nhắn hỗ trợ từ Admin"
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                enableLights(true)
                enableVibration(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        val mainIntent = Intent(context, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        }
        val mainPendingIntent = PendingIntent.getActivity(
            context,
            notificationId,
            mainIntent,
            PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
        )

        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(android.R.drawable.stat_notify_chat)
            .setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(true)
            .setContentIntent(mainPendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)

        if (isOrderSuccess) {
            val rateIntent = Intent(context, MainActivity::class.java).apply {
                putExtra("OPEN_RATING", true)
                addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            val ratePendingIntent = PendingIntent.getActivity(
                context,
                notificationId + 1,
                rateIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_IMMUTABLE
            )

            builder.addAction(android.R.drawable.btn_star_big_on, "⭐ Đánh giá 5 sao", ratePendingIntent)

            if (!imageUrl.isNullOrEmpty()) {
                val executor = Executors.newSingleThreadExecutor()
                executor.execute {
                    var bitmap: Bitmap? = null
                    try {
                        val url = URL(imageUrl)
                        bitmap = BitmapFactory.decodeStream(url.openConnection().getInputStream())
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }

                    if (bitmap != null) {
                        builder.setLargeIcon(bitmap)
                        builder.setStyle(
                            NotificationCompat.BigPictureStyle()
                                .bigPicture(bitmap)
                                .bigLargeIcon(null as Bitmap?)
                        )
                    }

                    Handler(Looper.getMainLooper()).post {
                        notificationManager.notify(notificationId, builder.build())
                    }
                }
                return
            }
        }

        notificationManager.notify(notificationId, builder.build())
    }
}