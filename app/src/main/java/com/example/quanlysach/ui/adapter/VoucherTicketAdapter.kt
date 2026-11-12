package com.example.quanlysach.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Voucher
import java.text.NumberFormat
import java.util.Locale

class VoucherTicketAdapter(private val list: List<Voucher>) :
    RecyclerView.Adapter<VoucherTicketAdapter.TicketViewHolder>() {

    class TicketViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val layoutTicket: LinearLayout = view.findViewById(R.id.layoutTicket)
        val tvText: TextView = view.findViewById(R.id.tvVoucherTicketText)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): TicketViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_voucher_ticket, parent, false)
        return TicketViewHolder(view)
    }

    override fun onBindViewHolder(holder: TicketViewHolder, position: Int) {
        val voucher = list[position]

        // Format Text hiển thị (VD: Giảm 10k, Giảm 15%, Freeship)
        if (voucher.loaiVoucher == "FREESHIP") {
            holder.tvText.text = "Freeship"
            holder.layoutTicket.setBackgroundResource(R.drawable.bg_voucher_freeship) // Nếu bị lỗi R.layout thì đổi thành R.drawable.bg_voucher_freeship
            holder.tvText.setTextColor(Color.parseColor("#00897B")) // Xanh ngọc
        } else {
            if (voucher.type == "PERCENT") {
                holder.tvText.text = "Giảm ${voucher.discountValue.toInt()}%"
            } else {
                val formatter = NumberFormat.getInstance(Locale("vi", "VN"))
                val tienGiam = formatter.format(voucher.discountValue).replace(".000", "k")
                holder.tvText.text = "Giảm $tienGiam"
            }
            holder.layoutTicket.setBackgroundResource(R.drawable.bg_voucher_discount)
            holder.tvText.setTextColor(Color.parseColor("#EE4D2D")) // Cam đỏ
        }
    }

    override fun getItemCount() = list.size
}