package com.flixvora.data.network

import com.flixvora.data.model.*
import retrofit2.http.*

/**
 * Retrofit surface for official MovieBox endpoints reached through the local
 * proxy's generic `/api-proxy/` passthrough, which signs and forwards each
 * call. Every path and parameter set here was verified against the live API.
 */
interface ExtendedApi {

    // ---- Discovery & browse ----
    @GET("api-proxy/wefeed-mobile-bff/tab/ranking-list")
    suspend fun getRankingList(
        @Query("type") type: String? = null,
        @Query("page") page: Int = 1,
        @Query("perPage") perPage: Int = 30
    ): RankingResponse

    @GET("api-proxy/wefeed-mobile-bff/subject-api/filter-items")
    suspend fun getFilterOptions(
        @Query("categoryId") categoryId: Int = 2
    ): FilterItemsResponse

    @POST("api-proxy/wefeed-mobile-bff/subject-api/genre-top")
    suspend fun getGenreTop(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): GenreTopResponse

    @POST("api-proxy/wefeed-mobile-bff/subject-api/daily-movie-rec")
    suspend fun getDailyMovieRec(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): HomeListResponse

    @GET("api-proxy/wefeed-mobile-bff/tab-operating")
    suspend fun getTabOperating(
        @Query("tabId") tabId: Int,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30
    ): TabOperatingResponse

    // ---- Community posts (Shorts) ----
    @GET("api-proxy/wefeed-mobile-bff/post/list-trending/sport")
    suspend fun getShortsFeed(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): PostListResponse

    // ---- Live TV ----
    @GET("api-proxy/wefeed-mobile-bff/live/tv-country-list")
    suspend fun getTvCountries(): TvCountryResponse

    @GET("api-proxy/wefeed-mobile-bff/live/tv-channel-list-by-country")
    suspend fun getTvChannelsByCountry(
        @Query("countryCode") countryCode: String,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30
    ): TvChannelResponse

    @GET("api-proxy/wefeed-mobile-bff/live/tv-channel")
    suspend fun getTvChannel(
        @Query("channelId") channelId: String
    ): TvChannelDetailResponse

    // ---- Sports ----
    @GET("api-proxy/wefeed-mobile-bff/sport/match-list")
    suspend fun getSportMatchList(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): SportMatchResponse

    @GET("api-proxy/wefeed-mobile-bff/sport/match-range-list")
    suspend fun getSportMatchRangeList(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): SportMatchResponse

    @GET("api-proxy/wefeed-mobile-bff/sport-news/trending")
    suspend fun getSportNewsTrending(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): SportNewsResponse

    // ---- Comments & social ----
    @GET("api-proxy/wefeed-mobile-bff/comment/list")
    suspend fun getComments(
        @Query("topicId") topicId: String,
        @Query("topicType") topicType: Int = 1,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): CommentListResponse

    @POST("api-proxy/wefeed-mobile-bff/comment")
    suspend fun postComment(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): CommonResponse

    @POST("api-proxy/wefeed-mobile-bff/interactive/post/like")
    suspend fun likeSubject(
        @Body request: Map<String, @JvmSuppressWildcards Any>
    ): CommonResponse

    // ---- Notifications ----
    @GET("api-proxy/wefeed-mobile-bff/message/internal/list")
    suspend fun getNotifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20
    ): HomeListResponse

    @GET("api-proxy/wefeed-mobile-bff/message/internal/has-new")
    suspend fun getUnreadCounts(): UnreadCountsResponse

    // ---- Watchlist ----
    @POST("api-proxy/wefeed-mobile-bff/subject-api/want-to-see")
    suspend fun wantToSee(
        @Body request: WantToSeeRequest
    ): CommonResponse
}
