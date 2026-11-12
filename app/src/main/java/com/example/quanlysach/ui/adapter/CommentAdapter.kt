package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.CommentItem
import com.example.quanlysach.R
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import de.hdodenhof.circleimageview.CircleImageView

class CommentAdapter : RecyclerView.Adapter<CommentAdapter.VH>() {

    private val items = mutableListOf<CommentItem>()
    private val likedComments = mutableSetOf<String>()

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgAvatar: CircleImageView   = v.findViewById(R.id.imgCommentAvatar)
        val tvUsername: TextView         = v.findViewById(R.id.tvCommentUsername)
        val tvContent: TextView          = v.findViewById(R.id.tvCommentContent)
        val tvTime: TextView             = v.findViewById(R.id.tvCommentTime)
        val btnLike: ImageButton         = v.findViewById(R.id.btnLikeComment)
        val tvLikeCount: TextView        = v.findViewById(R.id.tvLikeCommentCount)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_comment, parent, false))

    override fun getItemCount() = items.size

    var onDeleteComment: ((CommentItem) -> Unit)? = null
    var onCommentLongClick: ((CommentItem) -> Unit)? = null

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]

        holder.tvUsername.text = item.username
        holder.tvContent.text  = item.content
        holder.tvLikeCount.text = item.likeCount.toString()
        holder.tvTime.text = formatTime(item.timestamp)


        Glide.with(holder.itemView.context)
            .load(item.avatarUrl)
            .placeholder(R.drawable.ic_default_avatar)
            .circleCrop()
            .into(holder.imgAvatar)

        val dbUserAvatarRef = FirebaseDatabase.getInstance()
            .getReference("Users")
            .child(item.username)
            .child("avata")

        dbUserAvatarRef.addListenerForSingleValueEvent(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val latestAvatarUrl = snapshot.getValue(String::class.java)

                // Nếu tìm thấy ảnh mới trên bảng Users và nó KHÁC với ảnh đang hiển thị
                if (!latestAvatarUrl.isNullOrEmpty() && latestAvatarUrl != item.avatarUrl) {
                    // Đè ảnh mới nhất lên ImageView
                    Glide.with(holder.itemView.context)
                        .load(latestAvatarUrl)
                        .placeholder(R.drawable.ic_default_avatar)
                        .circleCrop()
                        .into(holder.imgAvatar)
                }
            }

            override fun onCancelled(error: DatabaseError) {
            }
        })

        // Trạng thái like giữ nguyên
        val isLiked = likedComments.contains(item.commentId)
        holder.btnLike.setColorFilter(
            if (isLiked) 0xFFFF0000.toInt() else 0xFFCCCCCC.toInt()
        )

        holder.btnLike.setOnClickListener {
            val db = FirebaseDatabase.getInstance(
                "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
            )
            val likeRef = db.getReference(
                "video_comments/${item.streamId}/${item.commentId}/likeCount"
            )
            val liked = likedComments.contains(item.commentId)

            if (liked) {
                likedComments.remove(item.commentId)
                if (item.likeCount > 0) {
                    item.likeCount--
                }
                holder.btnLike.setColorFilter(0xFFCCCCCC.toInt())
            } else {
                likedComments.add(item.commentId)
                item.likeCount++
                holder.btnLike.setColorFilter(0xFFFF0000.toInt())
            }
            likeRef.setValue(item.likeCount)
            holder.tvLikeCount.text = item.likeCount.toString()
        }

        holder.itemView.setOnLongClickListener {
            onCommentLongClick?.invoke(item)
            true
        }
    }

    fun addComment(item: CommentItem) {
        items.add(item)
        notifyItemInserted(items.size - 1)
    }

    fun getCommentCount(): Int = items.size

    private fun formatTime(timestamp: Long): String {
        if (timestamp == 0L) return "vừa xong"
        val diff = System.currentTimeMillis() - timestamp
        return when {
            diff < 60_000 -> "vừa xong"
            diff < 3_600_000 -> "${diff / 60_000} phút"
            diff < 86_400_000 -> "${diff / 3_600_000} giờ"
            else -> "${diff / 86_400_000} ngày"
        }
    }

    fun removeComment(commentId: String) {
        val index = items.indexOfFirst { it.commentId == commentId }
        if (index != -1) {
            items.removeAt(index)
            notifyItemRemoved(index)
        }
    }
}