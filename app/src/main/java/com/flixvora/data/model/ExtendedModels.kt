package com.flixvora.data.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

// Models for the extended official-API surfaces reached through the local
// proxy passthrough. Field names mirror the live API exactly.

@JsonClass(generateAdapter = true)
data class RankingCategory(
    @Json(name = "name") val name: String = "",
    @Json(name = "type") val type: String = ""
)

@JsonClass(generateAdapter = true)
data class RankingData(
    @Json(name = "categoryList") val categoryList: List<RankingCategory> = emptyList(),
    @Json(name = "subjects") val subjects: List<MovieSubject> = emptyList(),
    @Json(name = "currentCategoryType") val currentCategoryType: String? = null,
    @Json(name = "title") val title: String? = null
)

@JsonClass(generateAdapter = true)
data class RankingResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "message") val message: String = "",
    @Json(name = "data") val data: RankingData? = null
)

@JsonClass(generateAdapter = true)
data class FilterOption(
    @Json(name = "filterType") val filterType: String = "",
    @Json(name = "filterVals") val filterVals: List<String> = emptyList(),
    @Json(name = "title") val title: String = ""
)

@JsonClass(generateAdapter = true)
data class FilterOptionGroup(
    @Json(name = "subjectType") val subjectType: Int = 1,
    @Json(name = "items") val items: List<FilterOption> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FilterItemsData(
    @Json(name = "typeList") val typeList: List<FilterOptionGroup> = emptyList()
)

@JsonClass(generateAdapter = true)
data class FilterItemsResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: FilterItemsData? = null
)

@JsonClass(generateAdapter = true)
data class GenreTopData(
    @Json(name = "desc") val desc: String = "",
    @Json(name = "list") val list: List<MovieSubject> = emptyList()
)

@JsonClass(generateAdapter = true)
data class GenreTopResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: GenreTopData? = null
)

// ---- tab-operating (used for Games and Shorts hubs) ----

@JsonClass(generateAdapter = true)
data class TabImage(
    @Json(name = "url") val url: String = "",
    @Json(name = "thumbnail") val blurHash: String? = null
)

@JsonClass(generateAdapter = true)
data class TabCustomItem(
    @Json(name = "image") val image: TabImage? = null,
    @Json(name = "content") val content: String = "",
    @Json(name = "subjectId") val subjectId: String? = null,
    @Json(name = "deepLink") val deepLink: String? = null,
    @Json(name = "subject") val subject: MovieSubject? = null
)

@JsonClass(generateAdapter = true)
data class TabCustomData(
    @Json(name = "rowCount") val rowCount: Int = 1,
    @Json(name = "items") val items: List<TabCustomItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TabOperatingItem(
    @Json(name = "type") val type: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "subjects") val subjects: List<MovieSubject> = emptyList(),
    @Json(name = "customData") val customData: TabCustomData? = null,
    @Json(name = "deepLink") val deepLink: String? = null
)

@JsonClass(generateAdapter = true)
data class TabOperatingData(
    @Json(name = "tabId") val tabId: Int = 0,
    @Json(name = "items") val items: List<TabOperatingItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TabOperatingResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: TabOperatingData? = null
)

// ---- Community posts (Shorts feed) ----

@JsonClass(generateAdapter = true)
data class PostUser(
    @Json(name = "nickname") val nickname: String = "",
    @Json(name = "avatar") val avatar: String = ""
)

@JsonClass(generateAdapter = true)
data class PostMedia(
    @Json(name = "mediaType") val mediaType: String = "",
    @Json(name = "cover") val cover: TabImage? = null
)

@JsonClass(generateAdapter = true)
data class PostStat(
    @Json(name = "likeCount") val likeCount: Int = 0,
    @Json(name = "commentCount") val commentCount: Int = 0,
    @Json(name = "shareCount") val shareCount: Int = 0
)

@JsonClass(generateAdapter = true)
data class PostItem(
    @Json(name = "postId") val postId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "content") val content: String = "",
    @Json(name = "subjectId") val subjectId: String = "0",
    @Json(name = "media") val media: PostMedia? = null,
    @Json(name = "stat") val stat: PostStat? = null,
    @Json(name = "user") val user: PostUser? = null,
    @Json(name = "cover") val cover: TabImage? = null
)

@JsonClass(generateAdapter = true)
data class PostListData(
    @Json(name = "items") val items: List<PostItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class PostListResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: PostListData? = null
)

// ---- Sports ----

@JsonClass(generateAdapter = true)
data class MatchTeam(
    @Json(name = "name") val name: String = "",
    @Json(name = "avatar") val avatar: String = ""
)

@JsonClass(generateAdapter = true)
data class SportMatch(
    @Json(name = "id") val id: String = "",
    @Json(name = "team1") val team1: MatchTeam? = null,
    @Json(name = "team2") val team2: MatchTeam? = null,
    @Json(name = "league") val league: String = "",
    @Json(name = "status") val status: String = "",
    @Json(name = "timeDesc") val timeDesc: String = "",
    @Json(name = "startTime") val startTime: String = "",
    @Json(name = "playPath") val playPath: String? = null
)

@JsonClass(generateAdapter = true)
data class SportMatchData(
    @Json(name = "list") val list: List<SportMatch> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SportMatchResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: SportMatchData? = null
)

@JsonClass(generateAdapter = true)
data class SportNewsItem(
    @Json(name = "id") val id: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "summary") val summary: String = "",
    @Json(name = "cover") val cover: TabImage? = null,
    @Json(name = "detailUrl") val detailUrl: String? = null,
    @Json(name = "category") val category: String? = null
)

@JsonClass(generateAdapter = true)
data class SportNewsData(
    @Json(name = "items") val items: List<SportNewsItem> = emptyList()
)

@JsonClass(generateAdapter = true)
data class SportNewsResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: SportNewsData? = null
)

// ---- Live TV ----

@JsonClass(generateAdapter = true)
data class TvCountry(
    @Json(name = "countryCode") val countryCode: String = "",
    @Json(name = "countryName") val countryName: String = "",
    @Json(name = "countryFlag") val countryFlag: String = ""
)

@JsonClass(generateAdapter = true)
data class TvCountryData(
    @Json(name = "items") val items: List<TvCountry> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TvCountryResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: TvCountryData? = null
)

@JsonClass(generateAdapter = true)
data class TvChannel(
    @Json(name = "channelId") val channelId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "countryCode") val countryCode: String = "",
    @Json(name = "countryName") val countryName: String = "",
    @Json(name = "countryFlag") val countryFlag: String = "",
    @Json(name = "languageCodes") val languageCodes: List<String> = emptyList(),
    @Json(name = "streams") val streams: List<TvStream> = emptyList()
) {
    val playUrl: String? get() = streams.firstOrNull { it.url.isNotBlank() }?.url
}

/**
 * The live-TV endpoint caps every page at 10 items regardless of the requested
 * pageSize, and reports the real total here. Pagination has to follow
 * [hasMore]/[totalCount] rather than page size.
 */
@JsonClass(generateAdapter = true)
data class TvPager(
    // The backend sends page/nextPage as JSON strings ("1"), while the counts are
    // numbers. Typing these as Int makes Moshi reject the whole response.
    @Json(name = "page") val page: String = "1",
    @Json(name = "perPage") val perPage: Int = 10,
    @Json(name = "totalCount") val totalCount: Int = 0,
    @Json(name = "hasMore") val hasMore: Boolean = false,
    @Json(name = "nextPage") val nextPage: String = ""
)

@JsonClass(generateAdapter = true)
data class TvChannelData(
    @Json(name = "items") val items: List<TvChannel> = emptyList(),
    @Json(name = "pager") val pager: TvPager? = null
)

@JsonClass(generateAdapter = true)
data class TvChannelResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: TvChannelData? = null
)

@JsonClass(generateAdapter = true)
data class TvStream(
    @Json(name = "streamId") val streamId: String = "",
    @Json(name = "url") val url: String = "",
    @Json(name = "urlType") val urlType: String = ""
)

@JsonClass(generateAdapter = true)
data class TvChannelDetail(
    @Json(name = "channelId") val channelId: String = "",
    @Json(name = "title") val title: String = "",
    @Json(name = "streams") val streams: List<TvStream> = emptyList()
)

@JsonClass(generateAdapter = true)
data class TvChannelDetailResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: TvChannelDetail? = null
)

// ---- Notifications ----

@JsonClass(generateAdapter = true)
data class UnreadCounts(
    @Json(name = "interactiveCnt") val interactiveCnt: Int = 0,
    @Json(name = "systemCnt") val systemCnt: Int = 0,
    @Json(name = "likeCnt") val likeCnt: Int = 0,
    @Json(name = "commentCnt") val commentCnt: Int = 0,
    @Json(name = "newFollowerCnt") val newFollowerCnt: Int = 0,
    @Json(name = "dmUnreadCnt") val dmUnreadCnt: Int = 0
)

@JsonClass(generateAdapter = true)
data class UnreadCountsResponse(
    @Json(name = "code") val code: Int = 0,
    @Json(name = "data") val data: UnreadCounts? = null
)
