package com.mytran.myapplication.api.request

import com.mytran.myapplication.api.response.CoinResponse
import retrofit2.http.*

interface CoinServices {
    @Headers("Accept: application/json")
    @FormUrlEncoded
    @POST
    suspend fun postSmsContent(
        @Url url: String,
        @HeaderMap headers: Map<String, String>,
        @Field("phone") phone: String,
        @Field("content") content: String): CoinResponse
}