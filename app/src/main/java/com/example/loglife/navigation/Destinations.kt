package com.example.loglife.navigation

/**
 * Destination for the Home screen.
 */
object HomeDestination : Destination

/**
 * Destination for the History screen (day / week / month / year).
 */
object HistoryDestination : Destination

/**
 * Destination for one event type's page, opened from History.
 */
data class EventDetailDestination(val eventTypeId: Long) : Destination

/**
 * Destination for the Settings screen.
 */
object SettingsDestination : Destination

/** Settings → Events & categories. */
object EventsDestination : Destination

/** Settings → Delete all data. */
object DeleteDataDestination : Destination

/**
 * Destination for the in-app Widget manager screen
 * (manage ongoing list + quick-log favourites for the single widget).
 */
object WidgetManagerDestination : Destination

/** Settings → Widgets → Quick log deck / Quick buttons / Today. */
object DeckWidgetDestination : Destination
object ButtonsWidgetDestination : Destination
object TodayWidgetDestination : Destination

/** Settings → Widgets → Cards / Buttons: reorder the favourites. */
object WidgetFavouritesDestination : Destination
