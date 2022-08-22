package com.gps.zazor.ui.photo.panorama

import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface PanoramaViewModel : BaseViewModel<PanoramaContract.State, PanoramaContract.Event>

class PanoramaViewModelImpl : BaseViewModelImpl<PanoramaContract.State, PanoramaContract.Event>(),
    PanoramaViewModel {

    override suspend fun initialState(): PanoramaContract.State? = null

    override fun onEventArrived(event: PanoramaContract.Event?) = Unit
}