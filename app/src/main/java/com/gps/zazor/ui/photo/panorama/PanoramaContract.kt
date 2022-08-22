package com.gps.zazor.ui.photo.panorama

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class PanoramaContract {

    sealed class Event : UiEvent

    sealed class State : UiState
}