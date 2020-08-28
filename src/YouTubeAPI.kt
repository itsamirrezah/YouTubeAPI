package com.itsamirrezah.youtubeapi

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeAPI {

    @GET("/youtube/v3/playlistItems?part=snippet&maxResults=50")
    fun getPlaylistItems(
        @Query("playlistId") playlistId: String,
        @Query("pageToken") pageToken: String  //point to the next page
    ): Call<PlaylistItems>
}

object RetrofitBuilder {

    var youtube: YouTubeAPI
    private var httpClient = OkHttpClient.Builder()

    init {
        loggingInterceptor()
        //add google api key to all requests
        httpClient.interceptors().add(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): Response {
                var request = chain.request()
                val httpUrl = request.url.newBuilder().addQueryParameter("key", GOOGLE_API_KEY).build()
                request = request.newBuilder().url(httpUrl).build()
                return chain.proceed(request)
            }
        })

        youtube = Retrofit.Builder()
            .baseUrl("https://www.googleapis.com")
            .addConverterFactory(GsonConverterFactory.create())
            .client(httpClient.build())
            .build()
            .create(YouTubeAPI::class.java)
    }

    private fun loggingInterceptor() {
        val logging = HttpLoggingInterceptor()
        logging.setLevel(HttpLoggingInterceptor.Level.BODY)

        httpClient.addInterceptor(logging)
    }
}

fun playlistItems(playlistId: String?): PlaylistItems {

    //initial values
    var nextPageToken: String? = ""
    val playlistItems = mutableListOf<Item>()

    //retrieve items from youtube api
    while (nextPageToken != null) {     // while additional result are available, retrieve them
        //synchronous call
        val responseBody = RetrofitBuilder.youtube.getPlaylistItems(playlistId!!, nextPageToken).execute()
            .body()!!
        playlistItems.addAll(responseBody.items)
        //point to the next page
        nextPageToken = responseBody.nextPageToken
    }
    return PlaylistItems(playlistItems)
}

/**  models **/
data class PlaylistItems(
    val nextPageToken: String,
    val items: List<Item>
) {
    constructor(items: List<Item>) : this("", items)
}

data class Item(val snippet: Snippet)

data class Snippet(val resourceId: ResourceId)

data class ResourceId(val videoId: String)

