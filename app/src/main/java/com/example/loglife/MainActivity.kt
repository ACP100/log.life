package com.example.loglife

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import com.example.loglife.navigation.SettingsDestination
import com.example.loglife.navigation.WidgetManagerDestination
import com.example.loglife.ui.main.MainScreen
import com.example.loglife.ui.theme.LoglifeTheme
import com.example.loglife.ui.widgets.WidgetSync
import com.example.loglife.navigation.NavigationViewModel

import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.example.loglife.data.settings.AppSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Main activity that hosts the LogLife application.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    companion object {
        /** Set by widget empty states ("Pick favourites"): open the Widget screen. */
        const val EXTRA_OPEN_WIDGET_SETTINGS = "open_widget_settings"

        /** Set by the Today widget's +: open Home with the log sheet up. */
        const val EXTRA_OPEN_LOG_SHEET = "open_log_sheet"

        /** Set by a reminder's "It ended earlier…": ask when that session ended. */
        const val EXTRA_FIX_END_INSTANCE = "fix_end_instance"
    }

    private val navViewModel: NavigationViewModel by viewModels()

    @Inject
    lateinit var settings: AppSettingsRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Rebind placed widgets on every app start so they always render with
        // the current layout/code (e.g. ListView -> StackView migrations)
        // without the user having to remove/re-add them.
        try {
            WidgetSync.refreshAll(this)
        } catch (_: Exception) {
        }
        if (savedInstanceState == null) handleWidgetIntent(intent)
        setContent {
            val appSettings by settings.settings.collectAsState()
            LoglifeTheme(themeMode = appSettings.theme) {
                MainScreen(navViewModel = navViewModel)
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleWidgetIntent(intent)
    }

    private fun handleWidgetIntent(intent: Intent?) {
        when {
            intent?.getBooleanExtra(EXTRA_OPEN_WIDGET_SETTINGS, false) == true -> {
                navViewModel.navigateTo(SettingsDestination)
                navViewModel.navigateTo(WidgetManagerDestination)
            }
            intent?.getBooleanExtra(EXTRA_OPEN_LOG_SHEET, false) == true ->
                navViewModel.requestLogSheet()
            (intent?.getLongExtra(EXTRA_FIX_END_INSTANCE, -1L) ?: -1L) != -1L ->
                navViewModel.requestFixEnd(intent!!.getLongExtra(EXTRA_FIX_END_INSTANCE, -1L))
        }
    }
}