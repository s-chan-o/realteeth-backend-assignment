package com.seungchan.realteeth.global.config

import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.web.client.RestClient

@Configuration
@EnableConfigurationProperties(MockWorkerProperties::class, JobProperties::class)
class RestClientConfig {

    @Bean
    fun mockWorkerRestClient(properties: MockWorkerProperties): RestClient =
        RestClient.builder()
            .baseUrl(properties.baseUrl)
            .build()
}