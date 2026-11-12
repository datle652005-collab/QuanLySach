package com.example.quanlysach.ui.adapter

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.quanlysach.R
import com.example.quanlysach.data.model.CartItem
import com.example.quanlysach.data.model.Voucher
import java.text.DecimalFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class VoucherUserAdapter(

    private val list: List<Voucher>,

    private val checkoutItems: List<CartItem>,

    private val currentSubtotal: Double,

    private val onVoucherSelected: (Voucher) -> Unit

) : RecyclerView.Adapter<VoucherUserAdapter.UserVH>() {

    private var selectedPosition = -1

    private val dec =
        DecimalFormat("#,###")

    private val sdf =
        SimpleDateFormat(
            "dd/MM/yyyy",
            Locale("vi", "VN")
        )

    // =====================================================
    // NORMALIZE CATEGORY
    // =====================================================

    private fun normalizeCategory(
        text: String?
    ): String {

        return text
            ?.replace("Thể loại:", "")
            ?.trim()
            ?.lowercase()
            ?: ""
    }

    // =====================================================
    // FILTER VOUCHER
    // Chỉ hiện voucher phù hợp
    // =====================================================

    private val filteredList: List<Voucher>
        get() = list.filter { voucher ->

            when (voucher.applyTo.uppercase()) {

                // =========================
                // ALL
                // =========================

                "ALL" -> {

                    currentSubtotal >= voucher.minOrderValue
                }

                // =========================
                // BOOK
                // =========================

                "BOOK" -> {

                    val validItems =
                        checkoutItems.filter {

                            it.bookId.toString() ==
                                    voucher.applyTargetId.toString()
                        }

                    val total =
                        validItems.sumOf {
                            it.gia.toDouble() * it.soLuong
                        }

                    validItems.isNotEmpty() &&
                            total >= voucher.minOrderValue
                }

                // =========================
                // CATEGORY
                // =========================

                "CATEGORY" -> {

                    val voucherCategory =
                        normalizeCategory(
                            voucher.applyTargetName
                        )

                    val validItems =
                        checkoutItems.filter {

                            normalizeCategory(it.theLoai) ==
                                    voucherCategory
                        }

                    val total =
                        validItems.sumOf {
                            it.gia.toDouble() * it.soLuong
                        }

                    validItems.isNotEmpty() &&
                            total >= voucher.minOrderValue
                }

                else -> false
            }
        }

    // =====================================================
    // VIEW HOLDER
    // =====================================================

    class UserVH(v: View) :
        RecyclerView.ViewHolder(v) {

        val title: TextView =
            v.findViewById(R.id.tvVoucherTitle)

        val info: TextView =
            v.findViewById(R.id.tvVoucherInfo)

        val tvApplyTo: TextView =
            v.findViewById(R.id.tvApplyTo)

        val rbSelect: RadioButton =
            v.findViewById(R.id.rbSelectVoucher)

        val layoutLeft: LinearLayout =
            v.findViewById(R.id.layoutLeft)

        val ivLeftIcon: ImageView =
            v.findViewById(R.id.ivLeftIcon)

        val tvLeftLabel: TextView =
            v.findViewById(R.id.tvLeftLabel)

        val root: View = v
    }

    // =====================================================
    // CREATE VIEW HOLDER
    // =====================================================

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserVH {

        val view =
            LayoutInflater.from(parent.context)
                .inflate(
                    R.layout.item_voucher_user,
                    parent,
                    false
                )

        return UserVH(view)
    }

    // =====================================================
    // BIND VIEW HOLDER
    // =====================================================

    override fun onBindViewHolder(
        holder: UserVH,
        position: Int
    ) {

        val item =
            filteredList[position]

        // =====================================================
        // TITLE
        // =====================================================

        val maxText =
            if (item.maxDiscountAmount > 0) {

                " (Tối đa ${
                    dec.format(
                        item.maxDiscountAmount
                    )
                }đ)"

            } else {

                ""
            }

        if (
            item.loaiVoucher.equals(
                "FREESHIP",
                true
            )
        ) {

            holder.title.text =
                "Miễn phí vận chuyển$maxText"

        } else {

            val valueText =
                if (
                    item.type.equals(
                        "PERCENT",
                        true
                    )
                ) {

                    "${item.discountValue}%"

                } else {

                    "${dec.format(item.discountValue)}đ"
                }

            holder.title.text =
                "Giảm $valueText$maxText"
        }

        // =====================================================
        // INFO
        // =====================================================

        val targetText =
            when (item.applyTo.uppercase()) {

                "ALL" -> {

                    "Toàn bộ sản phẩm"
                }

                "BOOK" -> {

                    "Sách: ${item.applyTargetName}"
                }

                "CATEGORY" -> {

                    item.applyTargetName
                }

                else -> {

                    item.applyTargetName
                }
            }

        holder.info.text =
            "Áp dụng cho: $targetText\n" +
                    "Đơn tối thiểu ${
                        dec.format(
                            item.minOrderValue
                        )
                    }đ"

        holder.info.setTextColor(
            Color.parseColor("#00BFA5")
        )

        // =====================================================
        // HẠN SỬ DỤNG
        // =====================================================

        holder.tvApplyTo.text =
            "HSD: ${
                sdf.format(
                    Date(item.endDate)
                )
            }"

        // =====================================================
        // LEFT UI
        // =====================================================

        var leftColor = "#EE4D2D"

        if (
            item.loaiVoucher.equals(
                "FREESHIP",
                true
            )
        ) {

            leftColor = "#00BFA5"

            holder.tvLeftLabel.text =
                "FREESHIP"

            holder.ivLeftIcon.setImageResource(
                R.drawable.ic_shipping_truck
            )

        } else {

            holder.ivLeftIcon.setImageResource(
                R.drawable.ic_money
            )

            when (item.applyTo.uppercase()) {

                "BOOK" -> {

                    leftColor = "#10B981"

                    holder.tvLeftLabel.text =
                        "MÃ SÁCH"
                }

                "CATEGORY" -> {

                    leftColor = "#F97316"

                    holder.tvLeftLabel.text =
                        "THỂ LOẠI"
                }

                else -> {

                    leftColor = "#EE4D2D"

                    holder.tvLeftLabel.text =
                        "TẤT CẢ"
                }
            }
        }

        holder.layoutLeft.setBackgroundColor(
            Color.parseColor(leftColor)
        )

        // =====================================================
        // RADIO BUTTON
        // =====================================================

        holder.rbSelect.isChecked =
            position == selectedPosition

        holder.root.setOnClickListener {

            val oldPosition =
                selectedPosition

            selectedPosition =
                holder.adapterPosition

            if (oldPosition >= 0) {

                notifyItemChanged(oldPosition)
            }

            notifyItemChanged(selectedPosition)

            onVoucherSelected(item)
        }
    }

    // =====================================================
    // ITEM COUNT
    // =====================================================

    override fun getItemCount(): Int {

        return filteredList.size
    }
}