// worker/src/main/kotlin/com/litvidan/worker/WorkerConfig.kt
package com.litvidan.worker

import java.util.*

object WorkerConfig {
    private const val DEFAULT_CONFIG_FILE = "worker.properties"

    data class Config(
        val rabbitmqHost: String,
        val alphabet: String
    )

    val current: Config by lazy { loadConfig() }

    private fun loadConfig(): Config {
        val props = Properties()
        val inputStream = javaClass.classLoader.getResourceAsStream(DEFAULT_CONFIG_FILE)
            ?: throw IllegalStateException("Configuration file '$DEFAULT_CONFIG_FILE' not found")

        inputStream.use { props.load(it) }

        return Config(
            rabbitmqHost = props.getProperty("rabbitmq.host") ?: "rabbitmq",
            alphabet = props.getProperty("alphabet") ?: "abcdefghijklmnopqrstuvwxyz0123456789"
        )
    }
}