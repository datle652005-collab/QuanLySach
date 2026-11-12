package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.data.model.CartItem
import com.example.quanlysach.databinding.ItemOrderDetailProductBinding

import java.text.NumberFormat
import java.util.*

class OrderItemAdapter(private val list: List<CartItem>) : RecyclerView.Adapter<OrderItemAdapter.VH>() {

    inner class VH(val binding: ItemOrderDetailProductBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val binding = ItemOrderDetailProductBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return VH(binding)
    }

    override fun onBindViewHolder(holder: VH, position: Int) {
        val item = list[position]
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        with(holder.binding) {
            tvProductName.text = item.tenSach
            tvProductPrice.text = formatter.format(item.gia)
            tvQuantity.text = "x${item.soLuong}"

            Glide.with(root.context)
                .load(item.hinhAnh)
                .into(imgProduct)
        }
    }

    override fun getItemCount() = list.size
}