package com.gps.zazor.ui.share

import com.gps.zazor.data.models.Photo
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import java.io.File

class ShareContract {

    /** What travels along with the pictures. */
    data class Options(
        val coordinates: Boolean = true,
        /** An address is a position in words; it can be dropped on its own. */
        val address: Boolean = true,
        val track: Boolean = true,
        /** What the person wrote or recorded about each frame. */
        val voiceNotes: Boolean = true
    )

    sealed class Event : UiEvent {

        data class Load(val paths: List<String>) : Event()

        data class ToggleCoordinates(val on: Boolean) : Event()

        data class ToggleAddress(val on: Boolean) : Event()

        data class ToggleTrack(val on: Boolean) : Event()

        data class ToggleVoiceNotes(val on: Boolean) : Event()

        object SendPhotos : Event()

        object SendBundle : Event()

        object SendReport : Event()
    }

    sealed class State : UiState {

        data class Content(
            val photos: List<Photo>,
            val options: Options,
            val isPreparing: Boolean = false
        ) : State()
    }

    sealed class Effect {

        data class SharePhotos(val photos: List<Photo>) : Effect()

        data class ShareBundle(val file: File) : Effect()

        /** A file to hand over as it is, with the type the receiver's phone should open it with. */
        data class ShareFile(val file: File, val mimeType: String) : Effect()

        data class ShareText(val text: String) : Effect()

        object Empty : Effect()

        object Failed : Effect()
    }
}
