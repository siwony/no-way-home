package com.nowayhome.status

import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/status")
class AppStatusController(
    private val jdbcTemplate: JdbcTemplate,
) {
    @GetMapping
    fun status(): AppStatusResponse {
        val databaseTime = jdbcTemplate.queryForObject("select now()", String::class.java)

        return AppStatusResponse(
            service = "no-way-home",
            database = DatabaseStatus(
                connected = true,
                serverTime = databaseTime,
            ),
        )
    }
}
