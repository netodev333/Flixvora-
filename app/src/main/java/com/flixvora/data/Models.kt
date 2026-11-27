package com.flixvora.data

enum class MediaCategory(val label: String) {
    MOVIE("Movies"),
    SERIES("TV Shows"),
    ANIME("Anime")
}

enum class ProfileType(val id: String, val label: String) {
    MAIN("main", "Main Member"),
    KIDS("kids", "Kids Safe"),
    ADULT("adult", "Adult (18+)")
}

enum class MainNavTab(val label: String) {
    HOME("Home"),
    LIVE_TV("Live TV"),
    WATCHLIST("Watchlist"),
    DOWNLOADS("Downloads"),
    ME("Me")
}

data class AudioTrackMetadata(
    val id: String,
    val language: String,
    val code: String,
    val codec: String,
    val channels: String,
    val bitrate: String,
    val isDefault: Boolean = false
)

data class CastMember(
    val name: String,
    val role: String
)

data class EpisodeItem(
    val number: Int,
    val title: String,
    val duration: String,
    val synopsis: String,
    val sizeMb: Int
)

data class SeasonItem(
    val seasonNumber: Int,
    val label: String,
    val episodes: List<EpisodeItem>
)

data class MediaTitle(
    val id: String,
    val tmdbId: Int,
    val title: String,
    val editorialTagline: String,
    val category: MediaCategory,
    val year: Int,
    val duration: String,
    val rating: String,
    val ageRating: String,
    val quality: String,
    val genres: List<String>,
    val ottProvider: String,
    val synopsis: String,
    val posterUrl: String,
    val backdropUrl: String,
    val continueProgress: Int? = null,
    val continueEpisode: String? = null,
    val rank: Int? = null,
    val introEndSeconds: Int = 85,
    val isRestricted: Boolean = false,
    val cast: List<CastMember> = emptyList(),
    val seasons: List<SeasonItem> = emptyList(),
    val audioTracks: List<AudioTrackMetadata> = emptyList(),
    val releaseEpochMs: Long? = null
)

data class PersonalizedRecommendation(
    val item: MediaTitle,
    val matchPercent: Int,
    val reason: String
)

data class MoodCollection(
    val id: String,
    val title: String,
    val subtitle: String,
    val matchGenres: List<String>,
    val accentTag: String
)

data class ComingSoonRelease(
    val id: String,
    val title: String,
    val categoryLabel: String,
    val releaseEpochMs: Long,
    val quality: String,
    val tagline: String,
    val backdropUrl: String,
    val genres: List<String>
)

data class EpgSlot(
    val timeWindow: String,
    val title: String,
    val isNow: Boolean = false
)

data class LiveChannel(
    val id: String,
    val number: String,
    val name: String,
    val category: String,
    val currentProgram: String,
    val nextProgram: String,
    val timeWindow: String,
    val streamFormat: String,
    val progressPercent: Int,
    val previewUrl: String,
    val epgSlots: List<EpgSlot>
)

enum class DownloadStatus { DOWNLOADING, PAUSED, COMPLETED }

data class DownloadChunk(
    val chunkIndex: Int,
    val startMb: Int,
    val endMb: Int,
    val downloadedMb: Int,
    val completed: Boolean,
    val speedMbps: Float
)

data class DownloadTask(
    val id: String,
    val titleId: String,
    val title: String,
    val subtitle: String,
    val posterUrl: String,
    val sizeMb: Int,
    val progress: Int,
    val quality: String,
    val status: DownloadStatus,
    val chunks: List<DownloadChunk>
)

data class StreamSource(
    val id: String,
    val name: String,
    val provider: String,
    val protocol: String,
    val quality: String,
    val pingMs: Int,
    val bitrateMbps: Float,
    val isPrimary: Boolean = false,
    val url: String = "",
    val headers: Map<String, String> = emptyMap()
)

data class CommentItem(
    val id: String,
    val author: String,
    val timestamp: String,
    val body: String,
    val hasSpoiler: Boolean,
    val likes: Int
)

data class SmartStorageConfig(
    val wifiOnly: Boolean = true,
    val autoDeleteWatched: Boolean = false,
    val watchMinutesTotal: Int = 384,
    val mediaBufferMb: Int = 112,
    val thumbCacheMb: Int = 148,
    val feedCacheMb: Int = 64,
    val dbCacheMb: Int = 28
) {
    val totalTempCacheMb: Int
        get() = mediaBufferMb + thumbCacheMb + feedCacheMb + dbCacheMb
}

data class PlayerSession(
    val title: String,
    val subtitle: String,
    val backdropUrl: String,
    val durationSeconds: Int,
    val tmdbId: Int,
    val audioTracks: List<AudioTrackMetadata>,
    val isLive: Boolean = false,
    val introEndSeconds: Int = 85,
    /** Resolved, playable media URL (HLS/DASH/MP4). Empty means unresolved. */
    val streamUrl: String = "",
    val subtitleUrl: String? = null,
    /** Extra HTTP headers required by the CDN (Cookie, Referer, Origin...). */
    val headers: Map<String, String> = emptyMap(),
    val subjectId: String = "",
    val season: Int = 0,
    val episode: Int = 0
)

/** One real, titled row of the home feed (e.g. "Trending Movies"). */
data class CatalogRail(
    val id: String,
    val title: String,
    val items: List<MediaTitle>
)

/** Real update state sourced from GitHub Releases. */
data class AppUpdateState(
    val currentVersion: String = "",
    val latestVersion: String? = null,
    val isChecking: Boolean = false,
    val isUpdateAvailable: Boolean = false,
    val releaseUrl: String? = null,
    val apkUrl: String? = null,
    val notes: String? = null,
    val message: String? = null
)
