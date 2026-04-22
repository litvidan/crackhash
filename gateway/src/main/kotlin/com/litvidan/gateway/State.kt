package com.litvidan.gateway

enum class RequestStatus {
    IN_PROGRESS,
    READY,
    ERROR
}

data class CrackRequestState(
    val status: RequestStatus,
    val data: List<String>? = null
)