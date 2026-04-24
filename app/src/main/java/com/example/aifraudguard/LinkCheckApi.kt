package com.example.aifraudguard

import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

data class UrlCheckRequest(val url: String)

data class CheckDetail(
    val name: String,
    val score: Int,
    val message: String,
    val status: String,
    val color: String,
    val icon: String
)

data class CheckResult(
    val url: String,
    val final_score: Int,
    val verdict: String,
    val color: String,
    val simple_message: String,
    val recommendation: String,
    val checks: List<CheckDetail>
)

interface LinkCheckApi {
    @POST("/check-url")
    suspend fun checkUrl(@Body request: UrlCheckRequest): CheckResult

    @GET("/health")
    suspend fun checkHealth(): Map<String, String>
}
