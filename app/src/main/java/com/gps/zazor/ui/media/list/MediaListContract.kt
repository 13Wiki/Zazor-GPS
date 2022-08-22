package com.gps.zazor.ui.media.list

import com.gps.zazor.data.models.Photo
import com.gps.zazor.ui.base.UiEvent
import com.gps.zazor.ui.base.UiState

class MediaListContract {

    sealed class Event : UiEvent {

        data class DeletePhoto(val photo: Photo) : Event()

        data class SwitchPhotoSelected(val photo: Photo,
                                       val isSelected: Boolean) : Event()

        object TurnOnSelectionMode : Event()

        object SharePhotos : Event()
    }

    sealed class State : UiState {

        data class Initial(val photos: List<Photo>) : State()

        object ClearSelectedMode : State()

        data class ShareSelectedPhotos(val photos: List<Photo>) : State()
    }
}