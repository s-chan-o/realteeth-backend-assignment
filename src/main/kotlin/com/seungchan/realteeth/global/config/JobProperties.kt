package com.seungchan.realteeth.global.config

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "job")
data class JobProperties(
    val processingTimeoutMinutes: Long = 30,
)