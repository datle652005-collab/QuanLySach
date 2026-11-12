package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.ui.activity.TopBook


class TopBookAdapter(private val list: List<TopBook>) : RecyclerView.Adapter<TopBookAdapter.ViewHolder>() {
    class ViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val img = v.findViewById<ImageView>(R.id.imgBook)
        val name = v.findViewById<TextView>(R.id.tvBookName)
        val sales = v.findViewById<TextView>(R.id.tvBookSales)
        val rank = v.findViewById<TextView>(R.id.tvRank)
    }
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_top_book, parent, false)
        return ViewHolder(v)
    }
    override fun onBindViewHolder(h: ViewHolder, p: Int) {
        val item = list[p]
        h.name.text = item.name
        h.sales.text = "Đã bán: ${item.sales} cuốn"
        h.rank.text = "#${p + 1}"
        Glide.with(h.itemView.context).load(item.imageUrl).placeholder(R.drawable.ic_book_placeholder).into(h.img)
    }
    override fun getItemCount() = list.size
}