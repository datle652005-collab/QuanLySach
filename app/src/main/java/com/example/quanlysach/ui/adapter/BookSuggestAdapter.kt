package com.example.quanlysach.ui.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Glide
import com.example.quanlysach.R

class BookSuggestAdapter(
    context: Context,
    private val allBooks: List<com.example.quanlysach.data.model.Book>
) : ArrayAdapter<com.example.quanlysach.data.model.Book>(context, 0, allBooks) {

    private var displayList: List<com.example.quanlysach.data.model.Book> = allBooks

    override fun getCount(): Int = displayList.size

    override fun getItem(position: Int): com.example.quanlysach.data.model.Book? {
        return if (position in displayList.indices) displayList[position] else null
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_suggest_book, parent, false)

        val book = getItem(position)
        val img = view.findViewById<ImageView>(R.id.imgBook)
        val name = view.findViewById<TextView>(R.id.tvName)
        val cate = view.findViewById<TextView>(R.id.tvCate)

        book?.let {
            name.text = it.tenSach
            cate.text = it.theLoai

            Glide.with(context)
                .load(it.hinhAnh) // Khớp với model: hinhAnh
                .placeholder(android.R.drawable.ic_menu_report_image)
                .error(android.R.drawable.ic_menu_report_image)
                .into(img)
        }
        return view
    }

    override fun getFilter(): Filter {
        return object : Filter() {
            override fun performFiltering(constraint: CharSequence?): FilterResults {
                val results = FilterResults()
                val query = constraint?.toString()?.lowercase()?.trim() ?: ""

                val filtered = if (query.isEmpty()) {
                    allBooks
                } else {
                    allBooks.filter { it.tenSach.lowercase().contains(query) }
                }

                results.values = filtered
                results.count = filtered.size
                return results
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                val values = results?.values
                if (values is List<*>) {
                    // Fix lỗi "Cannot infer type for type parameter 'R'"
                    displayList = values.filterIsInstance<com.example.quanlysach.data.model.Book>()
                }
                notifyDataSetChanged()
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return (resultValue as? com.example.quanlysach.data.model.Book)?.tenSach ?: ""
            }
        }
    }
}