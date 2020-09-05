package com.itsamirrezah.youtubeapi

import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

interface YouTubeAPI {

    @GET("/youtube/v3/playlistItems?part=contentDetails&maxResults=50")
    suspend fun getPlaylistItems(
        @Query("playlistId") playlistId: String,
        @Query("pageToken") pageToken: String  //point to the next page
    ): Response<ItemsResponse>

    @GET("/youtube/v3/videos?part=snippet,contentDetails,statistics")
    suspend fun getVideo(@Query("id") id: String): Response<ItemsResponse>
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
        logging.setLevel(HttpLoggingInterceptor.Level.HEADERS)
        httpClient.addInterceptor(logging)
    }
}

suspend fun playlistItems(playlistId: String?): List<Video> {

    return coroutineScope {

        //initial values
        var nextPageToken: String? = ""
        val videos = mutableListOf<Video>()
        //retrieve items from youtube api
        while (nextPageToken != null) {     // while additional result are available, retrieve them
            //synchronous call
            val responseBody = RetrofitBuilder.youtube.getPlaylistItems(playlistId!!, nextPageToken).body()!!
            for (item in responseBody.items) {
                launch {
                    log("$item: sending request")
                    val video = RetrofitBuilder.youtube.getVideo(item.contentDetails.videoId).body()!!.items.first()
                    videos.add(
                        Video(
                            id= video.id,
                            title = video.snippet.title,
                            channelTitle = video.snippet.channelTitle,
                            url = YOUTUBE_W + video.id,
                            duration = video.contentDetails.duration,
                            viewCount = video.statistics.viewCount,
                            likeCount = video.statistics.likeCount,
                            dislikeCount = video.statistics.dislikeCount,
                            favoriteCount = video.statistics.favoriteCount,
                            commentCount = video.statistics.commentCount
                        )
                    )
                }
            }
            //point to the next page
            nextPageToken = responseBody.nextPageToken
        }
        videos
    }
}


/**  models **/

data class Video(
    val id: String,
    val title: String,
    val channelTitle: String,
    val url: String,
    val duration: String,
    val viewCount: String,
    val likeCount: String,
    val dislikeCount: String,
    val favoriteCount: String,
    val commentCount: String
)


/** youtube models **/
data class ItemsResponse(
    val nextPageToken: String,
    val items: List<Item>
)

data class Item(
    val id: String,
    val snippet: Snippet,
    val contentDetails: ContentDetails,
    val statistics: Statistics
)

data class ContentDetails(
    val videoId: String,
    val videoPublishedAt: String,
    val duration: String,
    val definition: String
)

data class Snippet(
    val title: String,
    val description: String,
    val channelTitle: String,
    val tags: List<String>
)

data class Statistics(
    val viewCount: String,
    val likeCount: String,
    val dislikeCount: String,
    val favoriteCount: String,
    val commentCount: String
)
