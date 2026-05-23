package com.nowayhome.status

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppStatusResponseTest {
    @Test
    fun createsStatusResponseWhenDatabaseIsConnected() {
        val response = AppStatusResponse(
            service = "no-way-home",
            database = DatabaseStatus(
                connected = true,
                serverTime = "2026-05-24T00:00:00Z",
            ),
        )

        assertEquals("no-way-home", response.service)
        assertTrue(response.database.connected)
    }
}
