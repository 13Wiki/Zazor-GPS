package com.gps.zazor.ui.media.list

import com.gps.zazor.data.models.Photo
import com.gps.zazor.utils.export.TrackFormat
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import java.io.File

class MediaListContract {

    sealed class Event : UiEvent {

        data class DeletePhoto(val photo: Photo) : Event()

        data class SwitchPhotoSelected(val photo: Photo,
                                       val isSelected: Boolean) : Event()

        object TurnOnSelectionMode : Event()

        object SharePhotos : Event()

        /** Writes the visible photos out as a track file for another app to open. */
        data class ExportTrack(val format: TrackFormat) : Event()
    }

    sealed class State : UiState {

        data class Initial(val photos: List<Photo>) : State()

        object ClearSelectedMode : State()

        data class ShareSelectedPhotos(val photos: List<Photo>) : State()
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
    }
}
