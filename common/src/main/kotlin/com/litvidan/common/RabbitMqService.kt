package com.litvidan.common

import com.rabbitmq.client.*
import kotlinx.coroutines.*
import kotlinx.serialization.json.Json

class RabbitMqService(
    host: String = "rabbitmq",
    maxRetries: Int = 30,
    retryDelayMs: Long = 3000
) {
    private val factory = ConnectionFactory().apply { this.host = host }
    private val json = Json { ignoreUnknownKeys = true }

    lateinit var connection: Connection
    lateinit var channel: Channel

    init {
        var attempt = 0
        var connected = false
        var lastException: Exception? = null

        while (attempt < maxRetries && !connected) {
            try {
                connection = factory.newConnection()
                channel = connection.createChannel()
                // Объявляем очереди, exchange и настраиваем QoS
                channel.queueDeclare(TASK_QUEUE, true, false, false, null)
                channel.queueDeclare(RESULT_QUEUE, true, false, false, null)
                channel.exchangeDeclare(TASK_EXCHANGE, "direct", true)
                channel.exchangeDeclare(RESULT_EXCHANGE, "direct", true)
                channel.queueBind(TASK_QUEUE, TASK_EXCHANGE, TASK_ROUTING_KEY)
                channel.queueBind(RESULT_QUEUE, RESULT_EXCHANGE, RESULT_ROUTING_KEY)
                channel.basicQos(1)

                println("[RabbitMqService] Connected successfully")
                connected = true
            } catch (e: Exception) {
                lastException = e
                println("[RabbitMqService] Connection attempt $attempt failed: ${e.message}")
                attempt++
                if (attempt < maxRetries) {
                    Thread.sleep(retryDelayMs)
                }
            }
        }
        if (!connected) {
            throw IllegalStateException(
                "Could not connect to RabbitMQ at $host after $maxRetries attempts",
                lastException
            )
        }
    }

    suspend fun sendTask(task: TaskMessage) {
        val jsonString = json.encodeToString(TaskMessage.serializer(), task)
        channel.basicPublish(
            TASK_EXCHANGE,
            TASK_ROUTING_KEY,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            jsonString.toByteArray()
        )
    }

    suspend fun sendResult(result: ResultMessage) {
        val jsonString = json.encodeToString(ResultMessage.serializer(), result)
        channel.basicPublish(
            RESULT_EXCHANGE,
            RESULT_ROUTING_KEY,
            MessageProperties.PERSISTENT_TEXT_PLAIN,
            jsonString.toByteArray()
        )
    }

    fun startTaskConsumer(handler: suspend (TaskMessage, Long) -> Unit): String {
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray
            ) {
                val jsonString = String(body)
                val task = json.decodeFromString(TaskMessage.serializer(), jsonString)
                GlobalScope.launch {
                    try {
                        handler(task, envelope.deliveryTag)
                        channel.basicAck(envelope.deliveryTag, false)
                    } catch (e: Exception) {
                        channel.basicNack(envelope.deliveryTag, false, true)
                    }
                }
            }
        }
        return channel.basicConsume(TASK_QUEUE, false, consumer)
    }

    fun startResultConsumer(handler: suspend (ResultMessage) -> Unit): String {
        val consumer = object : DefaultConsumer(channel) {
            override fun handleDelivery(
                consumerTag: String?,
                envelope: Envelope,
                properties: AMQP.BasicProperties,
                body: ByteArray
            ) {
                val jsonString = String(body)
                val result = json.decodeFromString(ResultMessage.serializer(), jsonString)
                GlobalScope.launch {
                    handler(result)
                    channel.basicAck(envelope.deliveryTag, false)
                }
            }
        }
        return channel.basicConsume(RESULT_QUEUE, false, consumer)
    }

    fun close() {
        channel.close()
        connection.close()
    }

    companion object {
        const val TASK_QUEUE = "task.queue"
        const val RESULT_QUEUE = "result.queue"
        const val TASK_EXCHANGE = "task.exchange"
        const val RESULT_EXCHANGE = "result.exchange"
        const val TASK_ROUTING_KEY = "task"
        const val RESULT_ROUTING_KEY = "result"
    }
}