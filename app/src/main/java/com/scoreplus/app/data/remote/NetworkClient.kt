package com.scoreplus.app.data.remote

import com.scoreplus.app.data.remote.api.ParaDostApi
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object NetworkClient {

    // Geliştirme sırasında localhost (emülatör için 10.0.2.2)
    // Production deploy sonrası gerçek URL ile değiştir
    const val BASE_URL = "https://paradostapi.onrender.com/"

    fun create(tokenStore: TokenStore): ParaDostApi {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        val client = OkHttpClient.Builder()
            .addInterceptor(AuthInterceptor(tokenStore))
            .addInterceptor(logging)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ParaDostApi::class.java)
    }
}
