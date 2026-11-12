package com.example.quanlysach.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.viewpager2.widget.ViewPager2
import com.example.quanlysach.R
import com.example.quanlysach.ui.activity.LiveStreamActivity
import com.example.quanlysach.utils.MailHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class BlockBottomSheet : BottomSheetDialogFragment() {

    var onConfirm: ((String) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_block_options, container, false)

    @OptIn(UnstableApi::class)
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseBlock)
        val rowCreator = view.findViewById<RelativeLayout>(R.id.rowBlockCreator)
        val rowStream = view.findViewById<RelativeLayout>(R.id.rowBlockStream)
        val rbCreator = view.findViewById<RadioButton>(R.id.rbBlockCreator)
        val rbStream = view.findViewById<RadioButton>(R.id.rbBlockStream)
        val btnConfirm = view.findViewById<Button>(R.id.btnConfirmBlock)

        rbCreator.isClickable = false
        rbStream.isClickable = false

        btnClose.setOnClickListener { dismiss() }

        fun updateConfirmButton() {
            val selected = rbCreator.isChecked || rbStream.isChecked
            btnConfirm.isEnabled = selected
            btnConfirm.backgroundTintList = requireContext().getColorStateList(
                if (selected) R.color.shopee_orange else R.color.grey_300
            )
        }

        // Cho phép bỏ chọn khi ấn lại
        var lastSelectedId: Int = -1

        rowCreator.setOnClickListener {
            if (lastSelectedId == rbCreator.id) {
                rbCreator.isChecked = false
                lastSelectedId = -1
            } else {
                rbCreator.isChecked = true
                rbStream.isChecked = false
                lastSelectedId = rbCreator.id
            }
            updateConfirmButton()
        }

        rowStream.setOnClickListener {
            if (lastSelectedId == rbStream.id) {
                rbStream.isChecked = false
                lastSelectedId = -1
            } else {
                rbStream.isChecked = true
                rbCreator.isChecked = false
                lastSelectedId = rbStream.id
            }
            updateConfirmButton()
        }

        btnConfirm.setOnClickListener {
            val reason = if (rbCreator.isChecked) "Chặn Nhà sáng tạo này" else "Chặn sản phẩm này"
            saveNotification("Chặn", reason)

            // 1. Chuyển video tiếp
            val viewPager = requireActivity()
                .findViewById<ViewPager2>(R.id.viewPagerStreams)
            (requireActivity() as? LiveStreamActivity)?.removeCurrentStream()

            // 2. Hiện toast
            Toast.makeText(requireContext(), "Đã chặn: $reason", Toast.LENGTH_SHORT).show()

            // 3. Gửi email
            val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
            val username = pref.getString("USERNAME", "Khách") ?: "Khách"
            lifecycleScope.launch {
                try {
                    MailHelper.sendReportToAdmin(
                        type = "Chặn",
                        reason = reason,
                        streamerName = arguments?.getString("streamerName") ?: "N/A",
                        streamId = arguments?.getString("streamId") ?: "N/A",
                        reporterUsername = username
                    )
                } catch (e: Exception) { }
            }

            dismiss()
        }
    }

    companion object { const val TAG = "BlockBottomSheet" }

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