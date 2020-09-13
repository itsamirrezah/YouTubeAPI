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

suspend fun playlistItems(playlistId: String?, part: String): List<Video> {

    return coroutineScope {

        val startTime = System.currentTimeMillis()
        log("coroutineScope: starting at ${System.currentTimeMillis() - startTime}")

        //initial values
        var nextPageToken: String? = ""
        var page = 0
        val videos = mutableListOf<Video>()
        //retrieve items from youtube api
        while (nextPageToken != null) {     // while additional result are available, retrieve them
            //synchronous call
            log("playlistItem request at ${System.currentTimeMillis() - startTime}")
            val responseBody = RetrofitBuilder.youtube.getPlaylistItems(playlistId!!, nextPageToken).body()!!
            log("getting result from playlistItem at ${System.currentTimeMillis() - startTime}")

            val ids = responseBody.items.map {
                videos.add(Video(it.contentDetails.videoId))
                it.contentDetails.videoId
            }

            if (part.contains(VIDEO_ARG)) {
                val currentPage = page
                launch {
                    RetrofitBuilder.youtube.getVideo(ids.joinToString(",")).body()!!.items
                        .also { log("getting 50 video at ${System.currentTimeMillis() - startTime}") }
                        .mapIndexed { index, item ->
                            val idx = currentPage * 50 + index
                            log("video #$idx at ${System.currentTimeMillis() - startTime}")
                            videos[idx].title = item.snippet.title
                            videos[idx].channelTitle = item.snippet.channelTitle
                            videos[idx].duration = item.contentDetails.duration
                            videos[idx].viewCount = item.statistics.viewCount
                            videos[idx].likeCount = item.statistics.likeCount
                            videos[idx].dislikeCount = item.statistics.dislikeCount
                            videos[idx].favoriteCount = item.statistics.favoriteCount
                            videos[idx].commentCount = item.statistics.commentCount
                        }
                }
            }

            if (part.contains(COMMENT_ARG)) {
                val currentPage = page
                ids.mapIndexed { index, id ->
                    launch {
                        val idx = currentPage * 50 + index
                        log("sending comment request for video #$idx at ${System.currentTimeMillis() - startTime}")
                        val response = RetrofitBuilder.youtube.getComments(id).body()!!.items
                        log("getting comment for video #$idx at ${System.currentTimeMillis() - startTime}")
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
            }
            log("point to the next page #$page at ${System.currentTimeMillis() - startTime}")
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
    val url: String
        get() = YOUTUBE_W + id
    lateinit var title: String
    lateinit var channelTitle: String
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
