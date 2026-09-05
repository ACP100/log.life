package com.example.loglife

import android.app.Application
import com.example.loglife.data.repository.EventInstanceRepository
import com.example.loglife.data.repository.EventTypeRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class LogLifeApplication : Application() {

    @Inject
    lateinit var eventTypeRepository: EventTypeRepository

    @Inject
    lateinit var eventInstanceRepository: EventInstanceRepository
}
