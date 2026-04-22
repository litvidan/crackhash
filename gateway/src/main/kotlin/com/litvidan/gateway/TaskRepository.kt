package com.litvidan.gateway

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList

class TaskRepository {
    private val collection: MongoCollection<TaskDocument> =
        MongoConfig.database.getCollection("tasks")

    suspend fun save(task: TaskDocument) {
        collection.insertOne(task)
    }

    suspend fun updateStatus(requestId: String, status: TaskStatus) {
        collection.updateOne(
            Filters.eq(TaskDocument::requestId.name, requestId),
            Updates.set(TaskDocument::status.name, status)
        )
    }

    suspend fun addFoundWord(requestId: String, word: String) {
        collection.updateOne(
            Filters.eq(TaskDocument::requestId.name, requestId),
            Updates.push(TaskDocument::foundWords.name, word)
        )
    }

    suspend fun findByRequestId(requestId: String): TaskDocument? {
        return collection.find(Filters.eq(TaskDocument::requestId.name, requestId))
            .firstOrNull()
    }

    suspend fun findAllByStatus(status: TaskStatus): List<TaskDocument> {
        return collection.find(Filters.eq(TaskDocument::status.name, status))
            .toList()
    }
}