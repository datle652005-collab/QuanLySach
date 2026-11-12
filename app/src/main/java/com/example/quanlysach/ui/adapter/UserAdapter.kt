package com.example.quanlysach.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.User
import com.google.firebase.database.FirebaseDatabase

class UserAdapter(
    private var list: List<User>,
    private val onDetailClick: (User) -> Unit
) : RecyclerView.Adapter<UserAdapter.ViewHolder>() {

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val imgAvatar: ImageView = view.findViewById(R.id.imgUserAvatar)
        val tvName: TextView = view.findViewById(R.id.tvUserName)
        val tvAccount: TextView = view.findViewById(R.id.tvUserAccount) // Hiển thị username
        val tvRole: TextView = view.findViewById(R.id.tvRoleBadge)
        val tvStatus: TextView = view.findViewById(R.id.tvStatusBadge)
        val btnLock: ImageView = view.findViewById(R.id.btnLockUnlock)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_user, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val user = list[position]

        holder.tvName.text = user.ten_user
        holder.tvAccount.text = "@${user.username}"

        Glide.with(holder.itemView.context)
            .load(user.avata)
            .placeholder(R.drawable.ic_launcher_background)
            .into(holder.imgAvatar)

        // 1. Xử lý hiển thị Badge Vai trò
        if (user.vai_tro == "admin") {
            holder.tvRole.text = "Admin"
            holder.tvRole.visibility = View.VISIBLE
            // Vô hiệu hóa nút khóa nếu là Admin
            holder.btnLock.visibility = View.INVISIBLE // Hoặc dùng View.GONE để ẩn hẳn
        } else {
            holder.tvRole.text = "Khách hàng"
            holder.tvRole.visibility = View.VISIBLE
            holder.btnLock.visibility = View.VISIBLE
        }

        // 2. Xử lý hiển thị Badge Trạng thái
        if (user.trang_thai == "bi_khoa") {
            holder.tvStatus.text = "Bị khóa"
            holder.tvStatus.visibility = View.VISIBLE
            holder.btnLock.setImageResource(android.R.drawable.ic_partial_secure) // Icon mở khóa
        } else {
            holder.tvStatus.visibility = View.GONE
            holder.btnLock.setImageResource(android.R.drawable.ic_secure) // Icon khóa
        }

        // 3. Sự kiện Click
        holder.btnLock.setOnClickListener {
            if (user.vai_tro != "admin") {
                confirmLockToggle(holder.itemView.context, user)
            } else {
                Toast.makeText(holder.itemView.context, "Không thể khóa tài khoản Admin", Toast.LENGTH_SHORT).show()
            }
        }

        holder.itemView.setOnClickListener { onDetailClick(user) }
    }

    override fun getItemCount(): Int = list.size

    fun updateList(newList: List<User>) {
        this.list = newList
        notifyDataSetChanged()
    }

    private fun confirmLockToggle(context: Context, user: User) {
        if (user.username.isEmpty()) return

        val isLocked = user.trang_thai == "bi_khoa"
        val nextStatus = if (isLocked) "hoat_dong" else "bi_khoa"
        val title = if (isLocked) "Mở khóa" else "Khóa tài khoản"

        AlertDialog.Builder(context)
            .setTitle(title)
            .setMessage("Bạn có chắc chắn muốn thực hiện thao tác này với ${user.ten_user}?")
            .setPositiveButton("Xác nhận") { _, _ ->
                val dbRef = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                    .getReference("Users").child(user.username)

                val updates = mapOf<String, Any>("trang_thai" to nextStatus)

                dbRef.updateChildren(updates).addOnSuccessListener {
                    Toast.makeText(context, "Đã cập nhật!", Toast.LENGTH_SHORT).show()
                }
            }
            .setNegativeButton("Hủy", null)
            .show()
    }
}