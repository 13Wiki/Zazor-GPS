package com.gps.zazor.ui.outings

import com.gps.zazor.data.models.Outing
import com.gps.zazor.data.models.Photo
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState
import com.gps.zazor.utils.export.TrackFormat
import java.io.File

class OutingsContract {

    sealed class Event : UiEvent {

        object Reload : Event()

        /** Wipes a whole day: its photos, their files and, with them, that day's track. */
        data class DeleteOuting(val outing: Outing) : Event()

        data class SelectOuting(val outing: Outing) : Event()

        data class ExportOuting(val outing: Outing, val format: TrackFormat) : Event()
    }

    sealed class State : UiState {

        data class Content(
            val outings: List<Outing>,
            val selected: Outing?
        ) : State()
    }

    sealed class Effect {

        data class Deleted(val photoCount: Int) : Effect()

        data class Exported(val file: File, val format: TrackFormat) : Effect()

        object ExportFailed : Effect()

        data class OpenInMaps(val photo: Photo) : Effect()
    }
}
