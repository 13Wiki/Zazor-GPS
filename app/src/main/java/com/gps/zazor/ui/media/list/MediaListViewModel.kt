package com.gps.zazor.ui.media.list

import androidx.lifecycle.viewModelScope
import com.gps.zazor.data.models.Photo
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

interface MediaListViewModel : BaseViewModel<MediaListContract.State, MediaListContract.Event> {

    fun backPressed(): Boolean
}

class MediaListViewModelImpl(private val photoRepository: PhotoRepository) : BaseViewModelImpl<MediaListContract.State, MediaListContract.Event>(), MediaListViewModel {

    private var selectedPhotos: MutableList<Photo>? = null

    override suspend fun initialState(): MediaListContract.State =
        MediaListContract.State.Initial(photoRepository.getPhotos())

    override fun backPressed(): Boolean {
        selectedPhotos?.let {
            selectedPhotos = null
            uiState.value = MediaListContract.State.ClearSelectedMode
            return false
        }
        return true
    }

    override fun onEventArrived(event: MediaListContract.Event?) {
        when (event) {
            is MediaListContract.Event.DeletePhoto -> {
                deletePhoto(event.photo)
            }
            is MediaListContract.Event.SwitchPhotoSelected -> {
                if (event.isSelected) {
                    selectedPhotos?.add(event.photo)
                } else {
                    selectedPhotos?.remove(event.photo)
                }
            }
            is MediaListContract.Event.SharePhotos -> {
                selectedPhotos?.let {
                    uiState.value = MediaListContract.State.ShareSelectedPhotos(it)
                }
            }
            is MediaListContract.Event.TurnOnSelectionMode -> {
                if (selectedPhotos == null) {
                    selectedPhotos = mutableListOf()
                }
            }
        }
    }

    private fun deletePhoto(photo: Photo) {
        viewModelScope.launch(Dispatchers.IO) {
            uiState.value = MediaListContract.State.Initial(photoRepository.deletePhoto(photo))
        }
    }
}