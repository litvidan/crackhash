package com.litvidan.gateway

import com.mongodb.client.model.Filters
import com.mongodb.client.model.Updates
import com.mongodb.kotlin.client.coroutine.MongoCollection
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.toList
import org.bson.conversions.Bson

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

    suspend fun findByRequestId(requestId: String): TaskDocument? {
        return collection.find(Filters.eq(TaskDocument::requestId.name, requestId))
            .firstOrNull()
    }

    suspend fun findAllByStatus(status: TaskStatus): List<TaskDocument> {
        return collection.find(Filters.eq(TaskDocument::status.name, status))
            .toList()
    }

    /**
     * Atomically process a worker response.
     * @return true if this part was new (not yet processed), false if it was a duplicate.
     */
// TaskRepository.kt – tryProcessPart
    suspend fun tryProcessPart(requestId: String, partId: Int, foundWord: String): Boolean {
        val filter = Filters.and(
            Filters.eq(TaskDocument::requestId.name, requestId),
            Filters.not(Filters.eq("${TaskDocument::processedParts.name}.$partId", partId))
        )

        val operators = mutableListOf<Bson>(
            Updates.addToSet(TaskDocument::processedParts.name, partId),
            Updates.inc(TaskDocument::completedParts.name, 1)
        )
        if (foundWord.isNotEmpty()) {
            operators.add(Updates.push(TaskDocument::foundWords.name, foundWord))
        }
        val updates = Updates.combine(operators)

        val result = collection.updateOne(filter, updates)
        if (result.modifiedCount > 0) {
            val updatedDoc = findByRequestId(requestId)
            if (updatedDoc != null && updatedDoc.completedParts >= updatedDoc.totalParts) {
                updateStatus(requestId, TaskStatus.COMPLETED)
            }
            return true
        }
        return false
    }
}