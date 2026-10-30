package com.reevan.godfatherreevz

import android.graphics.Color
import android.os.Bundle
import android.view.View
import android.view.ViewTreeObserver
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.reevan.godfatherreevz.ui.GodfatherApp
import com.reevan.godfatherreevz.ui.settings.PreferencesViewModel
import com.reevan.godfatherreevz.ui.theme.GodfatherReevzTheme
import com.reevan.godfatherreevz.util.LocalCaller
import com.reevan.godfatherreevz.util.TodayClock
import com.reevan.godfatherreevz.util.clockChanges
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    // Same instance Settings gets from viewModel(), so a theme change applies immediately.
    private val preferences: PreferencesViewModel by viewModels { PreferencesViewModel.Factory }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        holdFirstFrameUntilSettingsLoad()

        // Restarted on every return to the foreground, so "today" is re-read after the phone has
        // slept through midnight, then kept ticking while the app stays on screen. A clock, time
        // zone or date change restarts it too: the midnight delay can't see the wall clock jump.
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                clockChanges(this@MainActivity).collectLatest {
                    TodayClock.instance.tickAtMidnight()
                }
            }
        }

        setContent {
            val settings by preferences.settings.collectAsStateWithLifecycle()
            val theme = settings?.theme ?: return@setContent
            val dark = theme.isDark

            // enableEdgeToEdge() alone picks status bar icon colours from the *phone's* light/dark
            // setting. Re-applying it with the app theme's darkness keeps them legible when the two
            // disagree.
            DisposableEffect(dark) {
                val style = SystemBarStyle.auto(Color.TRANSPARENT, Color.TRANSPARENT) { dark }
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
                onDispose {}
            }

            GodfatherReevzTheme(theme = theme) {
                CompositionLocalProvider(LocalCaller provides (application as GodfatherApplication).caller) {
                    GodfatherApp()
                }
            }
        }
    }

    /**
     * Keeps the system splash screen up until the stored theme has been read — a single-row
     * query, so a few milliseconds — instead of drawing the default theme and then switching.
     */
    private fun holdFirstFrameUntilSettingsLoad() {
        val content: View = findViewById(android.R.id.content)
        content.viewTreeObserver.addOnPreDrawListener(
            object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (preferences.settings.value == null) return false
                    content.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            },
        )
    }
}
