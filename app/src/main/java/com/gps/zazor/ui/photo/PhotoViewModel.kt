package com.gps.zazor.ui.photo

import com.gps.zazor.data.repositories.PhotoRepository
import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface PhotoViewModel : BaseViewModel<PhotoContract.State, PhotoContract.Event>

class PhotoViewModelImpl(private val photoRepository: PhotoRepository) :
    BaseViewModelImpl<PhotoContract.State, PhotoContract.Event>(), PhotoViewModel {

    private var isPermissionGranted: Boolean? = null

    private var lastPhotoPath: String? = null

    override suspend fun initialState(): PhotoContract.State {
        lastPhotoPath = photoRepository.getLastPhoto()?.path
        return content()
    }

    override fun onEventArrived(event: PhotoContract.Event?) {
        when (event) {
            is PhotoContract.Event.PermissionResult -> {
                isPermissionGranted = event.granted
                uiState.value = content()
            }
            is PhotoContract.Event.EditPhotoClosed -> launchIo {
                lastPhotoPath = photoRepository.getLastPhoto()?.path
                uiState.value = content()
            }
            else -> Unit
        }
    }

    private fun content() = PhotoContract.State.Content(lastPhotoPath, isPermissionGranted)
}
