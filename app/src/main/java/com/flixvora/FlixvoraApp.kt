package com.flixvora

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.flixvora.data.*
import com.flixvora.ui.components.*
import com.flixvora.ui.screens.*
import com.flixvora.ui.state.FlixvoraViewModel
import com.flixvora.ui.theme.BorderHairline
import com.flixvora.ui.theme.FlixvoraTheme
import com.flixvora.ui.theme.LocalAccentTheme
import com.flixvora.ui.theme.TextSecondary

@Composable
fun FlixvoraApp(
    deepLinkTitleId: String? = null,
    onEnterPictureInPicture: () -> Unit = {},
    viewModel: FlixvoraViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(deepLinkTitleId) {
        if (!deepLinkTitleId.isNullOrBlank()) {
            viewModel.openDeepLinkedTitle(deepLinkTitleId)
        }
    }

    LaunchedEffect(uiState.toastMessage) {
        val msg = uiState.toastMessage
        if (!msg.isNullOrBlank()) {
            snackbarHostState.showSnackbar(msg, duration = SnackbarDuration.Short)
            viewModel.clearToast()
        }
    }

    FlixvoraTheme(accentTheme = uiState.accentTheme) {
        val accent = LocalAccentTheme.current

        Scaffold(
            containerColor = accent.canvas,
            snackbarHost = {
                SnackbarHost(hostState = snackbarHostState) { data ->
                    Snackbar(
                        snackbarData = data,
                        containerColor = accent.surface,
                        contentColor = Color.White,
                        actionColor = accent.primary
                    )
                }
            },
            bottomBar = {
                if (!uiState.isTvMode && uiState.selectedDetail == null && uiState.activePlayerSession == null) {
                    Column {
                        uiState.miniPlayerSession?.let { mini ->
                            PersistentMiniPlayerBar(
                                session = mini,
                                isPlaying = uiState.miniPlayerPlaying,
                                onTogglePlay = { viewModel.toggleMiniPlayerPlay() },
                                onExpand = { viewModel.expandMiniPlayer() },
                                onClose = { viewModel.closeMiniPlayer() }
                            )
                        }
                        NavigationBar(
                            containerColor = accent.surface,
                            tonalElevation = 0.dp
                        ) {
                            MainNavTab.entries.forEach { tab ->
                                val selected = uiState.activeTab == tab
                                val icon = when (tab) {
                                    MainNavTab.HOME -> Icons.Default.Home
                                    MainNavTab.LIVE_TV -> Icons.Default.LiveTv
                                    MainNavTab.WATCHLIST -> Icons.Default.Bookmark
                                    MainNavTab.DOWNLOADS -> Icons.Default.Download
                                    MainNavTab.ME -> Icons.Default.Person
                                }
                                NavigationBarItem(
                                    selected = selected,
                                    onClick = { viewModel.selectTab(tab) },
                                    icon = { Icon(icon, contentDescription = tab.label) },
                                    label = { Text(tab.label, fontSize = 10.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium) },
                                    colors = NavigationBarItemDefaults.colors(
                                        selectedIconColor = accent.primary,
                                        selectedTextColor = accent.primary,
                                        unselectedIconColor = TextSecondary,
                                        unselectedTextColor = TextSecondary,
                                        indicatorColor = accent.primary.copy(alpha = 0.14f)
                                    )
                                )
                            }
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(accent.canvas)
                    .padding(innerPadding)
            ) {
                when {
                    uiState.isTvMode -> {
                        TvLeanbackScreen(
                            catalog = uiState.safeCatalog,
                            onPlayTitle = { item ->
                                viewModel.startPlayback(
                                    PlayerSession(
                                        title = item.title,
                                        subtitle = "${item.year} · ${item.quality}",
                                        backdropUrl = item.backdropUrl,
                                        durationSeconds = 7200,
                                        tmdbId = item.tmdbId,
                                        audioTracks = item.audioTracks,
                                        introEndSeconds = item.introEndSeconds
                                    )
                                )
                            },
                            onWatchChannel = { ch ->
                                viewModel.startPlayback(
                                    PlayerSession(
                                        title = ch.name,
                                        subtitle = "CH ${ch.number} · ${ch.currentProgram}",
                                        backdropUrl = ch.previewUrl,
                                        durationSeconds = 3600,
                                        tmdbId = 900101,
                                        audioTracks = SampleCatalog.defaultMovieTracks,
                                        isLive = true
                                    )
                                )
                            },
                            onExitTvMode = { viewModel.toggleTvMode() }
                        )
                    }

                    uiState.selectedDetail != null -> {
                        val detail = uiState.selectedDetail!!
                        DetailScreen(
                            title = detail,
                            inWatchlist = uiState.watchlist.contains(detail.id),
                            comments = uiState.commentsByTitle[detail.id].orEmpty(),
                            shareUrl = viewModel.buildDeepLinkUrl(detail.id),
                            onBack = { viewModel.closeDetail() },
                            onPlay = {
                                viewModel.startPlayback(
                                    PlayerSession(
                                        title = detail.title,
                                        subtitle = "${detail.year} · ${detail.quality}",
                                        backdropUrl = detail.backdropUrl,
                                        durationSeconds = 7200,
                                        tmdbId = detail.tmdbId,
                                        audioTracks = detail.audioTracks,
                                        introEndSeconds = detail.introEndSeconds
                                    )
                                )
                            },
                            onPlayEpisode = { seasonNum, ep ->
                                viewModel.startPlayback(
                                    PlayerSession(
                                        title = "${detail.title} · S${seasonNum}E${ep.number}",
                                        subtitle = ep.title,
                                        backdropUrl = detail.backdropUrl,
                                        durationSeconds = 1800,
                                        tmdbId = detail.tmdbId,
                                        audioTracks = detail.audioTracks,
                                        introEndSeconds = detail.introEndSeconds
                                    )
                                )
                            },
                            onDownloadQuality = { qualityLabel, sizeMb ->
                                viewModel.enqueueDownload(detail, qualityLabel, sizeMb)
                            },
                            onDownloadEpisode = { seasonNum, ep ->
                                viewModel.enqueueDownload(detail, "S$seasonNum · Ep ${ep.number} · ${ep.title}", ep.sizeMb)
                            },
                            onToggleWatchlist = { viewModel.toggleWatchlist(detail.id) },
                            onOpenWatchParty = { viewModel.openWatchParty(detail) },
                            onOpenSources = { viewModel.setStreamResolverOpen(true) },
                            onPostComment = { body, spoiler -> viewModel.addComment(detail.id, body, spoiler) },
                            onNotify = { viewModel.showToast(it) }
                        )
                    }

                    else -> {
                        AnimatedContent(
                            targetState = uiState.activeTab,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "MainTabTransition"
                        ) { tab ->
                            when (tab) {
                                MainNavTab.HOME -> HomeScreen(
                                    uiState = uiState,
                                    onSearchChange = { viewModel.updateSearchQuery(it) },
                                    onSelectCategory = { viewModel.setCategory(it) },
                                    onSelectGenre = { viewModel.setGenre(it) },
                                    onSelectOtt = { viewModel.setOtt(it) },
                                    onToggleMood = { viewModel.toggleMood(it) },
                                    onSyncTrakt = { viewModel.syncTrakt() },
                                    onToggleReminder = { rel -> viewModel.toggleComingSoonReminder(rel.id, rel.title) },
                                    onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                    onPlayTitle = { item ->
                                        viewModel.startPlayback(
                                            PlayerSession(
                                                title = item.title,
                                                subtitle = "${item.year} · ${item.quality}",
                                                backdropUrl = item.backdropUrl,
                                                durationSeconds = 7200,
                                                tmdbId = item.tmdbId,
                                                audioTracks = item.audioTracks,
                                                introEndSeconds = item.introEndSeconds
                                            )
                                        )
                                    },
                                    onDownloadTitle = { item -> viewModel.enqueueDownload(item) },
                                    onOpenDetail = { viewModel.openDetail(it) }
                                )

                                MainNavTab.LIVE_TV -> LiveTvScreen(
                                    channels = uiState.displayChannels,
                                    backgroundAudioEnabled = uiState.backgroundAudioEnabled,
                                    onWatchChannel = { ch ->
                                        viewModel.startPlayback(
                                            PlayerSession(
                                                title = ch.name,
                                                subtitle = "CH ${ch.number} · ${ch.currentProgram}",
                                                backdropUrl = ch.previewUrl,
                                                durationSeconds = 3600,
                                                tmdbId = 900101,
                                                audioTracks = SampleCatalog.defaultMovieTracks,
                                                isLive = true
                                            )
                                        )
                                    },
                                    onPlayAudioCommentary = { ch ->
                                        viewModel.startPlayback(
                                            PlayerSession(
                                                title = "${ch.name} (Audio Commentary)",
                                                subtitle = "CH ${ch.number} · ${ch.currentProgram}",
                                                backdropUrl = ch.previewUrl,
                                                durationSeconds = 3600,
                                                tmdbId = 900101,
                                                audioTracks = SampleCatalog.defaultMovieTracks,
                                                isLive = true
                                            )
                                        )
                                        viewModel.minimizeToBackgroundAudio()
                                    }
                                )

                                MainNavTab.WATCHLIST -> WatchlistScreen(
                                    uiState = uiState,
                                    onOpenDetail = { viewModel.openDetail(it) },
                                    onPlayTitle = { item ->
                                        viewModel.startPlayback(
                                            PlayerSession(
                                                title = item.title,
                                                subtitle = "${item.year} · ${item.quality}",
                                                backdropUrl = item.backdropUrl,
                                                durationSeconds = 7200,
                                                tmdbId = item.tmdbId,
                                                audioTracks = item.audioTracks,
                                                introEndSeconds = item.introEndSeconds
                                            )
                                        )
                                    },
                                    onRemoveWatchlist = { viewModel.toggleWatchlist(it) },
                                    onClearAll = { viewModel.clearWatchlist() }
                                )

                                MainNavTab.DOWNLOADS -> DownloadsScreen(
                                    uiState = uiState,
                                    onToggleNetwork = { viewModel.toggleNetworkSimulation() },
                                    onToggleTaskStatus = { viewModel.toggleDownloadStatus(it) },
                                    onRemoveTask = { viewModel.removeDownload(it) },
                                    onClearAll = { viewModel.clearDownloads() }
                                )

                                MainNavTab.ME -> MeProfileScreen(
                                    uiState = uiState,
                                    onSwitchProfile = { viewModel.requestProfileSwitch(it) },
                                    onSelectTheme = { viewModel.setAccentTheme(it) },
                                    onToggleTvMode = { viewModel.toggleTvMode() },
                                    onToggleBackgroundAudio = { viewModel.toggleBackgroundAudio() },
                                    onOpenStreamResolver = { viewModel.setStreamResolverOpen(true) },
                                    onSyncTrakt = { viewModel.syncTrakt() },
                                    onToggleWifiOnly = { viewModel.updateStorageWifiOnly(it) },
                                    onToggleAutoDelete = { viewModel.updateAutoDeleteWatched(it) },
                                    onClearTempCache = { viewModel.clearTempMediaCacheOnly() },
                                    onClearDownloads = { viewModel.clearDownloads() },
                                    onCheckUpdates = { viewModel.showToast("Flixvora v4.2.0 is up to date via GitHub Releases") }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen ExoPlayer Overlay
        uiState.activePlayerSession?.let { session ->
            VideoPlayerOverlay(
                session = session,
                backgroundAudioEnabled = uiState.backgroundAudioEnabled,
                onClose = { viewModel.closePlayer() },
                onEnterPip = onEnterPictureInPicture,
                onOpenSourceResolver = { viewModel.setStreamResolverOpen(true) },
                onMinimizeToBackground = { viewModel.minimizeToBackgroundAudio() },
                onNotify = { viewModel.showToast(it) }
            )
        }

        // Multi-Source Backup Stream Resolver Modal
        if (uiState.showStreamResolver) {
            val activeTitle = uiState.activePlayerSession?.title ?: uiState.selectedDetail?.title ?: "Skyfall Citadel"
            val activeTmdb = uiState.activePlayerSession?.tmdbId ?: uiState.selectedDetail?.tmdbId ?: 948210
            StreamResolverSheet(
                mediaTitle = activeTitle,
                tmdbId = activeTmdb,
                sources = uiState.streamSources,
                activeSourceId = uiState.activeSourceId,
                onSelectSource = { viewModel.selectStreamSource(it) },
                onSimulateFailover = { viewModel.simulateStreamFailover() },
                onClose = { viewModel.setStreamResolverOpen(false) }
            )
        }

        // Parental PIN Lock Dialog
        if (uiState.showPinDialogFor != null) {
            PinLockDialog(
                onVerify = { pin -> viewModel.verifyPinAndUnlock(pin) },
                onDismiss = { viewModel.dismissPinDialog() }
            )
        }

        // Watch Party Modal
        uiState.showWatchPartyFor?.let { title ->
            WatchPartyDialog(
                title = title,
                onStartWatchParty = {
                    viewModel.openWatchParty(null)
                    viewModel.startPlayback(
                        PlayerSession(
                            title = "${title.title} (Watch Party)",
                            subtitle = "Synced Room FX-8492 · ${title.quality}",
                            backdropUrl = title.backdropUrl,
                            durationSeconds = 7200,
                            tmdbId = title.tmdbId,
                            audioTracks = title.audioTracks,
                            introEndSeconds = title.introEndSeconds
                        )
                    )
                },
                onDismiss = { viewModel.openWatchParty(null) }
            )
        }
    }
}
