package com.flixvora.ui.state

import android.app.Application
import android.content.Intent
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flixvora.BuildConfig
import com.flixvora.data.AppUpdateState
import com.flixvora.data.AppVersion
import com.flixvora.data.CatalogRail
import com.flixvora.data.CommentItem
import com.flixvora.data.ComingSoonRelease
import com.flixvora.data.DataBridge
import com.flixvora.data.DownloadChunk
import com.flixvora.data.DownloadStatus
import com.flixvora.data.DownloadTask
import com.flixvora.data.EpisodeItem
import com.flixvora.data.LiveChannel
import com.flixvora.data.MainNavTab
import com.flixvora.data.MediaCategory
import com.flixvora.data.MediaTitle
import com.flixvora.data.PlayerSession
import com.flixvora.data.ProfileType
import com.flixvora.data.SmartStorageConfig
import com.flixvora.data.StreamSource
import com.flixvora.data.local.FavoriteEntity
import com.flixvora.data.local.MovieDatabase
import com.flixvora.data.model.CommentItem as ApiComment
import com.flixvora.data.model.StreamItem
import com.flixvora.data.network.MovieBoxRetrofitClient
import com.flixvora.data.network.StreamUrlPolicy
import com.flixvora.data.preferences.PreferencesManager
import com.flixvora.data.repository.MovieRepository
import com.flixvora.download.DownloadService
import com.flixvora.download.DownloadStatus as RealDownloadStatus
import com.flixvora.download.DownloadTask as RealDownloadTask
import com.flixvora.ui.theme.FlixvoraAccentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

data class HistoryProgress(val percent: Int, val label: String? = null)

data class FlixvoraUiState(
    val activeTab: MainNavTab = MainNavTab.HOME,
    val selectedCategory: String = "Trending",
    val selectedGenre: String = "Trending",
    val selectedOtt: String = "All",
    val selectedMoodId: String? = null,
    val searchQuery: String = "",
    val activeProfile: ProfileType = ProfileType.MAIN,
    val adultUnlocked: Boolean = false,
    val parentalPin: String = "1234",
    val showPinDialogFor: ProfileType? = null,
    val accentTheme: FlixvoraAccentTheme = FlixvoraAccentTheme.EMERALD_NEON,
    val isTvMode: Boolean = false,
    val backgroundAudioEnabled: Boolean = true,
    val traktConnected: Boolean = false,
    val traktLastSync: String? = null,
    val watchlist: Set<String> = emptySet(),
    val watchlistItems: List<MediaTitle> = emptyList(),
    val remindedReleases: Set<String> = emptySet(),
    val downloads: List<DownloadTask> = emptyList(),
    val networkOnline: Boolean = true,
    val storageConfig: SmartStorageConfig = SmartStorageConfig(),
    val selectedDetail: MediaTitle? = null,
    val activePlayerSession: PlayerSession? = null,
    val miniPlayerSession: PlayerSession? = null,
    val miniPlayerPlaying: Boolean = true,
    val streamSources: List<StreamSource> = emptyList(),
    val activeSourceId: String = "",
    val showStreamResolver: Boolean = false,
    val showWatchPartyFor: MediaTitle? = null,
    val liveCatalog: List<MediaTitle> = emptyList(),
    val rails: List<CatalogRail> = emptyList(),
    val liveTvChannels: List<LiveChannel> = emptyList(),
    val liveComingSoon: List<ComingSoonRelease> = emptyList(),
    val commentsByTitle: Map<String, List<CommentItem>> = emptyMap(),
    val detailRecommendations: List<MediaTitle> = emptyList(),
    val detailQualities: List<String> = emptyList(),
    val progressByTitle: Map<String, HistoryProgress> = emptyMap(),
    val searchResults: List<MediaTitle> = emptyList(),
    val searchInProgress: Boolean = false,
    val isLoading: Boolean = false,
    val loadError: String? = null,
    val update: AppUpdateState = AppUpdateState(currentVersion = BuildConfig.VERSION_NAME),
    val toastMessage: String? = null
) {
    val safeCatalog: List<MediaTitle>
        get() {
            val base = liveCatalog.filter { item ->
                when (activeProfile) {
                    ProfileType.KIDS -> !item.isRestricted && item.ageRating !in setOf("R", "TV-MA", "18+")
                    ProfileType.MAIN -> if (adultUnlocked) true else !item.isRestricted
                    ProfileType.ADULT -> true
                }
            }
            return base.map { item ->
                val progress = progressByTitle[item.id]
                if (progress != null) {
                    item.copy(continueProgress = progress.percent, continueEpisode = progress.label)
                } else item
            }
        }

    val displayChannels: List<LiveChannel>
        get() = liveTvChannels

    val displayComingSoon: List<ComingSoonRelease>
        get() = liveComingSoon
}

class FlixvoraViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val database = MovieDatabase.getDatabase(application)
    val repository = MovieRepository(
        MovieBoxRetrofitClient.getApi(prefs),
        database.movieDao(),
        prefs
    )

    private val http = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var searchJob: Job? = null
    private val loadedCategoryTabs = mutableSetOf<Int>()
    private val channelStreamUrls = mutableMapOf<String, String>()

    @Volatile
    private var homeLoaded = false

    private val _uiState = MutableStateFlow(
        FlixvoraUiState(
            adultUnlocked = prefs.adultContentEnabled,
            isLoading = true
        )
    )
    val uiState: StateFlow<FlixvoraUiState> = _uiState.asStateFlow()

    init {
        observeWatchlist()
        observeHistory()
        observeDownloads()
        loadHome()
        checkForUpdates(isManual = false)
    }

    // ---------------- Real catalogue ----------------

    private data class HomePayload(
        val rails: List<CatalogRail>,
        val trending: List<MediaTitle>
    )

    fun loadHome(force: Boolean = false) {
        if (homeLoaded && !force) return
        _uiState.update { it.copy(isLoading = true, loadError = null) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                // The on-device proxy binds on a background coroutine; give it
                // a moment on the very first load so the first request is not
                // answered by the (region-gated) upstream fallback.
                if (!homeLoaded) delay(600)
                if (force) repository.clearFeedCaches()
                val payload = coroutineScope {
                    val tabIds = listOf(2, 5, 8, 18)
                    val tabDeferred = tabIds.map { id ->
                        id to async {
                            runCatching { repository.getTabOperatingModules(id) }.getOrDefault(emptyList())
                        }
                    }
                    val trendingDeferred = async {
                        runCatching { repository.getTrending(1, 30) }.getOrDefault(emptyList())
                    }
                    val animeDeferred = async {
                        runCatching { repository.getAnimeRails() }.getOrDefault(emptyList())
                    }
                    val rails = mutableListOf<CatalogRail>()
                    val seenRailKeys = mutableSetOf<String>()

                    fun addRail(id: String, title: String, items: List<MediaTitle>) {
                        if (items.isEmpty()) return
                        val key = id.ifBlank { title.lowercase() }
                        if (!seenRailKeys.add(key)) return
                        rails.add(CatalogRail(key, title.ifBlank { "More to explore" }, items.distinctBy { it.id }))
                    }

                    val trendingSubjects = trendingDeferred.await()
                    val trendingTitles = trendingSubjects.map { DataBridge.movieSubjectToMediaTitle(it) }
                    addRail("trending", "Trending Now", trendingTitles)

                    val tabSections = tabDeferred.map { (id, deferred) -> id to deferred.await() }
                    for ((tabId, sections) in tabSections) {
                        if (sections.isEmpty()) {
                            val fallback = runCatching { repository.getCategoryList(tabId, 1, 50) }
                                .getOrDefault(emptyList())
                            if (fallback.isNotEmpty()) {
                                val label = when (tabId) {
                                    2 -> "Popular on MovieBox"
                                    5 -> "Top Series"
                                    8 -> "Top Anime"
                                    18 -> "Asian Dramas"
                                    else -> "Tab $tabId"
                                }
                                addRail("tab_$tabId", label, fallback.map { DataBridge.movieSubjectToMediaTitle(it) })
                            }
                        } else {
                            sections.forEachIndexed { index, section ->
                                addRail(
                                    "tab_${tabId}_${index}",
                                    section.title,
                                    section.items.map { DataBridge.movieSubjectToMediaTitle(it) }
                                )
                            }
                        }
                    }

                    val animeSections = animeDeferred.await()
                    animeSections.forEachIndexed { index, section ->
                        addRail(
                            "anifam_$index",
                            section.title,
                            section.items.map { DataBridge.movieSubjectToMediaTitle(it) }
                        )
                    }

                    HomePayload(rails = rails, trending = trendingTitles)
                }

                val catalog = payload.rails.flatMap { it.items }.distinctBy { it.id }
                val comingSoon = payload.trending.take(6).mapIndexed { idx, subject ->
                    ComingSoonRelease(
                        id = "cs_${subject.id}",
                        title = subject.title,
                        categoryLabel = if (subject.category == MediaCategory.SERIES) "SERIES" else "MOVIE",
                        releaseEpochMs = System.currentTimeMillis() + ((idx + 1) * 3L * 24 * 60 * 60 * 1000),
                        quality = "Coming Soon",
                        tagline = subject.editorialTagline.ifBlank { "Coming soon to Flixvora." },
                        backdropUrl = subject.backdropUrl,
                        genres = subject.genres
                    )
                }

                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        current.copy(
                            liveCatalog = if (catalog.isNotEmpty()) catalog else current.liveCatalog,
                            rails = payload.rails,
                            liveComingSoon = comingSoon,
                            isLoading = false,
                            loadError = if (catalog.isEmpty()) {
                                "Live catalogue could not be loaded. Pull to refresh or check your connection."
                            } else null
                        )
                    }
                }
                homeLoaded = true
                loadChannels()
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            loadError = "Live catalogue failed: ${e.message ?: "unknown error"}"
                        )
                    }
                }
            }
        }
    }

    private suspend fun loadChannels() {
        val channels = runCatching {
            val india = repository.getAllTvChannels("IN", maxPages = 6)
            if (india.isNotEmpty()) india else repository.getAllTvChannels("US", maxPages = 6)
        }.getOrDefault(emptyList())
        if (channels.isEmpty()) return
        val mapped = channels.mapIndexed { idx, tv -> DataBridge.tvChannelToLiveChannel(tv, idx) }
        withContext(Dispatchers.Main) {
            _uiState.update { it.copy(liveTvChannels = mapped) }
        }
    }

    fun refresh() = loadHome(force = true)

    // ---------------- Watchlist / history / downloads ----------------

    private fun observeWatchlist() {
        viewModelScope.launch {
            repository.favorites.collect { favorites ->
                val items = favorites.map { favorite ->
                    MediaTitle(
                        id = favorite.id,
                        tmdbId = favorite.id.filter { it.isDigit() }.takeLast(9).toIntOrNull() ?: 0,
                        title = favorite.name,
                        editorialTagline = "",
                        category = if (favorite.isTvShow) MediaCategory.SERIES else MediaCategory.MOVIE,
                        year = favorite.year?.toIntOrNull() ?: 0,
                        duration = "-",
                        rating = favorite.rating ?: "-",
                        ageRating = "U/A 13+",
                        quality = "HD",
                        genres = emptyList(),
                        ottProvider = if (favorite.id.startsWith("anifam_")) "AniFam" else "MovieBox",
                        synopsis = "",
                        posterUrl = favorite.poster,
                        backdropUrl = favorite.poster,
                        audioTracks = emptyList()
                    )
                }
                _uiState.update { it.copy(watchlist = favorites.map { f -> f.id }.toSet(), watchlistItems = items) }
            }
        }
    }

    private fun observeHistory() {
        viewModelScope.launch {
            repository.history.collect { history ->
                val progress = mutableMapOf<String, HistoryProgress>()
                history.forEach { h ->
                    if (h.totalTime > 0) {
                        val percent = ((h.seeTime * 100) / h.totalTime).toInt().coerceIn(0, 100)
                        if (percent in 1..97) {
                            progress[h.subjectId] = HistoryProgress(
                                percent = percent,
                                label = if (h.isTvShow) "S${h.season} · Ep${h.episode}" else null
                            )
                        }
                    }
                }
                val watchedMinutes = (history.sumOf { it.seeTime } / 60_000L).toInt()
                _uiState.update {
                    it.copy(
                        progressByTitle = progress,
                        storageConfig = it.storageConfig.copy(watchMinutesTotal = watchedMinutes)
                    )
                }
            }
        }
    }

    private fun observeDownloads() {
        viewModelScope.launch {
            DownloadService.snapshots.collect { snapshot ->
                _uiState.update { it.copy(downloads = snapshot.tasks.map { task -> mapDownload(task) }) }
            }
        }
    }

    private fun mapDownload(task: RealDownloadTask): DownloadTask {
        val sizeMb = ((task.totalBytes / (1024L * 1024L)).toInt()).coerceAtLeast(1)
        val status = when (task.status) {
            RealDownloadStatus.PAUSED -> DownloadStatus.PAUSED
            RealDownloadStatus.COMPLETED -> DownloadStatus.COMPLETED
            else -> DownloadStatus.DOWNLOADING
        }
        val progress = task.progress
        val chunkCount = 6
        val chunks = (0 until chunkCount).map { idx ->
            val start = idx * sizeMb / chunkCount
            val end = if (idx == chunkCount - 1) sizeMb else (idx + 1) * sizeMb / chunkCount
            val range = (end - start).coerceAtLeast(1)
            val done = ((range * progress) / 100).coerceIn(0, range)
            DownloadChunk(
                chunkIndex = idx,
                startMb = start,
                endMb = end,
                downloadedMb = if (status == DownloadStatus.COMPLETED) range else done,
                completed = status == DownloadStatus.COMPLETED || done >= range,
                speedMbps = 0f
            )
        }
        return DownloadTask(
            id = "dl_${task.id}",
            titleId = task.title,
            title = task.title,
            subtitle = if (task.isHls) "HLS stream" else "Direct file",
            posterUrl = task.posterUrl,
            sizeMb = sizeMb,
            progress = if (status == DownloadStatus.COMPLETED) 100 else progress,
            quality = "Auto",
            status = status,
            chunks = chunks
        )
    }

    // ---------------- Navigation / filters ----------------

    fun selectTab(tab: MainNavTab) {
        _uiState.update { it.copy(activeTab = tab, selectedDetail = null) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        val tabId = when (category) {
            "Movies" -> 1
            "TV Shows" -> 5
            "Anime" -> 8
            "Asian" -> 18
            else -> 2
        }
        if (!loadedCategoryTabs.add(tabId)) return
        viewModelScope.launch(Dispatchers.IO) {
            val subjects = runCatching { repository.getCategoryList(tabId, 1, 50) }.getOrDefault(emptyList())
            if (subjects.isNotEmpty()) {
                val titles = subjects.map { DataBridge.movieSubjectToMediaTitle(it) }
                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        current.copy(liveCatalog = (current.liveCatalog + titles).distinctBy { it.id })
                    }
                }
            }
        }
    }

    fun setGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
        if (genre == "Trending") return
        viewModelScope.launch(Dispatchers.IO) {
            val subjects = runCatching { repository.getGenreTop(genre) }.getOrDefault(emptyList())
            if (subjects.isNotEmpty()) {
                val titles = subjects.map { DataBridge.movieSubjectToMediaTitle(it) }
                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        current.copy(liveCatalog = (current.liveCatalog + titles).distinctBy { it.id })
                    }
                }
            }
        }
    }

    fun setOtt(ott: String) {
        _uiState.update { it.copy(selectedOtt = ott) }
    }

    fun toggleMood(moodId: String?) {
        _uiState.update { current ->
            current.copy(selectedMoodId = if (current.selectedMoodId == moodId) null else moodId)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        searchJob?.cancel()
        if (query.trim().length < 2) {
            _uiState.update { it.copy(searchResults = emptyList(), searchInProgress = false) }
            return
        }
        searchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(350)
            _uiState.update { it.copy(searchInProgress = true) }
            val subjects = runCatching { repository.search(query.trim()) }.getOrDefault(emptyList())
            val titles = subjects.map { DataBridge.movieSubjectToMediaTitle(it) }
            withContext(Dispatchers.Main) {
                _uiState.update { current ->
                    current.copy(
                        searchResults = titles,
                        searchInProgress = false,
                        liveCatalog = (current.liveCatalog + titles).distinctBy { it.id }
                    )
                }
            }
        }
    }

    // ---------------- Detail ----------------

    fun openDetail(item: MediaTitle) {
        _uiState.update {
            it.copy(selectedDetail = item, detailRecommendations = emptyList(), detailQualities = emptyList())
        }
        viewModelScope.launch(Dispatchers.IO) {
            val detail = runCatching { repository.getDetail(item.id) }.getOrNull()
            val updated = detail?.let { DataBridge.movieSubjectToMediaTitle(it) } ?: item
            val recs = runCatching { repository.getDetailRec(item.id) }.getOrDefault(emptyList())
                .map { DataBridge.movieSubjectToMediaTitle(it) }
            val apiComments = runCatching { repository.getComments(item.id) }.getOrDefault(emptyList())
            val comments = apiComments.map { it.toUiComment() }
            val qualities = resolveStreams(updated, 0, 0).first.map { it.quality }.distinct()
            withContext(Dispatchers.Main) {
                _uiState.update { current ->
                    if (current.selectedDetail?.id != item.id) current
                    else current.copy(
                        selectedDetail = updated,
                        detailRecommendations = recs,
                        detailQualities = qualities,
                        commentsByTitle = current.commentsByTitle + (item.id to comments)
                    )
                }
            }
        }
    }

    private fun ApiComment.toUiComment(): CommentItem = CommentItem(
        id = id,
        author = userName.ifBlank { "User" },
        timestamp = createTime,
        body = content,
        hasSpoiler = false,
        likes = likeCount
    )

    fun closeDetail() {
        _uiState.update { it.copy(selectedDetail = null) }
    }

    fun openDeepLinkedTitle(titleId: String) {
        val match = _uiState.value.safeCatalog.firstOrNull { it.id == titleId }
        if (match != null) {
            _uiState.update { it.copy(activeTab = MainNavTab.HOME) }
            openDetail(match)
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            val subject = runCatching { repository.getDetail(titleId) }.getOrNull() ?: return@launch
            val title = DataBridge.movieSubjectToMediaTitle(subject)
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(activeTab = MainNavTab.HOME, liveCatalog = (it.liveCatalog + title).distinctBy { m -> m.id })
                }
                openDetail(title)
            }
        }
    }

    // ---------------- Watchlist ----------------

    fun toggleWatchlist(id: String) {
        val inList = _uiState.value.watchlist.contains(id)
        viewModelScope.launch(Dispatchers.IO) {
            if (inList) {
                repository.deleteFavorite(id)
            } else {
                val title = _uiState.value.liveCatalog.firstOrNull { it.id == id }
                    ?: _uiState.value.selectedDetail
                repository.insertFavorite(
                    FavoriteEntity(
                        id = id,
                        name = title?.title ?: id,
                        poster = title?.posterUrl.orEmpty(),
                        isTvShow = title?.category != MediaCategory.MOVIE,
                        rating = title?.rating,
                        year = title?.year?.takeIf { it > 0 }?.toString()
                    )
                )
            }
        }
        showToast(if (inList) "Removed from Watchlist" else "Saved to Watchlist")
    }

    fun clearWatchlist() {
        viewModelScope.launch(Dispatchers.IO) { repository.clearFavorites() }
        showToast("Cleared Watchlist")
    }

    // ---------------- Downloads ----------------

    fun enqueueDownload(item: MediaTitle, label: String = "${item.year} · ${item.quality}", sizeMb: Int = 0) {
        val first = item.seasons.firstOrNull()?.episodes?.firstOrNull()
        enqueueDownloadInternal(
            item = item,
            season = if (item.category == MediaCategory.MOVIE) 0 else 1,
            episode = if (item.category == MediaCategory.MOVIE) 0 else (first?.number ?: 1),
            label = label
        )
    }

    fun enqueueEpisodeDownload(item: MediaTitle, season: Int, episode: EpisodeItem) {
        enqueueDownloadInternal(item, season, episode.number, "S$season · Ep${episode.number} · ${episode.title}")
    }

    private fun enqueueDownloadInternal(item: MediaTitle, season: Int, episode: Int, label: String) {
        showToast("Preparing download: ${item.title}")
        viewModelScope.launch(Dispatchers.IO) {
            val streams = resolveStreams(item, season, episode).first
            val stream = streams.firstOrNull()
            if (stream == null) {
                withContext(Dispatchers.Main) { showToast("No downloadable stream found for ${item.title}") }
                return@launch
            }
            val headers = buildHeaders(stream.cookie, item)
            val cookie = headers.entries.joinToString("; ") { "${it.key}=${it.value}" }
            DownloadService.enqueue(
                getApplication<Application>(),
                RealDownloadTask(
                    id = System.currentTimeMillis(),
                    title = "${item.title} · $label",
                    streamUrl = stream.url,
                    cookies = cookie,
                    isHls = stream.url.contains(".m3u8", ignoreCase = true),
                    posterUrl = item.posterUrl
                )
            )
            withContext(Dispatchers.Main) { showToast("Download started: ${item.title}") }
        }
    }

    fun toggleDownloadStatus(id: String) {
        val numeric = id.removePrefix("dl_").toLongOrNull() ?: return
        val current = _uiState.value.downloads.firstOrNull { it.id == id }
        if (current?.status == DownloadStatus.PAUSED) {
            DownloadService.resume(getApplication<Application>(), numeric)
        } else {
            DownloadService.pause(getApplication<Application>(), numeric)
        }
    }

    fun removeDownload(id: String) {
        val numeric = id.removePrefix("dl_").toLongOrNull() ?: return
        DownloadService.cancel(getApplication<Application>(), numeric)
        showToast("Download removed")
    }

    fun clearDownloads() {
        _uiState.value.downloads.forEach { task ->
            task.id.removePrefix("dl_").toLongOrNull()?.let { DownloadService.cancel(getApplication<Application>(), it) }
        }
        showToast("Cleared all downloads")
    }

    fun toggleNetworkSimulation() {
        _uiState.update { current ->
            val next = !current.networkOnline
            current.copy(
                networkOnline = next,
                toastMessage = if (next) "Network reconnected" else "Simulated offline network drop"
            )
        }
    }

    // ---------------- Playback ----------------

    private suspend fun resolveStreams(
        item: MediaTitle,
        season: Int,
        episode: Int
    ): Pair<List<StreamItem>, List<com.flixvora.data.model.SubtitleItem>> {
        val attempts = mutableListOf(season to episode)
        if (item.category != MediaCategory.MOVIE) {
            if (season <= 0) attempts.add(1 to 1)
            if (episode <= 0) attempts.add(0 to 0)
        }
        for ((s, e) in attempts.distinct()) {
            val info = runCatching { repository.getPlayInfo(item.id, s, e) }.getOrNull() ?: continue
            val playable = info.streamList
                .filter { it.url.isNotBlank() && !StreamUrlPolicy.isDecoyUrl(it.url) }
                .sortedByDescending { qualityRank(it.quality) }
            if (playable.isNotEmpty()) return playable to info.subTitleList
        }
        return emptyList<StreamItem>() to emptyList()
    }

    private fun qualityRank(quality: String): Int =
        quality.filter { it.isDigit() }.toIntOrNull() ?: 0

    private fun buildHeaders(cookie: String, item: MediaTitle): Map<String, String> {
        val headers = mutableMapOf<String, String>()
        if (cookie.isNotBlank()) {
            if (cookie.contains("=") && (cookie.contains("Referer=") || cookie.contains("Origin="))) {
                cookie.split(";").forEach { pair ->
                    val parts = pair.split("=", limit = 2)
                    if (parts.size == 2) headers[parts[0].trim()] = parts[1].trim()
                }
            } else {
                headers["Cookie"] = cookie
            }
        }
        if (item.id.startsWith("anifam_") || item.category == MediaCategory.ANIME) {
            headers["Referer"] = "https://zokoanime.video/"
            headers["Origin"] = "https://zokoanime.video"
        } else {
            // MovieBox CDN only serves DASH/HLS manifests to this referer.
            headers["Referer"] = "https://sportslive.wine"
            headers["Origin"] = "https://sportslive.wine"
        }
        return headers
    }

    private fun toStreamSources(streams: List<StreamItem>, item: MediaTitle): List<StreamSource> =
        streams.mapIndexed { index, stream ->
            StreamSource(
                id = "src_${index}_${stream.url.hashCode()}",
                name = "Source ${index + 1} · ${stream.quality}",
                provider = if (item.id.startsWith("anifam_")) "AniFam" else "MovieBox",
                protocol = when {
                    stream.url.contains(".m3u8", true) -> "HLS"
                    stream.url.contains(".mpd", true) -> "DASH"
                    else -> "MP4"
                },
                quality = stream.quality,
                pingMs = 0,
                bitrateMbps = 0f,
                isPrimary = index == 0,
                url = stream.url,
                headers = buildHeaders(stream.cookie, item)
            )
        }

    fun playTitle(item: MediaTitle) = play(item, 0, 0)

    fun playEpisode(item: MediaTitle, season: Int, episode: EpisodeItem) =
        play(item, season, episode.number)

    private fun play(item: MediaTitle, season: Int, episode: Int) {
        _uiState.update { it.copy(showStreamResolver = false) }
        showToast("Resolving stream: ${item.title}")
        viewModelScope.launch(Dispatchers.IO) {
            val (streams, subtitles) = resolveStreams(item, season, episode)
            val best = streams.firstOrNull()
            if (best == null) {
                withContext(Dispatchers.Main) { showToast("No playable stream found for ${item.title}") }
                return@launch
            }
            val sources = toStreamSources(streams, item)
            val session = PlayerSession(
                title = item.title,
                subtitle = "${item.year} · ${best.quality}",
                backdropUrl = item.backdropUrl,
                durationSeconds = 0,
                tmdbId = item.tmdbId,
                audioTracks = item.audioTracks,
                isLive = false,
                introEndSeconds = item.introEndSeconds,
                streamUrl = best.url,
                subtitleUrl = subtitles.firstOrNull()?.url,
                headers = sources.firstOrNull()?.headers ?: emptyMap(),
                subjectId = item.id,
                season = season,
                episode = episode
            )
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(
                        activePlayerSession = session,
                        streamSources = sources,
                        activeSourceId = sources.firstOrNull()?.id ?: ""
                    )
                }
            }
        }
    }

    fun playChannel(channel: LiveChannel) {
        showToast("Tuning ${channel.name}...")
        viewModelScope.launch(Dispatchers.IO) {
            val url = channelStreamUrls[channel.id]
                ?: runCatching { repository.getTvChannelStream(channel.id) }.getOrNull()
            if (url.isNullOrBlank()) {
                withContext(Dispatchers.Main) { showToast("Live stream unavailable for ${channel.name}") }
                return@launch
            }
            channelStreamUrls[channel.id] = url
            val session = PlayerSession(
                title = channel.name,
                subtitle = "CH ${channel.number} · ${channel.currentProgram}",
                backdropUrl = channel.previewUrl,
                durationSeconds = 0,
                tmdbId = 900101,
                audioTracks = emptyList(),
                isLive = true,
                streamUrl = url,
                headers = mapOf("User-Agent" to "Mozilla/5.0 (Linux; Android 13)"),
                subjectId = channel.id
            )
            withContext(Dispatchers.Main) {
                _uiState.update {
                    it.copy(activePlayerSession = session, streamSources = emptyList(), activeSourceId = "")
                }
            }
        }
    }

    fun startPlayback(session: PlayerSession) {
        if (session.streamUrl.isNotBlank()) {
            _uiState.update { it.copy(activePlayerSession = session) }
            return
        }
        val match = _uiState.value.liveCatalog.firstOrNull { it.id == session.subjectId }
        if (match != null) {
            play(match, session.season, session.episode)
        } else {
            showToast("Stream not resolved for ${session.title}")
        }
    }

    fun saveProgress(session: PlayerSession, positionMs: Long, durationMs: Long) {
        if (session.subjectId.isBlank() || session.isLive || durationMs <= 0L) return
        viewModelScope.launch(Dispatchers.IO) {
            val historyId = if (session.season > 0) {
                "${session.subjectId}_S${session.season}_E${session.episode}"
            } else {
                session.subjectId
            }
            repository.insertHistory(
                com.flixvora.data.local.HistoryEntity(
                    id = historyId,
                    subjectId = session.subjectId,
                    name = session.title,
                    poster = session.backdropUrl,
                    isTvShow = session.season > 0,
                    season = session.season,
                    episode = session.episode,
                    seeTime = positionMs,
                    totalTime = durationMs
                )
            )
        }
    }

    fun closePlayer() {
        _uiState.update { it.copy(activePlayerSession = null) }
    }

    fun minimizeToBackgroundAudio() {
        _uiState.update { current ->
            if (!current.backgroundAudioEnabled) {
                return@update current.copy(toastMessage = "Enable Background Audio in Me first")
            }
            current.copy(
                miniPlayerSession = current.activePlayerSession,
                activePlayerSession = null,
                miniPlayerPlaying = true,
                toastMessage = "Background audio ready"
            )
        }
    }

    fun toggleMiniPlayerPlay() {
        _uiState.update { it.copy(miniPlayerPlaying = !it.miniPlayerPlaying) }
    }

    fun expandMiniPlayer() {
        _uiState.update { it.copy(activePlayerSession = it.miniPlayerSession, miniPlayerSession = null) }
    }

    fun closeMiniPlayer() {
        _uiState.update { it.copy(miniPlayerSession = null) }
    }

    fun setStreamResolverOpen(open: Boolean) {
        _uiState.update { it.copy(showStreamResolver = open) }
    }

    fun selectStreamSource(source: StreamSource) {
        _uiState.update { current ->
            val session = current.activePlayerSession
            current.copy(
                activeSourceId = source.id,
                activePlayerSession = if (session != null && source.url.isNotBlank()) {
                    session.copy(streamUrl = source.url, headers = source.headers)
                } else session,
                toastMessage = "Switched to ${source.name}"
            )
        }
    }

    fun simulateStreamFailover() {
        val sources = _uiState.value.streamSources
        if (sources.size < 2) {
            showToast("Only one source available")
            return
        }
        val currentIndex = sources.indexOfFirst { it.id == _uiState.value.activeSourceId }
        val next = sources[(currentIndex + 1).coerceAtLeast(0) % sources.size]
        selectStreamSource(next)
    }

    // ---------------- Community / party ----------------

    fun openWatchParty(item: MediaTitle?) {
        _uiState.update { it.copy(showWatchPartyFor = item) }
    }

    fun addComment(titleId: String, body: String, hasSpoiler: Boolean) {
        if (body.isBlank()) return
        val optimistic = CommentItem(
            id = "local_${System.currentTimeMillis()}",
            author = "You",
            timestamp = "Just now",
            body = body,
            hasSpoiler = hasSpoiler,
            likes = 0
        )
        _uiState.update { current ->
            val list = current.commentsByTitle[titleId].orEmpty()
            current.copy(commentsByTitle = current.commentsByTitle + (titleId to (listOf(optimistic) + list)))
        }
        viewModelScope.launch(Dispatchers.IO) {
            val posted = runCatching { repository.postComment(titleId, body) }.getOrDefault(false)
            if (!posted) {
                withContext(Dispatchers.Main) { showToast("Comment saved locally (offline)") }
            }
        }
    }

    fun buildDeepLinkUrl(titleId: String): String = "https://flixvora.app/?title=$titleId"

    // ---------------- Settings / profiles / storage ----------------

    fun clearTempMediaCacheOnly() {
        _uiState.update { current ->
            current.copy(
                storageConfig = current.storageConfig.copy(
                    mediaBufferMb = 0,
                    thumbCacheMb = 24,
                    feedCacheMb = 8
                ),
                toastMessage = "Cleared temporary media & buffer caches"
            )
        }
    }

    fun updateStorageWifiOnly(wifiOnly: Boolean) {
        _uiState.update {
            it.copy(
                storageConfig = it.storageConfig.copy(wifiOnly = wifiOnly),
                toastMessage = if (wifiOnly) "Downloads set to Wi-Fi only" else "Cellular downloads enabled"
            )
        }
    }

    fun updateAutoDeleteWatched(autoDelete: Boolean) {
        _uiState.update {
            it.copy(
                storageConfig = it.storageConfig.copy(autoDeleteWatched = autoDelete),
                toastMessage = if (autoDelete) "Auto-delete completed titles ON" else "Auto-delete OFF"
            )
        }
    }

    fun requestProfileSwitch(target: ProfileType) {
        if (target == ProfileType.ADULT && !_uiState.value.adultUnlocked) {
            _uiState.update { it.copy(showPinDialogFor = target) }
            return
        }
        _uiState.update { it.copy(activeProfile = target, toastMessage = "Switched to ${target.label}") }
    }

    fun verifyPinAndUnlock(pin: String): Boolean {
        return if (pin == _uiState.value.parentalPin) {
            prefs.adultContentEnabled = true
            _uiState.update {
                it.copy(
                    adultUnlocked = true,
                    activeProfile = ProfileType.ADULT,
                    showPinDialogFor = null,
                    toastMessage = "Adult profile unlocked"
                )
            }
            true
        } else false
    }

    fun dismissPinDialog() {
        _uiState.update { it.copy(showPinDialogFor = null) }
    }

    fun setAccentTheme(theme: FlixvoraAccentTheme) {
        _uiState.update { it.copy(accentTheme = theme, toastMessage = "Theme applied: ${theme.label}") }
    }

    fun toggleTvMode() {
        _uiState.update { current ->
            val next = !current.isTvMode
            current.copy(
                isTvMode = next,
                toastMessage = if (next) "Android TV 10-Foot Mode active" else "Returned to Mobile UI"
            )
        }
    }

    fun toggleBackgroundAudio() {
        _uiState.update { current ->
            val next = !current.backgroundAudioEnabled
            current.copy(
                backgroundAudioEnabled = next,
                toastMessage = if (next) "Background Audio Enabled" else "Background Audio Disabled"
            )
        }
    }

    fun syncTrakt() {
        showToast("Trakt sync needs an account link — not configured in this build")
    }

    fun toggleComingSoonReminder(id: String, title: String) {
        _uiState.update { current ->
            val added = !current.remindedReleases.contains(id)
            val next = if (added) current.remindedReleases + id else current.remindedReleases - id
            current.copy(
                remindedReleases = next,
                toastMessage = if (added) "Reminder set for $title" else "Removed reminder for $title"
            )
        }
    }

    // ---------------- Real GitHub release updates ----------------

    private data class ReleaseInfo(
        val version: String,
        val releaseUrl: String?,
        val apkUrl: String?,
        val notes: String?
    )

    fun checkForUpdates(isManual: Boolean = true) {
        if (_uiState.value.update.isChecking) return
        _uiState.update {
            it.copy(update = it.update.copy(isChecking = true, currentVersion = BuildConfig.VERSION_NAME))
        }
        viewModelScope.launch(Dispatchers.IO) {
            val release = fetchLatestRelease()
            withContext(Dispatchers.Main) {
                if (release == null) {
                    _uiState.update {
                        it.copy(
                            update = it.update.copy(
                                isChecking = false,
                                message = "Update check failed — could not reach GitHub."
                            )
                        )
                    }
                    if (isManual) showToast("Update check failed")
                    return@withContext
                }
                val newer = AppVersion.isNewer(release.version, BuildConfig.VERSION_NAME)
                val message = if (newer) {
                    "Update available: v${release.version} (you have v${BuildConfig.VERSION_NAME})"
                } else {
                    "You are on the latest version (v${BuildConfig.VERSION_NAME})"
                }
                _uiState.update {
                    it.copy(
                        update = AppUpdateState(
                            currentVersion = BuildConfig.VERSION_NAME,
                            latestVersion = release.version,
                            isChecking = false,
                            isUpdateAvailable = newer,
                            releaseUrl = release.releaseUrl,
                            apkUrl = release.apkUrl,
                            notes = release.notes,
                            message = message
                        )
                    )
                }
                if (isManual) showToast(message)
            }
        }
    }

    private fun fetchLatestRelease(): ReleaseInfo? {
        val endpoints = listOf(BuildConfig.UPDATE_MANIFEST_URL, BuildConfig.UPDATE_MANIFEST_URL_2)
        for (endpoint in endpoints) {
            try {
                val request = Request.Builder()
                    .url(endpoint)
                    .header("Accept", "application/vnd.github+json")
                    .header("User-Agent", "Flixvora-Android")
                    .build()
                val body = http.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) null else response.body?.string()
                } ?: continue
                val trimmed = body.trimStart()
                val obj = if (trimmed.startsWith("[")) JSONArray(body).optJSONObject(0) else JSONObject(body)
                if (obj == null) continue
                val tag = obj.optString("tag_name").ifBlank { obj.optString("versionName") }
                if (tag.isBlank()) continue
                var apkUrl: String? = null
                val assets = obj.optJSONArray("assets")
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        if (asset.optString("name").endsWith(".apk", ignoreCase = true)) {
                            apkUrl = asset.optString("browser_download_url").ifBlank { null }
                            break
                        }
                    }
                }
                return ReleaseInfo(
                    version = tag.removePrefix("v"),
                    releaseUrl = obj.optString("html_url").ifBlank { null },
                    apkUrl = apkUrl,
                    notes = obj.optString("body").ifBlank { null }
                )
            } catch (_: Exception) {
            }
        }
        return null
    }

    fun downloadUpdate() {
        val update = _uiState.value.update
        val target = update.apkUrl ?: update.releaseUrl
        if (target.isNullOrBlank()) {
            showToast("No update download available")
            return
        }
        try {
            getApplication<Application>().startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(target))
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        } catch (_: Exception) {
            showToast("Could not open the update link")
        }
    }

    // ---------------- Toast ----------------

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
