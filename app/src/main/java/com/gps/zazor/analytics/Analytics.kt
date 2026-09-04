package com.gps.zazor.analytics

/**
 * Anonymous counters, and nothing else.
 *
 * The owner needs to know how many people use the app and which parts they use. That is answered
 * by counting events. It is *not* answered by storing coordinates, photos, notes or anything that
 * identifies a person - and this interface is deliberately shaped so those cannot be sent even by
 * accident: an event is a name plus, at most, a count.
 *
 * Collecting more would also be self-defeating. The app's whole claim is that the position lives
 * on the picture and never travels inside it; a server holding user locations would turn that
 * claim into the product's biggest liability.
 */
interface Analytics {

    /**
     * @param event a fixed name from [Event]; never free text, so nothing personal can slip in.
     * @param count an optional magnitude, such as how many frames a series held.
     */
    fun track(event: Event, count: Int? = null)

    /** Turns collection on or off. Off means nothing is sent at all. */
    fun setEnabled(enabled: Boolean)

    enum class Event(val key: String) {
        APP_OPENED("app_opened"),
        PHOTO_TAKEN("photo_taken"),
        SERIES_FINISHED("series_finished"),
        WIDE_USED("wide_used"),
        MARK_ADDED("mark_added"),
        VOICE_NOTE_ADDED("voice_note_added"),
        SHARED_PHOTOS("shared_photos"),
        SHARED_BUNDLE("shared_bundle"),
        TRACK_EXPORTED("track_exported"),
        OUTINGS_OPENED("outings_opened"),
        OUTING_DELETED("outing_deleted"),
        PRO_PURCHASED("pro_purchased")
    }
}
