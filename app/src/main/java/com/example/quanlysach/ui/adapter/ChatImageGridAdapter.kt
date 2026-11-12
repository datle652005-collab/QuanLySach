package com.example.quanlysach.ui.adapter

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R

class ChatImageGridAdapter(
    private val context: Context,
    private val urls: List<String>,
    private val onImageClick: (Int) -> Unit // Callback trả về vị trí ảnh được click dưới dạng Int
) : RecyclerView.Adapter<ChatImageGridAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(context).apply {
            // Thiết lập kích thước mặc định ban đầu cho ô ảnh
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            scaleType = ImageView.ScaleType.CENTER_CROP
        }
        return ViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val params = holder.imageView.layoutParams

        // Tự động tính toán kích thước ô vuông co giãn thông minh tùy theo số lượng ảnh gửi cùng lúc
        if (urls.size == 1) {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT
            params.height = (220 * context.resources.displayMetrics.density).toInt() // 1 ảnh đơn thì cho cao rộng rõ ràng
        } else {
            val density = context.resources.displayMetrics.density
            // Nếu 2 ảnh thì chia đôi khung chat (khoảng 116dp/ô), từ 3 ảnh trở lên chia 3 (khoảng 76dp/ô) giống Zalo
            val sizeInDp = if (urls.size == 2) (116 * density).toInt() else (76 * density).toInt()
            params.width = sizeInDp
            params.height = sizeInDp
        }
        holder.imageView.layoutParams = params

        // Sử dụng Glide để tải ảnh vào từng ô vuông nhỏ trong lưới
        Glide.with(context)
            .load(urls[position])
            .placeholder(R.drawable.user_avatar_placeholder)
            .error(R.drawable.user_avatar_placeholder)
            .into(holder.imageView)

        // Bắt sự kiện click vào ảnh và truyền ngược vị trí (position) về cho ConsultAdapter xử lý mở bộ lướt ảnh
        holder.imageView.setOnClickListener {
            onImageClick(position)
        }
    }

    override fun getItemCount(): Int = urls.size

    class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}