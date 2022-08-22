package com.gps.zazor.ui.photo

import androidx.lifecycle.viewModelScope
import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl
import com.gps.zazor.ui.photo.editPhoto.EditPhotoContract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.launch

interface PhotoViewModel : BaseViewModel<PhotoContract.State, PhotoContract.Event>

class PhotoViewModelImpl(private val editPhotoFlow: SharedFlow<EditPhotoContract.Flow>,
                         private val photoRepository: PhotoRepository)
    : BaseViewModelImpl<PhotoContract.State, PhotoContract.Event>(),
    PhotoViewModel {

    override suspend fun initialState(): PhotoContract.State =
        PhotoContract.State.Initial(photoRepository.getLastPhoto()?.path)

    override fun onEventArrived(event: PhotoContract.Event?) {
        when (event) {
            is PhotoContract.Event.PermissionResult -> handlePermission(event.granted)
            is PhotoContract.Event.EditPhotoClosed -> {
                viewModelScope.launch(Dispatchers.IO) {
                    uiState.value = initialState()
                }
            }
        }
    }

    private fun handlePermission(granted: Boolean) {
        uiState.value = if (granted) PhotoContract.State.PermissionGranted
        else PhotoContract.State.PermissionDenied
    }
}