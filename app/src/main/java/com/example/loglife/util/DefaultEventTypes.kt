package com.example.loglife.util

import com.example.loglife.data.model.EventMode
import com.example.loglife.data.model.EventType

/**
 * Provides default event types for initial app setup.
 * These match the suggestions from the product specifications.
 */
object DefaultEventTypes {

    // Icon constants - using Material Design icon names
    private val ICON_WAKE_UP = "access_alarm"
    private val ICON_SLEEP = "nightlight_round"
    private val ICON_EAT = "restaurant"
    private val ICON_SNACK = "cookie"
    private val ICON_SHOWER = "shower"
    private val ICON_NAP = "hotel"
    private val ICON_WORK = "work"
    private val ICON_STUDY = "school"
    private val ICON_CODING = "code"
    private val ICON_READING = "book"
    private val ICON_GYM = "fitness_center"
    private val ICON_EXERCISE = "directions_run"
    private val ICON_CYCLING = "pedal_bike"
    private val ICON_WALKING = "directions_walk"
    private val ICON_RUNNING = "directions_run"
    private val ICON_GO_OUTSIDE = "nature"
    private val ICON_SOCIAL = "people"
    private val ICON_GAMING = "sports_esports"
    private val ICON_WATCHING_TV = "tv"
    private val ICON_WATCHING_MOVIE = "movie"
    private val ICON_MUSIC = "music_note"
    private val ICON_RELAXING = "spa"

    /**
     * Returns a list of default event types to be inserted on first app launch.
     */
    fun getDefaultEventTypes(): List<EventType> {
        return listOf(
            // Everyday
            EventType(
                name = "Wake Up",
                icon = ICON_WAKE_UP,
                eventMode = EventMode.INSTANT,
                category = "Daily Routine"
            ),
            EventType(
                name = "Sleep",
                icon = ICON_SLEEP,
                eventMode = EventMode.DURATION,
                category = "Daily Routine"
            ),
            EventType(
                name = "Eat",
                icon = ICON_EAT,
                eventMode = EventMode.DURATION,
                category = "Daily Routine"
            ),
            EventType(
                name = "Snack",
                icon = ICON_SNACK,
                eventMode = EventMode.DURATION,
                category = "Daily Routine"
            ),
            EventType(
                name = "Shower",
                icon = ICON_SHOWER,
                eventMode = EventMode.DURATION,
                category = "Daily Routine"
            ),
            EventType(
                name = "Nap",
                icon = ICON_NAP,
                eventMode = EventMode.DURATION,
                category = "Daily Routine"
            ),

            // Work / Learning
            EventType(
                name = "Work",
                icon = ICON_WORK,
                eventMode = EventMode.DURATION,
                category = "Work"
            ),
            EventType(
                name = "Study",
                icon = ICON_STUDY,
                eventMode = EventMode.DURATION,
                category = "Learning"
            ),
            EventType(
                name = "Coding",
                icon = ICON_CODING,
                eventMode = EventMode.DURATION,
                category = "Work"
            ),
            EventType(
                name = "Reading",
                icon = ICON_READING,
                eventMode = EventMode.DURATION,
                category = "Learning"
            ),

            // Activity
            EventType(
                name = "Gym",
                icon = ICON_GYM,
                eventMode = EventMode.DURATION,
                category = "Health"
            ),
            EventType(
                name = "Exercise",
                icon = ICON_EXERCISE,
                eventMode = EventMode.DURATION,
                category = "Health"
            ),
            EventType(
                name = "Cycling",
                icon = ICON_CYCLING,
                eventMode = EventMode.DURATION,
                category = "Health"
            ),
            EventType(
                name = "Walking",
                icon = ICON_WALKING,
                eventMode = EventMode.DURATION,
                category = "Health"
            ),
            EventType(
                name = "Running",
                icon = ICON_RUNNING,
                eventMode = EventMode.DURATION,
                category = "Health"
            ),
            EventType(
                name = "Go Outside",
                icon = ICON_GO_OUTSIDE,
                eventMode = EventMode.DURATION,
                category = "Health"
            ),

            // Social / Leisure
            EventType(
                name = "Social",
                icon = ICON_SOCIAL,
                eventMode = EventMode.DURATION,
                category = "Leisure"
            ),
            EventType(
                name = "Gaming",
                icon = ICON_GAMING,
                eventMode = EventMode.DURATION,
                category = "Leisure"
            ),
            EventType(
                name = "Watching TV",
                icon = ICON_WATCHING_TV,
                eventMode = EventMode.DURATION,
                category = "Leisure"
            ),
            EventType(
                name = "Watching Movie",
                icon = ICON_WATCHING_MOVIE,
                eventMode = EventMode.DURATION,
                category = "Leisure"
            ),
            EventType(
                name = "Music",
                icon = ICON_MUSIC,
                eventMode = EventMode.DURATION,
                category = "Leisure"
            ),
            EventType(
                name = "Relaxing",
                icon = ICON_RELAXING,
                eventMode = EventMode.DURATION,
                category = "Leisure"
            )
        )
    }
}