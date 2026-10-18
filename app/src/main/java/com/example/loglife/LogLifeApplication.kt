package com.example.loglife

import android.app.Application
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import com.example.loglife.notifications.SessionNotifications
import com.example.loglife.ui.widgets.WidgetSync
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltAndroidApp
class LogLifeApplication : Application() {

    // Exposed for the app widget provider, which runs outside Hilt's
    // activity/fragment injection and reads repositories via the Application.
    @Inject
    lateinit var eventTypeRepository: EventTypeRepository

    @Inject
    lateinit var eventInstanceRepository: EventInstanceRepository

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    override fun onCreate() {
        super.onCreate()
        SessionNotifications.createChannels(this)
        // App -> widget sync: ViewModels emit on WidgetSync.requestRefresh()
        // after every insert/update; re-render all placed widgets here where
        // a Context is available.
        appScope.launch {
            WidgetSync.refreshRequests.collectLatest {
                try {
                    WidgetSync.refreshAll(this@LogLifeApplication)
                } catch (_: Exception) {
                }
            }
        }
    }
}
