package com.gps.zazor.ui.series

import com.gps.zazor.data.models.ApproachSeries
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class SeriesContract {

    sealed class Event : UiEvent {

        /** Which series to show; sent once, when the screen opens. */
        data class Load(val seriesId: String) : Event()

        object Reload : Event()
    }

    sealed class State : UiState {

        data class Content(val series: ApproachSeries) : State()

        /** The series lost its last frame - there is nothing left to show. */
        object Empty : State()
    }
}
