package com.litvidan.gateway

import org.bson.codecs.pojo.annotations.BsonId
import org.bson.types.ObjectId

data class TaskDocument(
    @BsonId
    val id: ObjectId = ObjectId(),
    val requestId: String,
    val hash: String,
    val maxLength: Int,
    val alphabet: String,
    var status: TaskStatus = TaskStatus.PENDING_QUEUE,
    val foundWords: MutableList<String> = mutableListOf(),
    val createdAt: Long = System.currentTimeMillis()
)

enum class TaskStatus {
    PENDING_QUEUE,    // Task saved, but not yet in queue
    PENDING_WORKER,   // Sent to queue, processing awaits
    COMPLETED,        // At least one result has been received
    ERROR             // Critical error
}