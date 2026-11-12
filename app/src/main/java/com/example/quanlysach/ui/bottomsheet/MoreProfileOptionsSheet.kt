package com.example.quanlysach.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.launch
import android.content.Context
import com.example.quanlysach.R
import com.example.quanlysach.utils.MailHelper

class MoreProfileOptionsSheet : BottomSheetDialogFragment() {

    // Sửa lỗi nặc danh nullability bằng cách chỉ định rõ kiểu trả về là View?
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.layout_more_options_sheet, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        view.findViewById<LinearLayout>(R.id.btnBlockUser).setOnClickListener {
            dismiss()
            val sheet = BlockBottomSheet().apply {
                onConfirm = { reason ->
                    // 1. Hiện toast
                    Toast.makeText(requireContext(), "Đã chặn: $reason", Toast.LENGTH_SHORT).show()

                    // 2. Gửi email
                    val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
                    val username = pref.getString("USERNAME", "Khách") ?: "Khách"
                    lifecycleScope.launch {
                        try {
                            MailHelper.sendReportToAdmin(
                                type = "Chặn",
                                reason = reason,
                                streamerName = "N/A",
                                streamId = "N/A",
                                reporterUsername = username
                            )
                        } catch (e: Exception) { }
                    }

                    // 3. Đóng trang profile
                    requireActivity().finish()
                }
            }
            sheet.show(parentFragmentManager, BlockBottomSheet.TAG)
        }

        view.findViewById<TextView>(R.id.btnCancel).setOnClickListener {
            dismiss()
        }
    }
}