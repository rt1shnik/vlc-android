/*
 * ************************************************************************
 *  AddToGroupDialog.kt
 * *************************************************************************
 * Copyright © 2020 VLC authors and VideoLAN
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

package org.videolan.vlc.gui.dialogs

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.core.content.ContextCompat
import androidx.core.view.isEmpty
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.ObsoleteCoroutinesApi
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import org.videolan.libvlc.MediaPlayer
import org.videolan.tools.DependencyProvider
import org.videolan.tools.dp
import org.videolan.tools.setGone
import org.videolan.tools.setVisible
import org.videolan.vlc.PlaybackService
import org.videolan.vlc.R
import org.videolan.vlc.databinding.PlayerOverlayTracksBinding
import org.videolan.vlc.gui.dialogs.adapters.TrackAdapter
import org.videolan.vlc.gui.helpers.getBitmapFromDrawable
import org.videolan.vlc.gui.video.VideoPlayerActivity

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
class VideoTracksDialog : VLCBottomSheetDialogFragment() {
    override fun getDefaultState(): Int = STATE_EXPANDED

    override fun needToManageOrientation(): Boolean = true

    private lateinit var binding: PlayerOverlayTracksBinding

    override fun initialFocusedView(): View = binding.subtitleTracks.emptyView

    lateinit var menuItemListener: (VideoTrackOption) -> Unit
    lateinit var trackSelectionListener: (Int, TrackType) -> Unit

    private fun onServiceChanged(service: PlaybackService?) {
        service?.let { playbackService ->
            if (playbackService.videoTracksCount <= 2) {
                binding.videoTracks.viewStub.setGone()
                binding.tracksSeparator3.setGone()
            } else {
                playbackService.videoTracks?.let { trackList ->
                    val trackAdapter = TrackAdapter(trackList as Array<MediaPlayer.TrackDescription>, trackList.firstOrNull { it.id == playbackService.videoTrack })
                    trackAdapter.setOnTrackSelectedListener { track ->
                        trackSelectionListener.invoke(track.id, TrackType.VIDEO)
                    }
                    binding.videoTracks.viewStub.setVisible()
                    val titleTextView =  binding.videoTracks.root.findViewById<TextView>(R.id.track_title)
                    titleTextView.text = getString(R.string.video)
                    val trackRecyclerView = binding.videoTracks.root.findViewById<RecyclerView>(R.id.track_list)
                    trackRecyclerView.layoutManager = LinearLayoutManager(requireActivity())
                    trackRecyclerView.adapter = trackAdapter
                }
            }

            if (playbackService.audioTracksCount <= 0) {
                binding.audioTracks.trackContainer.setGone()
                binding.tracksSeparator2.setGone()
            }

            playbackService.audioTracks?.let { trackList ->
                val trackAdapter = TrackAdapter(trackList as Array<MediaPlayer.TrackDescription>, trackList.firstOrNull { it.id == playbackService.audioTrack })
                trackAdapter.setOnTrackSelectedListener { track ->
                    trackSelectionListener.invoke(track.id, TrackType.AUDIO)
                }
                binding.audioTracks.trackList.adapter = trackAdapter
            }
            playbackService.spuTracks?.let { trackList ->
                if (!playbackService.hasRenderer()) {
                    val trackAdapter = TrackAdapter(trackList as Array<MediaPlayer.TrackDescription>, trackList.firstOrNull { it.id == playbackService.spuTrack })
                    trackAdapter.setOnTrackSelectedListener { track ->
                        trackSelectionListener.invoke(track.id, TrackType.SPU)
                    }
                    binding.subtitleTracks.trackList.adapter = trackAdapter
                } else {
                    binding.subtitleTracks.emptyView.text = getString(R.string.no_sub_renderer)
                    binding.subtitleTracks.emptyView.setVisible()
                    binding.subtitleTracks.trackMore.setGone()
                }
                if (trackList.isEmpty()) binding.subtitleTracks.emptyView.setVisible()
            }
            if (playbackService.spuTracks == null) binding.subtitleTracks.emptyView.setVisible()
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = PlayerOverlayTracksBinding.inflate(layoutInflater, container, false)
        val start = System.currentTimeMillis()
        binding.root.addOnLayoutChangeListener(OnLayoutListener(start))
        return binding.root
    }

    internal inner class OnLayoutListener(private val start: Long) : View.OnLayoutChangeListener {
        override fun onLayoutChange(
            v: View,
            left: Int,
            top: Int,
            right: Int,
            bottom: Int,
            oldLeft: Int,
            oldTop: Int,
            oldRight: Int,
            oldBottom: Int
        ) {
            if (v.visibility == View.VISIBLE) {
                val end = System.currentTimeMillis()
                val animTime = end - start
                println("VideoTracks UI appearing time: $animTime")
                val activity = requireActivity() as VideoPlayerActivity
                println("from click to VideoTracks UI appearing: ${end - activity.audioClickTime}")
                v.removeOnLayoutChangeListener(this)
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding.audioTracks.trackTitle.text = getString(R.string.audio)
        binding.subtitleTracks.trackTitle.text = getString(R.string.subtitles)

        binding.audioTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())
        binding.subtitleTracks.trackList.layoutManager = LinearLayoutManager(requireActivity())

        //prevent focus
        binding.tracksSeparator3.isEnabled = false
        binding.tracksSeparator2.isEnabled = false


        binding.audioTracks.options.setAnimationUpdateListener {
            binding.audioTracks.trackMore.rotation = if (binding.audioTracks.options.isCollapsed) 180F - (180F * it) else 180F * it
        }

        binding.subtitleTracks.options.setAnimationUpdateListener {
            binding.subtitleTracks.trackMore.rotation = if (binding.subtitleTracks.options.isCollapsed) 180F - (180F * it) else 180F * it
        }

        binding.audioTracks.trackMore.setOnClickListener {
            val options = binding.audioTracks.options
            if (options.isEmpty()) {
                generateSeparator(options)
                generateOptionItem(options, getString(R.string.audio_delay), R.drawable.ic_delay, VideoTrackOption.AUDIO_DELAY)
                generateSeparator(options, true)
            }

            binding.audioTracks.options.toggle()
            binding.subtitleTracks.options.collapse()
        }

        binding.subtitleTracks.trackMore.setOnClickListener {
            val options = binding.subtitleTracks.options
            if (options.isEmpty()) {
                generateSeparator(options)
                generateOptionItem(options, getString(R.string.spu_delay), R.drawable.ic_delay, VideoTrackOption.SUB_DELAY)
                generateOptionItem(options, getString(R.string.subtitle_select), R.drawable.ic_subtitles_file, VideoTrackOption.SUB_PICK)
                generateOptionItem(options, getString(R.string.download_subtitles), R.drawable.ic_download, VideoTrackOption.SUB_DOWNLOAD)
                generateSeparator(options, true)
            }

            binding.subtitleTracks.options.toggle()
            binding.audioTracks.options.collapse()
        }
        super.onViewCreated(view, savedInstanceState)
        PlaybackService.serviceFlow.onEach { onServiceChanged(it) }.launchIn(lifecycleScope)
    }

    private fun generateSeparator(parent: ViewGroup, margin: Boolean = false) {
        val view = View(context)
        view.setBackgroundColor(ContextCompat.getColor(requireContext(), R.color.white_transparent_50))
        val lp = LinearLayout.LayoutParams(-1, 1.dp)

        lp.marginStart = if (margin) 56.dp else 16.dp
        lp.marginEnd = 16.dp
        lp.topMargin = 8.dp
        lp.bottomMargin = 8.dp
        view.layoutParams = lp
        parent.addView(view)
    }

    private fun generateOptionItem(parent: ViewGroup, title: String, @DrawableRes icon: Int, optionId: VideoTrackOption) {
        val view = layoutInflater.inflate(R.layout.player_overlay_track_option_item, null)
        view.findViewById<TextView>(R.id.option_title).text = title
        view.findViewById<ImageView>(R.id.option_icon).setImageBitmap(requireContext().getBitmapFromDrawable(icon))
        view.setOnClickListener {
            menuItemListener.invoke(optionId)
            dismiss()
        }
        parent.addView(view)
    }

    companion object : DependencyProvider<Any>() {

        val TAG = "VLC/SavePlaylistDialog"
    }

    enum class TrackType {
        VIDEO, AUDIO, SPU
    }

    enum class VideoTrackOption {
        SUB_DELAY, SUB_PICK, SUB_DOWNLOAD, AUDIO_DELAY
    }
}

