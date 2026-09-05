package com.example.loglife.data.model

/**
 * Represents whether an event type is instant (single timestamp) or duration (start/end timestamps).
 */
enum class EventMode {
    INSTANT,
    DURATION
}