package com.example.quanlysach.ui.fragment

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.RelativeLayout
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import com.bumptech.glide.Glide
import com.example.quanlysach.ui.bottomsheet.CommentBottomSheet
import com.example.quanlysach.ui.bottomsheet.MoreOptionsBottomSheet
import com.example.quanlysach.R
import com.example.quanlysach.data.model.LiveStreamUiState
import com.example.quanlysach.ui.activity.BookDetailActivity
import com.example.quanlysach.ui.activity.CartActivity
import com.example.quanlysach.ui.activity.StreamerProfileActivity
import com.example.quanlysach.viewmodel.LiveStreamViewModel
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.AndroidEntryPoint
import de.hdodenhof.circleimageview.CircleImageView
import kotlinx.coroutines.launch

@AndroidEntryPoint
@UnstableApi
class LiveStreamFragment : Fragment() {

    companion object {
        fun newInstance(streamId: String) = LiveStreamFragment().apply {
            arguments = Bundle().apply { putString("streamId", streamId) }
        }
    }

    private val viewModel: LiveStreamViewModel by viewModels()
    private var exoPlayer: ExoPlayer? = null
    private lateinit var layoutEffectContainer: RelativeLayout
    private var lastTapTime: Long = 0

    private var currentBookName: String = ""

    // Views cũ
    private lateinit var tvStreamerName: TextView
    private lateinit var tvStreamerNameBottom: TextView
    private lateinit var tvProductCount: TextView
    private lateinit var tvDescription: TextView
    private lateinit var tvSeeMore: TextView
    private lateinit var tvLikeCount: TextView
    private lateinit var tvCommentCount: TextView
    private lateinit var imgStreamerAvatar: CircleImageView
    private lateinit var btnLike: ImageButton
    private lateinit var btnComment: ImageButton
    private lateinit var btnShare: ImageButton
    private lateinit var btnMore: ImageButton
    private lateinit var btnCart: ImageButton
    private lateinit var imgProduct: ImageView
    private lateinit var tvProductName: TextView
    private lateinit var tvProductPrice: TextView
    private lateinit var btnBuyProduct: Button
    private lateinit var tvShareCount: TextView
    private lateinit var playerView: PlayerView
    private lateinit var icPauseOverlay: ImageView
    private lateinit var layoutProduct: FrameLayout
    private lateinit var btnCloseProduct: ImageButton
    private lateinit var btnBack: ImageButton

    // 🌟 THÀNH PHẦN THÊM MỚI: Thanh tua video kiểu TikTok
    private lateinit var videoSeekBar: SeekBar
    private lateinit var layoutSeekTimeOverlay: View
    private lateinit var tvCurrentSeekTime: TextView
    private lateinit var tvTotalSeekTime: TextView
    private var isUserTrackingSeekBar = false

    private var isDescriptionExpanded = false
    private var lastSharedStreamId = ""
    private val handler = Handler(Looper.getMainLooper())
    private var isLongPressing = false

    // 🌟 Vòng lặp chạy ngầm cập nhật tiến trình video lên SeekBar mỗi giây
    private val updateProgressRunnable = object : Runnable {
        override fun run() {
            exoPlayer?.let { player ->
                if (player.isPlaying && !isUserTrackingSeekBar) {
                    val currentPos = player.currentPosition
                    val duration = player.duration
                    if (duration > 0) {
                        videoSeekBar.max = duration.toInt()
                        videoSeekBar.progress = currentPos.toInt()
                    }
                }
            }
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_live_stream, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
        val username = pref.getString("USERNAME", "guest") ?: "guest"
        viewModel.initUserLike(username)

        super.onViewCreated(view, savedInstanceState)
        bindViews(view)
        setupClickListeners()
        observeViewModel()
    }

    private fun bindViews(view: View) {
        layoutEffectContainer = view.findViewById(R.id.layoutEffectContainer)
        tvStreamerName        = view.findViewById(R.id.tvStreamerName)
        tvStreamerNameBottom  = view.findViewById(R.id.tvStreamerNameBottom)
        tvProductCount        = view.findViewById(R.id.tvProductCount)
        tvDescription         = view.findViewById(R.id.tvDescription)
        tvSeeMore             = view.findViewById(R.id.tvSeeMore)
        tvLikeCount           = view.findViewById(R.id.tvLikeCount)
        tvCommentCount        = view.findViewById(R.id.tvCommentCount)
        imgStreamerAvatar     = view.findViewById(R.id.imgStreamerAvatar)
        btnLike               = view.findViewById(R.id.btnLike)
        btnComment            = view.findViewById(R.id.btnComment)
        btnShare              = view.findViewById(R.id.btnShare)
        btnMore               = view.findViewById(R.id.btnMore)
        btnCart               = view.findViewById(R.id.btnCart)
        imgProduct            = view.findViewById(R.id.imgProduct)
        tvProductName         = view.findViewById(R.id.tvProductName)
        tvProductPrice        = view.findViewById(R.id.tvProductPrice)
        tvShareCount          = view.findViewById(R.id.tvShareCount)
        btnBuyProduct         = view.findViewById(R.id.btnBuyProduct)
        playerView            = view.findViewById(R.id.playerView)
        icPauseOverlay        = view.findViewById(R.id.icPauseOverlay)
        layoutProduct         = view.findViewById(R.id.layoutProduct)
        btnCloseProduct       = view.findViewById(R.id.btnCloseProduct)
        btnBack               = view.findViewById(R.id.btnBack)

        // 🌟 Ánh xạ các View phục vụ tính năng tua video
        videoSeekBar          = view.findViewById(R.id.videoSeekBar)
        layoutSeekTimeOverlay = view.findViewById(R.id.layoutSeekTimeOverlay)
        tvCurrentSeekTime     = view.findViewById(R.id.tvCurrentSeekTime)
        tvTotalSeekTime       = view.findViewById(R.id.tvTotalSeekTime)

        // Mặc định giấu cục tròn kéo đi, tạo thành 1 đường line mảnh tinh tế dưới đáy
        videoSeekBar.thumb.mutate().alpha = 0
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun setupClickListeners() {
        // 1. Avatar → trang profile
        imgStreamerAvatar.setOnClickListener {
            val state = viewModel.uiState.value
            val name = (state as? LiveStreamUiState.Success)?.info?.streamerName ?: "BookShop"
            startActivity(Intent(requireContext(), StreamerProfileActivity::class.java).apply {
                putExtra("STREAMER_NAME", name)
            })
        }

        // 2. Nút Like đơn lẻ
        btnLike.setOnClickListener {
            val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
            val username = pref.getString("USERNAME", "guest") ?: "guest"
            viewModel.toggleLike(username)
        }

        // 3. Double tap màn hình để Like + Hiệu ứng tim bay / Long press x2 / Single tap pause
        playerView.setOnTouchListener { v, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> {
                    isLongPressing = false
                    val currTime = System.currentTimeMillis()

                    if (currTime - lastTapTime < 300) {
                        // --- DOUBLE TAP (LIKE) ---
                        handler.removeCallbacksAndMessages(null)
                        showHeartEffect(event.x, event.y)

                        val pref = requireContext().getSharedPreferences("AppBanSachPrefs", Context.MODE_PRIVATE)
                        val username = pref.getString("USERNAME", "guest") ?: "guest"
                        viewModel.toggleLike(username)

                        lastTapTime = 0
                    } else {
                        // --- CHUẨN BỊ CHO SINGLE TAP HOẶC LONG PRESS ---
                        lastTapTime = currTime

                        handler.postDelayed({
                            isLongPressing = true
                            exoPlayer?.setPlaybackSpeed(2.0f)
                            Toast.makeText(context, "Đang tua x2", Toast.LENGTH_SHORT).show()
                        }, 500)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    val pressDuration = System.currentTimeMillis() - lastTapTime
                    handler.removeCallbacksAndMessages(null)

                    if (isLongPressing) {
                        // --- KẾT THÚC LONG PRESS (TRẢ TỐC ĐỘ) ---
                        exoPlayer?.setPlaybackSpeed(1.0f)
                        isLongPressing = false
                    } else if (pressDuration < 500 && lastTapTime != 0L) {
                        // --- XỬ LÝ SINGLE TAP (PLAY/PAUSE) ---
                        handler.postDelayed({
                            exoPlayer?.let {
                                if (it.isPlaying) {
                                    it.pause()
                                    icPauseOverlay.visibility = View.VISIBLE
                                } else {
                                    it.play()
                                    icPauseOverlay.visibility = View.GONE
                                }
                            }
                        }, 200)
                    }
                }
            }
            v.performClick()
            true
        }

        // 4. Giỏ hàng
        btnCart.setOnClickListener {
            startActivity(Intent(requireContext(), CartActivity::class.java))
        }

        // 5. Comment
        btnComment.setOnClickListener {
            val id = (viewModel.uiState.value as? LiveStreamUiState.Success)?.info?.streamId ?: "video_test"
            CommentBottomSheet.Companion.newInstance(id).show(parentFragmentManager, "CommentSheet")
        }

        // 6. Chia sẻ
        btnShare.setOnClickListener {
            val state = viewModel.uiState.value as? LiveStreamUiState.Success
            val streamId = state?.info?.streamId ?: "video_test"
            val shareText = "Xem video sách hay tại đây! mybookreview://video/$streamId"

            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_TEXT, shareText)
            }
            startActivityForResult(Intent.createChooser(intent, "Chia sẻ"), 1001)
            lastSharedStreamId = streamId
        }

        // 7. Tùy chọn khác (More)
        btnMore.setOnClickListener {
            val state = viewModel.uiState.value as? LiveStreamUiState.Success
            val name = state?.info?.streamerName ?: "N/A"
            val id = state?.info?.streamId ?: "N/A"

            MoreOptionsBottomSheet.Companion.newInstance(name, id, currentBookName, currentBookName, id)
                .show(parentFragmentManager, MoreOptionsBottomSheet.Companion.TAG)
        }

        // 8. Mở rộng mô tả video
        tvSeeMore.setOnClickListener {
            isDescriptionExpanded = !isDescriptionExpanded
            tvDescription.maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else 2
            tvSeeMore.text = if (isDescriptionExpanded) "thu gọn" else "thêm"
        }

        // 9. Đóng tab sản phẩm liên quan
        btnCloseProduct.setOnClickListener {
            layoutProduct.visibility = View.GONE
        }

        // 10. Thoát màn hình video
        btnBack.setOnClickListener {
            requireActivity().finish()
        }

        // 🌟 BỘ SỰ KIỆN LẮNG NGHE VUỐT KÉO THANH TUA VIDEO KIỂU TIKTOK
        videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isUserTrackingSeekBar = true
                layoutSeekTimeOverlay.visibility = View.VISIBLE
                videoSeekBar.thumb.mutate().alpha = 255

                exoPlayer?.let {
                    tvTotalSeekTime.text = stringForTime(it.duration)
                }
            }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    tvCurrentSeekTime.text = stringForTime(progress.toLong())
                }
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                exoPlayer?.let { player ->
                    player.seekTo(videoSeekBar.progress.toLong())
                }

                // 🔥 Đã đưa dòng này lên trên, ra khỏi block delay để mở khóa chạy tiếp ngay lập tức
                isUserTrackingSeekBar = false

                handler.postDelayed({
                    layoutSeekTimeOverlay.visibility = View.GONE
                    videoSeekBar.thumb.mutate().alpha = 0
                }, 500)
            }
        })
    }

    private fun observeViewModel() {
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {

                // Lắng nghe UI State tải video dữ liệu lên
                launch {
                    viewModel.uiState.collect { state ->
                        if (state is LiveStreamUiState.Success) {
                            tvStreamerName.text = state.info.streamerName
                            tvStreamerNameBottom.text = state.info.streamerName

                            val desc = state.info.title
                            if (desc.isNotEmpty()) {
                                tvDescription.text = desc
                                tvSeeMore.visibility = View.VISIBLE
                            } else {
                                tvSeeMore.visibility = View.GONE
                                tvDescription.visibility = View.GONE
                            }

                            playStream(state.info.streamUrl)

                            // Đếm số lượng comment realtime
                            FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("video_comments/${state.info.streamId}")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        tvCommentCount.text = snapshot.childrenCount.toString()
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    }
                }

                // Lắng nghe số lượng Like
                launch {
                    viewModel.likeCount.collect { count ->
                        tvLikeCount.text = formatCount(count)
                    }
                }

                // Đổi màu trái tim đỏ/trắng
                launch {
                    viewModel.isUserLiked.collect { liked ->
                        btnLike.setColorFilter(if (liked) Color.RED else Color.WHITE)
                    }
                }

                // Lắng nghe đếm lượt chia sẻ realtime
                launch {
                    viewModel.uiState.collect { state ->
                        if (state is LiveStreamUiState.Success) {
                            FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                                .getReference("Videos/${state.info.streamId}/shareCount")
                                .addValueEventListener(object : ValueEventListener {
                                    override fun onDataChange(snapshot: DataSnapshot) {
                                        tvShareCount.text = (snapshot.getValue(Long::class.java) ?: 0).toString()
                                    }
                                    override fun onCancelled(error: DatabaseError) {}
                                })
                        }
                    }
                }

                // Lắng nghe Sách liên quan đi kèm video
                launch {
                    viewModel.linkedBook.collect { book ->
                        FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                            .getReference("Books")
                            .addListenerForSingleValueEvent(object : ValueEventListener {
                                override fun onDataChange(snapshot: DataSnapshot) {
                                    tvProductCount.text = "${snapshot.childrenCount} Sản phẩm"
                                }
                                override fun onCancelled(error: DatabaseError) {}
                            })

                        if (book != null) {
                            currentBookName = book.tenSach
                            layoutProduct.visibility = View.VISIBLE
                            tvProductName.text = book.tenSach
                            tvProductPrice.text = String.format("%,dđ", book.gia)
                            Glide.with(requireContext()).load(book.hinhAnh).centerCrop()
                                .placeholder(R.drawable.ic_book_placeholder).into(imgProduct)

                            val openDetail = View.OnClickListener {
                                startActivity(Intent(requireContext(), BookDetailActivity::class.java).apply {
                                    putExtra("BOOK_ID", book.id)
                                })
                            }
                            layoutProduct.setOnClickListener(openDetail)
                            btnBuyProduct.setOnClickListener(openDetail)
                        } else {
                            layoutProduct.visibility = View.GONE
                        }
                    }
                }
            }
        }
    }

    private fun formatCount(count: Long): String {
        return if (count >= 1000) String.format("%.1fK", count / 1000.0) else count.toString()
    }

    private fun playStream(url: String) {
        if (exoPlayer == null) initExoPlayer()
        exoPlayer?.let { player ->
            try {
                player.setMediaItem(MediaItem.fromUri(url))
                player.prepare()
                player.playWhenReady = true
            } catch (e: Exception) {
                Toast.makeText(requireContext(), "Không thể tải video", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun initExoPlayer() {
        exoPlayer = ExoPlayer.Builder(requireContext()).build().apply {
            playerView.player = this
            repeatMode = ExoPlayer.REPEAT_MODE_ONE

            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    if (playbackState == ExoPlayer.STATE_READY) {
                        videoSeekBar.max = duration.toInt()
                    }
                }

                override fun onPositionDiscontinuity(
                    oldPosition: Player.PositionInfo,
                    newPosition: Player.PositionInfo,
                    reason: Int
                ) {
                    if (reason == Player.DISCONTINUITY_REASON_AUTO_TRANSITION) {
                        videoSeekBar.progress = 0
                    }
                }
            })
        }
    }

    // 🌟 Định dạng Milliseconds thành chuỗi MM:SS hiển thị thời gian tua
    private fun stringForTime(timeMs: Long): String {
        val totalSeconds = (timeMs + 500) / 1000
        val seconds = totalSeconds % 60
        val minutes = (totalSeconds / 60) % 60
        return String.format("%02d:%02d", minutes, seconds)
    }

    override fun onResume() {
        super.onResume()
        exoPlayer?.play()
        handler.post(updateProgressRunnable) // 🌟 Chạy tiến trình đồng bộ SeekBar thời gian thực
    }

    override fun onPause() {
        super.onPause()
        exoPlayer?.pause()
        handler.removeCallbacks(updateProgressRunnable) // 🌟 Dừng tiến trình chạy ngầm khi thoát màn hình để nhẹ máy
    }

    override fun onDestroyView() {
        super.onDestroyView()
        handler.removeCallbacksAndMessages(null)
        exoPlayer?.release()
        exoPlayer = null
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == 1001) {
            FirebaseDatabase.getInstance("https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/")
                .getReference("Videos/$lastSharedStreamId/shareCount")
                .runTransaction(object : Transaction.Handler {
                    override fun doTransaction(data: MutableData): Transaction.Result {
                        data.value = (data.getValue(Long::class.java) ?: 0L) + 1
                        return Transaction.success(data)
                    }
                    override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
                })
        }
    }

    private fun showHeartEffect(x: Float, y: Float) {
        val heart = ImageView(requireContext()).apply {
            setImageResource(R.drawable.ic_heart_filled)
            setColorFilter(Color.RED)
            val size = (60 * resources.displayMetrics.density).toInt()
            layoutParams = RelativeLayout.LayoutParams(size, size).apply {
                leftMargin = (x - size / 2).toInt()
                topMargin = (y - size / 2).toInt()
            }
        }

        layoutEffectContainer.addView(heart)

        val animY = ObjectAnimator.ofFloat(heart, "translationY", 0f, -400f)
        val animAlpha = ObjectAnimator.ofFloat(heart, "alpha", 1f, 0f)
        val scaleX = ObjectAnimator.ofFloat(heart, "scaleX", 0.5f, 1.5f)
        val scaleY = ObjectAnimator.ofFloat(heart, "scaleY", 0.5f, 1.5f)

        AnimatorSet().apply {
            playTogether(animY, animAlpha, scaleX, scaleY)
            duration = 800
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    layoutEffectContainer.removeView(heart)
                }
            })
            start()
        }
    }
}