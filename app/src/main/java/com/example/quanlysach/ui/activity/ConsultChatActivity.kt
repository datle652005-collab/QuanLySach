package com.example.quanlysach.ui.activity

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.util.Base64
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.text.Editable
import android.text.TextWatcher
import com.example.quanlysach.R
import com.example.quanlysach.data.model.ConsultMessage
import com.example.quanlysach.data.model.User
import com.example.quanlysach.databinding.ActivityConsultChatBinding
import com.example.quanlysach.ui.adapter.ConsultAdapter
import com.google.firebase.database.*
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.IOException

class ConsultChatActivity : AppCompatActivity() {
    private lateinit var binding: ActivityConsultChatBinding
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    private var customerName = ""
    private var myName = ""
    private var myRole = ""
    private var isActivityActive = false
    private lateinit var unreadCountersRef: DatabaseReference
    private lateinit var ref: DatabaseReference

    private val IMBB_API_KEY = "ed361b16e0c52c67d1122f4d031bbe85"

    private val pickMultipleImagesLauncher = registerForActivityResult(
        ActivityResultContracts.GetMultipleContents()
    ) { uris: List<Uri> ->
        if (!uris.isNullOrEmpty()) {
            runOnUiThread {
                Toast.makeText(this, "Đang xử lý và tải lên ${uris.size} ảnh...", Toast.LENGTH_SHORT).show()
            }

            val uploadedUrls = mutableListOf<String>()
            var uploadCount = 0

            for (uri in uris) {
                uploadImageToImgBB(uri) { url ->
                    if (url != null) {
                        uploadedUrls.add(url)
                    }
                    uploadCount++

                    if (uploadCount == uris.size) {
                        if (uploadedUrls.isNotEmpty()) {
                            val combinedUrls = uploadedUrls.joinToString(",")
                            runOnUiThread {
                                sendChatMessage(combinedUrls, "image")
                            }
                        } else {
                            runOnUiThread {
                                Toast.makeText(this@ConsultChatActivity, "Không thể tải các ảnh lên hệ thống!", Toast.LENGTH_SHORT).show()
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityConsultChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pref = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE)
        myName = pref.getString("USERNAME", "") ?: ""
        myRole = pref.getString("VAI_TRO", "khach_hang")!!

        customerName = intent.getStringExtra("RECEIVER_USERNAME") ?: myName

        unreadCountersRef = FirebaseDatabase.getInstance(dbUrl).getReference("UnreadChatCounters")

        // 🌟 XỬ LÝ HIỂN THỊ TIÊU ĐỀ HEADER (TOOLBAR) CỐ ĐỊNH CHUẨN ĐẸP
        setSupportActionBar(binding.toolbarConsult)
        binding.toolbarConsult.setNavigationOnClickListener { finish() }

        if (myRole == "admin") {
            supportActionBar?.title = "Hỗ trợ: $customerName"
            FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(customerName)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val userObj = snapshot.getValue(User::class.java)
                        if (userObj != null && !userObj.ten_user.isNullOrBlank()) {
                            supportActionBar?.title = "Hỗ trợ: ${userObj.ten_user}"
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        } else {
            supportActionBar?.title = "Tư vấn viên hệ thống"
        }

        val roomId = "admin_${customerName}_SUPPORT"
        ref = FirebaseDatabase.getInstance(dbUrl).getReference("Consultations").child(roomId)

        resetMyUnreadCounter()

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val list = mutableListOf<ConsultMessage>()
                for (node in snapshot.children) {
                    val msg = node.getValue(ConsultMessage::class.java)
                    if (msg != null) {
                        list.add(msg)
                        val currentReceiver = if (myRole == "admin") "admin" else myName
                        if (msg.receiverId == currentReceiver && !msg.read && isActivityActive) {
                            node.ref.child("read").setValue(true)
                        }
                    }
                }

                val adapter = ConsultAdapter(this@ConsultChatActivity, list)
                binding.rvConsultChat.layoutManager = LinearLayoutManager(this@ConsultChatActivity).apply { stackFromEnd = true }
                binding.rvConsultChat.adapter = adapter
                if (list.isNotEmpty()) binding.rvConsultChat.scrollToPosition(list.size - 1)
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        binding.btnConsultSelectImage.setOnClickListener {
            pickMultipleImagesLauncher.launch("image/*")
        }

        // 🌟 MẶC ĐỊNH BAN ĐẦU: Khi chưa gõ chữ, hiển thị icon hình quả tim nhanh
        binding.btnConsultSend.setImageResource(R.drawable.ic_heart_fast)

        // 🌟 LẮNG NGHE GÕ CHỮ: Đổi đổi linh hoạt icon giữa quả tim nhanh và mũi tên gửi gốc
        binding.etConsultMessage.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val content = s.toString().trim()
                if (content.isNotEmpty()) {
                    binding.btnConsultSend.setImageResource(R.drawable.ic_chat_send)
                } else {
                    binding.btnConsultSend.setImageResource(R.drawable.ic_heart_fast)
                }
            }
            override fun afterTextChanged(s: Editable?) {}
        })

        // 🌟 SỰ KIỆN CLICK NÚT GỬI: Có chữ thì gửi nội dung chữ, trống thì tự gửi nhanh tim "❤️"
        binding.btnConsultSend.setOnClickListener {
            val content = binding.etConsultMessage.text.toString().trim()
            if (content.isNotEmpty()) {
                sendChatMessage(content, "text")
            } else {
                sendChatMessage("❤️", "text")
            }
        }

        // 🌟 ĐÓNG MỞ EMOJI THEO CHUẨN LINEARLAYOUT (Tự động đẩy thanh soạn thảo lên trên)
        binding.btnConsultEmoji.setOnClickListener {
            val imm = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager

            if (binding.emojiPickerView.visibility == View.VISIBLE) {
                // Nếu đang hiện Emoji -> Ẩn đi và mở bàn phím thường để gõ chữ
                binding.emojiPickerView.visibility = View.GONE
                binding.etConsultMessage.requestFocus()
                imm.showSoftInput(binding.etConsultMessage, InputMethodManager.SHOW_IMPLICIT)
            } else {
                // Nếu đang gõ bàn phím chữ -> Ẩn bàn phím đi trước
                imm.hideSoftInputFromWindow(binding.etConsultMessage.windowToken, 0)

                // Đợi bàn phím thu xuống hoàn toàn (120ms) để lấy không gian đẩy bảng chọn Emoji lên cực kỳ mượt
                binding.btnConsultEmoji.postDelayed({
                    binding.emojiPickerView.visibility = View.VISIBLE

                    // Tự động cuộn danh sách tin nhắn xuống cuối để tránh bị che mất nội dung chat cũ
                    binding.rvConsultChat.adapter?.let { adapter ->
                        if (adapter.itemCount > 0) {
                            binding.rvConsultChat.scrollToPosition(adapter.itemCount - 1)
                        }
                    }
                }, 120)
            }
        }

        // 🌟 Khi người dùng nhấn vào ô nhập chữ -> Tự động ẩn bảng Emoji đi để ưu tiên không gian cho bàn phím
        binding.etConsultMessage.setOnClickListener {
            if (binding.emojiPickerView.visibility == View.VISIBLE) {
                binding.emojiPickerView.visibility = View.GONE
            }
        }

        // 🌟 Nhận Emoji được chọn từ bảng và điền chính xác vào vị trí con trỏ chuột trong ô soạn thảo
        binding.emojiPickerView.setOnEmojiPickedListener { emojiViewItem ->
            val emoji = emojiViewItem.emoji
            val start = binding.etConsultMessage.selectionStart
            val end = binding.etConsultMessage.selectionEnd

            binding.etConsultMessage.text.replace(start, end, emoji)
            binding.etConsultMessage.setSelection(start + emoji.length)
        }
    }

    private fun uploadImageToImgBB(uri: Uri, onComplete: (String?) -> Unit) {
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
                onComplete(null)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseData = response.body?.string()
                    val jsonObject = JSONObject(responseData!!)
                    val imageUrl = jsonObject.getJSONObject("data").getString("url")
                    onComplete(imageUrl)
                } else {
                    onComplete(null)
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

    private fun sendChatMessage(content: String, type: String) {
        val msgId = ref.push().key ?: return
        val sender = if (myRole == "admin") "admin" else myName
        val receiver = if (myRole == "admin") customerName else "admin"

        val msg = ConsultMessage(
            messageId = msgId,
            senderId = sender,
            receiverId = receiver,
            message = content,
            timestamp = System.currentTimeMillis(),
            read = false,
            type = type
        )

        ref.child(msgId).setValue(msg).addOnSuccessListener {
            if (type == "text") {
                binding.etConsultMessage.setText("")
            }
            unreadCountersRef.child(receiver).setValue(ServerValue.increment(1))

            val notiRef = FirebaseDatabase.getInstance(dbUrl).getReference("Notifications").child(receiver)

            val bodyText = if (type == "image") {
                val size = content.split(",").size
                if (size > 1) "[Đã gửi $size hình ảnh]" else "[Hình ảnh]"
            } else {
                content
            }

            val notiData = mapOf(
                "title" to if (myRole == "admin") "Phản hồi từ Admin" else "Tin nhắn từ $myName",
                "body" to bodyText
            )
            notiRef.push().setValue(notiData)

            getTokenAndSendPush(receiver, bodyText)
        }.addOnFailureListener {
            Toast.makeText(this, "Lỗi gửi tin nhắn!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getTokenAndSendPush(targetUser: String, message: String) {
        val userRef = FirebaseDatabase.getInstance(dbUrl).getReference("Users").child(targetUser)
        userRef.child("token").get().addOnSuccessListener { snapshot ->
            val token = snapshot.value as? String
            if (!token.isNullOrEmpty()) {
                val title = if (myRole == "admin") "Phản hồi từ Admin" else "Tin nhắn từ $myName"
                sendPushAPI(token, title, message)
            }
        }
    }

    private fun sendPushAPI(token: String, title: String, body: String) {
        val serverKey = "MÃ_SERVER_KEY_CỦA_BẠN_DÁN_VÀO_ĐÂY"
        val client = OkHttpClient()
        val mediaType = "application/json; charset=utf-8".toMediaType()

        val json = JSONObject().apply {
            put("to", token)
            put("notification", JSONObject().apply {
                put("title", title)
                put("body", body)
                put("sound", "default")
            })
        }

        val request = Request.Builder()
            .url("https://fcm.googleapis.com/fcm/send")
            .post(json.toString().toRequestBody(mediaType))
            .addHeader("Authorization", "key=$serverKey")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) { e.printStackTrace() }
            override fun onResponse(call: Call, response: Response) { response.close() }
        })
    }

    private fun resetMyUnreadCounter() {
        val myFirebaseKey = if (myRole == "admin") "admin" else myName
        if (::unreadCountersRef.isInitialized) {
            unreadCountersRef.child(myFirebaseKey).setValue(0)
        }
    }

    override fun onResume() {
        super.onResume()
        isActivityActive = true
        resetMyUnreadCounter()
    }

    override fun onPause() {
        super.onPause()
        isActivityActive = false
    }
}