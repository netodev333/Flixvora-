package com.flixvora.ui.viewmodel

import android.app.Application
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flixvora.BuildConfig
import com.flixvora.data.local.FavoriteEntity
import com.flixvora.data.local.HistoryEntity
import com.flixvora.data.local.MovieDatabase
import com.flixvora.data.model.CommentItem
import com.flixvora.data.model.FilterOption
import com.flixvora.data.model.PostItem
import com.flixvora.data.model.RankingCategory
import com.flixvora.data.model.SportMatch
import com.flixvora.data.model.SportNewsItem
import com.flixvora.data.model.CategorySection
import com.flixvora.data.model.Episode
import com.flixvora.data.model.MovieSubject
import com.flixvora.data.model.PlayInfo
import com.flixvora.data.model.StreamItem
import com.flixvora.data.model.SubtitleItem
import com.flixvora.data.network.MovieBoxRetrofitClient
import com.flixvora.data.preferences.PreferencesManager
import com.flixvora.data.repository.MovieRepository
import com.flixvora.download.DownloadService
import com.flixvora.download.DownloadStatus
import com.flixvora.download.DownloadTask
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import android.widget.Toast
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import androidx.compose.runtime.snapshotFlow

data class ActiveDownload(
    val id: Long,
    val title: String,
    val progress: Int,
    val downloadedBytes: Long,
    val totalBytes: Long,
    val status: Int = android.app.DownloadManager.STATUS_RUNNING,
    val reason: Int = 0,
    val url: String = "",
    val posterUrl: String = ""
)

/**
 * Single source of truth for watch-history row IDs. Writes and reads must always
 * go through these helpers, otherwise resume/progress lookups silently miss.
 */
object HistoryId {
    fun forSubject(subjectId: String, isTvShow: Boolean, season: Int, episode: Int): String =
        if (isTvShow) "${subjectId}_S${season}_E${episode}" else subjectId

    fun forEpisode(subjectId: String, season: Int, episode: Int): String =
        "${subjectId}_S${season}_E${episode}"
}

/**
 * AniFam rows are always persisted with the "anifam_" prefix, so classification
 * must be prefix-based. The old "all characters are digits" heuristic
 * misclassified numeric MovieBox ids as AniFam and mixed up favorites/history.
 */
internal fun isAniFamId(id: String): Boolean = id.startsWith("anifam_")

/** Catalogue id of the 18+ tab. Only read when the user opts in. */
internal const val ADULT_CATEGORY_ID = 9

private data class FeedPayload(
    val trending: List<MovieSubject>,
    val categories: List<MovieSubject>,
    val sections: List<CategorySection>,
    val adultPool: List<MovieSubject>
)

class MovieViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = PreferencesManager(application)
    private val database = MovieDatabase.getDatabase(application)
    val repository = MovieRepository(
        MovieBoxRetrofitClient.getApi(prefs),
        database.movieDao(),
        prefs
    )

    // Developer and Sandbox / Lockout State
    var isDeveloper by mutableStateOf(prefs.isDeveloper)
        private set

    var isEmulatorDetected by mutableStateOf(false)
        private set

    var isLocked by mutableStateOf(false)
        private set

    var passwordAttempts by mutableStateOf(0)
    var showPasswordDialog by mutableStateOf(false)
    var customMcc by mutableStateOf(prefs.spCode.ifEmpty { "40401" })
    var customCountryIso by mutableStateOf(prefs.localIso.ifEmpty { "in" })
    var mockHost by mutableStateOf(prefs.mockHost)
    var defaultStreamQuality by mutableStateOf(prefs.defaultStreamQuality)
    var defaultDownloadQuality by mutableStateOf(prefs.defaultDownloadQuality)
    var subtitleLanguage by mutableStateOf(prefs.subtitleLanguage)
        private set

    fun setSubtitleLanguagePref(language: String) {
        subtitleLanguage = language
        prefs.subtitleLanguage = language
    }

    fun setDefaultStreamQualityPref(quality: String) {
        defaultStreamQuality = quality
        prefs.defaultStreamQuality = quality
    }

    fun setDefaultDownloadQualityPref(quality: String) {
        defaultDownloadQuality = quality
        prefs.defaultDownloadQuality = quality
    }

    /**
     * Picks the subtitle track matching the Settings preference instead of
     * always defaulting to English, falling back to the first track when the
     * preferred language is not offered.
     */
    private fun pickPreferredSubtitle(list: List<SubtitleItem>): SubtitleItem? {
        if (list.isEmpty()) return null
        val wanted = subtitleLanguage.trim()
        if (wanted.isNotEmpty()) {
            list.find { it.language.contains(wanted, ignoreCase = true) }?.let { return it }
            // "English" vs "en" / "eng": compare the first two letters.
            val short = wanted.take(2)
            list.find { it.language.take(2).equals(short, ignoreCase = true) }?.let { return it }
        }
        return list.firstOrNull()
    }

    fun selectStreamByPreference(streamList: List<com.flixvora.data.model.StreamItem>, preference: String): com.flixvora.data.model.StreamItem? {
        if (streamList.isEmpty()) return null
        if (preference.equals("Auto", ignoreCase = true)) {
            val autoStream = streamList.find { it.quality.equals("Auto", ignoreCase = true) }
            if (autoStream != null) return autoStream
            return selectHighestQualityStream(streamList)
        }
        if (preference.equals("Highest", ignoreCase = true)) {
            return selectHighestQualityStream(streamList)
        }
        val prefDigits = preference.filter { it.isDigit() }.toIntOrNull()
        val exact = streamList.find { 
            val qDigits = it.quality.filter { c -> c.isDigit() }.toIntOrNull()
            qDigits == prefDigits
        }
        if (exact != null) return exact
        if (prefDigits != null) {
            val nonAutoStreams = streamList.filter { it.quality.filter { c -> c.isDigit() }.isNotEmpty() }
            if (nonAutoStreams.isNotEmpty()) {
                return nonAutoStreams.minByOrNull { 
                    val q = it.quality.filter { c -> c.isDigit() }.toIntOrNull() ?: 1080
                    Math.abs(q - prefDigits)
                }
            }
        }
        return streamList.firstOrNull()
    }

    fun selectHighestQualityStream(streamList: List<com.flixvora.data.model.StreamItem>): com.flixvora.data.model.StreamItem? {
        if (streamList.isEmpty()) return null
        return streamList.maxByOrNull { stream ->
            val q = stream.quality.lowercase()
            when {
                q.contains("4k") || q.contains("2160") -> 2160
                q.contains("1080") -> 1080
                q.contains("720") -> 720
                q.contains("480") -> 480
                q.contains("360") -> 360
                else -> q.filter { it.isDigit() }.toIntOrNull() ?: 0
            }
        } ?: streamList.firstOrNull()
    }

    // UI Dashboard State
    var selectedCategoryTab by mutableStateOf(2) // Defaults to Movies (ID 2)
    var selectedSubject by mutableStateOf<MovieSubject?>(null)
    var originalSubjectDetail by mutableStateOf<MovieSubject?>(null)
    var maxSeasonLimitConstraint by mutableStateOf<Int?>(null)
    var selectedResource by mutableStateOf<com.flixvora.data.model.ResourceDetector?>(null)
    var seasonsList by mutableStateOf<List<Int>>(emptyList())
    var selectedSeason by mutableStateOf(1)
    var episodesList by mutableStateOf<List<Episode>>(emptyList())
    var isLoadingEpisodes by mutableStateOf(false)
    var detailRecommendations by mutableStateOf<List<MovieSubject>>(emptyList())
    // Streams resolved for the detail sheet, so the quality ladder can show the
    // real resolutions and file sizes before the viewer starts playback.
    var detailStreams by mutableStateOf<List<StreamItem>>(emptyList())
    
    // Player / Streaming Resolver State
    var currentEpisode by mutableStateOf<Episode?>(null)
    var playInfo by mutableStateOf<PlayInfo?>(null)
    var selectedStream by mutableStateOf<StreamItem?>(null)
    var selectedSubtitle by mutableStateOf<SubtitleItem?>(null)
    var isPlaying by mutableStateOf(false)
    var playbackProgressMs by mutableStateOf(0L)
    var isPlaybackLoading by mutableStateOf(false)
    var mediaDurationMs by mutableStateOf(0L)
    var isLiveTvPlaying by mutableStateOf(false)


    // Data lists
    private val _trendingMovies = MutableStateFlow<List<MovieSubject>>(emptyList())
    val trendingMovies: StateFlow<List<MovieSubject>> = _trendingMovies.asStateFlow()

    private val _categoryMovies = MutableStateFlow<List<MovieSubject>>(emptyList())
    val categoryMovies: StateFlow<List<MovieSubject>> = _categoryMovies.asStateFlow()
    
    private val categoryCache = mutableMapOf<Int, List<MovieSubject>>()

    private val _categorySections = MutableStateFlow<List<CategorySection>>(emptyList())
    val categorySections: StateFlow<List<CategorySection>> = _categorySections.asStateFlow()

    private val categorySectionsCache = mutableMapOf<Int, List<CategorySection>>()

    /**
     * Adult (18+) visibility, mirrored from preferences so Compose recomposes
     * when it changes. Off means adult rows are absent from every feed; on means
     * they are shuffled into the same rails as normal content, never a separate
     * section.
     */
    var adultContentEnabled by mutableStateOf(prefs.adultContentEnabled)
        private set

    fun applyAdultContentSetting(enabled: Boolean) {
        if (adultContentEnabled == enabled) return
        prefs.adultContentEnabled = enabled
        adultContentEnabled = enabled
        // Cached feeds were built under the old rule; drop them so the next read
        // reflects the new one instead of waiting out the cache TTL.
        repository.clearFeedCaches()
        categoryCache.clear()
        categorySectionsCache.clear()
        _trendingMovies.value = emptyList()
        _categoryMovies.value = emptyList()
        _categorySections.value = emptyList()
        _animeRails.value = emptyList()
        loadDashboardContent()
    }

    // Anime shelves merged into the main feed so both catalogues are always on.
    private val _animeRails = MutableStateFlow<List<CategorySection>>(emptyList())
    val animeRails: StateFlow<List<CategorySection>> = _animeRails.asStateFlow()

    private val _searchResults = MutableStateFlow<List<MovieSubject>>(emptyList())
    val searchResults: StateFlow<List<MovieSubject>> = _searchResults.asStateFlow()

    var isSearching by mutableStateOf(false)
    var isDashboardLoading by mutableStateOf(false)
    var searchQuery by mutableStateOf("")

    private val _searchSuggestions = MutableStateFlow<List<String>>(emptyList())
    val searchSuggestions: StateFlow<List<String>> = _searchSuggestions.asStateFlow()

    // Both catalogues are merged, so favourites and history are unfiltered:
    // there is no "current provider" whose items should be hidden.
    val favoritesList: StateFlow<List<FavoriteEntity>> =
        repository.favorites.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val historyList: StateFlow<List<HistoryEntity>> =
        repository.history.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Provider switching is gone; both catalogues feed one list. `isAniFam`
    // now answers "is the subject on screen from the anime catalogue?", which
    // is what every playback and download branch actually needs.
    val isAniFam: Boolean
        get() = selectedSubject?.id?.startsWith("anifam_") == true

    private val aniFamApi: com.flixvora.data.network.AniFamApi
        get() = com.flixvora.data.network.AniFamRetrofitClient.getApi(prefs)

    // AniFam dynamic genres
    var aniFamGenres by mutableStateOf<List<com.flixvora.data.model.AniFamGenreItem>>(emptyList())
        private set
    var isLoadingAniFamGenres by mutableStateOf(false)
        private set

    // Dynamic Categories
    private val defaultMovieBoxCategories = listOf(
        2 to "Home",
        5 to "Series",
        8 to "Anime",
        18 to "Asian"
    )

    private val defaultAniFamCategories = listOf(
        0 to "Home"
    )

    val defaultCategories: List<Pair<Int, String>>
        get() = if (isAniFam) defaultAniFamCategories else defaultMovieBoxCategories
    
    private val _activeCategories = MutableStateFlow(defaultMovieBoxCategories)
    val activeCategories: StateFlow<List<Pair<Int, String>>> = _activeCategories

    // Download Cache State (Kept in memory for instant screen opening)
    var downloadedFiles by mutableStateOf<List<java.io.File>>(emptyList())
    var activeDownloads by mutableStateOf<List<ActiveDownload>>(emptyList())
    var inAppDownloads by mutableStateOf<List<ActiveDownload>>(emptyList())
    var queuedDownloads by mutableStateOf<List<ActiveDownload>>(emptyList())

    /**
     * Downloads run strictly one at a time, owned by [DownloadService] (a
     * foreground service) so they survive backgrounding and process death. Every
     * request is appended to the service's FIFO queue, so tapping "download all"
     * no longer saturates the network by running every episode in parallel.
     */
    private val downloadIdGenerator = java.util.concurrent.atomic.AtomicLong(System.currentTimeMillis())

    var isDownloadScanLoaded by mutableStateOf(false)
    var playingDownloadedFile by mutableStateOf<java.io.File?>(null)

    // App In-App Update State
    data class AppUpdateInfo(
        val latestVersionName: String,
        val latestVersionCode: Int,
        val changelog: String,
        val downloadUrl: String,
        val isForceUpdate: Boolean,
        val sha256: String = ""
    )

    var availableUpdateInfo by mutableStateOf<AppUpdateInfo?>(null)
    var isCheckingUpdate by mutableStateOf(false)
    var isDownloadingUpdate by mutableStateOf(false)
    var updateDownloadProgress by mutableStateOf(0)
    var updateStatusMessage by mutableStateOf("")

    init {
        if (prefs.mockHost.contains("10.0.2.2") || prefs.mockHost.contains("run.app") || prefs.mockHost.contains("ais-dev-") || prefs.mockHost.isEmpty()) {
            prefs.mockHost = "http://127.0.0.1:3000"
            mockHost = "http://127.0.0.1:3000"
        }
        loadActiveCategories()
        checkLockoutStatus()
        loadDashboardContent()
        loadSearchSuggestions()
        scanDownloadedFiles()
        checkForUpdates(isManualCheck = false)
        observeDownloadService()
    }

    // ---- Extended discovery / social state (official API passthrough) ----
    var rankingCategories by mutableStateOf<List<RankingCategory>>(emptyList())
    var rankingType by mutableStateOf<String?>(null)
    var rankingList by mutableStateOf<List<MovieSubject>>(emptyList())
    var isLoadingRanking by mutableStateOf(false)

    var genreOptions by mutableStateOf<List<String>>(emptyList())
    var genreFilterResults by mutableStateOf<List<MovieSubject>>(emptyList())
    var isLoadingGenreFilter by mutableStateOf(false)
    var genreFilterName by mutableStateOf<String?>(null)

    var shortsFeed by mutableStateOf<List<PostItem>>(emptyList())
    var isLoadingShorts by mutableStateOf(false)

    var sportMatches by mutableStateOf<List<SportMatch>>(emptyList())
    var sportNews by mutableStateOf<List<SportNewsItem>>(emptyList())
    var isLoadingSports by mutableStateOf(false)

    var gameCenterItems by mutableStateOf<List<MovieSubject>>(emptyList())
    var isLoadingGames by mutableStateOf(false)

    var comments by mutableStateOf<List<CommentItem>>(emptyList())
    // Comments written while the backend refused the write (no account system).
    var localComments by mutableStateOf<List<CommentItem>>(emptyList())
    var isLoadingComments by mutableStateOf(false)
    var isSubjectLiked by mutableStateOf(false)
    var isInWatchlist by mutableStateOf(false)

    fun loadRankings(type: String? = rankingType) {
        if (isLoadingRanking) return
        isLoadingRanking = true
        viewModelScope.launch {
            if (rankingCategories.isEmpty()) rankingCategories = repository.getRankingCategories()
            val chosen = type ?: rankingCategories.firstOrNull()?.type
            rankingType = chosen
            rankingList = repository.getRankingList(chosen)
            isLoadingRanking = false
        }
    }

    fun loadGenreFilter(genre: String) {
        genreFilterName = genre
        if (isLoadingGenreFilter) return
        isLoadingGenreFilter = true
        viewModelScope.launch {
            if (genreOptions.isEmpty()) {
                genreOptions = repository.getFilterOptions(2)
                    .firstOrNull { it.filterType == "genre" }?.filterVals ?: emptyList()
            }
            genreFilterResults = repository.getGenreTop(genre)
            isLoadingGenreFilter = false
        }
    }

    fun loadShorts() {
        if (isLoadingShorts) return
        isLoadingShorts = true
        viewModelScope.launch {
            shortsFeed = repository.getShortsFeed()
            isLoadingShorts = false
        }
    }

    fun loadSports() {
        if (isLoadingSports) return
        isLoadingSports = true
        viewModelScope.launch {
            sportMatches = repository.getSportMatches()
            sportNews = repository.getSportNews()
            isLoadingSports = false
        }
    }

    fun loadGames() {
        if (isLoadingGames) return
        isLoadingGames = true
        viewModelScope.launch {
            gameCenterItems = repository.getTabOperatingSubjects(11)
            isLoadingGames = false
        }
    }

    fun loadComments(subjectId: String) {
        isLoadingComments = true
        isSubjectLiked = false
        viewModelScope.launch {
            comments = repository.getComments(subjectId)
            isLoadingComments = false
        }
    }

    fun submitComment(subjectId: String, content: String) {
        if (content.isBlank()) return
        viewModelScope.launch {
            val ok = repository.postComment(subjectId, content.trim())
            // No account system: when the backend rejects the write we keep the
            // comment locally so the action still lands somewhere visible.
            if (ok) {
                loadComments(subjectId)
            } else {
                localComments = localComments + CommentItem(
                    id = "local-" + System.currentTimeMillis(),
                    content = content.trim(),
                    userName = "You",
                    avatar = "",
                    createTime = "Just now"
                )
                Toast.makeText(getApplication(), "Comment posted", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun toggleLike(subjectId: String) {
        viewModelScope.launch {
            val wanted = !isSubjectLiked
            // Optimistic: the heart flips immediately. A backend rejection no
            // longer blocks the gesture, because there is no account to sign in.
            isSubjectLiked = wanted
            repository.likeSubject(subjectId)
        }
    }

    fun toggleWatchlist(subjectId: String) {
        viewModelScope.launch {
            val action = if (isInWatchlist) 2 else 1
            val wanted = !isInWatchlist
            isInWatchlist = wanted
            // Mirror to the local favourites table so My List works offline and
            // without any sign-in, then try the remote call best-effort.
            runCatching {
                if (wanted) {
                    val subject = selectedSubject
                    repository.insertFavorite(
                        com.flixvora.data.local.FavoriteEntity(
                            id = subjectId,
                            name = subject?.name ?: "",
                            poster = subject?.poster ?: "",
                            isTvShow = subject?.isTvShow ?: false,
                            rating = subject?.rating,
                            year = subject?.year
                        )
                    )
                } else {
                    repository.deleteFavorite(subjectId)
                }
            }
            repository.addToWatchlist(subjectId, action = action)
            Toast.makeText(
                getApplication(),
                if (wanted) "Added to My List" else "Removed from My List",
                Toast.LENGTH_SHORT
            ).show()
        }
    }

    fun shareSubject(subjectId: String, title: String, context: android.content.Context) {
        // Shared link uses the public web detail URL; the app also handles the
        // oneroom:// scheme so recipients with Flixvora open it natively.
        val url = "https://moviebox.ph/detail/$subjectId"
        val intent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(android.content.Intent.EXTRA_TEXT, "$title\n$url")
        }
        context.startActivity(android.content.Intent.createChooser(intent, "Share via"))
    }

    fun checkForUpdates(isManualCheck: Boolean = false, onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch(Dispatchers.IO) {
            if (isManualCheck) isCheckingUpdate = true
            try {
                // Update manifest is fetched from GitHub Releases API.
                // Primary: latest release; Fallback: releases list (for versionCode comparison).
                val endpoints = listOfNotNull(
                    BuildConfig.UPDATE_MANIFEST_URL,        // https://api.github.com/repos/netodev333/Flixvora-/releases/latest
                    BuildConfig.UPDATE_MANIFEST_URL_2       // https://api.github.com/repos/netodev333/Flixvora-/releases
                ).filter { it.isNotBlank() }

                val client = okhttp3.OkHttpClient.Builder()
                    .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .readTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                    .build()

                var bodyString: String? = null
                var lastError: String? = null
                val attempts = if (isManualCheck) 2 else 3

                // Try each endpoint in order until we find a valid APK
                for (endpoint in endpoints) {
                    for (attempt in 0 until attempts) {
                        try {
                            val finalUrl = "$endpoint?t=${System.currentTimeMillis()}"
                            val request = okhttp3.Request.Builder()
                                .url(finalUrl)
                                .header("User-Agent", "FlixvoraApp")
                                .header("Accept", "application/vnd.github.v3+json")
                                .header("Cache-Control", "no-cache")
                                .build()

                            val response = client.newCall(request).execute()
                            if (!response.isSuccessful) {
                                lastError = "HTTP ${response.code} from ${java.net.URI(endpoint).host}"
                            }
                            if (response.isSuccessful) {
                                val candidate = response.body?.string()
                                if (!candidate.isNullOrBlank()) {
                                    bodyString = candidate
                                    break // Found valid response, exit retry loop
                                }
                            }
                        } catch (e: Exception) {
                            lastError = "${e.javaClass.simpleName}: ${e.message ?: "no detail"} (${runCatching { java.net.URI(endpoint).host }.getOrNull()})"
                        }
                        if (attempt < attempts - 1) {
                            kotlinx.coroutines.delay(3_000L * (attempt + 1))
                        }
                    }
                    if (!bodyString.isNullOrBlank()) break // Found valid response, exit endpoint loop
                }

                if (!bodyString.isNullOrBlank()) {
                    val json = org.json.JSONObject(bodyString)

                    // Parse GitHub release format
                    val tagName = json.optString("tag_name", "").trim()  // e.g., "v9.9"
                    val remoteVersionName = tagName.removePrefix("v")
                    val remoteVersionCode = remoteVersionName.toIntOrNull() ?: 0
                    val forceUpdate = json.optBoolean("forceUpdate", false)
                    val changelog = json.optString("body", "").trim()  // GitHub uses "body"
                    if (changelog.isEmpty()) changelog = "Bug fixes and improvements."

                    // Find APK asset in release
                    val assets = json.optJSONArray("assets")
                    var downloadUrl = ""
                    var sha256 = ""
                    if (assets != null) {
                        for (i in 0 until assets.length()) {
                            val asset = assets.getJSONObject(i)
                            val name = asset.optString("name", "")
                            if (name.endsWith(".apk", ignoreCase = true)) {
                                downloadUrl = asset.optString("browser_download_url", "")
                                break
                            }
                        }
                    }

    private fun isVersionGreater(remote: String, local: String): Boolean {
        if (remote.isEmpty() || local.isEmpty()) return false
        val rParts = remote.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toIntOrNull() }
        val lParts = local.split(".").mapNotNull { it.filter { char -> char.isDigit() }.toIntOrNull() }
        val maxLen = maxOf(rParts.size, lParts.size)
        for (i in 0 until maxLen) {
            val rVal = rParts.getOrElse(i) { 0 }
            val lVal = lParts.getOrElse(i) { 0 }
            if (rVal > lVal) return true
            if (rVal < lVal) return false
        }
        return false
    }

    fun dismissUpdate() {
        prefs.lastUpdateDismissedMillis = System.currentTimeMillis()
        // Don't clear availableUpdateInfo if APK is already downloaded
        // so user can still install it later
        val apkFile = getCachedUpdateApk()
        if (!apkFile.exists()) {
            availableUpdateInfo = null
        }
    }

    /**
     * Check if a valid update APK exists in cache
     */
    fun getCachedUpdateApk(): java.io.File {
        val app = getApplication<android.app.Application>()
        val cacheDir = app.externalCacheDir ?: app.cacheDir
        return java.io.File(cacheDir, "update.apk")
    }

    /**
     * Check if cached update APK is valid (exists and has reasonable size)
     */
    fun hasValidCachedUpdate(): Boolean {
        val apkFile = getCachedUpdateApk()
        return apkFile.exists() && apkFile.length() > 1024 * 1024 // At least 1MB
    }

    /**
     * Install the already-downloaded update APK from cache
     */
    fun installCachedUpdate(context: android.content.Context) {
        val apkFile = getCachedUpdateApk()
        if (apkFile.exists()) {
            installApk(context, apkFile)
        } else {
            Toast.makeText(context, "Update file not found. Please download again.", Toast.LENGTH_SHORT).show()
        }
    }

    fun dismissUpdate() {
        prefs.lastUpdateDismissedMillis = System.currentTimeMillis()
        // Don't clear availableUpdateInfo if APK is already downloaded
        // so user can still install it later
        val apkFile = getCachedUpdateApk()
        if (!apkFile.exists()) {
            availableUpdateInfo = null
        }
    }

    fun downloadAndInstallUpdate(context: android.content.Context, downloadUrl: String, expectedSha256: String = "") {
        if (isDownloadingUpdate) return
        if (!downloadUrl.startsWith("https://", ignoreCase = true)) {
            Toast.makeText(context, "Refusing to download update over an insecure connection.", Toast.LENGTH_LONG).show()
            return
        }
        isDownloadingUpdate = true
        updateDownloadProgress = 0
        updateStatusMessage = "Connecting..."

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder()
                    .followRedirects(true)
                    .followSslRedirects(true)
                    .build()

                val request = okhttp3.Request.Builder()
                    .url(downloadUrl)
                    .header("User-Agent", "FlixvoraApp")
                    .build()

                val response = client.newCall(request).execute()
                if (!response.isSuccessful) {
                    withContext(Dispatchers.Main) {
                        isDownloadingUpdate = false
                        Toast.makeText(context, "Download failed: HTTP ${response.code}", Toast.LENGTH_SHORT).show()
                    }
                    return@launch
                }

                val body = response.body ?: throw Exception("Empty response body")
                val contentLength = body.contentLength()
                val cacheDir = context.externalCacheDir ?: context.cacheDir
                val apkFile = java.io.File(cacheDir, "update.apk")
                if (apkFile.exists()) apkFile.delete()

                // Stream to a temp file, then verify, then atomically promote to
                // update.apk so a half-written download can never be installed.
                val tempFile = java.io.File(cacheDir, "update.apk.part")
                if (tempFile.exists()) tempFile.delete()

                val inputStream = body.byteStream()
                val outputStream = java.io.FileOutputStream(tempFile)
                val buffer = ByteArray(8 * 1024)
                var bytesRead: Int
                var totalBytesRead: Long = 0

                while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                    outputStream.write(buffer, 0, bytesRead)
                    totalBytesRead += bytesRead
                    if (contentLength > 0) {
                        val progress = ((totalBytesRead * 100) / contentLength).toInt()
                        withContext(Dispatchers.Main) {
                            updateDownloadProgress = progress
                            updateStatusMessage = "$progress%"
                        }
                    }
                }
                outputStream.flush()
                outputStream.close()
                inputStream.close()

                // Reject truncated/corrupt downloads before ever handing the file
                // to the system installer.
                if (contentLength > 0 && tempFile.length() != contentLength) {
                    tempFile.delete()
                    withContext(Dispatchers.Main) {
                        isDownloadingUpdate = false
                        updateStatusMessage = ""
                        Toast.makeText(context, "Update download incomplete. Please retry.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                when (val reason = apkRejectionReason(context, tempFile)) {
                    null -> Unit
                    ApkRejection.SIGNATURE -> {
                        tempFile.delete()
                        withContext(Dispatchers.Main) {
                            isDownloadingUpdate = false
                            updateStatusMessage = ""
                            Toast.makeText(
                                context,
                                "This update is signed with a different key than the installed app. Please uninstall Flixvora once, then install the new APK from the release page.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }
                    else -> {
                        tempFile.delete()
                        withContext(Dispatchers.Main) {
                            isDownloadingUpdate = false
                            updateStatusMessage = ""
                            Toast.makeText(
                                context,
                                "Update file is not a valid Flixvora build (${reason.label}).",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        return@launch
                    }
                }
                // If the manifest publishes a checksum, it must match exactly.
                if (expectedSha256.isNotBlank() && !verifySha256(tempFile, expectedSha256)) {
                    tempFile.delete()
                    withContext(Dispatchers.Main) {
                        isDownloadingUpdate = false
                        updateStatusMessage = ""
                        Toast.makeText(context, "Update file failed integrity check. Please retry.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }
                if (!tempFile.renameTo(apkFile)) {
                    tempFile.delete()
                    withContext(Dispatchers.Main) {
                        isDownloadingUpdate = false
                        updateStatusMessage = ""
                        Toast.makeText(context, "Could not prepare update file. Please retry.", Toast.LENGTH_LONG).show()
                    }
                    return@launch
                }

                withContext(Dispatchers.Main) {
                    isDownloadingUpdate = false
                    updateStatusMessage = "Ready to install"
                    installApk(context, apkFile)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDownloadingUpdate = false
                    Toast.makeText(context, "Update download failed. Check your internet connection.", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private enum class ApkRejection(val label: String) {
        UNREADABLE("unreadable archive"),
        WRONG_PACKAGE("wrong package"),
        SIGNATURE("signature mismatch")
    }

    /**
     * Returns null when [apkFile] is a Flixvora build signed with the installed
     * app's certificate; otherwise the specific reason it was rejected.
     */
    private fun apkRejectionReason(context: android.content.Context, apkFile: java.io.File): ApkRejection? {
        return try {
            val pm = context.packageManager
            @Suppress("DEPRECATION")
            val info = pm.getPackageArchiveInfo(apkFile.absolutePath, android.content.pm.PackageManager.GET_SIGNATURES)
                ?: return ApkRejection.UNREADABLE
            if (info.packageName != context.packageName) return ApkRejection.WRONG_PACKAGE

            // Reject any APK not signed with the exact same certificate as the
            // installed app: prevents a swapped/tampered build from being installed.
            @Suppress("DEPRECATION")
            val candidateSigs = info.signatures
            @Suppress("DEPRECATION")
            val installedSigs = pm.getPackageInfo(context.packageName, android.content.pm.PackageManager.GET_SIGNATURES).signatures
            if (candidateSigs.isNullOrEmpty() || installedSigs.isNullOrEmpty()) return ApkRejection.UNREADABLE
            val candidate = candidateSigs.map { it.toCharsString() }.toSet()
            val installed = installedSigs.map { it.toCharsString() }.toSet()
            if (candidate.any { it in installed }) null else ApkRejection.SIGNATURE
        } catch (_: Exception) {
            ApkRejection.UNREADABLE
        }
    }

    private fun installApk(context: android.content.Context, apkFile: java.io.File) {
        try {
            // Check if "Install Unknown Apps" permission is granted on Android 8.0+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                if (!context.packageManager.canRequestPackageInstalls()) {
                    val settingsIntent = android.content.Intent(
                        android.provider.Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        android.net.Uri.parse("package:${context.packageName}")
                    ).apply {
                        flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK
                    }
                    context.startActivity(settingsIntent)
                    Toast.makeText(context, "Please allow 'Install unknown apps' and tap Update again", Toast.LENGTH_LONG).show()
                    return
                }
            }

            val apkUri = androidx.core.content.FileProvider.getUriForFile(
                context,
                "${context.packageName}.provider",
                apkFile
            )

            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            }

            // Explicitly grant read permissions to all candidate activities
            val resInfoList = context.packageManager.queryIntentActivities(
                intent,
                android.content.pm.PackageManager.MATCH_DEFAULT_ONLY
            )
            for (resolveInfo in resInfoList) {
                val pkgName = resolveInfo.activityInfo.packageName
                context.grantUriPermission(
                    pkgName,
                    apkUri,
                    android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
            }

            // Prioritize standard Android/Google Package Installers over OEM phone managers
            val preferredInstallers = listOf(
                "com.google.android.packageinstaller",
                "com.android.packageinstaller"
            )
            val systemInstaller = resInfoList.firstOrNull { it.activityInfo.packageName in preferredInstallers }
            if (systemInstaller != null) {
                intent.setClassName(systemInstaller.activityInfo.packageName, systemInstaller.activityInfo.name)
            }

            context.startActivity(intent)
        } catch (e: Exception) {
            Toast.makeText(context, "Failed to launch installer: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    fun scanDownloadedFiles() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val app = getApplication<android.app.Application>()
                val candidateDirs = listOfNotNull(
                    java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "Flixvora"),
                    app.getExternalFilesDir(android.os.Environment.DIRECTORY_MOVIES)?.let { java.io.File(it, "Flixvora") },
                    java.io.File(app.filesDir, "Flixvora"),
                    java.io.File(android.os.Environment.getExternalStoragePublicDirectory(android.os.Environment.DIRECTORY_MOVIES), "Genga World")
                )
                val files = candidateDirs
                    .filter { it.exists() && it.isDirectory }
                    .flatMap { it.listFiles()?.toList() ?: emptyList() }
                    .filter { it.extension.equals("mp4", ignoreCase = true) }
                    .distinctBy { it.name }
                    .sortedBy { it.name }

                withContext(Dispatchers.Main) {
                    downloadedFiles = files
                    isDownloadScanLoaded = true
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    isDownloadScanLoaded = true
                }
            }
        }
    }
    
    fun loadAniFamGenres() {
        if (aniFamGenres.isEmpty()) {
            aniFamGenres = repository.defaultAniFamGenres
        }
        viewModelScope.launch {
            isLoadingAniFamGenres = true
            try {
                val list = repository.getAniFamGenres()
                if (list.isNotEmpty()) {
                    aniFamGenres = list
                }
            } catch (e: Exception) {
                if (aniFamGenres.isEmpty()) {
                    aniFamGenres = repository.defaultAniFamGenres
                }
            } finally {
                isLoadingAniFamGenres = false
            }
        }
    }

    fun loadActiveCategories() {
        val saved = prefs.activeCategories
        if (saved != null) {
            try {
                val list = saved.split(",").mapNotNull { 
                    val parts = it.split(":")
                    if (parts.size == 2) {
                        val id = parts[0].toIntOrNull() ?: return@mapNotNull null
                        val name = if (id == 2 || id == 0) "Home" else parts[1]
                        id to name
                    } else null
                }
                // Ensure default categories are always present at the start
                val combined = defaultCategories.toMutableList()
                list.forEach { savedCat ->
                    if (combined.none { it.first == savedCat.first }) {
                        combined.add(savedCat)
                    }
                }
                _activeCategories.value = combined
            } catch (e: Exception) {
                _activeCategories.value = defaultCategories
            }
        } else {
            _activeCategories.value = defaultCategories
        }
    }
    
    fun addCategory(id: Int, name: String) {
        val current = _activeCategories.value.toMutableList()
        if (current.none { it.first == id }) {
            current.add(id to name)
            saveActiveCategories(current)
        }
    }
    
    fun removeCategory(id: Int) {
        val current = _activeCategories.value.toMutableList()
        if (defaultCategories.none { it.first == id }) {
            current.removeAll { it.first == id }
            categoryCache.remove(id)
            saveActiveCategories(current)

            // If user removed the currently active tab, switch back to the first available category
            if (selectedCategoryTab == id) {
                val fallbackCategory = current.firstOrNull()?.first ?: defaultCategories.first().first
                loadCategoryList(fallbackCategory)
            }
        }
    }
    
    private fun saveActiveCategories(list: List<Pair<Int, String>>) {
        _activeCategories.value = list
        // Save only the extra categories
        val extraCategories = list.filter { cat -> defaultCategories.none { it.first == cat.first } }
        val toSave = extraCategories.joinToString(",") { "${it.first}:${it.second}" }
        prefs.activeCategories = toSave
    }

    private var suggestionsJob: kotlinx.coroutines.Job? = null

    fun loadSearchSuggestions(query: String? = null) {
        // Per-keystroke requests used to race: a slow reply for "av" could land
        // after "avengers" and overwrite it.
        suggestionsJob?.cancel()
        suggestionsJob = viewModelScope.launch {
            try {
                val suggestions = repository.getSearchSuggestions(query)
                _searchSuggestions.value = suggestions
            } catch (_: kotlinx.coroutines.CancellationException) {
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to load suggestions", e)
            }
        }
    }

    private fun checkLockoutStatus() {
        // Region/emulator gating is intentionally disabled for the Flixvora
        // build, but it must NOT silently grant developer mode: doing so
        // previously persisted is_developer=true for every user.
        isEmulatorDetected = checkIsEmulator()
        isLocked = false
    }

    private fun checkIsEmulator(): Boolean {
        val fingerprint = Build.FINGERPRINT ?: ""
        val model = Build.MODEL ?: ""
        val brand = Build.BRAND ?: ""
        val hardware = Build.HARDWARE ?: ""
        val device = Build.DEVICE ?: ""
        val product = Build.PRODUCT ?: ""

        return fingerprint.startsWith("generic") ||
                fingerprint.startsWith("unknown") ||
                model.contains("google_sdk") ||
                model.contains("Emulator") ||
                model.contains("Android SDK built for x86") ||
                brand.startsWith("generic") && device.startsWith("generic") ||
                "google_sdk" == product ||
                hardware.contains("goldfish") ||
                hardware.contains("ranchu")
    }

    fun submitDeveloperPassword(password: String): Boolean {
        // MD5 of lowercase input + salt "-321" against "031A68C3912D796E235A72EE0BF89C16"
        val salt = "-321"
        val targetHash = "031A68C3912D796E235A72EE0BF89C16"
        val saltedInput = password.lowercase() + salt
        val calculatedHash = md5Hash(saltedInput).uppercase()

        return if (calculatedHash == targetHash) {
            prefs.applyDeveloperBypass()
            isDeveloper = true
            isLocked = false
            showPasswordDialog = false
            passwordAttempts = 0
            loadDashboardContent()
            true
        } else {
            false
        }
    }

    /** Unlocks region/emulator gating without requiring the secret key. */
    fun applyAutoBypass() {
        prefs.applyDeveloperBypass()
        isDeveloper = true
        isLocked = false
        isEmulatorDetected = false
        showPasswordDialog = false
        passwordAttempts = 0
        loadDashboardContent()
    }

    private fun md5Hash(input: String): String {
        val md = MessageDigest.getInstance("MD5")
        val digest = md.digest(input.toByteArray())
        return digest.joinToString("") { String.format("%02x", it) }
    }

    /** Constant-time-ish streaming SHA-256 check used to validate a downloaded APK. */
    private fun verifySha256(file: java.io.File, expected: String): Boolean {
        return try {
            val digest = MessageDigest.getInstance("SHA-256")
            file.inputStream().use { input ->
                val buffer = ByteArray(64 * 1024)
                while (true) {
                    val read = input.read(buffer)
                    if (read <= 0) break
                    digest.update(buffer, 0, read)
                }
            }
            val actual = digest.digest().joinToString("") { String.format("%02x", it) }
            actual.equals(expected.trim(), ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    fun applyCustomSandbox(mcc: String, iso: String) {
        prefs.spCode = mcc
        prefs.localIso = iso
        prefs.localCountry = if (iso.lowercase() == "in") "India" else "United States"
        prefs.countryCode = if (iso.lowercase() == "in") "91" else "1"
        customMcc = mcc
        customCountryIso = iso

        // Clear in-memory caches to force fresh fetch for new region
        categoryCache.clear()
        categorySectionsCache.clear()
        _trendingMovies.value = emptyList()
        _categoryMovies.value = emptyList()
        _categorySections.value = emptyList()

        // Invalidate local proxy cache and guest token for the new region
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val client = okhttp3.OkHttpClient.Builder().connectTimeout(2, java.util.concurrent.TimeUnit.SECONDS).build()
                val req = okhttp3.Request.Builder().url("http://127.0.0.1:3000/clear-cache").build()
                client.newCall(req).execute().close()
            } catch (_: Exception) {}
            withContext(Dispatchers.Main) {
                loadDashboardContent()
                loadSearchSuggestions()
            }
        }
    }

    fun resetSandbox() {
        prefs.clearDeveloperBypass()
        isDeveloper = false
        checkLockoutStatus()
    }

    fun updateMockHost(host: String) {
        prefs.mockHost = host
        mockHost = host
        loadDashboardContent()
    }

    /**
     * Randomly mixes adult rows into a normal list, keeping the normal order.
     * Adult content never gets its own rail — it lands inside the rails the user
     * already sees. The pool is spread evenly across the list and capped at a
     * third of it, so a short rail cannot end up mostly adult.
     */
    private fun interleaveAdult(
        normal: List<MovieSubject>,
        adultPool: List<MovieSubject>
    ): List<MovieSubject> {
        if (!adultContentEnabled || adultPool.isEmpty() || normal.isEmpty()) return normal
        val capacity = (normal.size / 3).coerceAtLeast(1)
        val chosen = adultPool.shuffled().take(capacity)
        val out = ArrayList<MovieSubject>(normal.size + chosen.size)
        // Stride spreads the chosen items over the whole rail instead of
        // clustering them at the end.
        val stride = (normal.size + chosen.size) / (chosen.size + 1)
        var nextAdultAt = stride.coerceAtLeast(1)
        var nextAdult = 0
        normal.forEachIndexed { index, item ->
            out.add(item)
            if (nextAdult < chosen.size && index + 1 >= nextAdultAt) {
                out.add(chosen[nextAdult])
                nextAdult++
                nextAdultAt = index + 1 + stride.coerceAtLeast(1)
            }
        }
        while (nextAdult < chosen.size) {
            out.add(chosen[nextAdult])
            nextAdult++
        }
        return out
    }

    private fun interleaveAdultSections(
        sections: List<CategorySection>,
        adultPool: List<MovieSubject>
    ): List<CategorySection> {
        if (!adultContentEnabled || adultPool.isEmpty() || sections.isEmpty()) return sections
        val pending = adultPool.shuffled().toMutableList()
        if (pending.isEmpty()) return sections
        // Spread the pool round-robin so every rail gets a share instead of the
        // first rail swallowing all of it.
        val buckets = Array(sections.size) { mutableListOf<MovieSubject>() }
        pending.forEachIndexed { i, item -> buckets[i % buckets.size].add(item) }
        return sections.mapIndexed { i, section ->
            if (section.items.any { it.isAdult }) {
                section
            } else {
                section.copy(items = interleaveAdult(section.items, buckets[i]))
            }
        }
    }

    fun loadDashboardContent() {
        val cachedSec = repository.getCachedCategorySections(selectedCategoryTab)
        val cachedCat = repository.getCachedCategoryList(selectedCategoryTab)
        val cachedTrending = repository.getCachedTrending()

        if (cachedSec.isNotEmpty() || cachedCat.isNotEmpty()) {
            _categorySections.value = cachedSec
            _categoryMovies.value = cachedCat
            _trendingMovies.value = cachedTrending
            isDashboardLoading = false
        } else {
            isDashboardLoading = true
        }

        viewModelScope.launch {
            try {
                // Fetch trending and categories in parallel using coroutineScope
                val feed = coroutineScope {
                    val trendingDeferred = async(Dispatchers.IO) { repository.getTrending(1, 20) }
                    val categoriesDeferred = async(Dispatchers.IO) { repository.getCategoryList(selectedCategoryTab, 1, 50) }
                    val sectionsDeferred = async(Dispatchers.IO) { repository.getCategorySections(selectedCategoryTab, 1, 50) }
                    // Only when the user opted in: the 18+ catalogue is used purely
                    // as a mixing pool. It never becomes its own rail.
                    val adultDeferred = async(Dispatchers.IO) {
                        if (adultContentEnabled) repository.getCategoryList(ADULT_CATEGORY_ID, 1, 30)
                        else emptyList()
                    }
                    FeedPayload(
                        trending = trendingDeferred.await(),
                        categories = categoriesDeferred.await(),
                        sections = sectionsDeferred.await(),
                        adultPool = adultDeferred.await()
                    )
                }
                val trending = feed.trending
                val categories = feed.categories
                val sections = feed.sections
                val adultPool = feed.adultPool

                // Anime shelves ride along with the main feed; a failure here must
                // never blank out the MovieBox catalogue, so it is fire-and-forget.
                launch(Dispatchers.IO) {
                    val rails = repository.getAnimeRails()
                    if (rails.isNotEmpty()) _animeRails.value = rails
                }

                val adultIds = adultPool.map { it.id }.toSet()
                val mixedCategories = interleaveAdult(
                    categories.filterNot { it.id in adultIds }, adultPool
                )
                val mixedSections = interleaveAdultSections(
                    sections.map { sec -> sec.copy(items = sec.items.filterNot { it.id in adultIds }) }
                        .filter { it.items.isNotEmpty() },
                    adultPool
                )
                val mixedTrending = interleaveAdult(
                    trending.filterNot { it.id in adultIds }, adultPool
                )

                if (mixedTrending.isNotEmpty()) _trendingMovies.value = mixedTrending
                if (mixedCategories.isNotEmpty()) {
                    categoryCache[selectedCategoryTab] = mixedCategories
                    _categoryMovies.value = mixedCategories
                }
                if (mixedSections.isNotEmpty()) {
                    categorySectionsCache[selectedCategoryTab] = mixedSections
                    _categorySections.value = mixedSections
                }

                // Pre-warm poster images for all top items immediately during splash
                val app = getApplication<Application>()
                val imgLoader = coil.Coil.imageLoader(app)
                (trending.take(15) + categories.take(20)).forEach { item ->
                    if (item.poster.isNotEmpty()) {
                        val optimalUrl = com.flixvora.util.ImageOptimizer.getOptimalUrl(item.poster, 360)
                        val req = coil.request.ImageRequest.Builder(app)
                            .data(optimalUrl)
                            .memoryCacheKey(optimalUrl)
                            .diskCacheKey(optimalUrl)
                            .size(width = 330, height = 480)
                            .precision(coil.size.Precision.INEXACT)
                            .build()
                        imgLoader.enqueue(req)
                    }
                }

                // Prioritize visible poster loading - let Compose handle UI cards directly without network congestion
                // Background category prefetch is delayed by 3 seconds so visible screen posters load at 100% bandwidth
                launch {
                    kotlinx.coroutines.delay(3000)
                    _activeCategories.value.filter { it.first != selectedCategoryTab }.forEach { cat ->
                        try {
                            val catData = repository.getCategoryList(cat.first, 1, 30)
                            categoryCache[cat.first] = catData
                        } catch (_: Exception) {}
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to load dashboard content", e)
            } finally {
                isDashboardLoading = false
            }
        }
    }

    fun loadCategoryList(categoryId: Int) {
        selectedCategoryTab = categoryId
        
        // Immediately show cached data if available (RAM or Disk) for instant 0ms switching
        val cachedCat = repository.getCachedCategoryList(categoryId)
        val cachedSec = repository.getCachedCategorySections(categoryId)
        if (cachedCat.isNotEmpty()) {
            categoryCache[categoryId] = cachedCat
            _categoryMovies.value = cachedCat
        }
        if (cachedSec.isNotEmpty()) {
            categorySectionsCache[categoryId] = cachedSec
            _categorySections.value = cachedSec
        }

        viewModelScope.launch {
            try {
                val (categories, sections) = coroutineScope {
                    val catDeferred = async(Dispatchers.IO) { repository.getCategoryList(categoryId, 1, 50) }
                    val secDeferred = async(Dispatchers.IO) { repository.getCategorySections(categoryId, 1, 50) }
                    Pair(catDeferred.await(), secDeferred.await())
                }
                if (categories.isNotEmpty()) {
                    categoryCache[categoryId] = categories
                    _categoryMovies.value = categories
                }
                if (sections.isNotEmpty()) {
                    categorySectionsCache[categoryId] = sections
                    _categorySections.value = sections
                }
                // Pre-fetch details and warm up posters in the background
                val app = getApplication<Application>()
                val imgLoader = coil.Coil.imageLoader(app)
                categories.take(24).forEach { item ->
                    launch { repository.getDetail(item.id) }
                    if (item.poster.isNotEmpty()) {
                        launch(Dispatchers.IO) {
                            val optimalUrl = com.flixvora.util.ImageOptimizer.getOptimalUrl(item.poster, 360)
                            val request = coil.request.ImageRequest.Builder(app)
                                .data(optimalUrl)
                                .memoryCacheKey(optimalUrl)
                                .diskCacheKey(optimalUrl)
                                .size(width = 330, height = 480)
                                .precision(coil.size.Precision.INEXACT)
                                .build()
                            imgLoader.enqueue(request)
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to load category list", e)
            }
        }
    }

    var searchPage by mutableStateOf(1)
        private set
    var isSearchLoadingMore by mutableStateOf(false)
        private set
    var canSearchLoadMore by mutableStateOf(true)
        private set

    private var searchJob: kotlinx.coroutines.Job? = null

    fun searchMovies(query: String, immediate: Boolean = false) {
        searchQuery = query
        searchPage = 1
        canSearchLoadMore = true
        searchJob?.cancel()

        if (query.isBlank()) {
            _searchResults.value = emptyList()
            isSearching = false
            loadSearchSuggestions(null)
            return
        }

        searchJob = viewModelScope.launch {
            if (!immediate) {
                kotlinx.coroutines.delay(350)
            }
            isSearching = true
            try {
                loadSearchSuggestions(query)
                val results = repository.search(query, 1)
                _searchResults.value = results
                // Ask for more while either backend still returns a full page.
                canSearchLoadMore = !repository.lastSearchExhausted
            } catch (_: kotlinx.coroutines.CancellationException) {
            } catch (e: Exception) {
                _searchResults.value = emptyList()
            } finally {
                isSearching = false
            }
        }
    }

    fun loadMoreSearchResults() {
        val query = searchQuery
        if (query.isBlank() || isSearchLoadingMore || !canSearchLoadMore) return
        isSearchLoadingMore = true
        viewModelScope.launch {
            try {
                val nextPage = searchPage + 1
                val more = repository.search(query, nextPage)
                if (more.isNotEmpty()) {
                    searchPage = nextPage
                    val current = _searchResults.value.toMutableList()
                    val existingIds = current.map { it.id }.toSet()
                    val filtered = more.filter { !existingIds.contains(it.id) }
                    if (filtered.isNotEmpty()) {
                        current.addAll(filtered)
                        _searchResults.value = current
                    }
                    // A page that adds nothing new means the backend is repeating
                    // itself; stop instead of paging forever.
                    canSearchLoadMore = !repository.lastSearchExhausted && filtered.isNotEmpty()
                } else {
                    canSearchLoadMore = false
                }
            } catch (_: Exception) {
                canSearchLoadMore = false
            } finally {
                isSearchLoadingMore = false
            }
        }
    }

    private fun setupSubjectWithDetail(detail: MovieSubject) {
        selectedSubject = detail
        originalSubjectDetail = detail
        
        val autoMatch = detail.resourceDetectors.find { detector ->
            val titleMatch = Regex("\\[(.*?)\\]|\\((.*?)\\)").find(detail.name)
            val extractedLang = titleMatch?.groupValues?.find { v -> v.isNotEmpty() && v != titleMatch.value } ?: ""
            extractedLang.isNotEmpty() && detector.name.contains(extractedLang, ignoreCase = true)
        } ?: detail.resourceDetectors.firstOrNull()
        selectedResource = autoMatch

        if (!isAniFam && autoMatch != null && autoMatch.resourceId.startsWith("dub_")) {
            val dubSubjectId = autoMatch.resourceId.substringAfter("dub_")
            val dubDetail = repository.getCachedDetail(dubSubjectId)
            if (dubDetail != null) {
                selectedSubject = dubDetail
                val rawSeasons = if (dubDetail.isTvShow) dubDetail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                seasonsList = maxSeasonLimitConstraint?.let { limit -> rawSeasons.filter { it <= limit } } ?: rawSeasons
                if (!seasonsList.contains(selectedSeason)) {
                    selectedSeason = seasonsList.firstOrNull() ?: 1
                }
                episodesList = repository.getCachedEpisodes(dubSubjectId, selectedSeason)
            } else {
                val rawSeasons = if (detail.isTvShow) detail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                seasonsList = maxSeasonLimitConstraint?.let { limit -> rawSeasons.filter { it <= limit } } ?: rawSeasons
                if (!seasonsList.contains(selectedSeason)) {
                    selectedSeason = seasonsList.firstOrNull() ?: 1
                }
                episodesList = repository.getCachedEpisodes(detail.id, selectedSeason)
            }
        } else {
            val rawSeasons = if (detail.isTvShow) detail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
            seasonsList = maxSeasonLimitConstraint?.let { limit -> rawSeasons.filter { it <= limit } } ?: rawSeasons
            if (!seasonsList.contains(selectedSeason)) {
                selectedSeason = seasonsList.firstOrNull() ?: 1
            }
            episodesList = repository.getCachedEpisodes(detail.id, selectedSeason)
        }
    }

    fun selectSubjectById(subjectId: String) {
        viewModelScope.launch {
            val detail = repository.getDetail(subjectId)
            if (detail != null) selectSubject(detail)
        }
    }

    fun selectSubject(subject: MovieSubject) {
        maxSeasonLimitConstraint = parseMaxSeasonFromName(subject.name)
        currentEpisode = null
        playInfo = null
        detailStreams = emptyList()
        detailRecommendations = emptyList()
        episodesList = emptyList()
        seasonsList = emptyList()
        selectedSeason = 1
        isLoadingEpisodes = subject.isTvShow

        if (subject.id.startsWith("tmdb_") && !isAniFam) {
            selectedSubject = subject
            originalSubjectDetail = subject
            viewModelScope.launch {
                try {
                    val searchResults = repository.search(subject.name)
                    val matched = searchResults.firstOrNull { 
                        it.name.equals(subject.name, ignoreCase = true)
                    } ?: searchResults.firstOrNull {
                        it.name.contains(subject.name, ignoreCase = true) || subject.name.contains(it.name, ignoreCase = true)
                    } ?: searchResults.firstOrNull()

                    if (matched != null) {
                        val fullDetail = repository.getDetail(matched.id)
                        if (fullDetail != null) {
                            setupSubjectWithDetail(fullDetail)
                            selectedSubject = fullDetail
                            originalSubjectDetail = fullDetail
                            if (fullDetail.isTvShow) {
                                episodesList = repository.getEpisodes(fullDetail.id, selectedSeason)
                            }
                            try {
                                detailRecommendations = repository.getDetailRec(fullDetail.id)
                            } catch (_: Exception) {}
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MovieViewModel", "Error resolving TMDB subject to MovieBox", e)
                } finally {
                    isLoadingEpisodes = false
                }
            }
            return
        }

        // Check cache for instant load
        val cachedDetail = repository.getCachedDetail(subject.id)
        if (cachedDetail != null) {
            setupSubjectWithDetail(cachedDetail)
        } else {
            selectedSubject = subject
            originalSubjectDetail = subject
            val initialSeasons = if (subject.isTvShow) (1..subject.totalSeasons).toList() else emptyList()
            seasonsList = maxSeasonLimitConstraint?.let { limit -> initialSeasons.filter { it <= limit } } ?: initialSeasons
            val autoMatch = subject.resourceDetectors.find { detector ->
                val titleMatch = Regex("\\[(.*?)\\]|\\((.*?)\\)").find(subject.name)
                val extractedLang = titleMatch?.groupValues?.find { v -> v.isNotEmpty() && v != titleMatch.value } ?: ""
                extractedLang.isNotEmpty() && detector.name.contains(extractedLang, ignoreCase = true)
            } ?: subject.resourceDetectors.firstOrNull()
            selectedResource = autoMatch
        }
        
        viewModelScope.launch {
            try {
                val recs = repository.getDetailRec(subject.id)
                detailRecommendations = recs
                // Warm up recommendation poster images immediately like Vega
                val app = getApplication<Application>()
                val imgLoader = coil.Coil.imageLoader(app)
                recs.take(10).forEach { recMovie ->
                    if (recMovie.poster.isNotEmpty()) {
                        val req = coil.request.ImageRequest.Builder(app)
                            .data(recMovie.poster)
                            .memoryCacheKey(recMovie.poster)
                            .diskCacheKey(recMovie.poster)
                            .size(width = 330, height = 480)
                            .precision(coil.size.Precision.INEXACT)
                            .build()
                        imgLoader.enqueue(req)
                    }
                }
            } catch (e: Exception) {
                // Ignore failure for recommendations
            }
            try {
                val fullDetail = repository.getDetail(subject.id)
                if (fullDetail != null) {
                    setupSubjectWithDetail(fullDetail)
                    if (isAniFam) {
                        detailRecommendations = repository.getDetailRec(subject.id)
                    }
                    
                    val currentAutoMatch = selectedResource
                    if (!isAniFam && currentAutoMatch != null && currentAutoMatch.resourceId.startsWith("dub_")) {
                        val dubSubjectId = currentAutoMatch.resourceId.substringAfter("dub_")
                        val dubDetail = repository.getDetail(dubSubjectId)
                        if (dubDetail != null) {
                            selectedSubject = dubDetail
                            val rawSeasons = if (dubDetail.isTvShow) dubDetail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                            seasonsList = maxSeasonLimitConstraint?.let { limit -> rawSeasons.filter { it <= limit } } ?: rawSeasons
                            if (!seasonsList.contains(selectedSeason)) {
                                selectedSeason = seasonsList.firstOrNull() ?: 1
                            }
                            episodesList = repository.getEpisodes(dubSubjectId, selectedSeason)
                        }
                    } else if (fullDetail.isTvShow) {
                        episodesList = repository.getEpisodes(fullDetail.id, selectedSeason)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to fetch full subject detail", e)
            } finally {
                isLoadingEpisodes = false
            }
            loadDetailStreams(subject.id)
        }
    }

    /**
     * Resolves the stream list for the open subject so the detail sheet can list
     * the resolutions and file sizes the API really offers. Failures leave the
     * list empty, which hides the section rather than showing invented rows.
     */
    private fun loadDetailStreams(subjectId: String) {
        viewModelScope.launch {
            try {
                val season = if (selectedSubject?.isTvShow == true) selectedSeason else 1
                val episode = if (selectedSubject?.isTvShow == true) {
                    episodesList.firstOrNull()?.episodeNumber ?: 1
                } else 1
                val info = repository.getPlayInfo(
                    subjectId,
                    season,
                    episode,
                    resourceId = selectedResource?.resourceId
                )
                if (selectedSubject?.id == subjectId) {
                    detailStreams = info.streamList
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to prefetch detail streams", e)
            }
        }
    }

    fun loadEpisodesForSeason(season: Int) {
        selectedSeason = season
        val activeSubjectId = if (!isAniFam && selectedResource?.resourceId?.startsWith("dub_") == true) {
            selectedResource?.resourceId?.substringAfter("dub_") ?: (originalSubjectDetail?.id ?: selectedSubject?.id ?: return)
        } else {
            originalSubjectDetail?.id ?: selectedSubject?.id ?: return
        }
        isLoadingEpisodes = true
        viewModelScope.launch {
            try {
                episodesList = repository.getEpisodes(activeSubjectId, season)
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to load episodes for season $season", e)
            } finally {
                isLoadingEpisodes = false
            }
            loadDetailStreams(activeSubjectId)
        }
    }

    fun selectEpisode(episode: Episode) {
        currentEpisode = episode
        val subject = selectedSubject ?: return
        isPlaybackLoading = true
        viewModelScope.launch {
            try {
                val resolvedPlayInfo = repository.getPlayInfo(
                    subject.id,
                    episode.seasonNumber,
                    episode.episodeNumber,
                    resourceId = selectedResource?.resourceId
                )
                // Fetch history before setting playInfo to prevent race condition with ExoPlayer
                val historyId = HistoryId.forEpisode(subject.id, episode.seasonNumber, episode.episodeNumber)
                val existingProgress = repository.getHistoryById(historyId)
                if (existingProgress != null) {
                    playbackProgressMs = existingProgress.seeTime
                    mediaDurationMs = existingProgress.totalTime
                } else {
                    playbackProgressMs = 0L
                    mediaDurationMs = 0L
                }

                selectedStream = selectStreamByPreference(resolvedPlayInfo.streamList, defaultStreamQuality)
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else (pickPreferredSubtitle(resolvedPlayInfo.subTitleList))
                isPlaying = true
                playInfo = resolvedPlayInfo
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to fetch episode play info", e)
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    private fun safeToast(context: android.content.Context, msg: String, duration: Int = Toast.LENGTH_SHORT) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            try {
                Toast.makeText(context, msg, duration).show()
            } catch (_: Throwable) {}
        }
    }

    fun downloadSingleEpisode(
        context: android.content.Context,
        subject: MovieSubject,
        episode: com.flixvora.data.model.Episode
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val resId = selectedResource?.resourceId
                val downloadResId = when {
                    selectedResource?.resourceId?.contains("dub", ignoreCase = true) == true -> "dub"
                    else -> "sub"
                }
                val playInfoResult = repository.getPlayInfo(
                    subject.id,
                    episode.seasonNumber,
                    episode.episodeNumber,
                    quality = defaultStreamQuality,
                    resourceId = if (isAniFam) downloadResId else resId
                )
                val stream = selectStreamByPreference(playInfoResult.streamList, defaultStreamQuality)
                    ?: selectHighestQualityStream(playInfoResult.streamList)
                    ?: playInfoResult.streamList.firstOrNull()

                val streamUrl = stream?.url ?: ""
                if (streamUrl.isEmpty()) {
                    safeToast(context, "No stream found for download")
                    return@launch
                }

                val safeTitle = "${subject.name} S${episode.seasonNumber}E${episode.episodeNumber.toString().padStart(2, '0')}"
                val cookies = stream?.cookie ?: ""
                startDownload(context, subject, safeTitle, streamUrl, cookies)
            } catch (e: Exception) {
                safeToast(context, "Download failed: ${e.message}")
            }
        }
    }

    fun startDownload(
        context: android.content.Context,
        subject: MovieSubject,
        safeTitle: String,
        streamUrl: String,
        cookies: String = ""
    ) {
        val cleanTitle = safeTitle.replace(Regex("[^a-zA-Z0-9.-]"), "_")
        val isHls = streamUrl.contains(".m3u8") || isAniFam
        enqueueDownload(context, subject, cleanTitle, streamUrl, cookies, isHls)
    }

    /**
     * Hands a download to [DownloadService]. The service runs it in the
     * foreground so it survives backgrounding and process death, keeps the
     * partial file, and resumes from where it stopped instead of restarting.
     */
    private fun enqueueDownload(
        context: android.content.Context,
        subject: MovieSubject,
        safeTitle: String,
        streamUrl: String,
        cookies: String,
        isHls: Boolean,
        quiet: Boolean = false
    ) {
        if (streamUrl.isBlank()) {
            if (!quiet) safeToast(context, "No stream found for download")
            return
        }
        val id = downloadIdGenerator.incrementAndGet()
        DownloadService.enqueue(
            context.applicationContext,
            DownloadTask(
                id = id,
                title = safeTitle,
                streamUrl = streamUrl,
                cookies = cookies,
                isHls = isHls,
                posterUrl = subject.poster
            )
        )
        if (!quiet) safeToast(context, "Download started...", Toast.LENGTH_SHORT)
    }

    fun cancelInAppDownload(id: Long) {
        DownloadService.cancel(getApplication<android.app.Application>(), id)
    }

    fun pauseInAppDownload(id: Long) {
        DownloadService.pause(getApplication<android.app.Application>(), id)
    }

    fun resumeInAppDownload(id: Long) {
        DownloadService.resume(getApplication<android.app.Application>(), id)
    }

    /** Mirrors the foreground service's task list into the UI state. */
    private fun observeDownloadService() {
        viewModelScope.launch(Dispatchers.Main) {
            DownloadService.snapshots.collect { snapshot ->
                val tasks = snapshot.tasks
                inAppDownloads = tasks
                    .filter { it.status == DownloadStatus.RUNNING || it.status == DownloadStatus.PAUSED || it.status == DownloadStatus.FAILED }
                    .map { it.toActiveDownload() }
                queuedDownloads = tasks
                    .filter { it.status == DownloadStatus.QUEUED }
                    .map { it.toActiveDownload() }
            }
        }
    }

    private fun DownloadTask.toActiveDownload() = ActiveDownload(
        id = id,
        title = title,
        progress = progress,
        downloadedBytes = downloadedBytes,
        totalBytes = totalBytes,
        status = when (status) {
            DownloadStatus.QUEUED -> android.app.DownloadManager.STATUS_PENDING
            DownloadStatus.PAUSED -> android.app.DownloadManager.STATUS_PAUSED
            DownloadStatus.FAILED -> android.app.DownloadManager.STATUS_FAILED
            else -> android.app.DownloadManager.STATUS_RUNNING
        },
        reason = if (status == DownloadStatus.PAUSED) android.app.DownloadManager.PAUSED_WAITING_FOR_NETWORK else 0,
        url = streamUrl,
        posterUrl = posterUrl
    )

    fun downloadEpisodesBatch(
        context: android.content.Context,
        episodes: List<com.flixvora.data.model.Episode>,
        targetQuality: String? = null,
        onProgressUpdate: ((Int, Int) -> Unit)? = null
    ) {
        val subject = selectedSubject ?: return
        if (episodes.isEmpty()) return

        safeToast(context, "Preparing ${episodes.size} download(s)...", Toast.LENGTH_SHORT)
        viewModelScope.launch(Dispatchers.IO) {
            val resId = selectedResource?.resourceId
            val downloadResId = when {
                selectedResource?.resourceId?.contains("dub", ignoreCase = true) == true -> "dub"
                else -> "sub"
            }
            // Queue in season/episode order so the Download screen reads top-to-bottom.
            val ordered = episodes.sortedWith(
                compareBy({ it.seasonNumber }, { it.episodeNumber })
            )
            var queuedCount = 0
            for (ep in ordered) {
                try {
                    val playInfoResult = repository.getPlayInfo(
                        subject.id,
                        ep.seasonNumber,
                        ep.episodeNumber,
                        quality = targetQuality ?: "1080p",
                        resourceId = if (isAniFam) downloadResId else resId
                    )
                    val stream = (if (targetQuality != null) selectStreamByPreference(playInfoResult.streamList, targetQuality) else null)
                        ?: selectHighestQualityStream(playInfoResult.streamList)
                        ?: playInfoResult.streamList.firstOrNull()
                    val streamUrl = stream?.url ?: ""
                    if (streamUrl.isNotEmpty() && !streamUrl.contains("b164fbfb4347792950bdfbfb563d39d9.mp4")) {
                        val safeTitle = "${subject.name} S${ep.seasonNumber}E${ep.episodeNumber.toString().padStart(2, '0')}"
                        val cookies = stream?.cookie ?: ""
                        // Enqueue silently: the pump shows one progress row per episode.
                        enqueueDownload(context, subject, safeTitle, streamUrl, cookies, isAniFam || streamUrl.contains(".m3u8"), quiet = true)
                        queuedCount++
                        withContext(Dispatchers.Main) {
                            onProgressUpdate?.invoke(queuedCount, episodes.size)
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MovieViewModel", "Batch download failed for ep: ${ep.episodeNumber}", e)
                }
            }
            withContext(Dispatchers.Main) {
                if (queuedCount > 0) {
                    safeToast(context, "Queued $queuedCount episode(s) — downloading one at a time", Toast.LENGTH_LONG)
                } else {
                    safeToast(context, "No downloadable streams found", Toast.LENGTH_SHORT)
                }
            }
        }
    }

    fun selectMovieDirect() {
        val subject = selectedSubject ?: return
        if (subject.id.startsWith("tmdb_") && !isAniFam) {
            isPlaybackLoading = true
            viewModelScope.launch {
                try {
                    val searchResults = repository.search(subject.name)
                    val matched = searchResults.firstOrNull { 
                        it.name.equals(subject.name, ignoreCase = true)
                    } ?: searchResults.firstOrNull {
                        it.name.contains(subject.name, ignoreCase = true) || subject.name.contains(it.name, ignoreCase = true)
                    } ?: searchResults.firstOrNull()

                    if (matched != null) {
                        val fullDetail = repository.getDetail(matched.id)
                        if (fullDetail != null) {
                            setupSubjectWithDetail(fullDetail)
                            selectedSubject = fullDetail
                            selectMovieDirect()
                            return@launch
                        }
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MovieViewModel", "Failed to resolve tmdb for direct play", e)
                } finally {
                    isPlaybackLoading = false
                }
            }
            return
        }
        isPlaybackLoading = true
        viewModelScope.launch {
            try {
                val resolvedPlayInfo = repository.getPlayInfo(
                    subject.id,
                    0,
                    0,
                    resourceId = selectedResource?.resourceId
                )
                // Fetch history before setting playInfo to prevent race condition with ExoPlayer
                val existingProgress = repository.getHistoryById(subject.id)
                if (existingProgress != null) {
                    playbackProgressMs = existingProgress.seeTime
                    mediaDurationMs = existingProgress.totalTime
                } else {
                    playbackProgressMs = 0L
                    mediaDurationMs = 0L
                }

                selectedStream = selectStreamByPreference(resolvedPlayInfo.streamList, defaultStreamQuality)
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else (pickPreferredSubtitle(resolvedPlayInfo.subTitleList))
                isPlaying = true
                playInfo = resolvedPlayInfo
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to fetch movie play info", e)
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    fun playDirect(subject: MovieSubject) {
        if (subject.id.startsWith("tmdb_") && !isAniFam) {
            selectSubject(subject)
            return
        }
        selectedSubject = subject
        originalSubjectDetail = subject
        currentEpisode = null
        playInfo = null
        selectedStream = null
        selectedSubtitle = null
        selectedSeason = 1
        episodesList = emptyList()
        seasonsList = emptyList()
        selectedResource = subject.resourceDetectors.firstOrNull()
        detailRecommendations = emptyList()
        isPlaybackLoading = true

        viewModelScope.launch {
            try {
                val detail = repository.getDetail(subject.id) ?: subject
                selectedSubject = detail
                originalSubjectDetail = detail
                if (detail.resourceDetectors.isNotEmpty()) {
                    selectedResource = detail.resourceDetectors.firstOrNull()
                }
                try {
                    detailRecommendations = repository.getDetailRec(detail.id)
                } catch (_: Exception) {}

                if (detail.isTvShow) {
                    val rawSeasons = detail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted()
                    seasonsList = if (rawSeasons.isNotEmpty()) rawSeasons else listOf(1)
                    selectedSeason = seasonsList.first()
                    val eps = repository.getEpisodes(detail.id, selectedSeason)
                    episodesList = eps
                    val firstEp = eps.firstOrNull()
                    if (firstEp != null) {
                        currentEpisode = firstEp
                        val resolvedPlayInfo = repository.getPlayInfo(
                            detail.id,
                            firstEp.seasonNumber,
                            firstEp.episodeNumber,
                            resourceId = selectedResource?.resourceId
                        )
                        val existingProgress = repository.getHistoryById(
                            HistoryId.forEpisode(detail.id, firstEp.seasonNumber, firstEp.episodeNumber)
                        )
                        if (existingProgress != null) {
                            playbackProgressMs = existingProgress.seeTime
                            mediaDurationMs = existingProgress.totalTime
                        } else {
                            playbackProgressMs = 0L
                            mediaDurationMs = 0L
                        }
                        playInfo = resolvedPlayInfo
                        selectedStream = selectStreamByPreference(resolvedPlayInfo.streamList, defaultStreamQuality)
                        val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                        selectedSubtitle = if (isDubSelected) null else (pickPreferredSubtitle(resolvedPlayInfo.subTitleList))
                        isPlaying = true
                    } else {
                        safeToast(getApplication(), "No episodes available for this title")
                    }
                } else {
                    val resolvedPlayInfo = repository.getPlayInfo(
                        detail.id, 0, 0,
                        resourceId = selectedResource?.resourceId
                    )
                    val existingProgress = repository.getHistoryById(detail.id)
                    if (existingProgress != null) {
                        playbackProgressMs = existingProgress.seeTime
                        mediaDurationMs = existingProgress.totalTime
                    } else {
                        playbackProgressMs = 0L
                        mediaDurationMs = 0L
                    }
                    playInfo = resolvedPlayInfo
                    selectedStream = selectStreamByPreference(resolvedPlayInfo.streamList, defaultStreamQuality)
                    val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                    selectedSubtitle = if (isDubSelected) null else (pickPreferredSubtitle(resolvedPlayInfo.subTitleList))
                    isPlaying = true
                }
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "playDirect failed", e)
                safeToast(getApplication(), "Unable to play this title right now")
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    fun playEpisodeDirect(subject: MovieSubject, episode: Episode) {
        selectedSubject = subject
        originalSubjectDetail = subject
        currentEpisode = episode
        selectedSeason = episode.seasonNumber
        isPlaybackLoading = true
        viewModelScope.launch {
            try {
                val detail = repository.getDetail(subject.id) ?: subject
                selectedSubject = detail
                originalSubjectDetail = detail
                selectedResource = detail.resourceDetectors.firstOrNull() ?: selectedResource
                detailRecommendations = try { repository.getDetailRec(detail.id) } catch (_: Exception) { emptyList() }
                val rawSeasons = detail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted()
                seasonsList = if (rawSeasons.isNotEmpty()) rawSeasons else listOf(episode.seasonNumber)
                selectedSeason = episode.seasonNumber
                episodesList = repository.getEpisodes(detail.id, selectedSeason)
                val resolved = repository.getPlayInfo(detail.id, episode.seasonNumber, episode.episodeNumber, resourceId = selectedResource?.resourceId)
                val existing = repository.getHistoryById(
                    HistoryId.forEpisode(detail.id, episode.seasonNumber, episode.episodeNumber)
                )
                if (existing != null) {
                    playbackProgressMs = existing.seeTime
                    mediaDurationMs = existing.totalTime
                } else {
                    playbackProgressMs = 0L
                    mediaDurationMs = 0L
                }
                playInfo = resolved
                selectedStream = selectStreamByPreference(resolved.streamList, defaultStreamQuality)
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else (pickPreferredSubtitle(resolved.subTitleList))
                isPlaying = true
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "playEpisodeDirect failed", e)
                safeToast(getApplication(), "Unable to play this episode right now")
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    fun selectLanguageResource(resource: com.flixvora.data.model.ResourceDetector) {
        selectedResource = resource
        val originalSubject = originalSubjectDetail ?: selectedSubject ?: return
        isPlaybackLoading = true
        viewModelScope.launch {
            try {
                if (!isAniFam && resource.resourceId.startsWith("dub_")) {
                    val dubSubjectId = resource.resourceId.substringAfter("dub_")
                    val dubDetail = repository.getDetail(dubSubjectId)
                    if (dubDetail != null) {
                        selectedSubject = dubDetail
                        val rawSeasons = if (dubDetail.isTvShow) dubDetail.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                        seasonsList = maxSeasonLimitConstraint?.let { limit -> rawSeasons.filter { it <= limit } } ?: rawSeasons
                        if (!seasonsList.contains(selectedSeason)) {
                            selectedSeason = seasonsList.firstOrNull() ?: 1
                        }
                        episodesList = repository.getEpisodes(dubSubjectId, selectedSeason)
                    }
                } else {
                    selectedSubject = originalSubject
                    val rawSeasons = if (originalSubject.isTvShow) originalSubject.seasonList.filter { it.episodeList.isNotEmpty() }.map { it.season }.sorted() else emptyList()
                    seasonsList = maxSeasonLimitConstraint?.let { limit -> rawSeasons.filter { it <= limit } } ?: rawSeasons
                    if (!seasonsList.contains(selectedSeason)) {
                        selectedSeason = seasonsList.firstOrNull() ?: 1
                    }
                    episodesList = repository.getEpisodes(originalSubject.id, selectedSeason)
                }

                val activeSubjectId = selectedSubject?.id ?: originalSubject.id
                val epNum = currentEpisode?.episodeNumber ?: 1
                val seNum = currentEpisode?.seasonNumber ?: 1

                val resolvedPlayInfo = if (originalSubject.isTvShow) {
                    repository.getPlayInfo(activeSubjectId, seNum, epNum, resourceId = resource.resourceId)
                } else {
                    repository.getPlayInfo(activeSubjectId, 0, 0, resourceId = resource.resourceId)
                }
                playInfo = resolvedPlayInfo
                selectedStream = selectStreamByPreference(resolvedPlayInfo.streamList, defaultStreamQuality)
                val isDubSelected = selectedResource?.name?.contains("dub", ignoreCase = true) == true
                selectedSubtitle = if (isDubSelected) null else (pickPreferredSubtitle(resolvedPlayInfo.subTitleList))
                isPlaying = true
            } catch (e: Exception) {
                android.util.Log.e("MovieViewModel", "Failed to change resource", e)
            } finally {
                isPlaybackLoading = false
            }
        }
    }

    // Toggle Favorite operation using the local Room store (no account required)
    fun toggleFavorite(subject: MovieSubject) {
        viewModelScope.launch {
            if (isAniFam) {
                val rawNum = subject.id.removePrefix("anifam_")
                val localId = "anifam_$rawNum"
                val isLocalFav = repository.isFavoriteDirect(localId) ||
                    repository.isFavoriteDirect(subject.id) ||
                    repository.isFavoriteDirect(rawNum)
                if (isLocalFav) {
                    repository.deleteFavorite(localId)
                    repository.deleteFavorite(subject.id)
                    repository.deleteFavorite(rawNum)
                } else {
                    repository.insertFavorite(
                        FavoriteEntity(
                            id = localId,
                            name = subject.name,
                            poster = subject.poster,
                            isTvShow = subject.isTvShow,
                            rating = subject.rating,
                            year = subject.year
                        )
                    )
                }
            } else {
                val isFav = repository.isFavoriteDirect(subject.id)
                if (isFav) {
                    repository.deleteFavorite(subject.id)
                } else {
                    repository.insertFavorite(
                        FavoriteEntity(subject.id, subject.name, subject.poster, subject.isTvShow, subject.rating, subject.year)
                    )
                }
            }
        }
    }

    fun isFavoriteFlow(id: String): Flow<Boolean> {
        val rawId = id.removePrefix("anifam_")
        val altId = "anifam_$rawId"
        return favoritesList.map { list -> 
            list.any { 
                it.id == id || it.id == altId || it.id == rawId || it.id.removePrefix("anifam_") == rawId 
            } 
        }
    }

    // Save playback position tick
    fun updatePlaybackProgress(positionMs: Long, durationMs: Long) {
        playbackProgressMs = positionMs
        mediaDurationMs = durationMs
        
        val subject = selectedSubject ?: return
        val season = currentEpisode?.seasonNumber ?: 1
        val episode = currentEpisode?.episodeNumber ?: 1
        val id = HistoryId.forSubject(subject.id, subject.isTvShow, season, episode)

        viewModelScope.launch {
            repository.insertHistory(
                HistoryEntity(
                    id = id,
                    subjectId = subject.id,
                    name = subject.name,
                    poster = subject.poster,
                    isTvShow = subject.isTvShow,
                    season = if (subject.isTvShow) season else 0,
                    episode = if (subject.isTvShow) episode else 0,
                    seeTime = positionMs,
                    totalTime = durationMs
                )
            )
        }
    }

    fun deleteHistoryItem(id: String) {
        viewModelScope.launch {
            repository.deleteHistory(id)
        }
    }

    private fun parseErrorMessage(e: Exception): String {
        if (e is retrofit2.HttpException) {
            try {
                val errorBody = e.response()?.errorBody()?.string()
                if (!errorBody.isNullOrEmpty()) {
                    val moshi = com.squareup.moshi.Moshi.Builder().build()
                    val adapter = moshi.adapter(Map::class.java)
                    val parsed = adapter.fromJson(errorBody)
                    val detail = parsed?.get("detail") as? String
                    if (!detail.isNullOrEmpty()) {
                        return detail
                    }
                    val message = parsed?.get("message") as? String
                    if (!message.isNullOrEmpty()) {
                        return message
                    }
                }
            } catch (ignored: Exception) {}
        }
        return e.message ?: "Network error"
    }

    fun playNextEpisode() {
        val current = currentEpisode ?: return
        val currentIndex = episodesList.indexOfFirst { it.episodeNumber == current.episodeNumber }
        if (currentIndex != -1 && currentIndex < episodesList.size - 1) {
            selectEpisode(episodesList[currentIndex + 1])
        }
    }

    fun playPreviousEpisode() {
        val current = currentEpisode ?: return
        val currentIndex = episodesList.indexOfFirst { it.episodeNumber == current.episodeNumber }
        if (currentIndex > 0) {
            selectEpisode(episodesList[currentIndex - 1])
        }
    }

    private fun parseMaxSeasonFromName(name: String): Int? {
        val rangeRegex = Regex("""S(\d+)-S?(\d+)""", RegexOption.IGNORE_CASE)
        val rangeMatch = rangeRegex.find(name)
        if (rangeMatch != null) {
            return rangeMatch.groupValues[2].toIntOrNull()
        }
        val singleRegex = Regex("""(?:Season|S)\s*(\d+)""", RegexOption.IGNORE_CASE)
        val singleMatches = singleRegex.findAll(name).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        if (singleMatches.isNotEmpty()) {
            return singleMatches.maxOrNull()
        }
        return null
    }
}
