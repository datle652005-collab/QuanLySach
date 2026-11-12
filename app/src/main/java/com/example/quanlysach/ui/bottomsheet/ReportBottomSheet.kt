package com.example.quanlysach.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import android.content.Context
import androidx.viewpager2.widget.ViewPager2
import com.example.quanlysach.R
import com.example.quanlysach.ui.adapter.ReasonAdapter

import com.example.quanlysach.utils.MailHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class ReportBottomSheet : BottomSheetDialogFragment() {

    var onReasonSelected: ((String) -> Unit)? = null

    private val reasons = listOf(
        "Quảng cáo bất hợp pháp",
        "Nội dung nhạy cảm",
        "Trùng lặp nội dung từ nhiều tài khoản khác nhau",
        "Vi phạm quyền sở hữu",
        "Chủ nghĩa bạo lực cực đoạn",
        "Hành vi thù địch",
        "Hoạt động bất hợp pháp và hàng hóa bị kiểm soát",
        "Gian lận và lừa đảo",
        "Nội dung ghê rợn và bạo lực",
        "Hành vi nguy hiểm, tự tử và tự gây thương tích",
        "Quấy rối và bắt nạt"
    )

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_report_reasons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<ImageButton>(R.id.btnCloseReport).setOnClickListener { dismiss() }

        val rv = view.findViewById<RecyclerView>(R.id.rvReportReasons)
        rv.layoutManager = LinearLayoutManager(requireContext())
        rv.adapter = ReasonAdapter(reasons) { reason ->
            handleReport(reason) // Gọi hàm xử lý báo cáo
        }
    }

    private fun handleReport(reason: String) {
        saveNotification("Báo cáo", reason)
        // 1. Chuyển video tiếp theo
        val viewPager = requireActivity()
            .findViewById<ViewPager2>(R.id.viewPagerStreams)
        viewPager?.currentItem = (viewPager?.currentItem ?: 0) + 1

        // 2. Hiện thông báo
        Toast.makeText(requireContext(), "Cảm ơn bạn đã báo cáo", Toast.LENGTH_SHORT).show()

        // 3. Gửi email báo cáo cho Admin
        val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val username = pref.getString("USERNAME", "Khách") ?: "Khách"

        lifecycleScope.launch {
            try {
                MailHelper.sendReportToAdmin(
                    type = "Báo cáo nội dung",
                    reason = reason,
                    streamerName = arguments?.getString("streamerName") ?: "N/A",
                    streamId = arguments?.getString("streamId") ?: "N/A",
                    reporterUsername = username
                )
            } catch (_: Exception) { }
        }

        onReasonSelected?.invoke(reason)
        dismiss()
    }

    companion object { const val TAG = "ReportBottomSheet" }

    private fun saveNotification(type: String, reason: String) {
        val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val username = pref.getString("USERNAME", null) ?: return
        val productName = arguments?.getString("productName") ?: ""
        val videoId = arguments?.getString("streamId") ?: ""

        val title = when (type) {
            "Báo cáo" -> "Cảnh báo Báo cáo"
            "Chặn" -> "Cảnh báo Chặn"
            else -> "Không quan tâm"
        }
        val content = "Bạn đã $type: $reason"

        val notif = mapOf(
            "title" to title,
            "content" to content,
            "type" to type,
            "reason" to reason,
            "productName" to productName,
            "videoId" to videoId,
            "isRead" to false,
            "timestamp" to ServerValue.TIMESTAMP
        )

        FirebaseDatabase.getInstance(
            "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
        ).getReference("Notifications/$username").push().setValue(notif)
    }
}