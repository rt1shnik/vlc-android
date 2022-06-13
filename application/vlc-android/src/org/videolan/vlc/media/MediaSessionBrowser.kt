/*
 * ************************************************************************
 *  MediaSessionBrowser.kt
 * *************************************************************************
 *  Copyright © 2016-2020 VLC authors and VideoLAN
 *  Author: Geoffrey Métais
 *
 *  This program is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation; either version 2 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program; if not, write to the Free Software
 *  Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston MA 02110-1301, USA.
 *
 *  *************************************************************************
 */
package org.videolan.vlc.media

import android.content.Context
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.*
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.util.isSchemeStreaming

/**
 * The mediaId used in the media session browser is defined as an opaque string token which is left
 * up to the application developer to define. In practicality, mediaIds from multiple applications
 * may be combined into a single data structure, so we use a valid uri, and have have intentionally
 * prefixed it with a namespace. The value is stored as a string to avoid repeated type conversion;
 * however, it may be parsed by the uri class as needed. The uri starts with two forward slashes to
 * disambiguate the authority from the path, per RFC 3986, section 3.
 *
 * The mediaId structure is documented below for reference. The first (or second) letter of each
 * section is used in lieu of the entire word in order to shorten the id throughout the library.
 * The reduction of space consumed by the mediaId enables an increased number of records per page.
 *
 * Root node
 * //org.videolan.vlc/{r}oot[?{f}latten=1]
 * Root menu
 * //org.videolan.vlc/{r}oot/home
 * //org.videolan.vlc/{r}oot/playlist/<id>
 * //org.videolan.vlc/{r}oot/{l}ib
 * //org.videolan.vlc/{r}oot/stream
 * Home menu
 * //org.videolan.vlc/{r}oot/home/shuffle_all
 * //org.videolan.vlc/{r}oot/home/last_added[?{i}ndex=<track num>]
 * //org.videolan.vlc/{r}oot/home/history[?{i}ndex=<track num>]
 * Library menu
 * //org.videolan.vlc/{r}oot/{l}ib/a{r}tist[?{p}age=<page num>]
 * //org.videolan.vlc/{r}oot/{l}ib/a{r}tist/<id>
 * //org.videolan.vlc/{r}oot/{l}ib/a{l}bum[?{p}age=<page num>]
 * //org.videolan.vlc/{r}oot/{l}ib/a{l}bum/<id>
 * //org.videolan.vlc/{r}oot/{l}ib/{t}rack[?{p}age=<page num>]
 * //org.videolan.vlc/{r}oot/{l}ib/{t}rack[?{p}age=<page num>][&{i}ndex=<track num>]
 * //org.videolan.vlc/{r}oot/{l}ib/{g}enre[?{p}age=<page num>]
 * //org.videolan.vlc/{r}oot/{l}ib/{g}enre/<id>
 * Media
 * //org.videolan.vlc/media/<id>
 * Errors
 * //org.videolan.vlc/error/media
 * //org.videolan.vlc/error/playlist
 * Search
 * //org.videolan.vlc/search?query=<query>
 */
class MediaSessionBrowser {

    companion object {
        private const val TAG = "VLC/MediaSessionBrowser"
        private const val BASE_DRAWABLE_URI = "android.resource://${BuildConfig.APP_ID}/drawable"
        private val MENU_AUDIO_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_audio}".toUri()
        private val MENU_ALBUM_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_album}".toUri()
        private val MENU_GENRE_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_genre}".toUri()
        private val MENU_ARTIST_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_artist}".toUri()
        private val DEFAULT_ALBUM_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_album_unknown}".toUri()
        private val DEFAULT_ARTIST_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_artist_unknown}".toUri()
        private val DEFAULT_STREAM_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_stream_unknown}".toUri()
        private val DEFAULT_PLAYLIST_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_playlist_unknown}".toUri()
        private val DEFAULT_PLAYALL_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_playall}".toUri()
        val DEFAULT_TRACK_ICON = "${BASE_DRAWABLE_URI}/${R.drawable.ic_auto_nothumb}".toUri()
        private val instance = MediaSessionBrowser()

        // Root item
        // MediaIds are all strings. Maintain in uri parsable format.
        const val ID_ROOT = "//${BuildConfig.APP_ID}/r"
        const val ID_ROOT_NO_TABS = "$ID_ROOT?f=1"
        const val ID_MEDIA = "$ID_ROOT/media"
        const val ID_SEARCH = "$ID_ROOT/search"
        const val ID_SUGGESTED = "$ID_ROOT/suggested"
        const val ID_NO_MEDIA = "$ID_ROOT/error/media"
        const val ID_NO_PLAYLIST = "$ID_ROOT/error/playlist"

        // Top-level menu
        const val ID_HOME = "$ID_ROOT/home"
        const val ID_PLAYLIST = "$ID_ROOT/playlist"
        private const val ID_LIBRARY = "$ID_ROOT/l"
        const val ID_STREAM = "$ID_ROOT/stream"

        // Home menu
        const val ID_SHUFFLE_ALL = "$ID_HOME/shuffle_all"
        const val ID_LAST_ADDED = "$ID_HOME/last_added"
        const val ID_HISTORY = "$ID_HOME/history"

        // Library menu
        const val ID_ARTIST = "$ID_LIBRARY/r"
        const val ID_ALBUM = "$ID_LIBRARY/l"
        const val ID_TRACK = "$ID_LIBRARY/t"
        const val ID_GENRE = "$ID_LIBRARY/g"
        const val MAX_HISTORY_SIZE = 100
        const val MAX_COVER_ART_ITEMS = 50
        private const val MAX_EXTENSION_SIZE = 100
        private const val MAX_SUGGESTED_SIZE = 15
        const val MAX_RESULT_SIZE = 800

        fun getContentStyle(browsableHint: Int = CONTENT_STYLE_LIST_ITEM_HINT_VALUE, playableHint: Int = CONTENT_STYLE_LIST_ITEM_HINT_VALUE): Bundle {
            return Bundle().apply {
                putInt(CONTENT_STYLE_BROWSABLE_HINT, browsableHint)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, playableHint)
            }
        }

        fun generateMediaId(libraryItem: MediaLibraryItem): String {
            val prefix = when (libraryItem.itemType) {
                MediaLibraryItem.TYPE_ARTIST -> ID_ARTIST
                MediaLibraryItem.TYPE_GENRE -> ID_GENRE
                MediaLibraryItem.TYPE_PLAYLIST -> ID_PLAYLIST
                else -> ID_MEDIA
            }
            return "${prefix}/${libraryItem.id}"
        }

        fun isMediaAudio(libraryItem: MediaLibraryItem): Boolean {
            return libraryItem.itemType == MediaLibraryItem.TYPE_MEDIA && (libraryItem as MediaWrapper).type == MediaWrapper.TYPE_AUDIO
        }
    }
}
