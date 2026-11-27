package com.flixvora.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.flixvora.data.*
import com.flixvora.ui.theme.FlixvoraAccentTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

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
    val watchlist: Set<String> = setOf("skyfall-citadel", "phantom-shinobi", "neon-drift-77"),
    val remindedReleases: Set<String> = emptySet(),
    val downloads: List<DownloadTask> = SampleCatalog.initialDownloads,
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
    val commentsByTitle: Map<String, List<CommentItem>> = mapOf(
        "skyfall-citadel" to listOf(
            CommentItem("c1", "Riya Sharma", "2h ago", "Dolby Vision orbital storm sequence looks crisp on OLED.", false, 42),
            CommentItem("c2", "Arjun Mehta", "5h ago", "When Kiran overrides the magnetic shield core, soundtrack drops hard.", true, 19)
        )
    ),
    val toastMessage: String? = null
) {
    val safeCatalog: List<MediaTitle>
        get() = SampleCatalog.filterCatalogBySafety(SampleCatalog.mediaCatalog, activeProfile, adultUnlocked)
}

class FlixvoraViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(FlixvoraUiState())
    val uiState: StateFlow<FlixvoraUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            while (true) {
                delay(1500)
                advanceDownloadsTick()
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
    }

    fun setGenre(genre: String) {
        _uiState.update { it.copy(selectedGenre = genre) }
    }

    fun setOtt(ott: String) {
        _uiState.update { it.copy(selectedOtt = ott) }
    }

    fun toggleMood(moodId: String?) {
        _uiState.update {
            it.copy(selectedMoodId = if (it.selectedMoodId == moodId) null else moodId)
        }
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun openDetail(item: MediaTitle) {
        _uiState.update { it.copy(selectedDetail = item) }
    }

    fun openDeepLinkedTitle(titleId: String) {
        val match = _uiState.value.safeCatalog.firstOrNull { it.id == titleId }
        if (match != null) {
            _uiState.update { it.copy(selectedDetail = match, activeTab = MainNavTab.HOME) }
            showToast("Opened shared title: ${match.title}")
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
                toastMessage = "Queued WorkManager chunked download: ${item.title}"
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
        _uiState.update {
            it.copy(
                downloads = it.downloads.filterNot { d -> d.id == id },
                toastMessage = "Removed download"
            )
        }
    }

    fun clearDownloads() {
        _uiState.update { it.copy(downloads = emptyList(), toastMessage = "Cleared all offline downloads") }
    }

    fun toggleNetworkSimulation() {
        _uiState.update { current ->
            val online = !current.networkOnline
            val msg = if (online) {
                if (current.storageConfig.wifiOnly) "Wi-Fi restored — WorkManager resumed chunks from byte offset"
                else "Network restored — WorkManager resumed downloads"
            } else {
                "Network lost — WorkManager paused chunks safely"
            }
            current.copy(networkOnline = online, toastMessage = msg)
        }
    }

    fun clearTempMediaCacheOnly() {
        _uiState.update { current ->
            val freed = current.storageConfig.totalTempCacheMb
            current.copy(
                storageConfig = current.storageConfig.copy(
                    mediaBufferMb = 0,
                    thumbCacheMb = 0,
                    feedCacheMb = 0,
                    dbCacheMb = 0
                ),
                toastMessage = if (freed > 0) "Cleared $freed MB temporary buffer. Watchlist, history & settings preserved." else "Temporary cache already clean"
            )
        }
    }

    fun updateStorageWifiOnly(wifiOnly: Boolean) {
        _uiState.update {
            it.copy(storageConfig = it.storageConfig.copy(wifiOnly = wifiOnly))
        }
    }

    fun updateAutoDeleteWatched(autoDelete: Boolean) {
        _uiState.update {
            it.copy(storageConfig = it.storageConfig.copy(autoDeleteWatched = autoDelete))
        }
    }

    fun requestProfileSwitch(target: ProfileType) {
        if (target == ProfileType.ADULT && !_uiState.value.adultUnlocked) {
            _uiState.update { it.copy(showPinDialogFor = ProfileType.ADULT) }
            return
        }
        _uiState.update {
            it.copy(
                activeProfile = target,
                adultUnlocked = target == ProfileType.ADULT,
                toastMessage = "Switched to ${target.label}"
            )
        }
    }

    fun verifyPinAndUnlock(pin: String): Boolean {
        val current = _uiState.value
        return if (pin == current.parentalPin) {
            _uiState.update {
                it.copy(
                    activeProfile = current.showPinDialogFor ?: ProfileType.ADULT,
                    adultUnlocked = true,
                    showPinDialogFor = null,
                    toastMessage = "18+ PIN verified"
                )
            }
            true
        } else {
            false
        }
    }

    fun dismissPinDialog() {
        _uiState.update { it.copy(showPinDialogFor = null) }
    }

    fun setAccentTheme(theme: FlixvoraAccentTheme) {
        _uiState.update { it.copy(accentTheme = theme, toastMessage = "Theme set to ${theme.label}") }
    }

    fun toggleTvMode() {
        _uiState.update {
            val next = !it.isTvMode
            it.copy(
                isTvMode = next,
                toastMessage = if (next) "Android TV 10-Foot Leanback Mode active" else "Returned to Mobile UI"
            )
        }
    }

    fun toggleBackgroundAudio() {
        _uiState.update { current ->
            val next = !current.backgroundAudioEnabled
            current.copy(
                backgroundAudioEnabled = next,
                miniPlayerSession = if (!next) null else current.miniPlayerSession,
                toastMessage = if (next) "Background Audio & MediaSession enabled" else "Background Audio disabled"
            )
        }
    }

    fun syncTrakt() {
        _uiState.update {
            it.copy(
                traktConnected = true,
                traktLastSync = "Just now",
                toastMessage = "Synced watch history & recommendations with Trakt.tv"
            )
        }
    }

    fun toggleComingSoonReminder(id: String, title: String) {
        _uiState.update { current ->
            val added = !current.remindedReleases.contains(id)
            val next = if (added) current.remindedReleases + id else current.remindedReleases - id
            current.copy(
                remindedReleases = next,
                toastMessage = if (added) "Release alert set for $title" else "Removed reminder for $title"
            )
        }
    }

    fun startPlayback(session: PlayerSession) {
        _uiState.update {
            it.copy(activePlayerSession = session, miniPlayerSession = null)
        }
    }

    fun closePlayer() {
        _uiState.update { it.copy(activePlayerSession = null) }
    }

    fun minimizeToBackgroundAudio() {
        _uiState.update { current ->
            if (!current.backgroundAudioEnabled) {
                return@update current.copy(toastMessage = "Enable Background Audio in Me → Settings first")
            }
            current.copy(
                miniPlayerSession = current.activePlayerSession,
                activePlayerSession = null,
                miniPlayerPlaying = true,
                toastMessage = "Background Audio Mode active via MediaSessionService"
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
                activePlayerSession = current.activePlayerSession?.copy(
                    subtitle = "${source.protocol} · ${source.quality}"
                ),
                toastMessage = "Switched stream mirror: ${source.name}"
            )
        }
    }

    fun simulateStreamFailover() {
        _uiState.update { current ->
            val idx = current.streamSources.indexOfFirst { it.id == current.activeSourceId }.coerceAtLeast(0)
            val next = current.streamSources[(idx + 1) % current.streamSources.size]
            current.copy(
                activeSourceId = next.id,
                activePlayerSession = current.activePlayerSession?.copy(
                    subtitle = "${next.protocol} · ${next.quality}"
                ),
                toastMessage = "Auto-failover switched seamlessly to ${next.name}"
            )
        }
    }

    fun openWatchParty(item: MediaTitle?) {
        _uiState.update { it.copy(showWatchPartyFor = item) }
    }

    fun addComment(titleId: String, body: String, hasSpoiler: Boolean) {
        val clean = body.trim()
        if (clean.isEmpty()) return
        val comment = CommentItem(
            id = "c-${System.currentTimeMillis()}",
            author = "Flixvora Member",
            timestamp = "Just now",
            body = clean,
            hasSpoiler = hasSpoiler,
            likes = 1
        )
        _uiState.update { current ->
            val existing = current.commentsByTitle[titleId].orEmpty()
            current.copy(
                commentsByTitle = current.commentsByTitle + (titleId to (listOf(comment) + existing)),
                toastMessage = "Posted comment"
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
