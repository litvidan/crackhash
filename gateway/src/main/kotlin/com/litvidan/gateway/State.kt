package com.litvidan.gateway

import java.util.concurrent.ConcurrentHashMap

// --- Data Models for State Management ---

enum class RequestStatus {
    IN_PROGRESS,
    READY,
    ERROR
}

data class CrackRequestState(
    val status: RequestStatus,
    val data: List<String>? = null
)

// --- In-Memory Storage ---

// A thread-safe map to store the state of each crack request.
val requestStates = ConcurrentHashMap<String, CrackRequestState>()
