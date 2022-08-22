package com.gps.zazor.ui.photo.panorama

import android.os.Bundle
import android.view.View
import android.view.ViewGroup
import androidx.core.view.children
import com.dermandar.dmd_lib.DMD_Capture
import com.gps.zazor.R
import com.gps.zazor.databinding.FragmentPanoramaBinding
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.panorama.di.injectViewModel
import com.gps.zazor.utils.SimpleShooterCallback
import com.gps.zazor.utils.extensions.hide
import com.gps.zazor.utils.viewBinding.viewBinding

class PanoramaFragment : BaseFragment<PanoramaContract.State, PanoramaContract.Event>(R.layout.fragment_panorama) {

    override val viewModel by injectViewModel()

    override val screenTitle = R.string.panorama

    private val binding by viewBinding(FragmentPanoramaBinding::bind)

    private val shooterCallback = SimpleShooterCallback()

    private lateinit var dmdCapture: DMD_Capture
    private lateinit var vgCamera: ViewGroup

    override fun observeState(state: PanoramaContract.State?) = Unit

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initCapture()
    }

    override fun onResume() {
        super.onResume()
        callback?.onPanoramaShown()
        dmdCapture.startCamera(requireContext(), 400, 500)
    }

    private fun initCapture() {
        dmdCapture = DMD_Capture()
        vgCamera = dmdCapture.initShooter(requireContext(),
            shooterCallback,
            requireActivity().windowManager.defaultDisplay.rotation,
            false,
            false)
        binding.flContainer.addView(vgCamera)
        binding.flContainer.post {
            (binding.flContainer.children.first() as? ViewGroup)?.children?.forEach {
                if (it.javaClass.name == "com.dermandar.dmd_lib.YinYangGLView") {
                    it.hide()
                }
            }
        }
    }
}