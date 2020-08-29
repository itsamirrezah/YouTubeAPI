package com.itsamirrezah.youtubeapi

import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeAPI {

    @GET("/youtube/v3/playlistItems?part=snippet&maxResults=50")
    suspend fun getPlaylistItems(
        @Query("playlistId") playlistId: String,
        @Query("pageToken") pageToken: String  //point to the next page
    ): Response<PlaylistItems>
}

object RetrofitBuilder {

    var youtube: YouTubeAPI
    private var httpClient = OkHttpClient.Builder()

    init {
        loggingInterceptor()
        //add google api key to all requests
        httpClient.interceptors().add(object : Interceptor {
            override fun intercept(chain: Interceptor.Chain): okhttp3.Response {
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

suspend fun playlistItems(playlistId: String?): List<Video> {

    //initial values
    var nextPageToken: String? = ""
    val videos = mutableListOf<Video>()

    //retrieve items from youtube api
    while (nextPageToken != null) {     // while additional result are available, retrieve them
        //synchronous call
        val responseBody = RetrofitBuilder.youtube.getPlaylistItems(playlistId!!, nextPageToken).body()!!
        responseBody.items.map {
            val data = it.snippet
            videos.add(Video(data.title, data.channelTitle, YOUTUBE_W + data.resourceId.videoId, data.position))
        }
        //point to the next page
        nextPageToken = responseBody.nextPageToken
    }
    return videos
}


/**  models **/

data class Video(
    val title: String,
    val channelTitle: String,
    val url: String,
    val position: Int
)

/** youtube models **/
data class PlaylistItems(
    val nextPageToken: String,
    val items: List<Item>
) {
    constructor(items: List<Item>) : this("", items)
}

data class Item(val snippet: Snippet)

data class Snippet(
    val title: String,
    val channelTitle: String,
    val position: Int,
    val resourceId: ResourceId
)

data class ResourceId(val videoId: String)

