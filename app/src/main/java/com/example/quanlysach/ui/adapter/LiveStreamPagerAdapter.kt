package com.example.quanlysach.ui.adapter

import androidx.fragment.app.FragmentActivity
import androidx.media3.common.util.UnstableApi // Đảm bảo đã import cái này
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.example.quanlysach.ui.fragment.LiveStreamFragment


// Thêm annotation này vào đầu class
@UnstableApi
class LiveStreamPagerAdapter(
    activity: FragmentActivity,
    val streamIds: List<String> // đổi private thành val
) : FragmentStateAdapter(activity) {

    override fun getItemCount() = streamIds.size


    override fun createFragment(position: Int) =
        LiveStreamFragment.Companion.newInstance(streamIds[position])
}
