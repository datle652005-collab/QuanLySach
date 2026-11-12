package com.example.quanlysach.data.repository

import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.MutableData
import com.google.firebase.database.Transaction
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

@Singleton
class LiveStreamRepository @Inject constructor() {


    private val databaseUrl = "https://quanlysach-cd026-default-rtdb.asia-southeast1.firebasedatabase.app/"
    private val db = FirebaseDatabase.getInstance(databaseUrl)

    // Gửi Like
    suspend fun sendLike(streamId: String): Result<Long> = suspendCoroutine { continuation ->
        val ref = db.getReference("Videos/$streamId/likeCount")

        ref.runTransaction(object : Transaction.Handler {
            override fun doTransaction(data: MutableData): Transaction.Result {
                val current = data.getValue(Long::class.java) ?: 0L
                data.value = current + 1
                return Transaction.success(data)
            }

            override fun onComplete(error: DatabaseError?, committed: Boolean, snapshot: DataSnapshot?) {
                if (error != null) {
                    continuation.resume(Result.failure(error.toException()))
                } else {
                    val finalCount = snapshot?.getValue(Long::class.java) ?: 0L
                    continuation.resume(Result.success(finalCount))
                }
            }
        })
    }
}
