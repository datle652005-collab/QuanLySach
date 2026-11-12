package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Video
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class VideoThumbnailAdapter(
    private val items: List<Video>,
    private val onClick: (Video) -> Unit
) : RecyclerView.Adapter<VideoThumbnailAdapter.VH>() {


    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val imgThumb: ImageView = v.findViewById(R.id.imgVideoThumb)
        val tvTitle: TextView = v.findViewById(R.id.tvVideoTitle)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(LayoutInflater.from(parent.context).inflate(R.layout.item_video_thumbnail, parent, false))

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = items[position]
        holder.tvTitle.text = item.title

        if (item.bookId.isNotEmpty()) {
            FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Books/${item.bookId}/hinhAnh")
                .addListenerForSingleValueEvent(object : ValueEventListener {
                    override fun onDataChange(snapshot: DataSnapshot) {
                        val imgUrl = snapshot.getValue(String::class.java) ?: ""
                        Glide.with(holder.imgThumb).load(imgUrl)
                            .centerCrop()
                            .placeholder(R.drawable.ic_book_placeholder)
                            .into(holder.imgThumb)
                    }
                    override fun onCancelled(error: DatabaseError) {}
                })
        }

        holder.itemView.setOnClickListener { onClick(item) }
    }
}