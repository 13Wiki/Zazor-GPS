package com.gps.zazor.ui.photo

import android.content.Context
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import com.gps.zazor.ui.base.BaseFragment
import com.gps.zazor.ui.photo.base.BasePhotoFragment
import com.gps.zazor.ui.photo.basic.BasicPhotoFragment
import com.gps.zazor.ui.photo.collage.container.CollageContainerFragment
import com.gps.zazor.ui.photo.panorama.PanoramaFragment

class PhotoPagerAdapter(private val context: Context,
                        fragmentManager: FragmentManager) : FragmentPagerAdapter(fragmentManager, BEHAVIOR_RESUME_ONLY_CURRENT_FRAGMENT) {

    private val fragments = listOf<BaseFragment<*, *>>(CollageContainerFragment(), BasicPhotoFragment(), PanoramaFragment())

    override fun getCount(): Int = fragments.size

    override fun getItem(position: Int): Fragment = fragments[position]

    override fun getPageTitle(position: Int) =
        fragments[position].screenTitle?.let(context::getString)
}