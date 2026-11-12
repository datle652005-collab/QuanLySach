package com.example.quanlysach.ui.adapter

import android.content.Context
import android.graphics.Typeface
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.AdminChatRoom
import java.text.SimpleDateFormat
import java.util.*

class AdminChatListAdapter(
    private val context: Context,
    private val list: List<AdminChatRoom>,
    private val onItemClick: (AdminChatRoom) -> Unit
) : RecyclerView.Adapter<AdminChatListAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(context).inflate(R.layout.item_admin_chat_room, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val room = list[position]
        holder.tvName.text = room.displayName
        holder.tvLastMessage.text = room.lastMessage

        // Hiển thị thời gian tin nhắn
        if (room.lastTimestamp > 0) {
            val sdf = SimpleDateFormat("HH:mm", Locale.getDefault())
            holder.tvTime.text = sdf.format(Date(room.lastTimestamp))
            holder.tvTime.visibility = View.VISIBLE
        } else {
            holder.tvTime.visibility = View.GONE
        }

        // Xử lý trạng thái Chưa đọc / Đã đọc chuẩn Messenger
        if (room.unreadCount > 0) {
            holder.vUnreadDot.visibility = View.VISIBLE
            holder.tvLastMessage.setTypeface(null, Typeface.BOLD)
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(context, android.R.color.black))
        } else {
            holder.vUnreadDot.visibility = View.GONE
            holder.tvLastMessage.setTypeface(null, Typeface.NORMAL)
            holder.tvLastMessage.setTextColor(ContextCompat.getColor(context, android.R.color.darker_gray))
        }

        // ÉP ẢNH AVATAR THẬT THÀNH HÌNH TRÒN CHUẨN ĐẸP
        Glide.with(context)
            .load(room.avatarUrl)
            .placeholder(R.drawable.user_avatar_placeholder)
            .error(R.drawable.user_avatar_placeholder)
            .circleCrop()
            .into(holder.imgAvatar)

        holder.itemView.setOnClickListener { onItemClick(room) }
    }

    override fun getItemCount(): Int = list.size

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imgAvatar: ImageView = itemView.findViewById(R.id.imgRoomAvatar)
        val tvName: TextView = itemView.findViewById(R.id.tvRoomName)
        val tvLastMessage: TextView = itemView.findViewById(R.id.tvRoomLastMessage)
        val tvTime: TextView = itemView.findViewById(R.id.tvRoomTime)
        val vUnreadDot: View = itemView.findViewById(R.id.vUnreadDot)
    }
}