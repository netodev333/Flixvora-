package com.flixvora

import android.app.Activity
import android.widget.Toast
import androidx.activity.compose.BackHandler
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
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
    val context = LocalContext.current
    var lastBackPress by remember { mutableStateOf(0L) }

    // System back pops one layer (player, detail, filters, tab) before exiting.
    BackHandler {
        if (!viewModel.handleBack()) {
            val now = System.currentTimeMillis()
            if (now - lastBackPress < 2000L) {
                (context as? Activity)?.finish()
            } else {
                lastBackPress = now
                Toast.makeText(context, "Press back again to exit", Toast.LENGTH_SHORT).show()
            }
        }
    }

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
                            channels = uiState.displayChannels,
                            onPlayTitle = { item -> viewModel.playTitle(item) },
                            onWatchChannel = { ch -> viewModel.playChannel(ch) },
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
                            recommendations = uiState.detailRecommendations,
                            downloadQualities = uiState.detailQualities,
                            onOpenRecommendation = { viewModel.openDetail(it) },
                            onPlay = { viewModel.playTitle(detail) },
                            onPlayEpisode = { seasonNum, ep -> viewModel.playEpisode(detail, seasonNum, ep) },
                            onDownloadQuality = { qualityLabel, sizeMb ->
                                viewModel.enqueueDownload(detail, qualityLabel, sizeMb)
                            },
                            onDownloadEpisode = { seasonNum, ep ->
                                viewModel.enqueueEpisodeDownload(detail, seasonNum, ep)
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
                                    onRefresh = { viewModel.refresh() },
                                    onSearchChange = { viewModel.updateSearchQuery(it) },
                                    onSelectCategory = { viewModel.setCategory(it) },
                                    onSelectGenre = { viewModel.setGenre(it) },
                                    onSelectOtt = { viewModel.setOtt(it) },
                                    onToggleMood = { viewModel.toggleMood(it) },
                                    onToggleReminder = { rel -> viewModel.toggleComingSoonReminder(rel.id, rel.title) },
                                    onToggleWatchlist = { viewModel.toggleWatchlist(it) },
                                    onPlayTitle = { item -> viewModel.playTitle(item) },
                                    onDownloadTitle = { item -> viewModel.enqueueDownload(item) },
                                    onOpenDetail = { viewModel.openDetail(it) }
                                )

                                MainNavTab.LIVE_TV -> LiveTvScreen(
                                    channels = uiState.displayChannels,
                                    backgroundAudioEnabled = uiState.backgroundAudioEnabled,
                                    onWatchChannel = { ch -> viewModel.playChannel(ch) },
                                    onPlayAudioCommentary = { ch -> viewModel.playChannel(ch) }
                                )

                                MainNavTab.WATCHLIST -> WatchlistScreen(
                                    uiState = uiState,
                                    onOpenDetail = { viewModel.openDetail(it) },
                                    onPlayTitle = { item -> viewModel.playTitle(item) },
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
                                    onToggleBackgroundAudio = { viewModel.toggleBackgroundAudio() },
                                    onToggleWifiOnly = { viewModel.updateStorageWifiOnly(it) },
                                    onToggleAutoDelete = { viewModel.updateAutoDeleteWatched(it) },
                                    onClearTempCache = { viewModel.clearTempMediaCacheOnly() },
                                    onClearDownloads = { viewModel.clearDownloads() },
                                    onCheckUpdates = { viewModel.checkForUpdates(isManual = true) },
                                    onDownloadUpdate = { viewModel.downloadUpdate() }
                                )
                            }
                        }
                    }
                }
            }
        }

        // Fullscreen ExoPlayer Overlay
        uiState.activePlayerSession?.let { session ->
            RealPlayerOverlay(
                session = session,
                onClose = { viewModel.closePlayer() },
                onEnterPip = onEnterPictureInPicture,
                onNotify = { viewModel.showToast(it) },
                onPlayerError = { viewModel.playNextStreamSource() },
                onProgress = { position, duration -> viewModel.saveProgress(session, position, duration) }
            )
        }

        // Multi-Source Backup Stream Resolver Modal
        if (uiState.showStreamResolver) {
            val activeTitle = uiState.activePlayerSession?.title ?: uiState.selectedDetail?.title ?: "Stream sources"
            val activeTmdb = uiState.activePlayerSession?.tmdbId ?: uiState.selectedDetail?.tmdbId ?: 0
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
                    viewModel.playTitle(title)
                },
                onDismiss = { viewModel.openWatchParty(null) }
            )
        }
    }
}
