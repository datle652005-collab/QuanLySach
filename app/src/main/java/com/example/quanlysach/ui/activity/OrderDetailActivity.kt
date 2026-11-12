package com.example.quanlysach.ui.activity

import android.content.Intent
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.pdf.PdfDocument
import android.os.Bundle
import android.os.CancellationSignal
import android.os.ParcelFileDescriptor
import android.print.*
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.quanlysach.data.model.ConsultMessage
import com.example.quanlysach.data.model.Order
import com.example.quanlysach.databinding.ActivityOrderDetailBinding
import com.example.quanlysach.ui.adapter.OrderItemAdapter

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.io.FileOutputStream
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.*

class OrderDetailActivity : AppCompatActivity() {
    private lateinit var binding: ActivityOrderDetailBinding
    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val ADMIN_ID = "admin"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityOrderDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Toolbar setup
        binding.btnBackDetail.setOnClickListener {
            finish()
        }

        val order = intent.getSerializableExtra("ORDER_DATA") as? Order
        order?.let {
            displayOrderData(it)
            setupActionButtons(it)
        }
    }

    private fun displayOrderData(order: Order) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

        binding.apply {
            tvOrderIdDetail.text = "#${order.orderId.takeLast(8).uppercase()}"
            tvOrderTime.text = "Ngày đặt: ${sdf.format(Date(order.createdAt))}"
            tvDetailName.text = order.receiverName
            tvDetailPhone.text = "SĐT: ${order.phone}"
            tvDetailAddress.text = order.addressDetail

            tvSubtotal.text = formatter.format(order.totalProductPrice)
            tvShipFee.text = formatter.format(order.shippingFee)
            tvDiscount.text = "-${formatter.format(order.discountAmount)}"
            tvFinalTotalDetail.text = formatter.format(order.finalTotal)

            // 🌟 CẬP NHẬT: Hiển thị phương thức thanh toán rõ ràng, trực quan cho Admin dễ nhìn
            if (order.paymentMethod == "MOMO") {
                tvPaymentMethodDetail.text = "Thanh toán: Chuyển khoản MoMo"
                tvPaymentMethodDetail.setTextColor(Color.parseColor("#D81B60")) // Đổi sang màu hồng MoMo nổi bật
            } else {
                tvPaymentMethodDetail.text = "Thanh toán: Tiền mặt khi nhận hàng (COD)"
                tvPaymentMethodDetail.setTextColor(Color.parseColor("#1E293B"))
            }

            updateStatusUI(order.status)

            rvOrderItems.layoutManager = LinearLayoutManager(this@OrderDetailActivity)
            rvOrderItems.adapter = OrderItemAdapter(order.orderItems)
        }
    }

    private fun setupActionButtons(order: Order) {
        // 1. Nút Chat Tư Vấn
        binding.btnChatCustomer.setOnClickListener {
            val intent = Intent(this, ConsultChatActivity::class.java)
            intent.putExtra("RECEIVER_USERNAME", order.username)
            intent.putExtra("ORDER_ID", order.orderId)
            startActivity(intent)
        }

        // 2. Nút In Vận Đơn
        binding.btnPrintShipping.setOnClickListener {
            printOrder(order)
        }

        // 3. Nút Cập Nhật Trạng Thái
        binding.btnUpdateStatusDetail.setOnClickListener {
            showUpdateStatusDialog(order)
        }
    }

    private fun showUpdateStatusDialog(order: Order) {
        // Đơn MoMo khi hoàn tất thanh toán từ giỏ hàng sẽ có trạng thái "da_thanh_toan"
        // Admin chỉ việc bấm xác nhận đóng gói và đi đơn
        val names = arrayOf("Xác nhận & Đóng gói", "Giao cho Shipper", "Thành công", "Hủy đơn")
        val keys = arrayOf("da_xac_nhan", "dang_giao", "thanh_cong", "da_huy")

        AlertDialog.Builder(this)
            .setTitle("Thay đổi trạng thái đơn")
            .setItems(names) { _, which ->
                updateFirebaseStatus(order.orderId, keys[which], names[which])
            }.show()
    }

    private fun updateFirebaseStatus(
        orderId: String,
        newStatus: String,
        statusDisplayName: String
    ) {

        val dbRef = FirebaseDatabase.getInstance(dbUrl)
            .getReference("Orders")

        dbRef.addListenerForSingleValueEvent(object : ValueEventListener {

            override fun onDataChange(snapshot: DataSnapshot) {

                val currentOrder =
                    intent.getSerializableExtra("ORDER_DATA") as? Order ?: return

                var found = false

                for (node in snapshot.children) {

                    // 🔥 Nếu đã tìm thấy rồi thì dừng hẳn
                    if (found) break

                    // CASE 1: Orders/orderId
                    if (node.key == orderId) {

                        val dbUsername =
                            node.child("username")
                                .getValue(String::class.java) ?: ""

                        currentOrder.username = dbUsername

                        found = true

                        performUpdate(
                            node.ref,
                            currentOrder,
                            newStatus,
                            statusDisplayName
                        )

                        break
                    }

                    // CASE 2: Orders/username/orderId
                    for (sub in node.children) {

                        if (sub.key == orderId) {

                            currentOrder.username = node.key ?: ""

                            found = true

                            performUpdate(
                                sub.ref,
                                currentOrder,
                                newStatus,
                                statusDisplayName
                            )

                            break
                        }
                    }
                }
            }

            override fun onCancelled(error: DatabaseError) {}
        })
    }

    private fun performUpdate(ref: DatabaseReference, order: Order, status: String, name: String) {
        ref.child("status").setValue(status).addOnSuccessListener {
            order.status = status
            updateStatusUI(status)

            val cleanOrderId = order.orderId.takeLast(8).uppercase()
            val autoMsg = when (status) {
                "da_xac_nhan" -> "🤖 Hệ thống: Đơn hàng #$cleanOrderId đã được XÁC NHẬN và đang đóng gói!"
                "dang_giao" -> "🤖 Hệ thống: Đơn hàng #$cleanOrderId đã được GIAO CHO SHIPPER và đang trên đường tới bạn!"
                "thanh_cong" -> "🤖 Hệ thống: Đơn hàng #$cleanOrderId của bạn đã được giao THÀNH CÔNG! Cảm ơn bạn đã mua sách tại cửa hàng. ❤️"
                "da_huy" -> "🤖 Hệ thống: Đơn hàng #$cleanOrderId đã bị HỦY trên hệ thống."
                else -> "🤖 Hệ thống: Đơn hàng #$cleanOrderId đang được xử lý."
            }

            sendAutoConsultMessage(order, autoMsg)
            sendNotificationToCustomer(order, status)

            Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show()
        }
    }

    private fun updateStatusUI(status: String) {
        val (text, color) = when (status) {
            "dang_cho" -> "CHỜ XÁC NHẬN" to "#64748B"
            "da_thanh_toan" -> "ĐÃ THANH TOÁN (MOMO)" to "#D81B60" // 🌟 THÊM: Hiện nhãn đã nhận tiền MoMo
            "da_xac_nhan" -> "ĐÃ XÁC NHẬN" to "#3B82F6"
            "dang_giao" -> "ĐANG GIAO" to "#8B5CF6"
            "thanh_cong" -> "HOÀN THÀNH" to "#10B981"
            "da_huy" -> "ĐÃ HỦY" to "#EF4444"
            else -> "ĐANG XỬ LÝ" to "#64748B"
        }
        binding.tvStatusHeader.text = text
        binding.tvStatusHeader.setTextColor(Color.parseColor(color))
    }

    private fun printOrder(order: Order) {
        val printManager = getSystemService(PRINT_SERVICE) as PrintManager
        printManager.print("VanDon_${order.orderId}", object : PrintDocumentAdapter() {
            override fun onLayout(old: PrintAttributes?, newAttrs: PrintAttributes, signal: CancellationSignal?, cb: LayoutResultCallback, extras: Bundle?) {
                cb.onLayoutFinished(PrintDocumentInfo.Builder("vandon.pdf").setPageCount(1).build(), true)
            }

            override fun onWrite(pages: Array<out PageRange>?, dest: ParcelFileDescriptor?, signal: CancellationSignal?, cb: WriteResultCallback?) {
                val pdf = PdfDocument()
                val page = pdf.startPage(PdfDocument.PageInfo.Builder(595, 842, 1).create())
                val canvas = page.canvas
                val p = Paint()
                val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))

                p.style = Paint.Style.STROKE
                p.strokeWidth = 2f
                canvas.drawRect(RectF(50f, 50f, 545f, 380f), p)

                p.style = Paint.Style.FILL
                p.isFakeBoldText = true
                p.textSize = 22f
                canvas.drawText("PHIẾU GIAO HÀNG", 190f, 90f, p)

                p.strokeWidth = 1f
                canvas.drawLine(50f, 110f, 545f, 110f, p)

                p.isFakeBoldText = false
                p.textSize = 14f
                var yPos = 150f
                canvas.drawText("Người nhận: ${order.receiverName}", 70f, yPos, p); yPos += 30f
                canvas.drawText("Số điện thoại: ${order.phone}", 70f, yPos, p); yPos += 30f

                val address = if (order.addressDetail.length > 55) order.addressDetail.substring(0, 55) + "..." else order.addressDetail
                canvas.drawText("Địa chỉ: $address", 70f, yPos, p); yPos += 40f

                canvas.drawLine(70f, yPos, 525f, yPos, p); yPos += 40f

                p.isFakeBoldText = true
                p.textSize = 16f

                // 🌟 QUAN TRỌNG: Sửa logic tính tiền thu hộ khi in vận đơn giao hàng
                if (order.paymentMethod == "MOMO") {
                    canvas.drawText("HÌNH THỨC: ĐÃ THANH TOÁN (MOMO)", 70f, yPos, p); yPos += 25f
                    p.textSize = 16f
                    canvas.drawText("TỔNG TIỀN THU HỘ (COD):", 70f, yPos, p)
                    p.textSize = 24f
                    p.color = Color.parseColor("#10B981") // Đã thanh toán chuyển sang màu xanh lá
                    canvas.drawText("0đ", 300f, yPos, p)
                } else {
                    canvas.drawText("HÌNH THỨC: THANH TOÁN COD", 70f, yPos, p); yPos += 25f
                    p.textSize = 16f
                    canvas.drawText("TỔNG TIỀN THU HỘ (COD):", 70f, yPos, p)
                    p.textSize = 24f
                    p.color = Color.RED
                    canvas.drawText(formatter.format(order.finalTotal), 300f, yPos, p)
                }

                p.color = Color.BLACK
                p.textSize = 10f
                p.isFakeBoldText = false
                canvas.drawText("Mã đơn: ${order.orderId}", 70f, 360f, p)
                canvas.drawText("Ngày in: ${SimpleDateFormat("dd/MM/yyyy").format(Date())}", 400f, 360f, p)

                pdf.finishPage(page)
                try {
                    pdf.writeTo(FileOutputStream(dest?.fileDescriptor))
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                pdf.close()
                cb?.onWriteFinished(arrayOf(PageRange.ALL_PAGES))
            }
        }, null)
    }

    private fun sendAutoConsultMessage(order: Order, msg: String) {
        val roomId = "admin_${order.username}_SUPPORT"
        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("Consultations").child(roomId)
        val msgId = ref.push().key ?: ""

        val m = ConsultMessage(
            messageId = msgId,
            senderId = ADMIN_ID,
            receiverId = order.username,
            message = msg,
            timestamp = System.currentTimeMillis(),
            read = false,
            type = "text"
        )
        ref.child(msgId).setValue(m)
    }

    private fun sendNotificationToCustomer(order: Order, status: String) {
        if (order.username.isEmpty()) return

        val ref = FirebaseDatabase.getInstance(dbUrl).getReference("Notifications").child(order.username)
        val notiId = ref.push().key ?: return

        val cleanOrderId = order.orderId.takeLast(8).uppercase()

        val title: String
        val content: String
        val productImage = if (order.orderItems.isNotEmpty()) order.orderItems[0].hinhAnh else ""

        when (status) {
            "da_xac_nhan" -> {
                title = "📦 Đơn hàng #$cleanOrderId đã được xác nhận!"
                content = "Cửa hàng đang tiến hành đóng gói sản phẩm để chuẩn bị giao cho đơn vị vận chuyển."
            }
            "dang_giao" -> {
                title = "🚚 Đơn hàng #$cleanOrderId đang được giao!"
                content = "Đơn hàng đã được bàn giao cho Shipper và đang trên đường đến với bạn. Hãy chú ý điện thoại nhé!"
            }
            "thanh_cong" -> {
                title = "🎉 Đơn hàng #$cleanOrderId đã giao thành công!"
                content = "Giao hàng hoàn tất! Đơn hàng gồm ${order.orderItems.size} sản phẩm đã đến tay bạn. Đừng quên mở app đánh giá 5 sao cho shop nhé! 🥰"
            }
            "da_huy" -> {
                title = "❌ Đơn hàng #$cleanOrderId đã bị hủy!"
                content = "Đơn hàng của bạn đã bị hủy trên hệ thống. Nếu có thắc mắc, vui lòng liên hệ admin hỗ trợ."
            }
            else -> {
                title = "🔔 Cập nhật đơn hàng #$cleanOrderId"
                content = "Trạng thái đơn hàng của bạn đã được thay đổi trên hệ thống."
            }
        }

        val notificationMap = hashMapOf(
            "id" to notiId,
            "title" to title,
            "content" to content,
            "body" to content,
            "orderId" to order.orderId,
            "timestamp" to System.currentTimeMillis(),
            "type" to "order",
            "image" to productImage,
            "isRead" to false,
            "isProcessed" to false
        )

        ref.child(notiId).setValue(notificationMap)
    }
}