package com.flixvora

import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.flixvora.data.network.LocalMovieBoxServer
import com.flixvora.ui.screens.MovieBoxApp
import com.flixvora.ui.theme.MyApplicationTheme
import com.flixvora.ui.viewmodel.MovieViewModel

class MainActivity : AppCompatActivity() {
    private val viewModel: MovieViewModel by viewModels()
    private var localServer: LocalMovieBoxServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Ask for notification permission so the download progress notification
        // can appear on Android 13+ (downloads still work without it).
        if (android.os.Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9101)
        }
        
        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Start local MovieBox proxy server automatically inside the phone when opened
        localServer = LocalMovieBoxServer(applicationContext, 3000)
        localServer?.start()

        val imageDispatcher = okhttp3.Dispatcher().apply {
            maxRequests = 128
            maxRequestsPerHost = 48
        }

        // Dedicated high-speed OkHttpClient for image/poster loading with parallel connection pool
        val imageOkHttpClient = okhttp3.OkHttpClient.Builder()
            .dispatcher(imageDispatcher)
            .connectionPool(okhttp3.ConnectionPool(64, 15, java.util.concurrent.TimeUnit.MINUTES))
            .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
            .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .build()

        // Configure high-performance Coil ImageLoader with instant Memory & Disk cache and SVG support
        val imageLoader = coil.ImageLoader.Builder(applicationContext)
            .components {
                add(coil.decode.SvgDecoder.Factory())
            }
            .okHttpClient(imageOkHttpClient)
            .memoryCache {
                coil.memory.MemoryCache.Builder(applicationContext)
                    .maxSizePercent(0.35) // 35% app memory dedicated to instant poster display
                    .strongReferencesEnabled(true)
                    .build()
            }
            .diskCache {
                coil.disk.DiskCache.Builder()
                    .directory(applicationContext.cacheDir.resolve("image_cache"))
                    .maxSizeBytes(250L * 1024 * 1024) // 250MB persistent fast disk cache
                    .build()
            }
            .respectCacheHeaders(false) // Cache posters aggressively regardless of remote headers
            .crossfade(false) // Instant 0ms transition for zero display lag
            .allowHardware(true) // Direct GPU rendering for fastest draw speed
            .allowRgb565(true) // Reduce memory footprint by 50% for 2x faster bitmap decode
            .build()
        coil.Coil.setImageLoader(imageLoader)

        // Resume any downloads interrupted by a previous process death.
        try {
            com.flixvora.download.DownloadService.resumePendingIfAny(applicationContext)
        } catch (_: Throwable) {}

        enableEdgeToEdge()
        applySystemBars()
        setContent {
            MyApplicationTheme {
                MovieBoxApp(viewModel = viewModel)
            }
        }

        handleDeepLink(intent)
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleDeepLink(intent)
    }

    /**
     * Routes oneroom:// and moviebox:// links to the matching screen. Only
     * subject detail links are resolved for now; unknown hosts are ignored.
     */
    private fun handleDeepLink(intent: android.content.Intent?) {
        val data = intent?.data ?: return
        val subjectId = when {
            data.queryParameterNames?.contains("id") == true -> data.getQueryParameter("id")
            data.queryParameterNames?.contains("subjectId") == true -> data.getQueryParameter("subjectId")
            else -> data.lastPathSegment
        } ?: return
        if (subjectId.isBlank()) return
        viewModel.selectSubject(
            com.flixvora.data.model.MovieSubject(
                id = subjectId,
                name = "",
                poster = ""
            )
        )
    }

    /**
     * Edge-to-edge with both system bars visible. Content draws behind them and
     * the screens pad by the real insets, so the status bar sits over the header
     * and the three-button navigation bar never overlaps the tab bar.
     */
    private fun applySystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
            // Light-on-dark icons: the canvas is near black everywhere.
            isAppearanceLightStatusBars = false
            isAppearanceLightNavigationBars = false
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
    }

    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        // Dialogs, the keyboard and task switches can drop the bar visibility.
        if (hasFocus) applySystemBars()
    }

    override fun onResume() {
        super.onResume()
        applySystemBars()
    }

    override fun onDestroy() {
        super.onDestroy()
        localServer?.stop()
    }

    override fun onUserLeaveHint() {
        super.onUserLeaveHint()
        if (viewModel.playInfo != null) {
            val pipBuilder = android.app.PictureInPictureParams.Builder()
            val aspectRatio = android.util.Rational(16, 9)
            pipBuilder.setAspectRatio(aspectRatio)
            enterPictureInPictureMode(pipBuilder.build())
        }
    }
}
