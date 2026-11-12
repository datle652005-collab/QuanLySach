package com.example.quanlysach.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import android.content.Context
import androidx.viewpager2.widget.ViewPager2
import com.example.quanlysach.R
import com.example.quanlysach.utils.MailHelper
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ServerValue

class NotInterestedBottomSheet : BottomSheetDialogFragment() {

    var onSend: ((String) -> Unit)? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View =
        inflater.inflate(R.layout.fragment_not_interested_reasons, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val btnClose = view.findViewById<ImageButton>(R.id.btnCloseNotInterested)
        val radioGroup = view.findViewById<RadioGroup>(R.id.rgNotInterestedReasons)
        val btnSend = view.findViewById<Button>(R.id.btnSendNotInterested)
        btnSend.visibility = View.GONE // Ẩn nút gửi

        val radioMap = mapOf(
            R.id.rbNotCreator to "Tôi không quan tâm sản phẩm này",
            R.id.rbNotProduct to "Thông tin sai lệch",
            R.id.rbTooManySuggestions to "Hành vi lừa đảo và nội dung rác",
            R.id.rbSuggestedTwice to "Sản phẩm nhái và quyền sở hữu trí tuệ",
            R.id.rbBadQuality to "Nội dung gây sốc và phản cảm",
            R.id.rbOther to "Khác"
        )

        // Cho phép click cả row để chọn radio → gửi luôn
        radioMap.keys.forEach { id ->
            view.findViewById<RadioButton>(id).setOnClickListener {
                val reason = radioMap[id] ?: "Khác"
                saveNotification("Không quan tâm", reason)

                // 1. Chuyển video tiếp theo
                val viewPager = requireActivity()
                    .findViewById<ViewPager2>(R.id.viewPagerStreams)
                viewPager?.currentItem = (viewPager?.currentItem ?: 0) + 1

                // 2. Hiện toast
                Toast.makeText(requireContext(), "Đã bỏ qua video này", Toast.LENGTH_SHORT).show()

                // 3. Gửi email
                val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
                val username = pref.getString("USERNAME", "Khách") ?: "Khách"
                lifecycleScope.launch {
                    try {
                        MailHelper.sendReportToAdmin(
                            type = "Không quan tâm",
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

        btnClose.setOnClickListener { dismiss() }

        radioGroup.setOnCheckedChangeListener { _, checkedId ->
            val selected = checkedId != -1
            btnSend.isEnabled = selected
            btnSend.backgroundTintList = requireContext().getColorStateList(
                if (selected) R.color.shopee_orange else R.color.grey_300
            )
        }

        btnSend.setOnClickListener {
            val reason = radioMap[radioGroup.checkedRadioButtonId] ?: "Khác"
            onSend?.invoke(reason)
            dismiss()
        }
    }

    companion object { const val TAG = "NotInterestedBottomSheet" }

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