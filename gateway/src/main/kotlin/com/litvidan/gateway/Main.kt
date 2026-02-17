package com.litvidan.gateway

import com.expediagroup.graphql.server.ktor.GraphQL
import com.expediagroup.graphql.server.ktor.graphQLPostRoute
import io.ktor.server.application.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.routing.*

fun main() {
    // Force configuration loading at startup
    GatewayConfig.current

    embeddedServer(Netty, port = 8080) {
        install(GraphQL) {
            schema {
                packages = listOf("com.litvidan.gateway")
                queries = listOf(StatusQuery())
                mutations = listOf(CrackHashMutation())
            }
        }
        routing {
            graphQLPostRoute()
        }
    }.start(wait = true)
}