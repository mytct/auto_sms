package com.mytran.myapplication.api.repository

import com.mytran.myapplication.api.request.CoinServices

class CoinRepository(private val coinServices: CoinServices) {
    suspend fun postSmsContent(url: String, header: Map<String, String>, phone: String, content: String) = coinServices.postSmsContent(url, header, phone, content)
}