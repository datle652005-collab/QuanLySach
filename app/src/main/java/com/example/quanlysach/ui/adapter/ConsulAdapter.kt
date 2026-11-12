package com.example.quanlysach.ui.adapter

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.TextView // 🌟 ĐÃ THÊM: Sửa lỗi Unresolved reference 'TextView' và 'visibility'/'text'
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.quanlysach.R
import com.example.quanlysach.data.model.ConsultMessage

class ConsultAdapter(
    private val context: Context,
    private val messageList: List<ConsultMessage>
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val TYPE_SENT = 1
    private val TYPE_RECEIVED = 2

    override fun getItemViewType(position: Int): Int {
        val message = messageList[position]
        val pref = context.getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val myName = pref.getString("USERNAME", "") ?: ""
        val myRole = pref.getString("VAI_TRO", "khach_hang")!!

        val currentSender = if (myRole == "admin") "admin" else myName
        return if (message.senderId == currentSender) TYPE_SENT else TYPE_RECEIVED
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return if (viewType == TYPE_SENT) {
            val view = LayoutInflater.from(context).inflate(R.layout.item_chat_sent, parent, false)
            SentViewHolder(view)
        } else {
            val view = LayoutInflater.from(context).inflate(R.layout.item_chat_received, parent, false)
            ReceivedViewHolder(view)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val message = messageList[position]
        val isImage = message.type == "image"
        val urlList = if (isImage) message.message.split(",") else emptyList()

        if (holder is SentViewHolder) {
            if (isImage) {
                holder.tvSentMessage.visibility = View.GONE
                holder.rvSentImagesGrid.visibility = View.VISIBLE

                val spanCount = when {
                    urlList.size == 1 -> 1
                    urlList.size == 2 -> 2
                    else -> 3
                }

                holder.rvSentImagesGrid.layoutManager = GridLayoutManager(context, spanCount)
                holder.rvSentImagesGrid.adapter = ChatImageGridAdapter(context, urlList) { clickedPosition ->
                    showFullImageSwipeDialog(urlList, clickedPosition)
                }
            } else {
                holder.tvSentMessage.visibility = View.VISIBLE
                holder.rvSentImagesGrid.visibility = View.GONE
                holder.tvSentMessage.text = message.message
            }
        } else if (holder is ReceivedViewHolder) {
            if (isImage) {
                holder.tvReceivedMessage.visibility = View.GONE
                holder.rvReceivedImagesGrid.visibility = View.VISIBLE

                val spanCount = when {
                    urlList.size == 1 -> 1
                    urlList.size == 2 -> 2
                    else -> 3
                }

                holder.rvReceivedImagesGrid.layoutManager = GridLayoutManager(context, spanCount)
                holder.rvReceivedImagesGrid.adapter = ChatImageGridAdapter(context, urlList) { clickedPosition ->
                    showFullImageSwipeDialog(urlList, clickedPosition)
                }
            } else {
                holder.tvReceivedMessage.visibility = View.VISIBLE
                holder.rvReceivedImagesGrid.visibility = View.GONE
                holder.tvReceivedMessage.text = message.message
            }
        }
    }

    private fun showFullImageSwipeDialog(imageUrls: List<String>, startPosition: Int) {
        val dialog = Dialog(context, android.R.style.Theme_Black_NoTitleBar_Fullscreen)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)

        val viewPager = ViewPager2(context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(android.graphics.Color.BLACK)
        }

        val pagerAdapter = ImagePagerAdapter(context, imageUrls) {
            dialog.dismiss()
        }

        viewPager.adapter = pagerAdapter
        viewPager.setCurrentItem(startPosition, false)

        dialog.setContentView(viewPager)
        dialog.show()
    }

    override fun getItemCount(): Int = messageList.size

    // 🌟 ĐÃ SỬA: Ép kiểu <TextView> và <RecyclerView> rõ ràng để hết lỗi 'Cannot infer type'
    class SentViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvSentMessage: TextView = itemView.findViewById<TextView>(R.id.tvSentMessage)
        val rvSentImagesGrid: RecyclerView = itemView.findViewById<RecyclerView>(R.id.rvChatImagesGrid)
    }

    class ReceivedViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val tvReceivedMessage: TextView = itemView.findViewById<TextView>(R.id.tvReceivedMessage)
        val rvReceivedImagesGrid: RecyclerView = itemView.findViewById<RecyclerView>(R.id.rvChatImagesGrid)
    }
}