package org.videolan.vlc

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import android.util.Log
import android.view.KeyEvent
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.*
import org.videolan.medialibrary.Tools
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.resources.*
import org.videolan.tools.PLAYBACK_HISTORY
import org.videolan.tools.Settings
import org.videolan.tools.removeQuery
import org.videolan.vlc.media.MediaSessionBrowser
import java.security.SecureRandom
import kotlin.math.abs
import kotlin.math.min

@Suppress("unused")
private const val TAG = "VLC/MediaSessionCallback"
private const val ONE_SECOND = 1000L

@ObsoleteCoroutinesApi
@ExperimentalCoroutinesApi
internal class MediaSessionCallback(private val playbackService: PlaybackService) : MediaSessionCompat.Callback() {
    private var prevActionSeek = false

    override fun onPlay() {
        if (playbackService.hasMedia()) playbackService.play()
        else if (!AndroidDevices.isAndroidTv && Settings.getInstance(playbackService).getBoolean(PLAYBACK_HISTORY, true)) PlaybackService.loadLastAudio(playbackService)
    }

    override fun onMediaButtonEvent(mediaButtonEvent: Intent): Boolean {
        val keyEvent = mediaButtonEvent.getParcelableExtra(Intent.EXTRA_KEY_EVENT) as KeyEvent? ?: return false
        if (!playbackService.hasMedia()
                && (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY || keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE)) {
            return if (keyEvent.action == KeyEvent.ACTION_DOWN && Settings.getInstance(playbackService).getBoolean(PLAYBACK_HISTORY, true)) {
                PlaybackService.loadLastAudio(playbackService)
                true
            } else false
        }
        /**
         * Implement fast forward and rewind behavior by directly handling the previous and next button events.
         * Normally the buttons are triggered on ACTION_DOWN; however, we ignore the ACTION_DOWN event when
         * isAndroidAutoHardKey returns true, and perform the operation on the ACTION_UP event instead. If the previous or
         * next button is held down, a callback occurs with the long press flag set. When a long press is received,
         * invoke the onFastForward() or onRewind() methods, and set the prevActionSeek flag. The ACTION_UP event
         * action is bypassed if the flag is set. The prevActionSeek flag is reset to false for the next invocation.
         */
        if (isAndroidAutoHardKey(keyEvent) && (keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_PREVIOUS || keyEvent.keyCode == KeyEvent.KEYCODE_MEDIA_NEXT)) {
            when (keyEvent.action) {
                KeyEvent.ACTION_DOWN -> {
                    if (playbackService.isSeekable && keyEvent.isLongPress) {
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT -> onFastForward()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> onRewind()
                        }
                        prevActionSeek = true
                    }
                }
                KeyEvent.ACTION_UP -> {
                    if (!prevActionSeek) {
                        val enabledActions = playbackService.enabledActions
                        when (keyEvent.keyCode) {
                            KeyEvent.KEYCODE_MEDIA_NEXT -> if ((enabledActions and PlaybackStateCompat.ACTION_SKIP_TO_NEXT) != 0L) onSkipToNext()
                            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> if ((enabledActions and PlaybackStateCompat.ACTION_SKIP_TO_PREVIOUS) != 0L) onSkipToPrevious()
                        }
                    }
                    prevActionSeek = false
                }
            }
            return true
        }
        return super.onMediaButtonEvent(mediaButtonEvent)
    }

    /**
     * This function is based on the following KeyEvent captures. This may need to be updated if the behavior changes in the future.
     *
     * KeyEvent from Media Control UI:
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_NEXT, scanCode=0, metaState=0, flags=0x0, repeatCount=0, eventTime=0, downTime=0, deviceId=-1, source=0x0, displayId=0}
     *
     * KeyEvent from Android Auto Steering Wheel Control:
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_NEXT, scanCode=0, metaState=0, flags=0x4, repeatCount=0, eventTime=0, downTime=0, deviceId=0, source=0x0, displayId=0}
     *
     * KeyEvent from Android Auto Steering Wheel Control, Holding Switch (Long Press):
     * {action=ACTION_DOWN, keyCode=KEYCODE_MEDIA_NEXT, scanCode=0, metaState=0, flags=0x84, repeatCount=1, eventTime=0, downTime=0, deviceId=0, source=0x0, displayId=0}
     */
    @SuppressLint("LongLogTag")
    private fun isAndroidAutoHardKey(keyEvent: KeyEvent): Boolean {
        val carMode = AndroidDevices.isCarMode(playbackService.applicationContext)
        if (carMode) Log.i(TAG, "Android Auto Key Press: $keyEvent")
        return carMode && keyEvent.deviceId == 0 && (keyEvent.flags and KeyEvent.FLAG_KEEP_TOUCH_MODE != 0)
    }

    override fun onCustomAction(actionId: String?, extras: Bundle?) {
        when (actionId) {
            CUSTOM_ACTION_SPEED -> {
                val steps = listOf(0.50f, 0.80f, 1.00f, 1.10f, 1.20f, 1.50f, 2.00f)
                val index = 1 + steps.indexOf(steps.minByOrNull { abs(playbackService.rate - it) })
                playbackService.setRate(steps[index % steps.size], false)
            }
            CUSTOM_ACTION_BOOKMARK -> {
                playbackService.lifecycleScope.launch {
                    val context = playbackService.applicationContext
                    playbackService.currentMediaWrapper?.let {
                        val bookmark = it.addBookmark(playbackService.getTime())
                        val bookmarkName = context.getString(R.string.bookmark_default_name, Tools.millisToString(playbackService.getTime()))
                        bookmark?.setName(bookmarkName)
                        playbackService.displayPlaybackMessage(R.string.saved, bookmarkName)
                    }
                }
            }
            CUSTOM_ACTION_REWIND -> onRewind()
            CUSTOM_ACTION_FAST_FORWARD -> onFastForward()
            CUSTOM_ACTION_SHUFFLE -> if (playbackService.canShuffle()) playbackService.shuffle()
            CUSTOM_ACTION_REPEAT -> playbackService.repeatType = when (playbackService.repeatType) {
                PlaybackStateCompat.REPEAT_MODE_NONE -> PlaybackStateCompat.REPEAT_MODE_ALL
                PlaybackStateCompat.REPEAT_MODE_ALL -> PlaybackStateCompat.REPEAT_MODE_ONE
                PlaybackStateCompat.REPEAT_MODE_ONE -> PlaybackStateCompat.REPEAT_MODE_NONE
                else -> PlaybackStateCompat.REPEAT_MODE_NONE
            }
        }
    }

    override fun onPlayFromMediaId(mediaId: String, extras: Bundle?) {
        playbackService.lifecycleScope.launch {
            val context = playbackService.applicationContext
            try {
                    val mediaIdUri = Uri.parse(mediaId)
                    val position = mediaIdUri.getQueryParameter("i")?.toInt() ?: 0
                    val page = mediaIdUri.getQueryParameter("p")
                    val pageOffset = page?.toInt()?.times(MediaSessionBrowser.MAX_RESULT_SIZE) ?: 0
                    when (mediaIdUri.removeQuery().toString()) {
                        MediaSessionBrowser.ID_NO_MEDIA -> playbackService.displayPlaybackError(R.string.search_no_result)
                        MediaSessionBrowser.ID_NO_PLAYLIST -> playbackService.displayPlaybackError(R.string.noplaylist)
                    }
            } catch (e: Exception) {
                Log.e(TAG, "Could not play media: $mediaId", e)
                when {
                    playbackService.hasMedia() -> playbackService.play()
                    else -> playbackService.displayPlaybackError(R.string.search_no_result)
                }
            }
        }
    }

    private fun loadMedia(mediaList: List<MediaWrapper>?, position: Int = 0, allowRandom: Boolean = false) {
        mediaList?.let { mediaList ->
            if (AndroidDevices.isCarMode(playbackService.applicationContext))
                mediaList.forEach { if (it.type == MediaWrapper.TYPE_VIDEO) it.addFlags(MediaWrapper.MEDIA_FORCE_AUDIO) }
            // Pick a random first track if allowRandom is true and shuffle is enabled
            playbackService.load(mediaList, if (allowRandom && playbackService.isShuffling) SecureRandom().nextInt(min(mediaList.size, MEDIALIBRARY_PAGE_SIZE)) else position)
        }
    }

    private fun checkForSeekFailure(forward: Boolean) {
        if (playbackService.playlistManager.player.lastPosition == 0.0f && (forward || playbackService.getTime() > 0))
            playbackService.displayPlaybackMessage(R.string.unseekable_stream)
    }

    override fun onPlayFromUri(uri: Uri?, extras: Bundle?) = playbackService.loadUri(uri)

    override fun onPlayFromSearch(query: String?, extras: Bundle?) {
        val playbackState = PlaybackStateCompat.Builder()
                .setActions(playbackService.enabledActions)
                .setState(PlaybackStateCompat.STATE_CONNECTING, playbackService.getTime(), playbackService.speed)
                .build()
        playbackService.mediaSession.setPlaybackState(playbackState)
        playbackService.lifecycleScope.launch(Dispatchers.IO) {
            if (!isActive) return@launch
        }
    }

    override fun onSetShuffleMode(shuffleMode: Int) {
        playbackService.shuffleType = shuffleMode
    }

    override fun onSetRepeatMode(repeatMode: Int) {
        playbackService.repeatType = repeatMode
    }

    override fun onPause() = playbackService.pause()

    override fun onStop() = playbackService.stop()

    override fun onSkipToNext() = playbackService.next()

    override fun onSkipToPrevious() = playbackService.previous(false)

    override fun onSeekTo(pos: Long) = playbackService.seek(if (pos < 0) playbackService.getTime() + pos else pos, fromUser = true)

    override fun onFastForward() {
        playbackService.seek((playbackService.getTime() + Settings.audioJumpDelay * ONE_SECOND).coerceAtMost(playbackService.length), fromUser = true)
        checkForSeekFailure(forward = true)
    }

    override fun onRewind() {
        playbackService.seek((playbackService.getTime() - Settings.audioJumpDelay * ONE_SECOND).coerceAtLeast(0), fromUser = true)
        checkForSeekFailure(forward = false)
    }

    override fun onSkipToQueueItem(id: Long) = playbackService.playIndexOrLoadLastPlaylist(id.toInt())
}