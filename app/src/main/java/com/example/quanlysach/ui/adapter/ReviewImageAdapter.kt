package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R

class ReviewImageAdapter(
    private val images: List<String>,
    private val onImageClick: (String) -> Unit // ĐÂY LÀ DÒNG BẠN ĐANG THIẾU
) : RecyclerView.Adapter<ReviewImageAdapter.IVH>() {

    class IVH(v: View) : RecyclerView.ViewHolder(v) {
        val img: ImageView = v.findViewById(R.id.imgItemReview)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): IVH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_image_preview, parent, false)
        return IVH(v)
    }

    override fun onBindViewHolder(holder: IVH, position: Int) {
        val url = images[position]

        Glide.with(holder.itemView.context)
            .load(url)
            .into(holder.img)

        // Bắt sự kiện ấn vào ảnh và truyền ngược link ảnh ra ngoài
        holder.img.setOnClickListener {
            onImageClick(url)
        }
    }

    override fun getItemCount() = images.size
}