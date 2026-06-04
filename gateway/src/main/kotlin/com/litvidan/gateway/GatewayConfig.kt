package com.litvidan.gateway

import java.io.InputStream
import java.util.*

object GatewayConfig {
    private const val DEFAULT_CONFIG_FILE = "gateway.properties"
    private const val SYSTEM_PROPERTY_KEY = "gateway.config"

    data class Config(
        val rabbitmqHost: String,
        val mongoUri: String,
        val mongoDatabase: String,
        val alphabet: String,
        val taskPartitionCount: Int
    )

    val current: Config by lazy { loadConfig() }

    private fun loadConfig(): Config {
        val props = Properties()
        val configPath = System.getProperty(SYSTEM_PROPERTY_KEY)

        val inputStream: InputStream = if (configPath != null) {
            java.io.FileInputStream(configPath)
        } else {
            javaClass.classLoader.getResourceAsStream(DEFAULT_CONFIG_FILE)
                ?: throw IllegalStateException("Configuration file '$DEFAULT_CONFIG_FILE' not found")
        }

        inputStream.use { props.load(it) }

        return Config(
            rabbitmqHost = props.getProperty("rabbitmq.host") ?: "rabbitmq",
            mongoUri = props.getProperty("mongo.uri") ?: "mongodb://localhost:27017",
            mongoDatabase = props.getProperty("mongo.database") ?: "crackhash",
            alphabet = props.getProperty("alphabet") ?: "abcdefghijklmnopqrstuvwxyz0123456789",
            taskPartitionCount = props.getProperty("task.partition.count")?.toIntOrNull() ?: 9
        )
    }
}