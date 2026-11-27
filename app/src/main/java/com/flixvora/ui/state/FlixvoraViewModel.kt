package com.flixvora.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.flixvora.data.*
import com.flixvora.data.model.MovieSubject
import com.flixvora.data.network.MovieBoxRetrofitClient
import com.flixvora.data.preferences.PreferencesManager
import com.flixvora.data.local.MovieDatabase
import com.flixvora.data.repository.MovieRepository
import com.flixvora.ui.theme.FlixvoraAccentTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

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
    val remindedReleases: Set<String> = emptySet(),
    val downloads: List<DownloadTask> = emptyList(),
    val networkOnline: Boolean = true,
    val storageConfig: SmartStorageConfig = SmartStorageConfig(),
    val selectedDetail: MediaTitle? = null,
    val activePlayerSession: PlayerSession? = null,
    val miniPlayerSession: PlayerSession? = null,
    val miniPlayerPlaying: Boolean = true,
    val streamSources: List<StreamSource> = SampleCatalog.streamSources,
    val activeSourceId: String = SampleCatalog.streamSources.first().id,
    val showStreamResolver: Boolean = false,
    val showWatchPartyFor: MediaTitle? = null,
    val liveCatalog: List<MediaTitle> = emptyList(),
    val liveTvChannels: List<LiveChannel> = emptyList(),
    val liveComingSoon: List<ComingSoonRelease> = emptyList(),
    val commentsByTitle: Map<String, List<CommentItem>> = emptyMap(),
    val toastMessage: String? = null
) {
    val safeCatalog: List<MediaTitle>
        get() {
            val base = if (liveCatalog.isNotEmpty()) liveCatalog else SampleCatalog.mediaCatalog
            return SampleCatalog.filterCatalogBySafety(base, activeProfile, adultUnlocked)
        }

    val displayChannels: List<LiveChannel>
        get() = if (liveTvChannels.isNotEmpty()) liveTvChannels else SampleCatalog.liveChannels

    val displayComingSoon: List<ComingSoonRelease>
        get() = if (liveComingSoon.isNotEmpty()) liveComingSoon else SampleCatalog.comingSoonReleases
}

class FlixvoraViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = PreferencesManager(application)
    private val database = MovieDatabase.getDatabase(application)
    val repository = MovieRepository(
        MovieBoxRetrofitClient.getApi(prefs),
        database.movieDao(),
        prefs
    )

    private val _uiState = MutableStateFlow(FlixvoraUiState(
        adultUnlocked = prefs.adultContentEnabled
    ))
    val uiState: StateFlow<FlixvoraUiState> = _uiState.asStateFlow()

    init {
        loadRealData()

        viewModelScope.launch {
            while (true) {
                delay(1500)
                advanceDownloadsTick()
            }
        }
    }

    fun loadRealData() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val trending = repository.getTrending(1, 20)
                val catMovies = repository.getCategoryList(0, 1, 50)
                val sections = repository.getCategorySections(0, 1, 50)
                val anime = try { repository.getAnimeRails() } catch (_: Exception) { emptyList() }
                
                val combined = mutableListOf<MovieSubject>()
                combined.addAll(trending)
                combined.addAll(catMovies)
                sections.forEach { combined.addAll(it.items) }
                anime.forEach { combined.addAll(it.items) }

                val distinctSubjects = combined.distinctBy { it.id }
                val converted = distinctSubjects.map { DataBridge.movieSubjectToMediaTitle(it) }

                val comingSoonList = distinctSubjects.take(6).mapIndexed { idx, sub ->
                    DataBridge.movieSubjectToComingSoon(sub, idx)
                }

                val realChannels = try {
                    val channelsData = repository.getTvChannels("US", 1, 30)
                    if (channelsData.isNotEmpty()) {
                        channelsData.mapIndexed { idx, tv -> DataBridge.tvChannelToLiveChannel(tv, idx) }
                    } else emptyList()
                } catch (_: Exception) {
                    emptyList()
                }

                withContext(Dispatchers.Main) {
                    _uiState.update { current ->
                        current.copy(
                            liveCatalog = if (converted.isNotEmpty()) converted else current.liveCatalog,
                            liveComingSoon = if (comingSoonList.isNotEmpty()) comingSoonList else current.liveComingSoon,
                            liveTvChannels = if (realChannels.isNotEmpty()) realChannels else current.liveTvChannels
                        )
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    private fun advanceDownloadsTick() {
        val state = _uiState.value
        if (!state.networkOnline) return
        _uiState.update { current ->
            val updated = current.downloads.map { task ->
                if (task.status != DownloadStatus.DOWNLOADING) return@map task
                val nextChunks = task.chunks.map { ch ->
                    val range = (ch.endMb - ch.startMb).coerceAtLeast(1)
                    if (ch.downloadedMb >= range) ch.copy(completed = true, speedMbps = 0f)
                    else {
                        val nextDown = (ch.downloadedMb + 8).coerceAtMost(range)
                        ch.copy(
                            downloadedMb = nextDown,
                            completed = nextDown >= range,
                            speedMbps = if (nextDown >= range) 0f else 3.8f
                        )
                    }
                }
                val totalDown = nextChunks.sumOf { it.downloadedMb }
                val nextProgress = ((totalDown * 100) / task.sizeMb.coerceAtLeast(1)).coerceIn(0, 100)
                val done = nextProgress >= 100 || nextChunks.all { it.completed }
                task.copy(
                    chunks = nextChunks,
                    progress = if (done) 100 else nextProgress,
                    status = if (done) DownloadStatus.COMPLETED else DownloadStatus.DOWNLOADING
                )
            }
            current.copy(downloads = updated)
        }
    }

    fun selectTab(tab: MainNavTab) {
        _uiState.update { it.copy(activeTab = tab, selectedDetail = null) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
        viewModelScope.launch(Dispatchers.IO) {
            val tabId = when (category) {
                "Movies" -> 1
                "TV Shows" -> 2
                "Anime" -> 5
                else -> 0
            }
            try {
                val catList = repository.getCategoryList(tabId, 1, 50)
                val titles = catList.map { DataBridge.movieSubjectToMediaTitle(it) }
                if (titles.isNotEmpty()) {
                    withContext(Dispatchers.Main) {
                        _uiState.update { it.copy(liveCatalog = (titles + it.liveCatalog).distinctBy { m -> m.id }) }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun setGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
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
        if (query.trim().length >= 2) {
            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val results = repository.search(query.trim())
                    val converted = results.map { DataBridge.movieSubjectToMediaTitle(it) }
                    if (converted.isNotEmpty()) {
                        withContext(Dispatchers.Main) {
                            _uiState.update { it.copy(liveCatalog = (converted + it.liveCatalog).distinctBy { m -> m.id }) }
                        }
                    }
                } catch (_: Exception) {}
            }
        }
    }

    fun openDetail(item: MediaTitle) {
        _uiState.update { it.copy(selectedDetail = item) }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val fullDetail = repository.getDetail(item.id)
                if (fullDetail != null) {
                    val updated = DataBridge.movieSubjectToMediaTitle(fullDetail)
                    withContext(Dispatchers.Main) {
                        _uiState.update { current ->
                            if (current.selectedDetail?.id == item.id) {
                                current.copy(selectedDetail = updated)
                            } else current
                        }
                    }
                }
            } catch (_: Exception) {}
        }
    }

    fun openDeepLinkedTitle(titleId: String) {
        val match = _uiState.value.safeCatalog.firstOrNull { it.id == titleId }
        if (match != null) {
            openDetail(match)
            _uiState.update { it.copy(activeTab = MainNavTab.HOME) }
            showToast("Opened: ${match.title}")
        }
    }

    fun closeDetail() {
        _uiState.update { it.copy(selectedDetail = null) }
    }

    fun toggleWatchlist(id: String) {
        _uiState.update { current ->
            val next = if (current.watchlist.contains(id)) current.watchlist - id else current.watchlist + id
            current.copy(
                watchlist = next,
                toastMessage = if (next.contains(id)) "Saved to Watchlist" else "Removed from Watchlist"
            )
        }
    }

    fun clearWatchlist() {
        _uiState.update { it.copy(watchlist = emptySet(), toastMessage = "Cleared Watchlist") }
    }

    fun enqueueDownload(item: MediaTitle, label: String = "${item.year} · ${item.quality}", sizeMb: Int = 1480) {
        _uiState.update { current ->
            val exists = current.downloads.any { it.titleId == item.id && it.subtitle == label }
            if (exists) {
                return@update current.copy(toastMessage = "\"${item.title}\" is already in Downloads")
            }
            val task = SampleCatalog.createDownloadTask(item, label, sizeMb, 14, DownloadStatus.DOWNLOADING)
            current.copy(
                downloads = listOf(task) + current.downloads,
                toastMessage = "Queued download: ${item.title}"
            )
        }
    }

    fun toggleDownloadStatus(id: String) {
        _uiState.update { current ->
            current.copy(
                downloads = current.downloads.map { task ->
                    if (task.id != id || task.status == DownloadStatus.COMPLETED) task
                    else task.copy(
                        status = if (task.status == DownloadStatus.DOWNLOADING) DownloadStatus.PAUSED else DownloadStatus.DOWNLOADING
                    )
                }
            )
        }
    }

    fun removeDownload(id: String) {
        _uiState.update { current ->
            current.copy(
                downloads = current.downloads.filterNot { it.id == id },
                toastMessage = "Download removed"
            )
        }
    }

    fun clearDownloads() {
        _uiState.update { it.copy(downloads = emptyList(), toastMessage = "Cleared all downloads") }
    }

    fun toggleNetworkSimulation() {
        _uiState.update { current ->
            val next = !current.networkOnline
            current.copy(
                networkOnline = next,
                toastMessage = if (next) "Network reconnected · Resuming downloads" else "Simulated offline network drop"
            )
        }
    }

    fun clearTempMediaCacheOnly() {
        _uiState.update { current ->
            current.copy(
                storageConfig = current.storageConfig.copy(
                    mediaBufferMb = 0,
                    thumbCacheMb = 24,
                    feedCacheMb = 8
                ),
                toastMessage = "Cleared stale temporary media & buffer caches"
            )
        }
    }

    fun updateStorageWifiOnly(wifiOnly: Boolean) {
        _uiState.update { current ->
            current.copy(
                storageConfig = current.storageConfig.copy(wifiOnly = wifiOnly),
                toastMessage = if (wifiOnly) "Downloads set to Wi-Fi only" else "Cellular downloads enabled"
            )
        }
    }

    fun updateAutoDeleteWatched(autoDelete: Boolean) {
        _uiState.update { current ->
            current.copy(
                storageConfig = current.storageConfig.copy(autoDeleteWatched = autoDelete),
                toastMessage = if (autoDelete) "Auto-delete completed titles ON" else "Auto-delete OFF"
            )
        }
    }

    fun requestProfileSwitch(target: ProfileType) {
        if (target == ProfileType.ADULT && !_uiState.value.adultUnlocked) {
            _uiState.update { it.copy(showPinDialogFor = target) }
            return
        }
        _uiState.update {
            it.copy(
                activeProfile = target,
                toastMessage = "Switched to ${target.label}"
            )
        }
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
        _uiState.update {
            it.copy(
                traktConnected = true,
                traktLastSync = "Just now",
                toastMessage = "Trakt watchlist & history synced"
            )
        }
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

    fun startPlayback(session: PlayerSession) {
        _uiState.update {
            it.copy(activePlayerSession = session, miniPlayerSession = null)
        }
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val playInfo = repository.getPlayInfo(session.title, 0, 0)
                val streamUrl = playInfo.streamList.firstOrNull()?.url
                if (!streamUrl.isNullOrBlank()) {
                    withContext(Dispatchers.Main) {
                        _uiState.update { current ->
                            current.activePlayerSession?.let { active ->
                                current.copy(activePlayerSession = active.copy(durationSeconds = 7200))
                            } ?: current
                        }
                    }
                }
            } catch (_: Exception) {}
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
                toastMessage = "Background Audio Mode active"
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
            current.copy(
                activeSourceId = source.id,
                toastMessage = "Switched to ${source.name} (${source.quality})"
            )
        }
    }

    fun simulateStreamFailover() {
        val sources = _uiState.value.streamSources
        val next = sources.firstOrNull { it.id != _uiState.value.activeSourceId } ?: sources.first()
        _uiState.update {
            it.copy(
                activeSourceId = next.id,
                toastMessage = "Failover: Auto-routed to ${next.name} (${next.pingMs}ms)"
            )
        }
    }

    fun openWatchParty(item: MediaTitle?) {
        _uiState.update { it.copy(showWatchPartyFor = item) }
    }

    fun addComment(titleId: String, body: String, hasSpoiler: Boolean) {
        val newComment = CommentItem(
            id = "c_${System.currentTimeMillis()}",
            author = "You",
            timestamp = "Just now",
            body = body,
            hasSpoiler = hasSpoiler,
            likes = 1
        )
        _uiState.update { current ->
            val list = current.commentsByTitle[titleId].orEmpty()
            current.copy(
                commentsByTitle = current.commentsByTitle + (titleId to (listOf(newComment) + list)),
                toastMessage = "Comment posted"
            )
        }
    }

    fun buildDeepLinkUrl(titleId: String): String = "https://flixvora.app/?title=$titleId"

    fun showToast(msg: String) {
        _uiState.update { it.copy(toastMessage = msg) }
    }

    fun clearToast() {
        _uiState.update { it.copy(toastMessage = null) }
    }
}
