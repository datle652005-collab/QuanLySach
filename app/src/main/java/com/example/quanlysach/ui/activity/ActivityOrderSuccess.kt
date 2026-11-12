package com.example.quanlysach.ui.activity

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.example.quanlysach.R
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.*

class ActivityOrderSuccess : AppCompatActivity() { // 🔥 Đã đổi tên Class trùng với tên file để sửa lỗi Manifest

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_order_success)

        // 1. Nhận dữ liệu từ CheckoutActivity gửi sang (Dùng chuỗi thuần như ông giáo chọn)
        val orderId = intent.getStringExtra("ORDER_ID") ?: ""
        val total = intent.getIntExtra("TOTAL", 0)
        val address = intent.getStringExtra("ADDRESS") ?: "Chưa có địa chỉ nhận hàng"
        val payment = intent.getStringExtra("PAYMENT_METHOD") ?: "Tiền mặt"

        // 2. Ánh xạ các View
        val tvOrderMessage = findViewById<TextView>(R.id.tvOrderMessage)
        val tvTotalSuccess = findViewById<TextView>(R.id.tvTotalSuccess)
        val tvDeliveryDate = findViewById<TextView>(R.id.tvDeliveryDate)
        val tvPaymentInfo = findViewById<TextView>(R.id.tvPaymentInfo)
        val tvAddressInfo = findViewById<TextView>(R.id.tvAddressInfo)

        // 3. Hiển thị thông tin
        tvOrderMessage.text = "Cảm ơn bạn đã mua sắm.\nĐơn hàng #${orderId.uppercase()} của bạn đang được xử lý."

        val formatter = DecimalFormat("#,### đ")
        tvTotalSuccess.text = formatter.format(total)

        tvPaymentInfo.text = payment
        tvAddressInfo.text = address

        // 4. Ngày giao hàng dự kiến
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, 3)
        val sdf = SimpleDateFormat("EEEE, dd 'Tháng' MM", Locale("vi", "VN"))
        val dateString = sdf.format(calendar.time)
        tvDeliveryDate.text = dateString.replaceFirstChar { it.uppercase() }

        // 5. Cài đặt sự kiện nút bấm

        // Nút Tiếp tục mua sắm
        findViewById<Button>(R.id.btnContinueShopping).setOnClickListener {
            val intentMain = Intent(this, MainActivity::class.java)
            intentMain.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK
            startActivity(intentMain)
            finish()
        }

        // Nút Xem đơn hàng
        findViewById<Button>(R.id.btnViewOrder).setOnClickListener {
            if (orderId.isNotEmpty()) {
                val intentDetail = Intent(this, UserOrderDetailActivity::class.java)
                intentDetail.putExtra("ORDER_ID", orderId)
                startActivity(intentDetail)
                finish()
            } else {
                startActivity(Intent(this, MyOrdersActivity::class.java))
                finish()
            }
        }
    }
}