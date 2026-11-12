package com.example.quanlysach.ui.bottomsheet

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.database.*
import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import com.example.quanlysach.CommentItem
import com.example.quanlysach.R
import com.example.quanlysach.ui.adapter.CommentAdapter

class CommentBottomSheet : BottomSheetDialogFragment() {

    private lateinit var rvComments: RecyclerView
    private lateinit var etComment: EditText
    private lateinit var btnSend: ImageButton
    private lateinit var tvCommentCount: TextView
    private lateinit var commentAdapter: CommentAdapter

    // Lấy streamId từ arguments
    private val streamId by lazy {
        arguments?.getString("streamId") ?: "video_test"
    }

    private val db = FirebaseDatabase.getInstance(
        "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )

    companion object {
        fun newInstance(streamId: String) = CommentBottomSheet().apply {
            arguments = Bundle().apply {
                putString("streamId", streamId)
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View =
        inflater.inflate(
            R.layout.fragment_comment_bottom_sheet,
            container,
            false
        )

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        rvComments     = view.findViewById(R.id.rvComments)
        etComment      = view.findViewById(R.id.etComment)
        btnSend        = view.findViewById(R.id.btnSendComment)
        tvCommentCount = view.findViewById(R.id.tvCommentCount)
        commentAdapter = CommentAdapter()

        // lấy avat cho chỗ comment
        val imgMyAvatar = view.findViewById<de.hdodenhof.circleimageview.CircleImageView>(R.id.imgMyAvatar)
        val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val myUsername = pref.getString("USERNAME", "")

        if (!myUsername.isNullOrEmpty()) {
            // Trỏ thẳng đến Users -> username_cua_toi -> avata để lấy ảnh mới nhất
            db.getReference("Users").child(myUsername).child("avata")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val currentMyAvatarUrl = snapshot.getValue(String::class.java) ?: ""

                        if (isAdded) {
                            com.bumptech.glide.Glide.with(this@CommentBottomSheet)
                                .load(currentMyAvatarUrl)
                                .placeholder(R.drawable.ic_default_avatar)
                                .circleCrop()
                                .into(imgMyAvatar)
                        }
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        // Xóa comment
        commentAdapter.onCommentLongClick = { item ->

            val currentUsername =
                requireContext()
                    .getSharedPreferences(
                        "AppBanSachPrefs",
                        Context.MODE_PRIVATE
                    )
                    .getString("USERNAME", "")

            val actionSheet = CommentActionBottomSheet(

                onCopy = {

                    val clipboard =
                        requireContext().getSystemService(
                            Context.CLIPBOARD_SERVICE
                        ) as ClipboardManager

                    clipboard.setPrimaryClip(
                        ClipData.newPlainText(
                            "comment",
                            item.content
                        )
                    )

                    Toast.makeText(
                        requireContext(),
                        "Đã sao chép",
                        Toast.LENGTH_SHORT
                    ).show()
                },

                onDelete = {

                    db.getReference(
                        "video_comments/$streamId/${item.commentId}"
                    ).removeValue()
                },

                isMyComment =
                    (item.username == currentUsername)
            )

            actionSheet.show(
                parentFragmentManager,
                "ActionSheet"
            )
        }

        rvComments.layoutManager =
            LinearLayoutManager(requireContext()).apply {
                stackFromEnd = true
            }

        rvComments.adapter = commentAdapter

        // Khi bàn phím hiện lên thì scroll xuống cuối
        rvComments.addOnLayoutChangeListener {
                _, _, _, _, bottom, _, _, _, oldBottom ->

            if (bottom < oldBottom) {
                rvComments.postDelayed({
                    if (commentAdapter.itemCount > 0) {

                        rvComments.scrollToPosition(
                            commentAdapter.itemCount - 1
                        )
                    }
                }, 100)
            }
        }

        loadComments()

        btnSend.setOnClickListener {
            sendComment()
        }

        etComment.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendComment()
                true
            } else {
                false
            }
        }
        view.findViewById<ImageButton>(R.id.btnCloseComment)
            .setOnClickListener {
                dismiss()
            }
    }

    private fun loadComments() {

        db.getReference("video_comments/$streamId")
            .limitToLast(50)
            .addChildEventListener(object : ChildEventListener {

                override fun onChildAdded(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {

                    val item = CommentItem(
                        commentId = snapshot.key ?: "",

                        username =
                            snapshot.child("username")
                                .getValue(String::class.java)
                                ?: "Ẩn danh",

                        content =
                            snapshot.child("content")
                                .getValue(String::class.java)
                                ?: "",

                        avatarUrl =
                            snapshot.child("avatarUrl")
                                .getValue(String::class.java)
                                ?: "",

                        timestamp =
                            snapshot.child("timestamp")
                                .getValue(Long::class.java)
                                ?: 0L,

                        likeCount =
                            snapshot.child("likeCount")
                                .getValue(Int::class.java)
                                ?: 0,

                        streamId = streamId
                    )

                    commentAdapter.addComment(item)
                    tvCommentCount.text =
                        "${commentAdapter.itemCount} bình luận"
                    rvComments.scrollToPosition(
                        commentAdapter.itemCount - 1
                    )
                }

                override fun onChildChanged(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {}

                override fun onChildRemoved(snapshot: DataSnapshot) {

                    commentAdapter.removeComment(
                        snapshot.key ?: ""
                    )
                    tvCommentCount.text =
                        "${commentAdapter.itemCount} bình luận"
                }

                override fun onChildMoved(
                    snapshot: DataSnapshot,
                    previousChildName: String?
                ) {}

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // để ko bị bàn phím đè lên ô nd comment
    override fun onCreateDialog(savedInstanceState: Bundle?): android.app.Dialog {
        val dialog = super.onCreateDialog(savedInstanceState)
        dialog.window?.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE)
        return dialog
    }

    private fun sendComment() {
        val text = etComment.text.toString().trim()
        if (text.isEmpty()) return

        val pref = requireContext().getSharedPreferences(
            "AppBanSachPrefs",
            Context.MODE_PRIVATE
        )

        val username = pref.getString("USERNAME", null)

        if (username.isNullOrEmpty()) {
            Toast.makeText(
                requireContext(),
                "Vui lòng đăng nhập để có thể bình luận!",
                Toast.LENGTH_SHORT
            ).show()
            dismiss()
            return
        }

        // 🔥 GIẢI PHÁP CHÍ MẠNG TẠI ĐÂY:
        // Ngay khi nhấn nút gửi, chạy thẳng lên node Users -> [username của bro] -> "avata" để lấy link ảnh thật hiện tại trên Firebase
        db.getReference("Users").child(username).child("avata")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    // Bốc link ảnh mới nhất vừa đổi, nếu trống thì dùng chuỗi rỗng
                    val realTimeAvatarUrl = snapshot.getValue(String::class.java) ?: ""

                    // Sau khi có link ảnh chuẩn 100% rồi mới tiến hành push lên bảng comment
                    val ref = db.getReference("video_comments/$streamId").push()
                    ref.setValue(
                        mapOf(
                            "username"  to username,
                            "content"   to text,
                            "timestamp" to ServerValue.TIMESTAMP,
                            "likeCount" to 0,
                            "avatarUrl" to realTimeAvatarUrl // 🔥 Lưu link ảnh mới nhất vào đây
                        )
                    ).addOnSuccessListener {
                        etComment.setText("")
                        hideKeyboard()
                    }.addOnFailureListener {
                        Toast.makeText(requireContext(), "Gửi bình luận thất bại", Toast.LENGTH_SHORT).show()
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(requireContext(), "Lỗi kết nối khi gửi bình luận", Toast.LENGTH_SHORT).show()
                }
            })
    }

    private fun hideKeyboard() {

        val imm =
            requireContext().getSystemService(
                Context.INPUT_METHOD_SERVICE
            ) as InputMethodManager

        imm.hideSoftInputFromWindow(
            etComment.windowToken,
            0
        )
    }

    override fun onStart() {
        super.onStart()
        // Cấu hình bắt ép BottomSheet mở rộng tối đa (Full Screen) khi vừa hiện lên
        dialog?.let { dialog ->
            val bottomSheet = dialog.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.layoutParams?.height = ViewGroup.LayoutParams.MATCH_PARENT

            val behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet!!)
            behavior.state = com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
            behavior.skipCollapsed = true // Không cho phép vuốt nửa chừng
        }
    }
}

