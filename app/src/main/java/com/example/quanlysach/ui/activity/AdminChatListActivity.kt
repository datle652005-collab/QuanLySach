package com.example.quanlysach.ui.activity

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysach.data.model.AdminChatRoom
import com.example.quanlysach.data.model.ConsultMessage
import com.example.quanlysach.data.model.User
import com.example.quanlysach.databinding.ActivityAdminChatListBinding
import com.example.quanlysach.ui.adapter.AdminChatListAdapter
import com.google.firebase.database.*

class AdminChatListActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAdminChatListBinding
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private lateinit var database: DatabaseReference

    private val roomsMap = mutableMapOf<String, AdminChatRoom>()
    private val roomsList = mutableListOf<AdminChatRoom>()
    private val displayList = mutableListOf<AdminChatRoom>()
    private lateinit var adapter: AdminChatListAdapter

    private var myName = ""
    private var myRole = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAdminChatListBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pref = getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        myName = pref.getString("USERNAME", "") ?: ""
        myRole = pref.getString("VAI_TRO", "khach_hang")!!

        binding.tvTitle.text = if (myRole == "admin") "Tin nhắn hỗ trợ" else "Hộp thư hỗ trợ"
        binding.btnBack.setOnClickListener { finish() }

        database = FirebaseDatabase.getInstance(dbUrl).reference

        setupRecyclerView()
        setupSearchFilter()
        loadChatRoomsFromFirebase()
    }

    private fun setupRecyclerView() {
        adapter = AdminChatListAdapter(this, displayList) { selectedRoom ->
            val intent = Intent(this@AdminChatListActivity, ConsultChatActivity::class.java)
            val receiver = if (myRole == "admin") selectedRoom.username else "admin"
            intent.putExtra("RECEIVER_USERNAME", receiver)
            startActivity(intent)
        }
        binding.rvChatList.layoutManager = LinearLayoutManager(this)
        binding.rvChatList.adapter = adapter
    }

    private fun setupSearchFilter() {
        binding.etSearchChat.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                filterChatRooms(s.toString())
            }
            override fun afterTextChanged(s: Editable?) {}
        })
    }

    private fun loadChatRoomsFromFirebase() {
        database.child("Consultations").addValueEventListener(object : ValueEventListener {
            override fun onDataChange(consultSnapshot: DataSnapshot) {
                roomsMap.clear()
                for (roomNode in consultSnapshot.children) {
                    val roomId = roomNode.key ?: continue
                    if (roomId.startsWith("admin_") && roomId.endsWith("_SUPPORT")) {
                        val customerUsername = roomId.substring(6, roomId.length - 8)

                        if (myRole != "admin" && customerUsername != myName) {
                            continue
                        }

                        var lastMsgText = "Không có nội dung tin nhắn"
                        var lastTime = 0L
                        var unreadCount = 0

                        val messages = mutableListOf<ConsultMessage>()
                        for (msgNode in roomNode.children) {
                            val msg = msgNode.getValue(ConsultMessage::class.java)
                            if (msg != null) {
                                messages.add(msg)
                                val currentReceiver = if (myRole == "admin") "admin" else myName
                                if (msg.receiverId == currentReceiver && !msg.read) {
                                    unreadCount++
                                }
                            }
                        }

                        if (messages.isNotEmpty()) {
                            messages.sortBy { it.timestamp }
                            val finalMsg = messages.last()

                            lastMsgText = if (finalMsg.type == "image") {
                                val count = finalMsg.message.split(",").size
                                if (count > 1) "Đã gửi $count ảnh" else "Đã gửi một ảnh"
                            } else {
                                finalMsg.message
                            }

                            lastTime = finalMsg.timestamp
                        }

                        val chatRoom = AdminChatRoom(
                            username = customerUsername,
                            displayName = customerUsername,
                            lastMessage = lastMsgText,
                            lastTimestamp = lastTime,
                            unreadCount = unreadCount
                        )
                        roomsMap[customerUsername] = chatRoom
                    }
                }
                fetchUsersProfileDetails()
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun fetchUsersProfileDetails() {
        database.child("Users").addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(userSnapshot: DataSnapshot) {
                if (myRole != "admin") {
                    // 1. NẾU LÀ KHÁCH HÀNG: Hiển thị thông tin Admin lên đầu
                    for (userNode in userSnapshot.children) {
                        val userObj = userNode.getValue(User::class.java)
                        if (userObj != null && userObj.vai_tro == "admin") {
                            val dbUsername = userNode.key ?: ""

                            val finalName = if (!userObj.ten_user.isNullOrBlank()) userObj.ten_user else dbUsername

                            roomsMap[myName]?.displayName = finalName
                            roomsMap[myName]?.avatarUrl = userObj.avata
                            break
                        }
                    }
                } else {
                    // 2. NẾU LÀ ADMIN: Duyệt qua từng user để lấy Tên và Ảnh
                    for (userNode in userSnapshot.children) {
                        val userObj = userNode.getValue(User::class.java)
                        val dbUsername = userNode.key ?: ""

                        if (userObj != null) {
                            userObj.username = dbUsername

                            if (roomsMap.containsKey(userObj.username)) {
                                val finalName = if (!userObj.ten_user.isNullOrBlank()) userObj.ten_user else dbUsername

                                roomsMap[userObj.username]?.displayName = finalName
                                roomsMap[userObj.username]?.avatarUrl = userObj.avata
                            }
                        }
                    }
                }

                roomsList.clear()
                roomsList.addAll(roomsMap.values)
                roomsList.sortByDescending { it.lastTimestamp }

                filterChatRooms(binding.etSearchChat.text.toString())
            }
            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun filterChatRooms(query: String) {
        val cleanQuery = query.trim().lowercase()
        displayList.clear()
        if (cleanQuery.isEmpty()) {
            displayList.addAll(roomsList)
        } else {
            for (room in roomsList) {
                if (room.displayName.lowercase().contains(cleanQuery) ||
                    room.username.lowercase().contains(cleanQuery)) {
                    displayList.add(room)
                }
            }
        }
        adapter.notifyDataSetChanged()
    }
}