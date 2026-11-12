package com.example.quanlysach.ui.activity

import android.graphics.Color
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysach.data.model.Order

import com.example.quanlysach.databinding.ActivityUserOrderDetailBinding
import com.example.quanlysach.ui.adapter.OrderItemAdapter

import com.google.firebase.database.*
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class UserOrderDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityUserOrderDetailBinding
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityUserOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val orderId = intent.getStringExtra("ORDER_ID") ?: ""

        binding.btnBackUserDetail.setOnClickListener { finish() }

        if (orderId.isNotEmpty()) {
            fetchOrderById(orderId)
        } else {
            Toast.makeText(this, "Mã đơn hàng không hợp lệ", Toast.LENGTH_SHORT).show()
        }
    }

    // FIX LỖI: Trỏ thẳng vào Orders/mã_đơn theo cấu trúc phẳng mới
    private fun fetchOrderById(targetId: String) {
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("Orders").child(targetId)

        ref.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val order = snapshot.getValue(Order::class.java)
                if (order != null) {
                    updateUI(order)
                } else {
                    // Nếu không tìm thấy bằng child(id), quét toàn bộ phòng hờ
                    searchFlatList(targetId)
                }
            }
            override fun onCancelled(error: DatabaseError) {
                Toast.makeText(this@UserOrderDetailActivity, "Lỗi tải dữ liệu!", Toast.LENGTH_SHORT).show()
            }
        })
    }

    private fun searchFlatList(targetId: String) {
        FirebaseDatabase.getInstance(dbUrl).getReference("Orders")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    for (ds in snapshot.children) {
                        val o = ds.getValue(Order::class.java)
                        if (o?.orderId == targetId) {
                            updateUI(o)
                            return
                        }
                    }
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }

    private fun updateUI(order: Order) {
        // Sử dụng định dạng tiền tệ chuẩn VN
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val sdf = SimpleDateFormat("dd-MM-yyyy HH:mm", Locale.getDefault())

        binding.apply {
            // 1. Cập nhật Header trạng thái
            val config = when (order.status) {
                "thanh_cong" -> StatusTheme("#26AA99", "Giao hàng thành công", "Cảm ơn bạn đã mua sắm!")
                "da_huy" -> StatusTheme("#EE4D2D", "Đơn hàng đã hủy", "Tiếc quá, đơn hàng đã bị hủy")
                "dang_giao" -> StatusTheme("#26AA99", "Đang giao hàng", "Đơn hàng đang trên đường tới bạn")
                "da_xac_nhan" -> StatusTheme("#FF9800", "Đang đóng gói", "Người bán đang chuẩn bị hàng")
                else -> StatusTheme("#FF9800", "Đang chờ xác nhận", "Vui lòng đợi người bán xác nhận")
            }

            layoutStatusHeader.setBackgroundColor(Color.parseColor(config.color))
            tvStatusMain.text = config.title
            tvStatusSub.text = config.subTitle

            // 2. Thông tin người nhận
            tvUserNamePhone.text = "${order.receiverName} | ${order.phone}"
            tvUserAddress.text = order.addressDetail

            // 3. Danh sách sản phẩm
            rvUserItems.layoutManager = LinearLayoutManager(this@UserOrderDetailActivity)
            rvUserItems.adapter = OrderItemAdapter(order.orderItems)

            // 4. FIX LỖI HIỂN THỊ TIỀN: Đảm bảo lấy đúng trường dữ liệu
            tvUserTotalProduct.text = formatter.format(order.totalProductPrice ?: 0)
            tvUserShippingFee.text = formatter.format(order.shippingFee ?: 0)
            tvUserDiscount.text = "-${formatter.format(order.discountAmount ?: 0)}"
            tvUserFinalPrice.text = formatter.format(order.finalTotal ?: 0)

            // 5. Thông tin đơn hàng
            tvUserOrderCode.text = "Mã đơn hàng: ${order.orderId.uppercase()}"
            tvUserCreateTime.text = "Thời gian đặt: ${sdf.format(Date(order.createdAt))}"
            tvUserPaymentMethod.text = "Thanh toán: ${order.paymentMethod}"
        }
    }

    data class StatusTheme(val color: String, val title: String, val subTitle: String)
}