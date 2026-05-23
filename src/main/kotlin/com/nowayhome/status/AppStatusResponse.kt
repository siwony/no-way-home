package com.nowayhome.status

data class AppStatusResponse(
    val service: String,
    val database: DatabaseStatus,
)

data class DatabaseStatus(
    val connected: Boolean,
    val serverTime: String?,
)
