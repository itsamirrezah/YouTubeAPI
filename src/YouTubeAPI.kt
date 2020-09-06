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

    @GET("/youtube/v3/commentThreads?part=snippet&order=relevance&maxResults=3")
    suspend fun getComments(@Query("videoId") videoId: String): Response<ItemsResponse>
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
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC)
        httpClient.addInterceptor(logging)
    }
}

suspend fun playlistItems(playlistId: String?): List<Video> {

    return coroutineScope {

        //initial values
        var nextPageToken: String? = ""
        var page = 0
        val videos = mutableListOf<Video>()
        //retrieve items from youtube api
        while (nextPageToken != null) {     // while additional result are available, retrieve them
            //synchronous call
            val responseBody = RetrofitBuilder.youtube.getPlaylistItems(playlistId!!, nextPageToken).body()!!
            for ((index, item) in responseBody.items.withIndex()) {
                log("id: ${item.id.subSequence(item.id.length - 5, item.id.length)}")

                videos.add(Video(item.contentDetails.videoId))
                val idx = page * 50 + index
                launch {
                    log("index: $index sending video request")
                    val response = RetrofitBuilder.youtube.getVideo(item.contentDetails.videoId).body()!!.items.first()
                    videos[idx].title = response.snippet.title
                    videos[idx].channelTitle = response.snippet.channelTitle
                    videos[idx].url = YOUTUBE_W + response.id
                    videos[idx].duration = response.contentDetails.duration
                    videos[idx].viewCount = response.statistics.viewCount
                    videos[idx].likeCount = response.statistics.likeCount
                    videos[idx].dislikeCount = response.statistics.dislikeCount
                    videos[idx].favoriteCount = response.statistics.favoriteCount
                    videos[idx].commentCount = response.statistics.commentCount
                }

                launch {
                    log("index: $index sending comment request")
                    val response = RetrofitBuilder.youtube.getComments(item.contentDetails.videoId).body()!!.items

                    val comments = mutableListOf<Comment>()
                    for (comment in response) {
                        comments.add(
                            Comment(
                                authorName = comment.snippet.topLevelComment.snippet.authorDisplayName,
                                authorProfileImageUrl = comment.snippet.topLevelComment.snippet.authorProfileImageUrl,
                                text = comment.snippet.topLevelComment.snippet.textOriginal,
                                likeCount = comment.snippet.topLevelComment.snippet.likeCount,
                                publishedAt = comment.snippet.topLevelComment.snippet.publishedAt
                            )
                        )
                    }
                    videos[idx].topComment = comments
                }
            }
            //point to the next page
            nextPageToken = responseBody.nextPageToken
            page++
        }
        videos
    }
}


/**  models **/

data class Video(
    val id: String
) {
    lateinit var title: String
    lateinit var channelTitle: String
    lateinit var url: String
    lateinit var duration: String
    lateinit var viewCount: String
    lateinit var likeCount: String
    lateinit var dislikeCount: String
    lateinit var favoriteCount: String
    lateinit var commentCount: String
    lateinit var topComment: List<Comment>
}

data class Comment(
    val authorName: String,
    val authorProfileImageUrl: String,
    val text: String,
    val likeCount: String,
    val publishedAt: String


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
    //CommentThreads
    val totalReplyCount: String,
    val topLevelComment: TopLevelComment,
    val textOriginal: String,
    val authorDisplayName: String,
    val authorProfileImageUrl: String,
    val likeCount: String,
    val publishedAt: String
)

data class TopLevelComment(
    val snippet: Snippet
)

data class Statistics(
    val viewCount: String,
    val likeCount: String,
    val dislikeCount: String,
    val favoriteCount: String,
    val commentCount: String
)
