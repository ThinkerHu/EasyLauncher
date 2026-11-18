package com.vincenthu.launcher

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.ui.graphics.Color
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {
    private val model by viewModels<LauncherModel>()
    private val widgetHost by lazy { WidgetHostController(this, model) }
    private val homeRequest = mutableIntStateOf(0)
    private val packageReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) { model.refresh() }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        updateDesktopSystemBars(night = weatherWallpaperScene(model.weatherState, model.weatherWallpaper, System.currentTimeMillis())?.isDay == false)
        ContextCompat.registerReceiver(this, packageReceiver, IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED); addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_CHANGED); addAction(Intent.ACTION_PACKAGE_REPLACED); addDataScheme("package")
        }, ContextCompat.RECEIVER_EXPORTED)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(
                primary = Color(0xFF426D87), onPrimary = Color.White,
                surface = Color(0xFFF4F8FA), onSurface = Color(0xFF29465A),
                secondaryContainer = Color(0xFFD5E5EC), outline = Color(0xFF97ADB9)
            )) { LauncherHome(model, homeRequest.intValue, widgetHost) }
        }
    }

    override fun onStart() { super.onStart(); widgetHost.start() }
    internal fun updateDesktopSystemBars(night: Boolean) {
        val transparent = android.graphics.Color.TRANSPARENT
        val style = if (night) SystemBarStyle.dark(transparent) else SystemBarStyle.light(transparent, transparent)
        enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
    }
    override fun onResume() {
        super.onResume()
        model.refresh()
        model.refreshDefaultHome()
    }
    override fun onStop() { widgetHost.stop(); super.onStop() }
    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        if (intent.hasCategory(Intent.CATEGORY_HOME)) homeRequest.intValue++
    }
    override fun onDestroy() { widgetHost.cancelPending(); unregisterReceiver(packageReceiver); super.onDestroy() }
}
