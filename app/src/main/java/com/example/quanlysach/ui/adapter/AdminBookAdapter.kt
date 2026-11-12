package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.google.android.material.button.MaterialButton
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import java.text.NumberFormat
import java.util.Locale

class AdminBookAdapter(
    private val bookList: MutableList<com.example.quanlysach.data.model.Book>,
    private val onEdit: (com.example.quanlysach.data.model.Book) -> Unit,
    private val onDelete: (com.example.quanlysach.data.model.Book) -> Unit,
    private val onVideo: (com.example.quanlysach.data.model.Book) -> Unit,
    private val onReview: (com.example.quanlysach.data.model.Book) -> Unit
) : RecyclerView.Adapter<AdminBookAdapter.BookViewHolder>() {

    class BookViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val img: ImageView = view.findViewById(R.id.imgBook)
        val ten: TextView = view.findViewById(R.id.tvTenSach)
        val tacGia: TextView = view.findViewById(R.id.tvTacGia)
        val gia: TextView = view.findViewById(R.id.tvGia)
        val soLuong: TextView = view.findViewById(R.id.tvSoLuong)
        val theLoai: TextView = view.findViewById(R.id.tvTheLoai)
        val moTa: TextView = view.findViewById(R.id.tvDesc)

        val btnEdit: MaterialButton = view.findViewById(R.id.btnEdit)
        val btnDelete: MaterialButton = view.findViewById(R.id.btnDelete)
        val btnVideo: MaterialButton = view.findViewById(R.id.btnVideo)
        val btnReview: MaterialButton = view.findViewById(R.id.btnReview)

        // Badge hiển thị số lượng đánh giá mới
        val tvBadge: TextView = view.findViewById(R.id.tvReviewCountBadge)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): BookViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_admin_book, parent, false)
        return BookViewHolder(view)
    }

    override fun getItemCount(): Int = bookList.size

    override fun onBindViewHolder(holder: BookViewHolder, position: Int) {
        val book = bookList[position]

        holder.ten.text = book.tenSach
        holder.tacGia.text = book.tacGia

        val formatter = NumberFormat.getCurrencyInstance(Locale("vi", "VN"))
        holder.gia.text = formatter.format(book.gia)

        holder.soLuong.text = "Kho: ${book.soLuong}"
        holder.theLoai.text = book.theLoai
        holder.moTa.text = if (book.moTa.isEmpty()) "Không có mô tả" else book.moTa

        Glide.with(holder.itemView.context)
            .load(book.hinhAnh)
            .centerCrop()
            .placeholder(R.drawable.ic_book_placeholder)
            .into(holder.img)

        // 🌟 LẮNG NGHE SỐ LƯỢNG ĐÁNH GIÁ MỚI TỪ FIREBASE
        val dbRef = FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
            .getReference("BookReviewsCount").child(book.id)

        dbRef.addValueEventListener(object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val count = snapshot.getValue(Int::class.java) ?: 0
                if (count > 0) {
                    holder.tvBadge.visibility = View.VISIBLE
                    holder.tvBadge.text = if (count > 99) "99+" else count.toString()
                } else {
                    holder.tvBadge.visibility = View.GONE
                }
            }
            override fun onCancelled(error: DatabaseError) {}
        })

        // Xử lý sự kiện bấm nút
        holder.btnEdit.setOnClickListener { onEdit(book) }
        holder.btnDelete.setOnClickListener { onDelete(book) }
        holder.btnVideo.setOnClickListener { onVideo(book) }

        holder.btnReview.setOnClickListener {
            // Khi Admin bấm xem đánh giá, reset số thông báo về 0
            dbRef.setValue(0)
            onReview(book)
        }
    }

    fun updateList(newList: List<com.example.quanlysach.data.model.Book>) {
        bookList.clear()
        bookList.addAll(newList)
        notifyDataSetChanged()
    }
}