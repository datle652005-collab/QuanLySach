package com.example.quanlysach.ui.adapter

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.data.model.NotificationModel

import com.example.quanlysach.databinding.ItemNotificationBinding
import com.example.quanlysach.ui.activity.UserOrderDetailActivity

import java.text.SimpleDateFormat
import java.util.*
import kotlin.jvm.java

class NotificationAdapter(private val list: List<NotificationModel>) :
    RecyclerView.Adapter<NotificationAdapter.ViewHolder>() {

    class ViewHolder(val binding: ItemNotificationBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder(ItemNotificationBinding.inflate(
            LayoutInflater.from(parent.context), parent, false
        ))
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val item = list[position]
        val sdf = SimpleDateFormat("HH:mm dd-MM-yyyy", Locale.getDefault())

        holder.binding.apply {
            tvNotiTitle.text = item.title
            tvNotiContent.text = item.content
            tvNotiTime.text = sdf.format(Date(item.timestamp))

            // 1. Xử lý hiển thị ảnh từ Firebase
            if (item.image.isNotEmpty()) {
                imgNoti.visibility = View.VISIBLE
                Glide.with(root.context)
                    .load(item.image)
                    .centerCrop()
                    .into(imgNoti)
            } else {
                imgNoti.visibility = View.GONE
            }

            // 2. Xử lý sự kiện click để xem chi tiết đơn hàng (Shopee Style)
            root.setOnClickListener {
                if (!item.orderId.isNullOrEmpty()) {
                    // Mở màn hình UserOrderDetailActivity (Trang chi tiết cho khách)
                    val intent = Intent(root.context, UserOrderDetailActivity::class.java).apply {
                        putExtra("ORDER_ID", item.orderId)
                    }
                    root.context.startActivity(intent)
                } else {
                    // Nếu thông báo không gắn mã đơn (ví dụ thông báo khuyến mãi chung)
                    Toast.makeText(root.context, "Thông báo này không có chi tiết đơn hàng", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    override fun getItemCount(): Int = list.size
}