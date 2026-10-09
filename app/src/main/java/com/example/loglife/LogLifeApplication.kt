package com.example.loglife

import android.app.Application
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LogLifeApplication : Application() {

    // Exposed for the app widget provider, which runs outside Hilt's
    // activity/fragment injection and reads repositories via the Application.
    @Inject
    lateinit var eventTypeRepository: EventTypeRepository

    @Inject
    lateinit var eventInstanceRepository: EventInstanceRepository
}
