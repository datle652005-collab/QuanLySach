package com.example.quanlysach.ui.bottomsheet

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import com.example.quanlysach.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class CommentActionBottomSheet(
    val onCopy: () -> Unit,
    val onDelete: () -> Unit,
    val isMyComment: Boolean
) : BottomSheetDialogFragment() {

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        return inflater.inflate(R.layout.layout_comment_actions, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState) // Thêm super call cho chuẩn vòng đời

        view.findViewById<LinearLayout>(R.id.btnCopy).setOnClickListener {
            onCopy()
            dismiss()
        }

        val btnDelete = view.findViewById<LinearLayout>(R.id.btnDelete)

        if (isMyComment) {
            btnDelete.visibility = View.VISIBLE
            btnDelete.setOnClickListener {
                onDelete()
                dismiss() // Đóng sheet ngay khi bấm xóa để tạo cảm giác mượt mà
            }
        } else {
            btnDelete.visibility = View.GONE
        }
    }
}