package com.example.quanlysach.ui.adapter

import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.ChatMessage
import com.example.quanlysach.ui.activity.BookDetailActivity
import com.example.quanlysach.ui.activity.ChatActivity
import com.google.firebase.database.FirebaseDatabase
import io.noties.markwon.Markwon

class ChatAdapter(
    private val messageList: MutableList<ChatMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val TYPE_USER = 1
        private const val TYPE_BOT = 2

        private const val DB_URL =
            "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    }

    override fun getItemViewType(position: Int): Int {
        return if (messageList[position].isUser) TYPE_USER else TYPE_BOT
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        val layoutId =
            if (viewType == TYPE_USER)
                R.layout.item_chat_user
            else
                R.layout.item_chat_bot

        val view = LayoutInflater
            .from(parent.context)
            .inflate(layoutId, parent, false)

        return if (viewType == TYPE_USER) {
            UserViewHolder(view)
        } else {
            BotViewHolder(view)
        }
    }

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        val message = messageList[position]

        if (holder is UserViewHolder) {

            holder.tvMessage.text = message.text

        } else if (holder is BotViewHolder) {

            bindBotMessage(holder, message)
        }
    }

    private fun bindBotMessage(
        holder: BotViewHolder,
        message: ChatMessage
    ) {

        val context = holder.itemView.context

        // MARKDOWN
        val markwon = Markwon.create(context)
        markwon.setMarkdown(holder.tvMessage, message.text)

        // EFFECT TYPING
        if (message.text.startsWith("Tiệm đang gõ")) {

            val anim = AlphaAnimation(0.3f, 1.0f).apply {
                duration = 700
                repeatMode = Animation.REVERSE
                repeatCount = Animation.INFINITE
            }

            holder.tvMessage.startAnimation(anim)

        } else {
            holder.tvMessage.clearAnimation()
        }

        // KHÔNG CÓ ACTION
        if (message.actionType.isNullOrEmpty()) {
            holder.btnAction.visibility = View.GONE
            return
        }

        // CÓ ACTION
        holder.btnAction.visibility = View.VISIBLE

        when (message.actionType) {

            "VIEW" -> {
                holder.btnAction.text = "Xem chi tiết"
            }

            "ORDER" -> {
                holder.btnAction.text = "Đặt mua ngay"
            }

            else -> {
                holder.btnAction.visibility = View.GONE
                return
            }
        }

        holder.btnAction.setOnClickListener {

            when (message.actionType) {

                "VIEW" -> {
                    openBookDetail(holder, message)
                }

                "ORDER" -> {
                    quickOrder(holder, message)
                }
            }
        }
    }

    // =========================
    // MỞ CHI TIẾT SÁCH
    // =========================

    private fun openBookDetail(
        holder: BotViewHolder,
        message: ChatMessage
    ) {

        val context = holder.itemView.context

        // CHECK NULL
        if (message.bookId.isNullOrEmpty()) {

            Toast.makeText(
                context,
                "Không tìm thấy thông tin sách!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        // CHECK FIREBASE
        FirebaseDatabase
            .getInstance(DB_URL)
            .getReference("Books")
            .child(message.bookId!!)
            .get()
            .addOnSuccessListener { snapshot ->

                if (!snapshot.exists()) {

                    Toast.makeText(
                        context,
                        "Sách không tồn tại!",
                        Toast.LENGTH_SHORT
                    ).show()

                    return@addOnSuccessListener
                }

                try {

                    val intent = Intent(
                        context,
                        BookDetailActivity::class.java
                    )

                    intent.putExtra(
                        "BOOK_ID",
                        message.bookId
                    )

                    context.startActivity(intent)

                } catch (e: Exception) {

                    Toast.makeText(
                        context,
                        "Không thể mở trang chi tiết!",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
            .addOnFailureListener {

                Toast.makeText(
                    context,
                    "Lỗi kết nối dữ liệu!",
                    Toast.LENGTH_SHORT
                ).show()
            }
    }

    // =========================
    // ĐẶT HÀNG NHANH
    // =========================

    private fun quickOrder(
        holder: BotViewHolder,
        message: ChatMessage
    ) {

        val context = getActivity(holder.itemView.context)

        if (message.bookId.isNullOrEmpty()) {

            Toast.makeText(
                holder.itemView.context,
                "Không tìm thấy sản phẩm!",
                Toast.LENGTH_SHORT
            ).show()

            return
        }

        if (context is ChatActivity) {

            context.showQuickOrderDialog(message.bookId!!)

        } else {

            Toast.makeText(
                holder.itemView.context,
                "Không thể kết nối đặt hàng!",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    // =========================
    // LẤY ACTIVITY GỐC
    // =========================

    private fun getActivity(context: Context?): ChatActivity? {

        if (context == null) return null

        return when (context) {

            is ChatActivity -> context

            is ContextWrapper -> getActivity(context.baseContext)

            else -> null
        }
    }

    override fun getItemCount(): Int {
        return messageList.size
    }

    // =========================
    // VIEW HOLDER USER
    // =========================

    class UserViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val tvMessage: TextView =
            view.findViewById(R.id.tvMessage)
    }

    // =========================
    // VIEW HOLDER BOT
    // =========================

    class BotViewHolder(view: View) :
        RecyclerView.ViewHolder(view) {

        val tvMessage: TextView =
            view.findViewById(R.id.tvMessage)

        val btnAction: Button =
            view.findViewById(R.id.btnAction)
    }
}