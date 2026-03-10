package com.seungchan.realteeth.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "mock-worker")
data class MockWorkerProperties(
    val baseUrl: String,
    val apiKey: String,
)