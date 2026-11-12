package com.example.quanlysach.ui.adapter

import android.content.Context
import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.*
import com.example.quanlysach.data.model.Order
import com.example.quanlysach.databinding.ItemUserOrderBinding
import com.example.quanlysach.ui.activity.CheckoutActivity
import com.example.quanlysach.ui.activity.ConsultChatActivity
import com.example.quanlysach.ui.activity.DanhGiaSanPhamActivity
import com.example.quanlysach.ui.activity.UserOrderDetailActivity

import com.google.firebase.database.FirebaseDatabase
import java.text.NumberFormat
import java.util.*
import kotlin.jvm.java

class UserOrderAdapter(
    private var list: List<Order>,
    private val onAction: (Order, String) -> Unit
) : RecyclerView.Adapter<UserOrderAdapter.UserOrderVH>() {

    inner class UserOrderVH(val binding: ItemUserOrderBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): UserOrderVH {
        val b = ItemUserOrderBinding.inflate(LayoutInflater.from(parent.context), parent, false)
        return UserOrderVH(b)
    }

    override fun onBindViewHolder(holder: UserOrderVH, position: Int) {
        val order = list[position]
        val context = holder.itemView.context
        val fmt = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

        with(holder.binding) {
            // --- BƯỚC QUAN TRỌNG: RESET TRẠNG THÁI NÚT ---
            btnAction.isEnabled = true
            btnAction.alpha = 1.0f
            btnAction.visibility = View.VISIBLE
            // --------------------------------------------

            layoutItemOrder.setOnClickListener {
                val intent = Intent(context, UserOrderDetailActivity::class.java)
                intent.putExtra("ORDER_ID", order.orderId)
                context.startActivity(intent)
            }

            if (order.orderItems.isNotEmpty()) {
                val item = order.orderItems[0]
                tvBookName.text = item.tenSach
                tvQuantity.text = "Số lượng: ${item.soLuong}"
                tvOrderId.text = "Mã đơn: #${order.orderId.uppercase()}"
                Glide.with(context).load(item.hinhAnh).into(imgUserOrder)
                tvUserOrderPrice.text = fmt.format(order.finalTotal)
            }

            btnChat.visibility = View.VISIBLE
            btnAction.visibility = View.VISIBLE
            btnChat.setOnClickListener {
                context.startActivity(Intent(context, ConsultChatActivity::class.java))
            }

            when (order.status) {
                "dang_cho" -> {
                    tvUserStatus.text = "Chờ xác nhận"
                    btnAction.text = "Hủy đơn"
                    // Đảm bảo nút có thể bấm được
                    btnAction.setOnClickListener { showCancelDialog(context, order) }
                }
                "da_xac_nhan", "dang_giao" -> {
                    tvUserStatus.text = if (order.status == "da_xac_nhan") "Đóng gói" else "Đang giao"
                    btnAction.visibility = View.GONE
                }
                "thanh_cong" -> {
                    tvUserStatus.text = "Thành công"
                    if (order.reviewed == true) {
                        btnAction.text = "Đã đánh giá"
                        btnAction.isEnabled = false // Chỉ khóa ở ĐÂY
                        btnAction.alpha = 0.5f
                    } else {
                        btnAction.text = "Đánh giá"
                        btnAction.setOnClickListener {
                            val intent = Intent(context, DanhGiaSanPhamActivity::class.java)
                            intent.putExtra("LIST_ITEMS", ArrayList(order.orderItems))
                            intent.putExtra("ORDER_ID", order.orderId)
                            context.startActivity(intent)
                        }
                    }
                }
                "da_huy" -> {
                    tvUserStatus.text = "Đã hủy"
                    btnAction.text = "Mua lại"
                    // Đảm bảo nút mua lại bấm được
                    btnAction.setOnClickListener {
                        val intent = Intent(context, CheckoutActivity::class.java)
                        intent.putExtra("USERNAME", order.username)
                        intent.putExtra("LIST_CHECKOUT", ArrayList(order.orderItems))
                        context.startActivity(intent)
                    }
                }
            }
        }
    }

    private fun showCancelDialog(context: Context, order: Order) {
        val reasons = arrayOf(
            "Muốn thay đổi địa chỉ nhận hàng",
            "Muốn thay đổi sản phẩm trong đơn",
            "Tìm thấy giá rẻ hơn ở nơi khác",
            "Đổi ý, không muốn mua nữa",
            "Khác..."
        )

        var selectedReason = reasons[0] // Mặc định chọn lý do đầu tiên

        AlertDialog.Builder(context)
            .setTitle("Chọn lý do hủy đơn")
            .setSingleChoiceItems(reasons, 0) { _, which ->
                selectedReason = reasons[which]
            }
            .setPositiveButton("Xác nhận hủy") { _, _ ->
                performCancel(context, order, selectedReason)
            }
            .setNegativeButton("Đóng", null)
            .show()
    }

    private fun performCancel(context: Context, order: Order, reason: String) {
        val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("Orders")
            .child(order.orderId)

        val updates = mapOf(
            "status" to "da_huy",
            "cancelReason" to reason, // Lưu lý do vào đây
            "completedAt" to System.currentTimeMillis()
        )

        ref.updateChildren(updates).addOnSuccessListener {
            Toast.makeText(context, "Đã hủy đơn: $reason", Toast.LENGTH_SHORT).show()
        }.addOnFailureListener {
            Toast.makeText(context, "Lỗi khi hủy đơn", Toast.LENGTH_SHORT).show()
        }
    }

    override fun getItemCount() = list.size
}