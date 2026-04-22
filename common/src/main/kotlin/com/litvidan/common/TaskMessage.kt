package com.litvidan.common

import kotlinx.serialization.Serializable

@Serializable
data class TaskMessage(
    val requestId: String,
    val hash: String,
    val alphabet: String,
    val maxLength: Int,
    val startIndex: Long,
    val rangeSize: Long
)

@Serializable
data class ResultMessage(
    val requestId: String,
    val foundWord: String = ""
)