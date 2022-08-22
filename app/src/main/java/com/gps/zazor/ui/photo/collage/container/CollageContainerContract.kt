package com.gps.zazor.ui.photo.collage.container

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class CollageContainerContract {

    sealed class Event : UiEvent

    sealed class State : UiState
}