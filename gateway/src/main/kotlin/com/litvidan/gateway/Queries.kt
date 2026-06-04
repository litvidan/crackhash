package com.litvidan.gateway

import com.expediagroup.graphql.server.operations.Query
import kotlinx.coroutines.runBlocking

class StatusQuery : Query {
    private val repository = TaskRepository()

    fun hashStatus(requestId: String): CrackRequestState? = runBlocking {
        val doc = repository.findByRequestId(requestId) ?: return@runBlocking null

        val apiStatus = when (doc.status) {
            TaskStatus.PENDING_QUEUE, TaskStatus.PENDING_WORKER -> RequestStatus.IN_PROGRESS
            TaskStatus.COMPLETED -> RequestStatus.READY
            TaskStatus.ERROR -> RequestStatus.ERROR
        }

        CrackRequestState(
            status = apiStatus,
            data = doc.foundWords.takeIf { it.isNotEmpty() }
        )
    }
}