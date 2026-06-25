package com.mobrauntech.shoplist.data.remote

import com.mobrauntech.shoplist.BuildConfig
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Query
import java.util.concurrent.TimeUnit

interface ApiService {
    @POST("api/sync")
    suspend fun sync(@Body body: SyncRequest): SyncResponse

    @GET("api/state")
    suspend fun state(): SyncResponse

    @GET("api/suggest")
    suspend fun suggest(@Query("q") q: String): SuggestResponse

    @GET("api/images")
    suspend fun images(@Query("q") q: String): ImagesResponse

    @POST("api/ai/section")
    suspend fun aiSection(@Body body: SectionRequest): SectionResponse

    @Multipart
    @POST("api/upload")
    suspend fun upload(@Part image: MultipartBody.Part): UploadResponse
}

object Api {
    val service: ApiService by lazy {
        val logging = HttpLoggingInterceptor().apply {
            level = if (BuildConfig.DEBUG) HttpLoggingInterceptor.Level.BASIC
            else HttpLoggingInterceptor.Level.NONE
        }
        val client = OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .addInterceptor(logging)
            .build()

        Retrofit.Builder()
            .baseUrl(BuildConfig.BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
