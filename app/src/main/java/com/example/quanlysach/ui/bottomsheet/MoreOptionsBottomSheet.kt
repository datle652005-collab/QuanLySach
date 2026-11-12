package com.example.quanlysach.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.quanlysach.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class MoreOptionsBottomSheet : BottomSheetDialogFragment() {

    // Biến lưu trữ dữ liệu nhận được
    private var streamerName: String = ""
    private var streamId: String = ""
    private var videoTitle: String = ""
    private var productName: String = ""
    private var videoId: String = ""

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_more_options, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Lấy dữ liệu từ "túi đồ" arguments
        streamerName = arguments?.getString("streamerName") ?: "N/A"
        streamId = arguments?.getString("streamId") ?: "N/A"
        videoTitle = arguments?.getString("videoTitle") ?: ""
        productName = arguments?.getString("productName") ?: ""
        videoId = arguments?.getString("videoId") ?: ""

        // 1. Xử lý Báo cáo
        view.findViewById<LinearLayout>(R.id.optionReport).setOnClickListener {
            dismiss()
            val reportSheet = ReportBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("streamerName", streamerName)
                    putString("streamId", streamId)
                    putString("videoTitle", videoTitle)
                    putString("productName", productName)
                    putString("videoId", videoId)
                }
            }
            reportSheet.show(parentFragmentManager, ReportBottomSheet.TAG)
        }

        // 2. Xử lý Chặn
        view.findViewById<LinearLayout>(R.id.optionBlock).setOnClickListener {
            dismiss()
            val sheet = BlockBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("streamerName", streamerName)
                    putString("streamId", streamId)
                    putString("videoTitle", videoTitle)
                    putString("productName", productName)
                    putString("videoId", videoId)
                }
            }
            sheet.show(parentFragmentManager, BlockBottomSheet.TAG)
        }

        // 3. Xử lý Không quan tâm
        view.findViewById<LinearLayout>(R.id.optionNotInterested).setOnClickListener {
            dismiss()
            val sheet = NotInterestedBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("streamerName", streamerName)
                    putString("streamId", streamId)
                    putString("videoTitle", videoTitle)
                    putString("productName", productName)
                    putString("videoId", videoId)
                }
            }
            sheet.show(parentFragmentManager, NotInterestedBottomSheet.TAG)
        }
    }


    companion object {
        const val TAG = "MoreOptionsBottomSheet"

        // Hàm khởi tạo chuẩn để truyền dữ liệu
        fun newInstance(streamerName: String, streamId: String, videoTitle: String, productName: String, videoId: String) =
            MoreOptionsBottomSheet().apply {
                arguments = Bundle().apply {
                    putString("streamerName", streamerName)
                    putString("streamId", streamId)
                    putString("videoTitle", videoTitle)
                    putString("productName", productName)
                    putString("videoId", videoId) // thêm
                }
            }
    }
}