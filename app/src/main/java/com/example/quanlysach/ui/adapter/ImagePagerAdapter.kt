package com.example.quanlysach.ui.adapter

import android.content.Context
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide

class ImagePagerAdapter(
    private val context: Context,
    private val imageUrls: List<String>,
    private val onImageClick: () -> Unit
) : RecyclerView.Adapter<ImagePagerAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val imageView = ImageView(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER
            setBackgroundColor(android.graphics.Color.BLACK)
        }
        return ViewHolder(imageView)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        Glide.with(context)
            .load(imageUrls[position])
            .into(holder.imageView)

        holder.imageView.setOnClickListener { onImageClick() }
    }

    override fun getItemCount(): Int = imageUrls.size

    class ViewHolder(val imageView: ImageView) : RecyclerView.ViewHolder(imageView)
}