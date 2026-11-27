package com.flixvora

import android.app.PictureInPictureParams
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Rational
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.flixvora.data.network.LocalMovieBoxServer

class MainActivity : AppCompatActivity() {

    private var deepLinkTitleId by mutableStateOf<String?>(null)
    private var localServer: LocalMovieBoxServer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Notification permission for download progress on Android 13+
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(android.Manifest.permission.POST_NOTIFICATIONS) !=
            android.content.pm.PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 9101)
        }

        try {
            com.google.android.gms.cast.framework.CastContext.getSharedInstance(this)
        } catch (_: Exception) {}

        // Background server for MovieBox API
        localServer = LocalMovieBoxServer(applicationContext, 3000)
        localServer?.start()

        // Resume pending downloads
        try {
            com.flixvora.download.DownloadService.resumePendingIfAny(applicationContext)
        } catch (_: Throwable) {}

        enableEdgeToEdge()
        applySystemBars()
        deepLinkTitleId = extractTitleId(intent?.data)

        setContent {
            FlixvoraApp(
                deepLinkTitleId = deepLinkTitleId,
                onEnterPictureInPicture = { enterPip() }
            )
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractTitleId(intent.data)?.let { deepLinkTitleId = it }
    }

    private fun extractTitleId(uri: Uri?): String? {
        if (uri == null) return null
        return uri.getQueryParameter("title") 
            ?: uri.getQueryParameter("id") 
            ?: uri.getQueryParameter("subjectId") 
            ?: uri.lastPathSegment
    }

    private fun enterPip() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
            packageManager.hasSystemFeature(android.content.pm.PackageManager.FEATURE_PICTURE_IN_PICTURE)
        ) {
            try {
                val params = PictureInPictureParams.Builder()
                    .setAspectRatio(Rational(16, 9))
                    .build()
                enterPictureInPictureMode(params)
            } catch (_: Exception) {}
        }
    }

    private fun applySystemBars() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            window.attributes = window.attributes.apply {
                layoutInDisplayCutoutMode =
                    android.view.WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
            }
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            show(WindowInsetsCompat.Type.systemBars())
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
}
