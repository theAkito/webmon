package com.manimarank.websitemonitor.data.api

import retrofit2.Response
import retrofit2.http.GET

interface ApiClient {
    @GET("/")
    suspend fun getWebsiteStatus(): Response<Any>
}
