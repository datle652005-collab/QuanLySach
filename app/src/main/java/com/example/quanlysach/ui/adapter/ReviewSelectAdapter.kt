package com.example.quanlysach.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.CartItem

import com.google.android.material.card.MaterialCardView

class ReviewSelectAdapter(
    private val list: List<CartItem>,
    private val reviewedIds: Set<String>,
    private val onClick: (CartItem) -> Unit
) : RecyclerView.Adapter<ReviewSelectAdapter.VH>() {

    private var selectedPos = 0

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val img = v.findViewById<ImageView>(R.id.imgItemReviewSelect)
        val tvName = v.findViewById<TextView>(R.id.tvItemReviewName)
        val tvStatus = v.findViewById<TextView>(R.id.tvIsReviewed)
        val card = v.findViewById<MaterialCardView>(R.id.cardRoot)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_review_select, parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        holder.tvName.text = item.tenSach
        Glide.with(holder.itemView.context).load(item.hinhAnh).into(holder.img)

        // Trạng thái đánh giá
        if (reviewedIds.contains(item.bookId)) {
            holder.tvStatus.text = "Đã đánh giá ✅"
            holder.tvStatus.setTextColor(Color.parseColor("#10B981"))
        } else {
            holder.tvStatus.text = "Chưa đánh giá"
            holder.tvStatus.setTextColor(Color.parseColor("#94A3B8"))
        }

        // Highlight card được chọn
        holder.card.strokeColor = if (selectedPos == position)
            Color.parseColor("#2563EB") else Color.parseColor("#E2E8F0")
        holder.card.strokeWidth = if (selectedPos == position) 4 else 2

        holder.itemView.setOnClickListener {
            val old = selectedPos
            selectedPos = holder.adapterPosition
            notifyItemChanged(old)
            notifyItemChanged(selectedPos)
            onClick(item)
        }
    }

    override fun getItemCount() = list.size
}