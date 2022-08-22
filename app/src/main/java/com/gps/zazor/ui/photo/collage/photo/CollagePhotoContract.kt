package com.gps.zazor.ui.photo.collage.photo

import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class CollagePhotoContract {

    sealed class Event : UiEvent

    sealed class State : UiState
}