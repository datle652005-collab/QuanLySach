package com.example.quanlysach.viewmodel

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.quanlysach.data.model.Book
import com.example.quanlysach.data.model.LiveProduct
import com.example.quanlysach.data.model.LiveStreamEvent
import com.example.quanlysach.data.model.LiveStreamInfo
import com.example.quanlysach.data.model.LiveStreamUiState
import com.example.quanlysach.data.repository.LiveStreamRepository
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import com.google.firebase.database.ValueEventListener
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class LiveStreamViewModel @Inject constructor(
    private val repository: LiveStreamRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val streamId: String = savedStateHandle["streamId"] ?: ""

    // UI State
    private val _uiState = MutableStateFlow<LiveStreamUiState>(LiveStreamUiState.Loading)
    val uiState: StateFlow<LiveStreamUiState> = _uiState.asStateFlow()

    // Like count
    private val _likeCount = MutableStateFlow(0L)
    val likeCount: StateFlow<Long> = _likeCount.asStateFlow()

    // Trạng thái user hiện tại đã like chưa
    private val _isUserLiked = MutableStateFlow(false)
    val isUserLiked: StateFlow<Boolean> = _isUserLiked.asStateFlow()

    // Sách liên quan đến video
    private val _linkedBook = MutableStateFlow<Book?>(null)
    val linkedBook: StateFlow<Book?> = _linkedBook.asStateFlow()

    // Events
    private val _events = Channel<LiveStreamEvent>(Channel.Factory.BUFFERED)
    val events: Flow<LiveStreamEvent> = _events.receiveAsFlow()

    private val db = FirebaseDatabase.getInstance(
        "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    )

    init {
        loadVideoInfo()
        observeLikeCount()
    }

    private fun loadVideoInfo() {
        if (streamId.isEmpty()) {
            setFallback()
            return
        }

        db.getReference("Videos/$streamId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (!snapshot.exists()) {
                        setFallback()
                        return
                    }

                    val url = snapshot.child("url").getValue(String::class.java)
                        ?: "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8"

                    val title = snapshot.child("title").getValue(String::class.java) ?: ""

                    val description = snapshot.child("description")
                        .getValue(String::class.java) ?: ""

                    val bookId = snapshot.child("bookId")
                        .getValue(String::class.java) ?: ""

                    val likeCount = snapshot.child("likeCount")
                        .getValue(Long::class.java) ?: 0L

                    _likeCount.value = likeCount

                    _uiState.value = LiveStreamUiState.Success(
                        LiveStreamInfo(
                            streamId = streamId,
                            streamUrl = url,
                            streamerName = "My Book Review",
                            streamerAvatarUrl = "",
                            title = description,
                            viewerCount = 0,
                            likeCount = likeCount,
                            isFollowing = false,
                            pinnedProducts = emptyList()
                        )
                    )

                    if (bookId.isNotEmpty()) {
                        loadLinkedBook(bookId)
                    }
                }

                override fun onCancelled(error: DatabaseError) {
                    setFallback()
                }
            })
    }

    private fun setFallback() {
        _uiState.value = LiveStreamUiState.Success(
            LiveStreamInfo(
                streamId = streamId,
                streamUrl = "https://test-streams.mux.dev/x36xhzz/x36xhzz.m3u8",
                streamerName = "My Book Review",
                streamerAvatarUrl = "",
                title = "",
                viewerCount = 0,
                likeCount = 0L,
                isFollowing = false,
                pinnedProducts = emptyList()
            )
        )
    }

    private fun loadLinkedBook(bookId: String) {
        db.getReference("Books/$bookId")
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    try {
                        _linkedBook.value =
                            snapshot.getValue(Book::class.java)?.copy(id = bookId)
                    } catch (e: Exception) {
                        android.util.Log.e("LiveStreamViewModel", "Lỗi convert sách id $bookId: ${e.message}")
                    }
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // realtime like count
    private fun observeLikeCount() {
        db.getReference("Videos/$streamId/likeCount")
            .addValueEventListener(object : ValueEventListener {

                override fun onDataChange(snapshot: DataSnapshot) {
                    _likeCount.value =
                        snapshot.getValue(Long::class.java) ?: 0L
                }

                override fun onCancelled(error: DatabaseError) {}
            })
    }

    // like / unlike
    fun initUserLike(userId: String) {
        db.getReference("video_user_likes/$streamId/$userId")
            .addValueEventListener(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    _isUserLiked.value = snapshot.exists()
                }
                override fun onCancelled(error: DatabaseError) {}
            })
    }


    fun toggleLike(userId: String) {
        val userLikeRef = db.getReference("video_user_likes/$streamId/$userId")
        val countRef = db.getReference("Videos/$streamId/likeCount")

        if (_isUserLiked.value) {
            _isUserLiked.value = false
            userLikeRef.removeValue()
            countRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    data.value = if ((data.getValue(Long::class.java) ?: 0L) > 0)
                        (data.getValue(Long::class.java) ?: 0L) - 1 else 0
                    return Transaction.success(data)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
            })
        } else {
            _isUserLiked.value = true
            userLikeRef.setValue(true)
            countRef.runTransaction(object : Transaction.Handler {
                override fun doTransaction(data: MutableData): Transaction.Result {
                    data.value = (data.getValue(Long::class.java) ?: 0L) + 1
                    return Transaction.success(data)
                }
                override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {}
            })
        }
    }

    fun openCart() {
        viewModelScope.launch {
            _events.send(LiveStreamEvent.OpenCart)
        }
    }

    fun openProduct(product: LiveProduct) {
        viewModelScope.launch {
            _events.send(LiveStreamEvent.OpenProduct(product))
        }
    }
}