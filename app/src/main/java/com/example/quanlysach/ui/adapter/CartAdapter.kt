package com.example.quanlysach.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.quanlysach.R
import com.example.quanlysach.data.model.CartItem
import com.google.firebase.database.FirebaseDatabase
import java.util.Locale

class CartAdapter(
    private var list: MutableList<CartItem>,
    private val username: String,
    private val onAmountChanged: () -> Unit
) : RecyclerView.Adapter<CartAdapter.CartViewHolder>() {

    private val dbUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"

    // Ref tới giỏ hàng
    private val dbRef = FirebaseDatabase.getInstance(dbUrl).getReference("Carts").child(username)

    class CartViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        val cbSelect: CheckBox = v.findViewById(R.id.cbSelect)
        val img: ImageView = v.findViewById(R.id.imgCartBook)
        val name: TextView = v.findViewById(R.id.tvCartName)
        val price: TextView = v.findViewById(R.id.tvCartPrice)
        val qty: TextView = v.findViewById(R.id.tvQuantity)
        val btnPlus: ImageView = v.findViewById(R.id.btnPlus)
        val btnMinus: ImageView = v.findViewById(R.id.btnMinus)
        val btnDelete: ImageView = v.findViewById(R.id.btnDelete)
        val layoutRoot: View = v
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): CartViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_cart, parent, false)
        return CartViewHolder(view)
    }

    override fun onBindViewHolder(holder: CartViewHolder, position: Int) {
        val item = list[position]

        holder.name.text = item.tenSach
        holder.price.text = String.format(Locale("vi", "VN"), "%,dđ", item.gia)
        holder.qty.text = item.soLuong.toString()

        Glide.with(holder.itemView.context)
            .load(item.hinhAnh)
            .placeholder(R.drawable.banner1)
            .into(holder.img)

        holder.cbSelect.setOnCheckedChangeListener(null)
        holder.cbSelect.isChecked = item.isSelected

        holder.cbSelect.setOnCheckedChangeListener { _, isChecked ->
            item.isSelected = isChecked
            onAmountChanged()
        }

        holder.layoutRoot.setOnClickListener {
            holder.cbSelect.isChecked = !holder.cbSelect.isChecked
        }

        // ➕ TĂNG SỐ LƯỢNG (🔥 Đã fix: Trả lại logic gọi Real-time kiểm tra kho trực tiếp từ bảng Books)
        holder.btnPlus.setOnClickListener {
            val bookRef = FirebaseDatabase.getInstance(dbUrl).getReference("Books").child(item.bookId)

            bookRef.child("soLuong").get().addOnSuccessListener { snapshot ->
                val stock = snapshot.getValue(Int::class.java) ?: 0

                if (item.soLuong < stock) {
                    dbRef.child(item.bookId).child("soLuong").setValue(item.soLuong + 1)
                } else {
                    Toast.makeText(holder.itemView.context, "Kho chỉ còn $stock sản phẩm!", Toast.LENGTH_SHORT).show()
                }
            }.addOnFailureListener {
                Toast.makeText(holder.itemView.context, "Lỗi kiểm tra kho", Toast.LENGTH_SHORT).show()
            }
        }

        // ➖ GIẢM SỐ LƯỢNG (Tối thiểu là 1)
        holder.btnMinus.setOnClickListener {
            if (item.soLuong > 1) {
                dbRef.child(item.bookId).child("soLuong").setValue(item.soLuong - 1)
            }
        }

        // ❌ XOÁ SẢN PHẨM
        holder.btnDelete.setOnClickListener {
            dbRef.child(item.bookId).removeValue().addOnSuccessListener {
                Toast.makeText(holder.itemView.context, "Đã xoá khỏi giỏ hàng", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun getItemCount(): Int = list.size

    fun updateData(newList: MutableList<CartItem>) {
        newList.forEach { newItem ->
            val oldItem = this.list.find { it.bookId == newItem.bookId }
            if (oldItem != null) {
                newItem.isSelected = oldItem.isSelected
            }
        }
        this.list = newList
        notifyDataSetChanged()
    }
}