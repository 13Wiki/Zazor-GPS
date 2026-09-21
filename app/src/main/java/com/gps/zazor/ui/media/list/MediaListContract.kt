package com.gps.zazor.ui.media.list

import com.gps.zazor.data.models.Photo
import com.gps.zazor.utils.export.TrackFormat
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import java.io.File

class MediaListContract {

    /**
     * What a long feed can be narrowed to.
     *
     * Three, and no more: everything, the shots someone marked up, and the wide frames. A filter
     * that has to be explained is a filter nobody uses.
     */
    enum class Filter {
        ALL, MARKED, WIDE
    }

    sealed class Event : UiEvent {

        data class DeletePhoto(val photo: Photo) : Event()

        data class SwitchPhotoSelected(val photo: Photo,
                                       val isSelected: Boolean) : Event()

        object TurnOnSelectionMode : Event()

        object SharePhotos : Event()

        /** Removes everything currently ticked, after the user has confirmed it. */
        object DeleteSelected : Event()

        /** Writes the visible photos out as a track file for another app to open. */
        data class ExportTrack(val format: TrackFormat) : Event()

        data class FilterSelected(val filter: Filter) : Event()
    }

    sealed class State : UiState {

        data class Initial(
            val photos: List<Photo>,
            val filter: Filter,
            /** How many frames each approach series has, for the badge on a frame's card. */
            val seriesSizes: Map<String, Int> = emptyMap()
        ) : State()

        object ClearSelectedMode : State()

    }

    /**
     * One-shot results. These must not live in the conflated `uiState`: returning from the share
     * chooser re-delivers the last state, which would reopen the chooser in a loop, while
     * exporting the same file twice would be conflated away and appear to do nothing.
     */
    sealed class Effect {

        data class TrackExported(val file: File, val format: TrackFormat) : Effect()

        object ExportEmpty : Effect()

        object ExportFailed : Effect()

        /** Addresses resolved after the fact for photos taken offline. */
        data class AddressesFilled(val count: Int) : Effect()

        /** Hands the chosen photos to the transfer screen. */
        data class OpenShare(val paths: List<String>) : Effect()

        /** How many photos the bulk delete actually removed. */
        data class SelectionDeleted(val count: Int) : Effect()
    }
}
