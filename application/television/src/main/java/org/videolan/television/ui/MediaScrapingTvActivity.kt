/*
 * ************************************************************************
 *  NextTvActivity.kt
 * *************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
 * Author: Nicolas POMEPUY
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 * **************************************************************************
 *
 *
 */

/*****************************************************************************
 * SearchActivity.java
 *
 * Copyright © 2014-2015 VLC authors, VideoLAN and VideoLabs
 * Author: Geoffrey Métais
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 */
package org.videolan.television.ui

import android.annotation.TargetApi
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.core.os.bundleOf
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.television.R
import org.videolan.television.ui.browser.BaseTvActivity

@TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
class MediaScrapingTvActivity : BaseTvActivity() {

    private lateinit var fragment: MediaScrapingTvFragment
    private lateinit var emptyView: TextView

    public override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.tv_next)

        fragment = MediaScrapingTvFragment().apply { arguments = bundleOf(MEDIA to
                intent.getParcelableExtra<MediaWrapper>(MEDIA)) }
        supportFragmentManager.beginTransaction()
                .replace(R.id.fragment_placeholder, fragment)
                .commit()


        emptyView = findViewById(R.id.empty)
    }

    override fun refresh() {
        fragment.refresh()
    }

    fun updateEmptyView(empty: Boolean) {
        emptyView.visibility = if (empty) View.VISIBLE else View.GONE
    }

    override fun onSearchRequested(): Boolean {
        fragment.startRecognition()
        return true
    }

    companion object {
        const val MEDIA: String = "MEDIA"
        private val TAG = "VLC/SearchActivity"
    }
}
