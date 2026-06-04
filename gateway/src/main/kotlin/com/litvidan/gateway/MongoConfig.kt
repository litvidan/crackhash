package com.litvidan.gateway

import com.mongodb.ConnectionString
import com.mongodb.MongoClientSettings
import com.mongodb.WriteConcern
import com.mongodb.kotlin.client.coroutine.MongoClient
import com.mongodb.kotlin.client.coroutine.MongoDatabase
import org.bson.codecs.configuration.CodecRegistries
import org.bson.codecs.pojo.PojoCodecProvider

object MongoConfig {
    private val config = GatewayConfig.current

    val database: MongoDatabase by lazy {
        val pojoCodecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        )

        val settings = MongoClientSettings.builder()
            .applyConnectionString(ConnectionString(config.mongoUri))
            .writeConcern(WriteConcern.MAJORITY)
            .codecRegistry(pojoCodecRegistry)
            .build()

        val client = MongoClient.create(settings)
        client.getDatabase(config.mongoDatabase)
    }
}