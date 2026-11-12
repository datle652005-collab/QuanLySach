package com.example.quanlysach.data.model

class NotificationModel {
    var id: String = ""
    var title: String = ""
    var content: String = ""
    var body: String = ""
    var orderId: String = ""
    var timestamp: Long = 0L
    var type: String = ""
    var image: String = ""
    var isRead: Boolean = false
    var isProcessed: Boolean = false

    // Constructor không tham số bắt buộc phải có cho Firebase
    constructor()

    constructor(id: String, title: String, content: String, body: String, orderId: String, timestamp: Long, type: String, image: String, isRead: Boolean, isProcessed: Boolean) {
        this.id = id
        this.title = title
        this.content = content
        this.body = body
        this.orderId = orderId
        this.timestamp = timestamp
        this.type = type
        this.image = image
        this.isRead = isRead
        this.isProcessed = isProcessed
    }
}