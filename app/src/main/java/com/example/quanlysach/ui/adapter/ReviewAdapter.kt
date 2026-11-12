package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RatingBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Review
import com.example.quanlysach.ui.activity.AllReviewsActivity
import java.text.SimpleDateFormat
import java.util.*

class ReviewAdapter(private val list: List<Review>) : RecyclerView.Adapter<ReviewAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val tvUser: TextView = v.findViewById(R.id.tvReviewUser)
        val tvContent: TextView = v.findViewById(R.id.tvReviewContent)
        val tvDate: TextView = v.findViewById(R.id.tvReviewDate)
        val rating: RatingBar = v.findViewById(R.id.ratingReviewItem)
        val rvImages: RecyclerView = v.findViewById(R.id.rvReviewImages)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_review, parent, false)
        return VH(v)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val r = list[position]
        holder.tvUser.text = r.username
        holder.tvContent.text = r.content
        holder.rating.rating = r.rating.toFloat()

        val fmt = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tvDate.text = fmt.format(Date(r.timestamp))

        // Xử lý nếu đánh giá có kèm hình ảnh
        if (!r.images.isNullOrEmpty()) {
            holder.rvImages.visibility = View.VISIBLE
            holder.rvImages.layoutManager = LinearLayoutManager(
                holder.itemView.context,
                LinearLayoutManager.HORIZONTAL,
                false
            )

            // Gắn ReviewImageAdapter và xử lý sự kiện click để phóng to ảnh
            holder.rvImages.adapter = ReviewImageAdapter(r.images) { imageUrl ->
                val context = holder.itemView.context
                // Gọi hàm phóng to ảnh từ AllReviewsActivity
                if (context is AllReviewsActivity) {
                    context.showZoomedImage(imageUrl)
                }
            }
        } else {
            // Nếu không có ảnh thì ẩn vùng chứa ảnh đi
            holder.rvImages.visibility = View.GONE
        }
    }

    override fun getItemCount() = list.size
}