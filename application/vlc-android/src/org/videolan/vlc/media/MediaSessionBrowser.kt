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

import android.content.*
import android.content.pm.PackageManager
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.annotation.StringRes
import androidx.annotation.WorkerThread
import androidx.core.net.toUri
import org.videolan.medialibrary.interfaces.Medialibrary
import org.videolan.medialibrary.interfaces.media.Album
import org.videolan.medialibrary.interfaces.media.MediaWrapper
import org.videolan.medialibrary.media.MediaLibraryItem
import org.videolan.resources.*
import org.videolan.resources.AppContextProvider.appContext
import org.videolan.tools.*
import org.videolan.vlc.ArtworkProvider
import org.videolan.vlc.BuildConfig
import org.videolan.vlc.R
import org.videolan.vlc.gui.helpers.MediaComparators
import org.videolan.vlc.gui.helpers.MediaComparators.formatArticles
import org.videolan.vlc.gui.helpers.UiTools.getDefaultAudioDrawable
import org.videolan.vlc.gui.helpers.getBitmapFromDrawable
import org.videolan.vlc.isPathValid
import org.videolan.vlc.media.MediaUtils.getMediaAlbum
import org.videolan.vlc.media.MediaUtils.getMediaArtist
import org.videolan.vlc.media.MediaUtils.getMediaDescription
import org.videolan.vlc.media.MediaUtils.getMediaSubtitle
import org.videolan.vlc.util.ThumbnailsProvider
import org.videolan.vlc.util.isSchemeStreaming
import java.util.concurrent.Semaphore

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

        /**
         * The search method is passed a simple query string absent metadata indicating
         * the user's intent to load a playlist, album, artist, or song. This is slightly different
         * than PlaybackService.onPlayFromSearch (which is also invoked by voice search) and allows
         * the user to navigate to other content via on-screen menus.
         */
        @WorkerThread
        fun search(context: Context, query: String): List<MediaBrowserCompat.MediaItem> {
            val res = context.resources
            val results: MutableList<MediaBrowserCompat.MediaItem> = ArrayList()
            val searchAggregate = Medialibrary.getInstance().search(query, false)
            val searchMediaId = ID_SEARCH.toUri().buildUpon().appendQueryParameter("query", query).toString()
            results.addAll(buildMediaItems(context, ID_PLAYLIST, searchAggregate.playlists, res.getString(R.string.playlists)))
            results.addAll(buildMediaItems(context, ID_ARTIST, searchAggregate.artists, res.getString(R.string.artists)))
            results.addAll(buildMediaItems(context, ID_ALBUM, searchAggregate.albums, res.getString(R.string.albums)))
            results.addAll(buildMediaItems(context, searchMediaId, searchAggregate.tracks, res.getString(R.string.tracks)))
            if (results.isEmpty()) {
                val emptyMediaDesc = MediaDescriptionCompat.Builder()
                        .setMediaId(ID_NO_MEDIA)
                        .setIconUri(DEFAULT_TRACK_ICON)
                        .setTitle(context.getString(R.string.search_no_result))
                        .build()
                results.add(MediaBrowserCompat.MediaItem(emptyMediaDesc, MediaBrowserCompat.MediaItem.FLAG_PLAYABLE))
            }
            return results
        }


        /**
         * This function constructs a collection of MediaBrowserCompat.MediaItems for each applicable
         * array element in the MediaLibraryItems list passed from either the browse or search methods.
         *
         * @param context Application context to resolve string resources
         * @param parentId Identifies the position in the menu hierarchy. The browse function
         * will pass the argument from the calling application. The search function will use a
         * placeholder value to act as if the user navigated to the location.
         * @param list MediaLibraryItems to process into MediaBrowserCompat.MediaItems
         * @param groupTitle Common heading to group items (unused if null)
         * @param limitSize Limit the number of items returned (default is false)
         * @return List containing fully constructed MediaBrowser MediaItem
         */
        private fun buildMediaItems(context: Context, parentId: String, list: Array<out MediaLibraryItem>?, groupTitle: String?,
                                    limitSize: Boolean = false, suggestionMode: Boolean = false): List<MediaBrowserCompat.MediaItem> {
            if (list.isNullOrEmpty()) return emptyList()
            val res = context.resources
            val artworkToUriCache = HashMap<String, Uri>()
            val results: ArrayList<MediaBrowserCompat.MediaItem> = ArrayList()
            results.ensureCapacity(list.size.coerceAtMost(MAX_RESULT_SIZE))
            /* Iterate over list */
            val parentIdUri = parentId.toUri()
            for ((index, libraryItem) in list.withIndex()) {
                if (libraryItem.itemType == MediaLibraryItem.TYPE_MEDIA
                        && ((libraryItem as MediaWrapper).type == MediaWrapper.TYPE_STREAM || isSchemeStreaming(libraryItem.uri.scheme))) {
                    libraryItem.type = MediaWrapper.TYPE_STREAM
                } else if (libraryItem.itemType == MediaLibraryItem.TYPE_MEDIA && (libraryItem as MediaWrapper).type != MediaWrapper.TYPE_AUDIO)
                    continue

                /* Media ID */
                val mediaId = when (libraryItem.itemType) {
                    MediaLibraryItem.TYPE_MEDIA -> parentIdUri.buildUpon().appendQueryParameter("i", "$index").toString()
                    else -> generateMediaId(libraryItem)
                }

                /* Subtitle */
                val subtitle = when (libraryItem.itemType) {
                    MediaLibraryItem.TYPE_MEDIA -> {
                        val media = libraryItem as MediaWrapper
                        when {
                            media.type == MediaWrapper.TYPE_STREAM -> media.uri.toString()
                            parentId.startsWith(ID_ALBUM) -> getMediaSubtitle(media)
                            else -> getMediaDescription(getMediaArtist(context, media), getMediaAlbum(context, media))
                        }
                    }
                    MediaLibraryItem.TYPE_PLAYLIST -> res.getString(R.string.track_number, libraryItem.tracksCount)
                    MediaLibraryItem.TYPE_ARTIST -> {
                        val albumsCount = Medialibrary.getInstance().getArtist(libraryItem.id).albumsCount
                        res.getQuantityString(R.plurals.albums_quantity, albumsCount, albumsCount)
                    }
                    MediaLibraryItem.TYPE_GENRE -> {
                        val albumsCount = Medialibrary.getInstance().getGenre(libraryItem.id).albumsCount
                        res.getQuantityString(R.plurals.albums_quantity, albumsCount, albumsCount)
                    }
                    MediaLibraryItem.TYPE_ALBUM -> {
                        if (parentId.startsWith(ID_ARTIST))
                            res.getString(R.string.track_number, libraryItem.tracksCount)
                        else
                            libraryItem.description
                    }
                    else -> libraryItem.description
                }

                /* Extras */
                val extras = when (libraryItem.itemType) {
                    MediaLibraryItem.TYPE_ARTIST, MediaLibraryItem.TYPE_GENRE -> getContentStyle(CONTENT_STYLE_GRID_ITEM_HINT_VALUE, CONTENT_STYLE_GRID_ITEM_HINT_VALUE)
                    else -> Bundle()
                }
                if (groupTitle != null) extras.putString(EXTRA_CONTENT_STYLE_GROUP_TITLE_HINT, groupTitle)

                /* Icon */
                val iconUri = if (libraryItem.itemType != MediaLibraryItem.TYPE_PLAYLIST && !libraryItem.artworkMrl.isNullOrEmpty() && isPathValid(libraryItem.artworkMrl)) {
                    val iconUri = Uri.Builder()
                    when (libraryItem.itemType) {
                        MediaLibraryItem.TYPE_ARTIST ->{
                            iconUri.appendPath(ArtworkProvider.ARTIST)
                            iconUri.appendPath("${libraryItem.tracksCount}")
                        }
                        MediaLibraryItem.TYPE_ALBUM -> {
                            iconUri.appendPath(ArtworkProvider.ALBUM)
                            iconUri.appendPath("${libraryItem.tracksCount}")
                        }
                        else -> {
                            iconUri.appendPath(ArtworkProvider.MEDIA)
                            (libraryItem as? MediaWrapper)?.let { iconUri.appendPath("${it.lastModified}") }
                        }
                    }
                    iconUri.appendPath("${libraryItem.id}")
                    artworkToUriCache.getOrPut(libraryItem.artworkMrl) { ArtworkProvider.buildUri(iconUri.build()) }
                } else if (libraryItem.itemType == MediaLibraryItem.TYPE_MEDIA && (libraryItem as MediaWrapper).type == MediaWrapper.TYPE_STREAM)
                    DEFAULT_STREAM_ICON
                else {
                    when (libraryItem.itemType) {
                        MediaLibraryItem.TYPE_ARTIST -> DEFAULT_ARTIST_ICON
                        MediaLibraryItem.TYPE_ALBUM -> DEFAULT_ALBUM_ICON
                        MediaLibraryItem.TYPE_GENRE -> null
                        MediaLibraryItem.TYPE_PLAYLIST -> {
                            val trackList = libraryItem.tracks.toList()
                            val hasArtwork = trackList.any { (ThumbnailsProvider.isMediaVideo(it) || (!it.artworkMrl.isNullOrEmpty() && isPathValid(it.artworkMrl))) }
                            if (!hasArtwork) DEFAULT_PLAYLIST_ICON else {
                                val playAllPlaylist = Uri.Builder()
                                        .appendPath(ArtworkProvider.PLAY_ALL)
                                        .appendPath(ArtworkProvider.PLAYLIST)
                                        .appendPath("${ArtworkProvider.computeChecksum(trackList, true)}")
                                        .appendPath("${libraryItem.tracksCount}")
                                        .appendPath("${libraryItem.id}")
                                        .build()
                                ArtworkProvider.buildUri(playAllPlaylist)
                            }
                        }
                        else -> DEFAULT_TRACK_ICON
                    }
                }

                /**
                 * Media Description
                 * The media URI not used in the browser and takes up a significant number of bytes.
                 */
                val description = MediaDescriptionCompat.Builder()
                        .setTitle(libraryItem.title)
                        .setSubtitle(subtitle)
                        .setIconUri(iconUri)
                        .setMediaId(mediaId)
                        .setExtras(extras)
                        .build()

                /* Set Flags */
                var flags = when (libraryItem.itemType) {
                    MediaLibraryItem.TYPE_MEDIA, MediaLibraryItem.TYPE_PLAYLIST -> MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    else -> MediaBrowserCompat.MediaItem.FLAG_BROWSABLE
                }
                /* Suggestions must be playable. Skip entries without artwork. */
                if (suggestionMode) {
                    flags = MediaBrowserCompat.MediaItem.FLAG_PLAYABLE
                    if (iconUri == null || iconUri.toString().startsWith(BASE_DRAWABLE_URI)) continue
                }
                results.add(MediaBrowserCompat.MediaItem(description, flags))
                if ((limitSize && results.size == MAX_HISTORY_SIZE) || results.size == MAX_RESULT_SIZE) break
            }
            artworkToUriCache.clear()
            return results
        }

        fun getContentStyle(browsableHint: Int = CONTENT_STYLE_LIST_ITEM_HINT_VALUE, playableHint: Int = CONTENT_STYLE_LIST_ITEM_HINT_VALUE): Bundle {
            return Bundle().apply {
                putInt(CONTENT_STYLE_BROWSABLE_HINT, browsableHint)
                putInt(CONTENT_STYLE_PLAYABLE_HINT, playableHint)
            }
        }

        fun generateMediaId(libraryItem: MediaLibraryItem): String {
            val prefix = when (libraryItem.itemType) {
                MediaLibraryItem.TYPE_ALBUM -> ID_ALBUM
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
