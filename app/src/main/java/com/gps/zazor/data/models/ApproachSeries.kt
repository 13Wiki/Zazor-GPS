package com.gps.zazor.data.models

/**
 * Several frames of the same spot, shot from far to near.
 *
 * The point is finding a place, not proving one: the wide frame says which clearing, the next says
 * which tree, the last says which stone. Whoever receives it walks in by the pictures.
 *
 * Fix quality varies along the way - under a canopy it is poor, in the open it is good - so the
 * series takes its coordinate from its best frame rather than from the last one, which is usually
 * the closest to the object and, being under cover, often the worst fix of the set.
 */
data class ApproachSeries(
    val id: String,
    /** Oldest first: the order the person walked in. */
    val frames: List<Photo>
) {

    val size: Int get() = frames.size

    /**
     * The frame whose fix is tightest, and therefore the one whose coordinate the series reports.
     * Frames with no fix never win; a set with no fixes at all yields null.
     */
    val bestFix: Photo?
        get() = frames
            .filter { it.lat != null && it.lng != null && it.accuracyMeters != null }
            .minByOrNull { it.accuracyMeters!! }
            ?: frames.firstOrNull { it.lat != null && it.lng != null }

    val lat: Double? get() = bestFix?.lat

    val lng: Double? get() = bestFix?.lng

    val accuracyMeters: Float? get() = bestFix?.accuracyMeters

    /** The closest shot, shown as the series thumbnail: it is what the object actually looks like. */
    val closest: Photo? get() = frames.lastOrNull()

    companion object {

        fun from(photos: List<Photo>): List<ApproachSeries> =
            photos
                .filter { !it.seriesId.isNullOrBlank() }
                .groupBy { it.seriesId!! }
                .map { (id, frames) -> ApproachSeries(id, frames.sortedBy { it.date }) }
                .sortedByDescending { it.frames.lastOrNull()?.date }
    }
}
