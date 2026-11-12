package com.example.quanlysach.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Voucher
import com.google.android.material.button.MaterialButton
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class PromotionAdapter(
    private val list: MutableList<Voucher>, // 🔥 Đổi thành val vì không gán lại list mới nữa
    private val onSaveClick: (Voucher) -> Unit,
    private val onUseClick: (Voucher) -> Unit,
    private val isVoucherSaved: (Voucher) -> Boolean
) : RecyclerView.Adapter<PromotionAdapter.PromoVH>() {

    inner class PromoVH(view: View) : RecyclerView.ViewHolder(view) {
        val tvTitle: TextView = view.findViewById(R.id.tvPromoTitle)
        val tvTarget: TextView = view.findViewById(R.id.tvPromoTarget)
        val tvExpiry: TextView = view.findViewById(R.id.tvPromoExpiry)
        val btnAction: MaterialButton = view.findViewById(R.id.btnPromoAction)
        val ivIcon: ImageView = view.findViewById(R.id.ivPromoIcon)
        val tvType: TextView = view.findViewById(R.id.tvPromoType)
        val leftLayout: LinearLayout = view.findViewById(R.id.layoutPromoLeft)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PromoVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_promotion, parent, false)
        return PromoVH(view)
    }

    override fun onBindViewHolder(holder: PromoVH, position: Int) {
        val item = list[position]
        val dec = DecimalFormat("#,###")

        // Xử lý Tiêu đề (Title)
        val valueText = if (item.type == "PERCENT") {
            "${item.discountValue}%"
        } else {
            "${dec.format(item.discountValue)}đ"
        }

        holder.tvTitle.text = if (item.loaiVoucher == "FREESHIP") {
            "🚚 Miễn phí vận chuyển"
        } else {
            "🎉 Giảm $valueText"
        }

        // Xử lý Đối tượng áp dụng (Target)
        val targetText = when (item.applyTo) {
            "BOOK" -> "Sách: ${item.applyTargetName}"
            "CATEGORY" -> "Chủ đề: ${item.applyTargetName}"
            else -> "Toàn sàn"
        }
        holder.tvTarget.text = "Áp dụng: $targetText"

        // Xử lý Hạn sử dụng (Expiry)
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
        holder.tvExpiry.text = "HSD: ${sdf.format(Date(item.endDate ?: 0L))}"

        // Xử lý Màu sắc & Icon (Style)
        if (item.loaiVoucher == "FREESHIP") {
            holder.leftLayout.setBackgroundColor(Color.parseColor("#FFF1F2")) // Hồng nhạt
            holder.tvType.text = "FREESHIP"
            holder.tvType.setTextColor(Color.parseColor("#D65B82"))

            // 🔥 NẠP ẢNH TỪ DRAWABLE THAY VÌ TINT
            holder.ivIcon.setImageResource(R.drawable.img_2) // Đổi tên file ảnh của bạn
            holder.ivIcon.setColorFilter(null) // Xóa filter màu nếu có
        } else {
            holder.leftLayout.setBackgroundColor(Color.parseColor("#FFF5F7")) // Hồng nhạt
            holder.tvType.text = "ƯU ĐÃI"
            holder.tvType.setTextColor(Color.parseColor("#D65B82"))

            // 🔥 NẠP ẢNH TỪ DRAWABLE (Voucher Xtra của bạn)
            holder.ivIcon.setImageResource(R.drawable.img) // Ảnh voucher xịn xò của bạn
            holder.ivIcon.setColorFilter(null)
        }

        // 2. Xử lý Trạng thái Nút bấm (Tông màu hồng chủ đạo)
        val saved = isVoucherSaved(item)
        if (saved) {
            holder.btnAction.text = "Dùng ngay"
            holder.btnAction.setBackgroundColor(Color.parseColor("#D65B82")) // Màu hồng Heduci
            holder.btnAction.setOnClickListener { onUseClick(item) }
        } else {
            holder.btnAction.text = "Lưu mã"
            holder.btnAction.setBackgroundColor(Color.parseColor("#D65B82")) // Màu hồng Heduci
            holder.btnAction.setOnClickListener { onSaveClick(item) }
        }


    }

    override fun getItemCount() = list.size
}