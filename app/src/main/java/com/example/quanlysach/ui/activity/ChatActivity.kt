package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.api.GHNApiService
import com.example.quanlysach.data.model.Address
import com.example.quanlysach.data.model.CartItem
import com.example.quanlysach.data.model.ChatMessage
import com.example.quanlysach.data.model.FeeRequest
import com.example.quanlysach.data.model.FeeResponse
import com.example.quanlysach.data.model.Order
import com.example.quanlysach.ui.adapter.ChatAdapter
import com.google.ai.client.generativeai.Chat
import com.google.ai.client.generativeai.GenerativeModel
import com.google.ai.client.generativeai.type.content
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.firebase.database.*
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class ChatActivity : AppCompatActivity() {

    companion object {
        private const val API_KEY = ""
        private const val DB_URL = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    }

    private lateinit var rvChat: RecyclerView
    private lateinit var chatAdapter: ChatAdapter
    private val messageList = mutableListOf<ChatMessage>()

    private var generativeModel: GenerativeModel? = null
    private var chatSession: Chat? = null
    private var chatRef: DatabaseReference? = null

    // =====================================================
    // BIẾN QUẢN LÝ ĐẶT HÀNG NHANH
    // =====================================================
    private var quickOrderDialog: BottomSheetDialog? = null
    private var currentSelectedAddress: Address? = null

    // Cầu nối để gọi lại hàm tính phí ship sau khi chọn địa chỉ
    private var onAddressSelectedAction: (() -> Unit)? = null

    // API GHN
    private val ghnApiService by lazy {
        GHNApiService.create()
    }

    // Bộ hứng dữ liệu từ AddressListActivity trả về
    private val addressPickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == RESULT_OK) {
            val addr = result.data?.getSerializableExtra("SELECTED_ADDRESS") as? Address
            if (addr != null) {
                currentSelectedAddress = addr
                quickOrderDialog?.findViewById<TextView>(R.id.tvReceiverNamePhone)?.text = "${addr.receiverName} - ${addr.phone}"
                quickOrderDialog?.findViewById<TextView>(R.id.tvAddressDetail)?.text = "${addr.detail}, ${addr.ward}, ${addr.district}, ${addr.cityProvince}"

                // Kích hoạt tính lại phí ship
                onAddressSelectedAction?.invoke()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        initViews()
        initChatDatabase()
        loadBooksAndInitAI()
    }

    private fun initViews() {
        rvChat = findViewById(R.id.rvChat)
        val edtMessage = findViewById<EditText>(R.id.edtMessage)
        val btnSend = findViewById<ImageButton>(R.id.btnSend)
        val btnBack = findViewById<ImageView>(R.id.btnBack)

        chatAdapter = ChatAdapter(messageList)
        rvChat.apply {
            adapter = chatAdapter
            layoutManager = LinearLayoutManager(this@ChatActivity).apply {
                stackFromEnd = true
            }
        }

        btnBack.setOnClickListener { finish() }

        btnSend.setOnClickListener {
            val question = edtMessage.text.toString().trim()
            if (question.isEmpty()) return@setOnClickListener
            sendMessage(question, true)
            edtMessage.setText("")
            askGemini(question)
        }
    }

    private fun sendMessage(text: String, isUser: Boolean, bookId: String? = null, actionType: String? = null) {
        val msg = ChatMessage(text = text, isUser = isUser, bookId = bookId, actionType = actionType)
        messageList.add(msg)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        rvChat.scrollToPosition(messageList.size - 1)
        chatRef?.push()?.setValue(msg)
    }

    private fun initChatDatabase() {
        val username = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE).getString("USERNAME", null)
        if (username != null) {
            val safeUsername = username.replace(".", "_").replace("#", "_").replace("$", "_").replace("[", "_").replace("]", "_")
            chatRef = FirebaseDatabase.getInstance(DB_URL).getReference("Chats").child(safeUsername)
            loadHistoryFromFirebase()
        } else {
            addLocalMessage("Xin chào 👋 Em là trợ lý AI của Nhà sách Heduci 📚", false)
        }
    }

    private fun loadHistoryFromFirebase() {
        chatRef?.get()?.addOnSuccessListener { snapshot ->
            messageList.clear()
            for (data in snapshot.children) {
                val msg = data.getValue(ChatMessage::class.java)
                if (msg != null) messageList.add(msg)
            }
            if (messageList.isEmpty()) {
                addLocalMessage("Xin chào 👋 Em là trợ lý AI của Nhà sách Heduci 📚", false)
            } else {
                chatAdapter.notifyDataSetChanged()
                rvChat.scrollToPosition(messageList.size - 1)
            }
        }
    }

    private fun addLocalMessage(text: String, isUser: Boolean) {
        val msg = ChatMessage(text, isUser)
        messageList.add(msg)
        chatAdapter.notifyItemInserted(messageList.size - 1)
        rvChat.scrollToPosition(messageList.size - 1)
    }

    private fun processAiReply(reply: String) {
        val regex = Regex("\\[(VIEW|ORDER)\\|(.*?)\\]")
        val matches = regex.findAll(reply)
        var cleanText = reply
        var actionType: String? = null
        var targetBookId: String? = null

        for (match in matches) {
            actionType = match.groupValues[1]
            targetBookId = match.groupValues[2]
            cleanText = cleanText.replace(match.value, "")
        }

        sendMessage(text = cleanText.trim(), isUser = false, bookId = targetBookId, actionType = actionType)
    }

    private fun loadBooksAndInitAI() {
        FirebaseDatabase.getInstance(DB_URL).getReference("Books").get().addOnSuccessListener { snapshot ->
            var storeContext = ""
            for (book in snapshot.children) {
                val bookId = book.key ?: ""
                val ten = book.child("tenSach").value?.toString() ?: "Chưa rõ"
                val gia = book.child("gia").value?.toString() ?: "0"
                val moTa = book.child("moTa").value?.toString() ?: ""
                val soLuong = book.child("soLuong").value?.toString() ?: "0"

                storeContext += "ID: $bookId\nTên sách: $ten\nGiá: $gia VNĐ\nSố lượng: $soLuong\nMô tả: $moTa\n\n"
            }

            generativeModel = GenerativeModel(
                modelName = "gemini-2.5-flash",
                apiKey = API_KEY,
                systemInstruction = content {
                    text("""
                        Bạn là trợ lý ảo thông minh của 'Nhà sách Heduci'. 
                        Hãy tư vấn nhiệt tình về các loại sách, xưng hô là 'Em' và gọi khách là 'Anh/Chị' hoặc 'Bạn'. 
                        Nếu khách khen hoặc hỏi ai tạo ra bạn, hãy tự hào nói rằng bạn được tạo ra bởi lập trình viên xuất sắc Cao Thu Huệ. 
                        NHIỆM VỤ VÀ KỸ NĂNG CỦA BẠN: 
                        1. TƯ VẤN CHÍNH XÁC: Báo giá, tác giả và số lượng sách dựa CHÍNH XÁC vào danh sách trên. Nếu 'soLuong' = 0 thì báo khách là đã hết hàng. 
                        2. PHÂN TÍCH CHỦ ĐỀ & THỂ LOẠI (QUAN TRỌNG): Nếu khách yêu cầu tìm sách theo một CHỦ ĐỀ, THỂ LOẠI, hoặc CẢM XÚC, hãy tự động đọc, phân tích phần 'Nội dung' (moTa) và 'Tên sách' trong kho. Sau đó chọn ra những cuốn có nội dung phù hợp nhất. 
                        3. GỢI Ý KHÉO LÉO: Nếu khách tìm một chủ đề mà trong kho hoàn toàn không có cuốn nào phù hợp, hãy khéo léo xin lỗi và chủ động giới thiệu 1-2 cuốn sách khác đang có sẵn kèm lời mời gọi đọc thử. 
                        - Khi giới thiệu sách, khách hỏi về sách : gắn [VIEW|ID_SÁCH] 
                        - Khi khách hỏi mua sách, hoặc cách đặt sách thì mới gắn [ORDER|ID_SÁCH] - Không tự bịa ID sách, còn đâu khi hỏi hay giới thiệu về sách thôi thì gắn [VIEW|ID_SÁCH] 
                        - Chỉ dùng ID thật từ kho

                        DANH SÁCH SÁCH:
                        $storeContext
                    """.trimIndent())
                }
            )
            chatSession = generativeModel?.startChat()
        }
    }

    private fun askGemini(question: String) {
        if (chatSession == null) return
        val typing = ChatMessage("Tiệm đang gõ...", false)
        messageList.add(typing)
        val typingPosition = messageList.size - 1
        chatAdapter.notifyItemInserted(typingPosition)
        rvChat.scrollToPosition(typingPosition)

        lifecycleScope.launch {
            try {
                val response = chatSession!!.sendMessage(question)
                val reply = response.text ?: "Em chưa hiểu ý anh/chị"
                if (typingPosition < messageList.size) {
                    messageList.removeAt(typingPosition)
                    chatAdapter.notifyItemRemoved(typingPosition)
                }
                processAiReply(reply)
            } catch (e: Exception) {
                if (typingPosition < messageList.size) {
                    messageList.removeAt(typingPosition)
                    chatAdapter.notifyItemRemoved(typingPosition)
                }
                sendMessage("Kết nối mạng hơi yếu 🥺", false)
            }
        }
    }

    // =====================================================
    // QUICK ORDER (TÍCH HỢP GHN API)
    // =====================================================

    fun showQuickOrderDialog(bookId: String) {
        val db = FirebaseDatabase.getInstance(DB_URL)
        val username = getSharedPreferences("AppBanSachPrefs", MODE_PRIVATE).getString("USERNAME", "") ?: ""

        if (username.isEmpty()) {
            Toast.makeText(this, "Vui lòng đăng nhập!", Toast.LENGTH_SHORT).show()
            return
        }

        db.getReference("Books").child(bookId).get().addOnSuccessListener { snapshot ->
            if (!snapshot.exists()) {
                Toast.makeText(this, "Sách không tồn tại!", Toast.LENGTH_SHORT).show()
                return@addOnSuccessListener
            }

            val tenSach = snapshot.child("tenSach").value?.toString() ?: ""
            val gia = snapshot.child("gia").getValue(Long::class.java) ?: 0L
            val tonKho = snapshot.child("soLuong").getValue(Int::class.java) ?: 0
            val image = snapshot.child("hinhAnh").value?.toString() ?: snapshot.child("image").value?.toString() ?: ""
            val theLoai = snapshot.child("theLoai").value?.toString() ?: ""

            quickOrderDialog = BottomSheetDialog(this)
            val view = layoutInflater.inflate(R.layout.dialog_quick_order, null)
            quickOrderDialog?.setContentView(view)

            val tvBookName = view.findViewById<TextView>(R.id.tvBookName)
            val tvBookPrice = view.findViewById<TextView>(R.id.tvBookPrice)
            val tvStock = view.findViewById<TextView>(R.id.tvStock)
            val tvTotal = view.findViewById<TextView>(R.id.tvTotal)

            val layoutSelectAddress = view.findViewById<View>(R.id.layoutSelectAddress)
            val tvReceiverNamePhone = view.findViewById<TextView>(R.id.tvReceiverNamePhone)
            val tvAddressDetail = view.findViewById<TextView>(R.id.tvAddressDetail)

            val edtQuantity = view.findViewById<EditText>(R.id.edtQuantity)
            val btnMinus = view.findViewById<Button>(R.id.btnMinus)
            val btnPlus = view.findViewById<Button>(R.id.btnPlus)
            val btnConfirm = view.findViewById<Button>(R.id.btnConfirm)

            tvBookName.text = tenSach
            tvBookPrice.text = "${String.format("%,d", gia)} VNĐ"
            tvStock.text = "Còn $tonKho sản phẩm"

            var currentShippingFee = 30000 // Tạm tính mặc định

            // ==========================================
            // LOGIC TÍNH TỔNG & GỌI GHN TỰ ĐỘNG
            // ==========================================
            fun updateTotal() {
                val qty = edtQuantity.text.toString().toIntOrNull() ?: 1
                val totalProductPrice = qty * gia

                if (currentSelectedAddress == null || currentSelectedAddress!!.districtId == 0) {
                    currentShippingFee = 30000
                    val finalTotal = totalProductPrice + currentShippingFee
                    tvTotal.text = "Tổng tiền: ${String.format("%,d", finalTotal)} VNĐ (Ship dự kiến 30k)"
                    btnConfirm.isEnabled = true
                    return
                }

                tvTotal.text = "Đang tính phí vận chuyển..."
                btnConfirm.isEnabled = false // Khóa nút lúc đang tính

                val totalWeight = (qty * 300).coerceAtLeast(500) // 300g/cuốn
                val request = FeeRequest(
                    to_district_id = currentSelectedAddress!!.districtId,
                    to_ward_code = currentSelectedAddress!!.wardCode,
                    weight = totalWeight,
                    service_type_id = 2,
                    insurance_value = 0
                )

                ghnApiService.calculateFee(request=request).enqueue(object : Callback<FeeResponse> {
                    override fun onResponse(call: Call<FeeResponse>, response: Response<FeeResponse>) {
                        currentShippingFee = response.body()?.data?.total ?: 30000
                        val finalTotal = totalProductPrice + currentShippingFee
                        tvTotal.text = "Tổng tiền: ${String.format("%,d", finalTotal)} VNĐ (Ship ${String.format("%,d", currentShippingFee)}đ)"
                        btnConfirm.isEnabled = true
                    }

                    override fun onFailure(call: Call<FeeResponse>, t: Throwable) {
                        currentShippingFee = 30000
                        val finalTotal = totalProductPrice + currentShippingFee
                        tvTotal.text = "Tổng tiền: ${String.format("%,d", finalTotal)} VNĐ (Lỗi mạng, phí ship 30k)"
                        btnConfirm.isEnabled = true
                    }
                })
            }

            // Gán hành động tính ship vào cầu nối
            onAddressSelectedAction = { updateTotal() }

            // 1. TẢI ĐỊA CHỈ MẶC ĐỊNH
            currentSelectedAddress = null
            db.getReference("Addresses").child(username).orderByChild("isDefault").equalTo(true)
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        if (snapshot.exists()) {
                            for (addrSnap in snapshot.children) {
                                val addr = addrSnap.getValue(Address::class.java)
                                if (addr != null) {
                                    currentSelectedAddress = addr
                                    tvReceiverNamePhone.text = "${addr.receiverName} - ${addr.phone}"
                                    tvAddressDetail.text = "${addr.detail}, ${addr.ward}, ${addr.district}, ${addr.cityProvince}"
                                    updateTotal() // Tính ship ngay khi load xong địa chỉ mặc định
                                    break
                                }
                            }
                        } else {
                            tvReceiverNamePhone.text = "Chưa có thông tin"
                            tvAddressDetail.text = "Vui lòng chọn địa chỉ giao hàng"
                            updateTotal()
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })

            // 2. MỞ DANH SÁCH ĐỊA CHỈ
            layoutSelectAddress.setOnClickListener {
                val intent = Intent(this@ChatActivity, AddressListActivity::class.java)
                intent.putExtra("USERNAME", username)
                addressPickerLauncher.launch(intent)
            }

            // PLUS & MINUS
            btnPlus.setOnClickListener {
                var qty = edtQuantity.text.toString().toIntOrNull() ?: 1
                if (qty < tonKho) {
                    qty++
                    edtQuantity.setText(qty.toString())
                    updateTotal()
                }
            }

            btnMinus.setOnClickListener {
                var qty = edtQuantity.text.toString().toIntOrNull() ?: 1
                if (qty > 1) {
                    qty--
                    edtQuantity.setText(qty.toString())
                    updateTotal()
                }
            }

            edtQuantity.addTextChangedListener(object : TextWatcher {
                override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                override fun afterTextChanged(s: Editable?) { updateTotal() }
            })

            // CONFIRM
            btnConfirm.setOnClickListener {
                if (currentSelectedAddress == null) {
                    Toast.makeText(this, "Vui lòng chọn địa chỉ giao hàng!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val qty = edtQuantity.text.toString().toIntOrNull() ?: 1
                if (qty > tonKho) {
                    Toast.makeText(this, "Số lượng vượt quá tồn kho!", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val orderKey = db.getReference("Orders").push().key ?: ""
                val addr = currentSelectedAddress!!

                val cartItem = CartItem(
                    bookId = bookId, tenSach = tenSach, gia = gia.toInt(),
                    hinhAnh = image, soLuong = qty, isSelected = true,
                    theLoai = theLoai, soLuongTon = tonKho, giaGoc = gia.toInt(), giaGiam = gia.toInt()
                )

                val totalProductPrice = gia.toInt() * qty
                val discountAmount = 0
                val finalTotal = totalProductPrice + currentShippingFee - discountAmount // Tính chuẩn theo GHN

                val order = Order(
                    orderId = orderKey, username = username,
                    receiverName = addr.receiverName,
                    phone = addr.phone,
                    addressDetail = "${addr.detail}, ${addr.ward}, ${addr.district}, ${addr.cityProvince}",
                    orderItems = listOf(cartItem),
                    totalProductPrice = totalProductPrice,
                    shippingFee = currentShippingFee, discountAmount = discountAmount, finalTotal = finalTotal,
                    paymentMethod = "COD", status = "dang_cho",
                    createdAt = System.currentTimeMillis(), completedAt = 0, reviewed = false
                )

                // SAVE ORDER & UPDATE STOCK
                db.getReference("Orders").child(orderKey).setValue(order).addOnSuccessListener {
                    db.getReference("Books").child(bookId).child("soLuong").setValue(tonKho - qty)
                    quickOrderDialog?.dismiss()

                    sendMessage(
                        "✅ Đặt hàng thành công!\n\n📚 $tenSach\n🔢 Số lượng: $qty\n💰 Tổng thanh toán: ${String.format("%,d", finalTotal)} VNĐ\n🚚 Sẽ giao đến: ${addr.receiverName} (${addr.phone})\n\nĐơn hàng đang chờ Tiệm xác nhận ạ.",
                        false
                    )
                }.addOnFailureListener {
                    Toast.makeText(this, "Đặt hàng thất bại!", Toast.LENGTH_SHORT).show()
                }
            }

            quickOrderDialog?.show()
        }
    }
}