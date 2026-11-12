package com.example.quanlysach.ui.adapter

import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.SwitchCompat
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Voucher
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoucherManageAdapter(
    private val list: MutableList<Voucher>,
    private val onVoucherClick: (Voucher) -> Unit,
    private val onToggle: (Voucher, Boolean) -> Unit
) : RecyclerView.Adapter<VoucherManageAdapter.VoucherVH>() {

    inner class VoucherVH(view: View) : RecyclerView.ViewHolder(view) {
        val viewAccent: View = view.findViewById(R.id.viewAccent)
        val title: TextView = view.findViewById(R.id.tvVoucherTitle)
        val typeBadge: TextView = view.findViewById(R.id.tvVoucherType)
        val code: TextView = view.findViewById(R.id.tvVoucherCode)
        val expiry: TextView = view.findViewById(R.id.tvVoucherExpiry)
        val target: TextView = view.findViewById(R.id.tvVoucherTarget)
        val usageText: TextView = view.findViewById(R.id.tvUsageText)
        val progress: ProgressBar = view.findViewById(R.id.pbVoucherUsage)
        val swStatus: SwitchCompat = view.findViewById(R.id.swVoucherStatus)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VoucherVH {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_voucher_manage, parent, false)
        return VoucherVH(view)
    }

    override fun onBindViewHolder(holder: VoucherVH, position: Int) {
        val item = list[position]
        val dec = DecimalFormat("#,###")
        val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())

        // =====================================
        // TIÊU ĐỀ & MỨC GIẢM
        // =====================================
        val discountText = if (item.type == "PERCENT") {
            "${item.discountValue.toInt()}%"
        } else {
            "${dec.format(item.discountValue)}đ"
        }

        val maxDiscountText = if (item.type == "PERCENT" && item.maxDiscountAmount > 0) {
            " • Tối đa ${dec.format(item.maxDiscountAmount)}đ"
        } else {
            ""
        }
        holder.title.text = "Giảm $discountText$maxDiscountText"

        // =====================================
        // MÃ & ĐIỀU KIỆN & HẠN SỬ DỤNG
        // =====================================
        holder.code.text = item.code.uppercase()

        val minOrderText = if (item.minOrderValue > 0) " | Đơn từ ${dec.format(item.minOrderValue)}đ" else ""
        holder.target.text = "• ${item.applyTargetName}$minOrderText"

        // Cập nhật logic: Nếu endDate = 0 thì báo Không thời hạn
        if (item.endDate > 0L) {
            holder.expiry.text = "Hết hạn: ${sdf.format(Date(item.endDate))}"
        } else {
            holder.expiry.text = "Hết hạn: Không thời hạn"
        }

        // =====================================
        // LƯỢT SỬ DỤNG
        // =====================================
        if (item.maxUsage > 0) {
            holder.progress.visibility = View.VISIBLE
            holder.progress.max = item.maxUsage
            holder.progress.progress = item.usedCount.coerceAtMost(item.maxUsage)
            holder.usageText.text = "${item.usedCount}/${item.maxUsage}"

            // Cảnh báo đỏ nếu đã dùng hết 90% hoặc full 100%
            if (item.usedCount >= item.maxUsage * 0.9) {
                holder.progress.progressTintList = ColorStateList.valueOf(Color.parseColor("#EF4444"))
            } else {
                holder.progress.progressTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))
            }
        } else {
            holder.progress.visibility = View.INVISIBLE
            holder.usageText.text = "Đã dùng: ${item.usedCount} (Không giới hạn)"
        }

        // =====================================
        // CÔNG TẮC BẬT/TẮT VÀ GIAO DIỆN
        // =====================================
        holder.swStatus.setOnCheckedChangeListener(null)
        holder.swStatus.isChecked = item.active
        updateSwitchColor(holder.swStatus, item.active)

        // 🔥 ĐÃ SỬA LỖI BUG THỜI GIAN
        val isExpired = item.endDate > 0L && item.endDate < System.currentTimeMillis()
        val isDepleted = item.maxUsage > 0 && item.usedCount >= item.maxUsage // Lỗi hết lượt

        if (!item.active || isExpired || isDepleted) {
            setDisabledState(holder, item)
        } else {
            setNormalState(holder, item)
        }

        holder.swStatus.setOnCheckedChangeListener { _, isChecked ->
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                item.active = isChecked
                updateSwitchColor(holder.swStatus, isChecked)

                if (!isChecked || isExpired || isDepleted) {
                    setDisabledState(holder, item)
                } else {
                    setNormalState(holder, item)
                }
                onToggle(item, isChecked)
            }
        }

        holder.itemView.setOnClickListener {
            if (holder.adapterPosition != RecyclerView.NO_POSITION) {
                onVoucherClick(item)
            }
        }
    }

    private fun updateSwitchColor(sw: SwitchCompat, isChecked: Boolean) {
        if (isChecked) {
            sw.thumbTintList = ColorStateList.valueOf(Color.parseColor("#FFFFFF"))
            sw.trackTintList = ColorStateList.valueOf(Color.parseColor("#3B82F6"))
        } else {
            sw.thumbTintList = ColorStateList.valueOf(Color.parseColor("#F8FAFC"))
            sw.trackTintList = ColorStateList.valueOf(Color.parseColor("#CBD5E1"))
        }
    }

    private fun setNormalState(holder: VoucherVH, item: Voucher) {
        holder.itemView.alpha = 1f
        holder.viewAccent.setBackgroundColor(Color.parseColor("#3B82F6"))
        holder.title.setTextColor(Color.parseColor("#334155"))
        holder.code.setTextColor(Color.parseColor("#3B82F6"))

        if (item.loaiVoucher == "FREESHIP") {
            holder.typeBadge.text = "FREESHIP"
            holder.typeBadge.setTextColor(Color.parseColor("#10B981"))
            holder.typeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#ECFDF5"))
        } else {
            holder.typeBadge.text = "GIẢM GIÁ"
            holder.typeBadge.setTextColor(Color.parseColor("#F43F5E"))
            holder.typeBadge.backgroundTintList = ColorStateList.valueOf(Color.parseColor("#FFF1F2"))
        }
    }

    private fun setDisabledState(holder: VoucherVH, item: Voucher) {
        holder.itemView.alpha = 0.5f
        holder.viewAccent.setBackgroundColor(Color.parseColor("#94A3B8"))

        if (item.loaiVoucher == "FREESHIP") {
            holder.typeBadge.text = "FREESHIP"
        } else {
            holder.typeBadge.text = "GIẢM GIÁ"
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: List<Voucher>) {
        list.clear()
        list.addAll(newList)
        notifyDataSetChanged()
    }
}