package com.gps.zazor.ui.photo.collage.photo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.gps.zazor.R
import com.gps.zazor.databinding.ActivityCollagePhotoBinding
import com.gps.zazor.databinding.BottomSheetAddNoteBinding
import com.gps.zazor.ui.photo.*
import com.gps.zazor.ui.photo.editPhoto.EditPhotoBottomSheet
import com.gps.zazor.ui.settings.SettingsActivity
import com.gps.zazor.utils.extensions.gone
import com.gps.zazor.utils.viewBinding.viewBinding

class CollagePhotoActivity : AppCompatActivity(R.layout.activity_collage_photo),
    PhotoCallback {

    companion object {

        private const val INDEX_EXTRA_KEY = "indexExtra"

        fun newIntent(context: Context, index: Int) =
            Intent(context, CollagePhotoActivity::class.java)
                .putExtra(INDEX_EXTRA_KEY, index)
    }

    private var adapter: PhotoPagerAdapter? = null

    private val binding by viewBinding(ActivityCollagePhotoBinding::bind)

    private val sheetBinding by viewBinding(BottomSheetAddNoteBinding::bind) {
        it.findViewById(R.id.clRoot)
    }

    private val addNoteSheet by lazy {
        getCurrentPhotoHandler()?.let {
            EditPhotoBottomSheet(sheetBinding)
        } ?: error("Fragments are not initialized")
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding.ivCapture.setOnClickListener {
            getCurrentPhotoHandler()?.onCapturePhoto()
        }
        binding.ivFlip.setOnClickListener {
            getCurrentPhotoHandler()?.flipCamera()
        }
        intent?.getIntExtra(INDEX_EXTRA_KEY, 0)?.let {
            navigateTo(CollagePhotoFragment.newInstance(it))
        }
    }

    override fun onBackPressed() {
        if (!addNoteSheet.collapse()) super.onBackPressed()
    }

    override fun onCaptured() {
        binding.clPhotoPanel.gone()
        addNoteSheet.show()
    }

    override fun onCollageShown() = Unit

    override fun onPanoramaShown() = Unit

    override fun onPhotoShown() = Unit

    override fun openSettings() {
        startActivity(SettingsActivity.newIntent(this))
    }

    override fun openCollagePhoto(index: Int) = Unit

    override fun switchEnabledCapture(isEnabled: Boolean) = Unit

    override fun collapseEditPhoto() {
        addNoteSheet.behavior.run {
            peekHeight = 250
            state = BottomSheetBehavior.STATE_COLLAPSED
        }
    }

    override fun onPhotoEditCancel() {
        finish()
    }

    override fun clearAll() {
        addNoteSheet.clearAll()
    }

    private fun getCurrentPhotoHandler(): PhotoHandler? =
        supportFragmentManager.findFragmentById(R.id.flContainer) as? PhotoHandler

    private fun navigateTo(fragment: Fragment, addToBackStack: Boolean = false) {
        supportFragmentManager.beginTransaction().replace(R.id.flContainer, fragment).apply {
            if (addToBackStack) addToBackStack(fragment::class.simpleName)
        }.commit()
    }
}