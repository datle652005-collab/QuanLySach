package com.example.quanlysach.ui.adapter

import android.content.Intent
import android.graphics.Color
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Order
import com.example.quanlysach.databinding.ItemOrderRowBinding
import com.example.quanlysach.ui.activity.OrderDetailActivity
import java.text.NumberFormat
import java.util.*

class OrderAdapter(
    private val orderList: List<Order>,
    private val onUpdateClick: (Order) -> Unit
) : RecyclerView.Adapter<OrderAdapter.OrderViewHolder>() {

    inner class OrderViewHolder(val binding: ItemOrderRowBinding) :
        RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): OrderViewHolder {
        val binding = ItemOrderRowBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return OrderViewHolder(binding)
    }

    override fun onBindViewHolder(holder: OrderViewHolder, position: Int) {
        val order = orderList[position]
        val context = holder.itemView.context
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        with(holder.binding) {
            // 1. Hiển thị thông tin cơ bản
            tvOrderIdRow.text = "#${order.orderId.takeLast(8).uppercase()}"
            tvOrderUserRow.text = "Khách hàng: ${order.receiverName}"
            tvOrderTotalRow.text = formatter.format(order.finalTotal)

            // 2. Load ảnh sản phẩm đầu tiên của đơn hàng
            // Duyên nhớ thêm implementation 'com.github.bumptech.glide:glide:4.15.1' vào build.gradle nhé
            if (order.orderItems.isNotEmpty()) {
                val firstItemImage = order.orderItems[0].hinhAnh
                Glide.with(context)
                    .load(firstItemImage)
                    .placeholder(R.drawable.ic_launcher_background) // Thay bằng ic_book của Duyên
                    .error(R.drawable.ic_launcher_background)
                    .into(imgOrderPreview)
            }

            // 3. Xử lý trạng thái (Triple: Text - Màu chữ - Màu nền nhạt)
            val (statusText, statusColor, statusBg) = when (order.status) {
                "dang_cho" -> Triple("Chờ xác nhận", "#B45309", "#FEF3C7") // Cam đậm - Nền vàng nhạt
                "da_xac_nhan" -> Triple("Đã đóng gói", "#1D4ED8", "#DBEAFE") // Xanh dương - Nền xanh nhạt
                "dang_giao" -> Triple("Đang giao", "#6D28D9", "#EDE9FE")    // Tím - Nền tím nhạt
                "thanh_cong" -> Triple("Thành công", "#059669", "#D1FAE5")  // Xanh lá - Nền xanh nhạt
                "da_huy" -> Triple("Đã hủy", "#B91C1C", "#FEE2E2")         // Đỏ - Nền đỏ nhạt
                else -> Triple("Đang xử lý", "#4B5563", "#F3F4F6")          // Xám
            }

            tvOrderStatusRow.text = statusText
            tvOrderStatusRow.setTextColor(Color.parseColor(statusColor))

            // Đổi màu nền của nhãn trạng thái (Cần file bg_status_label.xml ở bước trước)
            tvOrderStatusRow.background.setTint(Color.parseColor(statusBg))

            // 4. Sự kiện Click xem chi tiết
            root.setOnClickListener {
                val intent = Intent(context, OrderDetailActivity::class.java)
                // Gửi nguyên object Order sang màn hình chi tiết
                intent.putExtra("ORDER_DATA", order)
                context.startActivity(intent)
            }

            // 5. Sự kiện Long Click để mở Dialog cập nhật nhanh (nếu cần)
            root.setOnLongClickListener {
                onUpdateClick(order)
                true
            }
        }
    }

    override fun getItemCount(): Int = orderList.size
}