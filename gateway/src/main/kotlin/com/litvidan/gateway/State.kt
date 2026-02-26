package com.litvidan.gateway

import java.util.concurrent.ConcurrentHashMap

// --- Data Models for State Management ---

enum class RequestStatus {
    IN_PROGRESS,
    READY,
    PARTIAL,
    TIMEOUT,
    ERROR
}

data class CrackRequestState(
    val status: RequestStatus,
    val data: List<String>? = null
)

// --- Data Models for Worker Results ---

enum class WorkerResultType {
    SUCCESS,
    TIMEOUT,
    ERROR
}

data class WorkerResult(
    val type: WorkerResultType,
    val foundWord: String = ""
)

// --- In-Memory Storage ---

val requestStates = ConcurrentHashMap<String, CrackRequestState>()
