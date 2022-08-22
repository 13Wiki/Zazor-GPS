package com.gps.zazor.ui.photo.collage.container

import android.os.Bundle
import android.view.View
import com.gps.zazor.R
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.PhotoHandler
import com.gps.zazor.ui.photo.collage.GridCollageFragment
import com.gps.zazor.ui.photo.collage.HorizontalCollageFragment
import com.gps.zazor.ui.photo.collage.VerticalCollageFragment
import com.gps.zazor.ui.photo.collage.base.CollageContract
import com.gps.zazor.ui.photo.collage.container.di.injectViewModel

class CollageContainerFragment : BaseFragment<CollageContract.State, CollageContract.Event>(R.layout.fragment_collage_container),
    CollageContainerListener,
    PhotoHandler {

    override val viewModel by injectViewModel()

    override val screenTitle = R.string.collage

    override fun observeState(state: CollageContract.State?) = Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navigateTo(GridCollageFragment(), R.id.flContainer)
    }

    override fun onResume() {
        super.onResume()
        callback?.onCollageShown()
    }

    override fun onCapturePhoto() {
        childFragmentManager.fragments.forEach {
            (it as? PhotoHandler)?.onCapturePhoto()
        }
    }

    override fun flipCamera() {
        childFragmentManager.fragments.forEach {
            (it as? PhotoHandler)?.flipCamera()
        }
    }

    override fun onGridSelected() {
        navigateTo(GridCollageFragment(), R.id.flContainer)
    }

    override fun onHorizontalSelected() {
        navigateTo(HorizontalCollageFragment(), R.id.flContainer)
    }

    override fun onVerticalSelected() {
        navigateTo(VerticalCollageFragment(), R.id.flContainer)
    }
}