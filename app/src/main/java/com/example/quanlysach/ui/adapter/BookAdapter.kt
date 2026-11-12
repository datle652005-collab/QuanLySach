package com.example.quanlysach.ui.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.Book
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class BookAdapter(
    private val itemClick: (Book) -> Unit
) : RecyclerView.Adapter<BookAdapter.BookViewHolder>() {

    private val displayList = mutableListOf<Book>()
    private val originalList = mutableListOf<Book>()

    // Firebase URL
    private val dbUrl =
        "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {

        val imgBook: ImageView = view.findViewById(R.id.imgBook)

        val tvTen: TextView = view.findViewById(R.id.tvTenSach)

        val tvGia: TextView = view.findViewById(R.id.tvGiaSach)

        val tvOldPrice: TextView = view.findViewById(R.id.tvOldPrice)

        val tvSaleBadge: TextView = view.findViewById(R.id.tvSaleBadge)

        val tvRatingBook: TextView = view.findViewById(R.id.tvRatingBook)

        val tvDaBanBook: TextView = view.findViewById(R.id.tvDaBanBook)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): BookViewHolder {

        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_book, parent, false)

        return BookViewHolder(view)
    }

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {

        val book = displayList[position]

        val context = holder.itemView.context

        holder.tvTen.text = book.tenSach

        // Reset dữ liệu tránh lỗi RecyclerView recycle
        holder.tvRatingBook.text = "..."
        holder.tvDaBanBook.text = "Đã bán 0"

        val formatter = NumberFormat.getInstance(Locale("vi", "VN"))

        // =====================================================
        // 1. XỬ LÝ GIÁ & GIẢM GIÁ
        // =====================================================

        if (book.giaGiam > 0 && book.giaGiam < book.gia) {

            holder.tvGia.text =
                "${formatter.format(book.giaGiam)}đ"

            holder.tvOldPrice.visibility = View.VISIBLE

            holder.tvOldPrice.text =
                "${formatter.format(book.gia)}đ"

            holder.tvOldPrice.paintFlags =
                holder.tvOldPrice.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG

            val percent =
                100 - (book.giaGiam * 100 / book.gia)

            holder.tvSaleBadge.visibility = View.VISIBLE
            holder.tvSaleBadge.text = "-$percent%"

        } else {

            holder.tvGia.text =
                "${formatter.format(book.gia)}đ"

            holder.tvOldPrice.visibility = View.GONE
            holder.tvSaleBadge.visibility = View.GONE
        }

        // =====================================================
        // 2. LOAD ĐÁNH GIÁ REALTIME
        // =====================================================

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Reviews")
            .child(book.id)
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    if (snapshot.exists()) {

                        var totalStars = 0f
                        var count = 0

                        for (ds in snapshot.children) {

                            val ratingValue =
                                ds.child("rating")
                                    .getValue(Float::class.java) ?: 0f

                            totalStars += ratingValue
                            count++
                        }

                        if (count > 0) {

                            val average =
                                totalStars / count

                            holder.tvRatingBook.text =
                                String.format(
                                    Locale.US,
                                    "%.1f",
                                    average
                                )

                        } else {

                            holder.tvRatingBook.text = "0"
                        }

                    } else {

                        holder.tvRatingBook.text = "0"
                    }
                }

                override fun onCancelled(error: DatabaseError) {

                    holder.tvRatingBook.text = "0"
                }
            })

        // =====================================================
        // 3. LOAD SỐ LƯỢNG ĐÃ BÁN REALTIME
        // =====================================================

        FirebaseDatabase.getInstance(dbUrl)
            .getReference("Orders")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {

                    var totalSold = 0

                    if (snapshot.exists()) {

                        for (orderSnapshot in snapshot.children) {

                            val status =
                                orderSnapshot.child("status")
                                    .getValue(String::class.java) ?: ""

                            // Chỉ tính đơn hoàn thành
                            if (status == "thanh_cong") {

                                val orderItemsSnapshot =
                                    orderSnapshot.child("orderItems")

                                for (itemSnapshot in orderItemsSnapshot.children) {

                                    val itemId =
                                        itemSnapshot.child("bookId")
                                            .getValue(String::class.java) ?: ""

                                    if (itemId == book.id) {

                                        val qty =
                                            itemSnapshot.child("soLuong")
                                                .getValue(Int::class.java) ?: 0

                                        totalSold += qty
                                    }
                                }
                            }
                        }
                    }

                    holder.tvDaBanBook.text =
                        "Đã bán $totalSold"
                }

                override fun onCancelled(error: DatabaseError) {

                    holder.tvDaBanBook.text =
                        "Đã bán 0"
                }
            })

        // =====================================================
        // 4. LOAD ẢNH
        // =====================================================

        Glide.with(context)
            .load(book.hinhAnh)
            .centerCrop()
            .placeholder(R.drawable.ic_book_placeholder)
            .error(R.drawable.ic_book_placeholder)
            .into(holder.imgBook)

        // =====================================================
        // 5. CLICK ITEM
        // =====================================================

        holder.itemView.setOnClickListener {

            itemClick(book)
        }
    }

    override fun getItemCount(): Int {

        return displayList.size
    }

    // =====================================================
    // SET DATA
    // =====================================================

    fun setData(newList: List<Book>) {

        originalList.clear()
        originalList.addAll(newList)

        displayList.clear()
        displayList.addAll(newList)

        notifyDataSetChanged()
    }

    // =====================================================
    // SEARCH FILTER
    // =====================================================

    fun filter(query: String) {

        val result = if (query.isEmpty()) {

            originalList

        } else {

            originalList.filter { book ->

                book.tenSach.contains(query, true)
                        || book.theLoai.contains(query, true)
                        || book.tags.any {
                    it.contains(query, true)
                }
            }
        }

        displayList.clear()
        displayList.addAll(result)

        notifyDataSetChanged()
    }

    // =====================================================
    // RESET LIST
    // =====================================================

    fun reset() {

        displayList.clear()
        displayList.addAll(originalList)

        notifyDataSetChanged()
    }
}