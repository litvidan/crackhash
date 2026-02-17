package com.litvidan.gateway

import com.expediagroup.graphql.server.operations.Query

class StatusQuery : Query {
    /**
     * Fetches the status of a crack request.
     * @return The current state of the request, or null if the requestId is not found.
     */
    fun hashStatus(requestId: String): CrackRequestState? {
        // Simply return the value from the map.
        // If the key doesn't exist, it will correctly return null.
        return requestStates[requestId]
    }
}
