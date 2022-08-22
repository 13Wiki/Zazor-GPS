package com.gps.zazor.ui.media

import com.gps.zazor.ui.base.BaseViewModel
import com.gps.zazor.ui.base.BaseViewModelImpl

interface MediaViewModel : BaseViewModel<MediaContract.State, MediaContract.Event>

class MediaViewModelImpl : BaseViewModelImpl<MediaContract.State, MediaContract.Event>(), MediaViewModel {

    override suspend fun initialState(): MediaContract.State = MediaContract.State.Initial

    override fun onEventArrived(event: MediaContract.Event?) = Unit
}