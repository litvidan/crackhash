package com.litvidan.gateway

import java.io.FileInputStream
import java.io.InputStream
import java.util.*

object GatewayConfig {
    private const val DEFAULT_CONFIG_FILE = "gateway.properties"
    private const val SYSTEM_PROPERTY_KEY = "gateway.config"

    data class Config(
        val workerHosts: List<String>,
        val workerPort: Int,
        val alphabet: String,
        val requestTimeoutMillis: Long,
        val workerCount: Int
    )

    val current: Config by lazy { loadConfig() }

    private fun loadConfig(): Config {
        val props = Properties()
        val configPath = System.getProperty(SYSTEM_PROPERTY_KEY)

        val inputStream: InputStream = if (configPath != null) {
            // If system property is set, try to load from file system
            FileInputStream(configPath)
        } else {
            // Otherwise, load from classpath resources
            val resourceStream = GatewayConfig::class.java.classLoader.getResourceAsStream(DEFAULT_CONFIG_FILE)
                ?: throw IllegalStateException("Configuration file '$DEFAULT_CONFIG_FILE' not found in classpath")
            resourceStream
        }

        inputStream.use { stream ->
            props.load(stream)
        }

        // Parse with validation
        val hosts = props.getProperty("worker.hosts")
            ?.split(",")
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            ?: throw IllegalArgumentException("Missing or empty 'worker.hosts' in config")

        val port = props.getProperty("worker.port")?.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid or missing 'worker.port'")

        val alphabet = props.getProperty("alphabet")
            ?: throw IllegalArgumentException("Missing 'alphabet'")

        val timeout = props.getProperty("request.timeout.ms")?.toLongOrNull()
            ?: throw IllegalArgumentException("Invalid or missing 'request.timeout.ms'")

        val count = props.getProperty("worker.count")?.toIntOrNull()
            ?: throw IllegalArgumentException("Invalid or missing 'worker.count'")

        return Config(
            workerHosts = hosts,
            workerPort = port,
            alphabet = alphabet,
            requestTimeoutMillis = timeout,
            workerCount = count
        )
    }
}