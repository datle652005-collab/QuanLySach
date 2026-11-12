package com.example.quanlysach.data.model

// ===== MODEL: Thông tin người dùng bình luận =====
data class LiveUser(
    val userId: String,
    val username: String,
    val avatarUrl: String = ""
)

// ===== MODEL: Một bình luận trong phiên live =====
data class LiveComment(
    val commentId: String,
    val user: LiveUser,
    val content: String,
    val timestamp: Long = System.currentTimeMillis(),
    val type: CommentType = CommentType.NORMAL
)

enum class CommentType {
    NORMAL,     // Bình luận thông thường
    JOIN,       // "[Tên] đã tham gia"
    FOLLOW,     // "[Tên] đã theo dõi"
    PURCHASE    // "[Tên] đã mua [Sản phẩm]"
}

// ===== MODEL: Sản phẩm được ghim trong live =====
data class LiveProduct(
    val productId: String,
    val name: String,
    val imageUrl: String,
    val originalPrice: Double,
    val salePrice: Double,
    val soldCount: Int = 0,
    val isPinned: Boolean = false
)

// ===== MODEL: Thông tin phiên livestream =====
data class LiveStreamInfo(
    val streamId: String,
    val streamerId: String = "",
    val streamUrl: String,           // HLS / RTMP URL
    val streamerName: String,
    val streamerAvatarUrl: String,
    val title: String,
    val viewerCount: Int = 0,
    val likeCount: Long = 0L,
    val isFollowing: Boolean = false,
    val pinnedProducts: List<LiveProduct> = emptyList()
)

// ===== SEALED CLASS: Trạng thái UI =====
sealed class LiveStreamUiState {
    object Loading : LiveStreamUiState()
    data class Success(val info: LiveStreamInfo) : LiveStreamUiState()
    data class Error(val message: String) : LiveStreamUiState()
    object Ended : LiveStreamUiState()         // Stream kết thúc
    object NetworkLost : LiveStreamUiState()   // Mất kết nối
}

// ===== SEALED CLASS: Sự kiện một lần (snackbar, navigation...) =====
sealed class LiveStreamEvent {
    data class ShowToast(val message: String) : LiveStreamEvent()
    object OpenCart : LiveStreamEvent()
    data class OpenProduct(val product: LiveProduct) : LiveStreamEvent()
    object CommentSent : LiveStreamEvent()
}