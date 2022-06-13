/*****************************************************************************
 * Medialibrary.java
 *****************************************************************************
 * Copyright © 2019 VLC authors and VideoLAN
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
 *****************************************************************************/

package org.videolan.medialibrary.interfaces;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.Tools;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.util.ArrayList;
import java.util.List;

abstract public class Medialibrary {

    // Sorting
    public final static int SORT_DEFAULT = 0;
    public final static int SORT_ALPHA = 1;
    public final static int SORT_DURATION = 2;
    public final static int SORT_INSERTIONDATE = 3;
    public final static int SORT_LASTMODIFICATIONDATE = 4;
    public final static int SORT_RELEASEDATE = 5;
    public final static int SORT_FILESIZE = 6;
    public final static int SORT_ARTIST = 7;
    public final static int SORT_PLAYCOUNT = 8;
    public final static int SORT_FILENAME = 10;
    public final static int TrackNumber = 11;
    public final static int TrackId = 12;
    public final static int NbVideo = 13;
    public final static int NbAudio = 14;
    public final static int NbMedia = 15;

    protected long mInstanceID;

    public static final int ML_INIT_SUCCESS = 0;
    public static final int ML_INIT_ALREADY_INITIALIZED = 1;
    public static final int ML_INIT_FAILED = 2;
    public static final int ML_INIT_DB_RESET = 3;
    public static final int ML_INIT_DB_CORRUPTED = 4;
    public static final int ML_INIT_DB_UNRECOVERABLE = 5;

    public static final int ML_SET_TIME_ERROR = 0;
    public static final int ML_SET_TIME_BEGIN = 1;
    public static final int ML_SET_TIME_AS_IS = 2;
    public static final int ML_SET_TIME_END = 3;

    public static final MediaWrapper[] EMPTY_COLLECTION = {};
    public static final String VLC_MEDIA_DB_NAME = "/vlc_media.db";
    public static final String THUMBS_FOLDER_NAME = "/thumbs";
    public static final String MEDIALIB_FOLDER_NAME = "/medialib";

    protected volatile boolean mIsInitiated = false;
    protected volatile boolean mIsWorking = false;
    protected static MutableLiveData<Boolean> sRunning = new MutableLiveData<>();

    protected final List<MediaCb> mMediaCbs = new ArrayList<>();
    protected final List<GenresCb> mGenreCbs = new ArrayList<>();
    protected final List<PlaylistsCb> mPlaylistCbs = new ArrayList<>();
    protected final List<HistoryCb> mHistoryCbs = new ArrayList<>();
    protected final List<MediaGroupCb> mMediaGroupCbs = new ArrayList<>();
    protected final List<OnMedialibraryReadyListener> onMedialibraryReadyListeners = new ArrayList<>();
    protected final List<OnDeviceChangeListener> onDeviceChangeListeners = new ArrayList<>();
    protected volatile boolean isMedialibraryStarted = false;
    protected final List<DevicesDiscoveryCb> devicesDiscoveryCbList = new ArrayList<>();
    protected final List<EntryPointsEventsCb> entryPointsEventsCbList = new ArrayList<>();
    private MedialibraryExceptionHandler mExceptionHandler;
    protected static Context sContext;

    protected static final Medialibrary instance = MLServiceLocator.getAbstractMedialibrary();

    public static Context getContext() {
        return sContext;
    }

    public static LiveData<Boolean> getState() {
        return sRunning;
    }

    public enum ThumbnailSizeType {
        /// A small sized thumbnail. Considered to be the default value before model 17
        Thumbnail,
        /// A banner type thumbnail. The exact size is application dependent.
        Banner
    }

    public boolean isStarted() {
        return isMedialibraryStarted;
    }

    protected void finalize() throws Throwable {
        super.finalize();
    }

    @NonNull
    public static Medialibrary getInstance() {
        return instance;
    }

    public long getId() {
        return mInstanceID;
    }

    public boolean isWorking() {
        return mIsWorking;
    }

    public boolean isInitiated() {
        return mIsInitiated;
    }

    public interface MediaCb {
        void onMediaAdded();
        void onMediaModified();
        void onMediaDeleted(long[] id);
        void onMediaConvertedToExternal(long[] id);
    }

    public interface GenresCb {
        void onGenresAdded();
        void onGenresModified();
        void onGenresDeleted();
    }

    public interface PlaylistsCb {
        void onPlaylistsAdded();
        void onPlaylistsModified();
        void onPlaylistsDeleted();
    }

    public interface HistoryCb {
        void onHistoryModified();
    }

    public interface MediaGroupCb {
        void onMediaGroupsAdded();
        void onMediaGroupsModified();
        void onMediaGroupsDeleted();
    }

    public interface OnMedialibraryReadyListener {
        void onMedialibraryReady();
        void onMedialibraryIdle();
    }

    public interface OnDeviceChangeListener {
        void onDeviceChange();
    }

    public interface MedialibraryExceptionHandler {
        void onUnhandledException(String context, String errMsg, boolean clearSuggested);
    }

    public MedialibraryExceptionHandler getExceptionHandler() {
        return mExceptionHandler;
    }

    public void setExceptionHandler(MedialibraryExceptionHandler mExceptionHandler) {
        this.mExceptionHandler = mExceptionHandler;
    }


    // If media is not in ML, find it with its path
    public MediaWrapper findMedia(MediaWrapper mw) {
        if (mIsInitiated && mw != null && mw.getId() == 0L) {
            final Uri uri = mw.getUri();
            final MediaWrapper libraryMedia = getMedia(uri);
            if (libraryMedia != null) {
                libraryMedia.addFlags(mw.getFlags());
                return libraryMedia;
            }
            if (TextUtils.equals("file", uri.getScheme()) &&
                    uri.getPath() != null && uri.getPath().startsWith("/sdcard")) {
                final MediaWrapper alternateMedia = getMedia(Tools.convertLocalUri(uri));
                if (alternateMedia != null) {
                    alternateMedia.addFlags(mw.getFlags());
                    return alternateMedia;
                }
            }
        }
        return mw;
    }

    @SuppressWarnings("unused")
    public void onMediaAdded(MediaWrapper[] mediaList) {
        synchronized (mMediaCbs) {
            for (MediaCb cb : mMediaCbs) cb.onMediaAdded();
        }
    }

    @SuppressWarnings("unused")
    public void onMediaUpdated() {
        synchronized (mMediaCbs) {
            for (MediaCb cb : mMediaCbs) cb.onMediaModified();
        }
    }

    @SuppressWarnings("unused")
    public void onMediaDeleted(long[] ids) {
        synchronized (mMediaCbs) {
            for (MediaCb cb : mMediaCbs) cb.onMediaDeleted(ids);
        }
    }

    @SuppressWarnings("unused")
    public void onMediaConvertedToExternal(long[] ids) {
        synchronized (mMediaCbs) {
            for (MediaCb cb : mMediaCbs) cb.onMediaConvertedToExternal(ids);
        }
    }

    @SuppressWarnings("unused")
    public void onGenresAdded() {
        synchronized (mGenreCbs) {
            for (GenresCb cb : mGenreCbs) cb.onGenresAdded();
        }
    }

    @SuppressWarnings("unused")
    public void onGenresModified() {
        synchronized (mGenreCbs) {
            for (GenresCb cb : mGenreCbs) cb.onGenresModified();
        }
    }

    @SuppressWarnings("unused")
    public void onGenresDeleted() {
        synchronized (mGenreCbs) {
            for (GenresCb cb : mGenreCbs) cb.onGenresDeleted();
        }
    }

    @SuppressWarnings("unused")
    public void onPlaylistsAdded() {
        synchronized (mPlaylistCbs) {
            for (PlaylistsCb cb : mPlaylistCbs) cb.onPlaylistsAdded();
        }
    }

    @SuppressWarnings("unused")
    public void onPlaylistsModified() {
        synchronized (mPlaylistCbs) {
            for (PlaylistsCb cb : mPlaylistCbs) cb.onPlaylistsModified();
        }
    }

    @SuppressWarnings("unused")
    public void onPlaylistsDeleted() {
        synchronized (mPlaylistCbs) {
            for (PlaylistsCb cb : mPlaylistCbs) cb.onPlaylistsDeleted();
        }
    }

    @SuppressWarnings("unused")
    public void onHistoryChanged(int type) {
        synchronized (mHistoryCbs) {
            for (HistoryCb cb : mHistoryCbs) cb.onHistoryModified();
        }
    }

    @SuppressWarnings("unused")
    public void onMediaGroupAdded() {
        synchronized (mMediaGroupCbs) {
            for (MediaGroupCb cb : mMediaGroupCbs) cb.onMediaGroupsAdded();
        }
    }

    @SuppressWarnings("unused")
    public void onMediaGroupModified() {
        synchronized (mMediaGroupCbs) {
            for (MediaGroupCb cb : mMediaGroupCbs) cb.onMediaGroupsModified();
        }
    }

    @SuppressWarnings("unused")
    public void onMediaGroupDeleted() {
        synchronized (mMediaGroupCbs) {
            for (MediaGroupCb cb : mMediaGroupCbs) cb.onMediaGroupsDeleted();
        }
    }

    public void onDiscoveryStarted() {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.isEmpty())
                for (DevicesDiscoveryCb cb : devicesDiscoveryCbList)
                    cb.onDiscoveryStarted();
        }
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onDiscoveryStarted();
        }
    }

    public void onDiscoveryProgress(String entryPoint) {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.isEmpty())
                for (DevicesDiscoveryCb cb : devicesDiscoveryCbList)
                    cb.onDiscoveryProgress(entryPoint);
        }
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onDiscoveryProgress(entryPoint);
        }
    }

    public void onDiscoveryCompleted() {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.isEmpty())
                for (DevicesDiscoveryCb cb : devicesDiscoveryCbList)
                    cb.onDiscoveryCompleted();
        }
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onDiscoveryCompleted();
        }
    }

    public void onDiscoveryFailed(String entryPoint) {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.isEmpty())
                for (DevicesDiscoveryCb cb : devicesDiscoveryCbList)
                    cb.onDiscoveryFailed(entryPoint);
        }
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onDiscoveryFailed(entryPoint);
        }
    }

    public void onParsingStatsUpdated(int done, int scheduled) {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.isEmpty())
                for (DevicesDiscoveryCb cb : devicesDiscoveryCbList)
                    cb.onParsingStatsUpdated(done, scheduled);
        }
    }

    @SuppressWarnings("unused")
    public void onBackgroundTasksIdleChanged(boolean isIdle) {
        mIsWorking = !isIdle;
        sRunning.postValue(mIsWorking);
        if (isIdle) {
            synchronized (onMedialibraryReadyListeners) {
                for (OnMedialibraryReadyListener listener : onMedialibraryReadyListeners) listener.onMedialibraryIdle();
            }
        }
    }
    @SuppressWarnings("unused")
    public void onUnhandledException(String context, String errMsg, boolean clearSuggested) {
        if (mExceptionHandler != null) mExceptionHandler.onUnhandledException(context, errMsg, clearSuggested);
    }

    @SuppressWarnings("unused")
    public void onReloadStarted(String entryPoint) {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.isEmpty())
                for (DevicesDiscoveryCb cb : devicesDiscoveryCbList)
                    cb.onReloadStarted(entryPoint);
        }
    }

    @SuppressWarnings("unused")
    public void onReloadCompleted(String entryPoint) {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.isEmpty())
                for (DevicesDiscoveryCb cb : devicesDiscoveryCbList)
                    cb.onReloadCompleted(entryPoint);
        }
    }

    @SuppressWarnings("unused")
    public void onEntryPointBanned(String entryPoint, boolean success) {
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onEntryPointBanned(entryPoint, success);
        }
    }

    @SuppressWarnings("unused")
    public void onEntryPointUnbanned(String entryPoint, boolean success) {
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onEntryPointUnbanned(entryPoint, success);
        }
    }


    @SuppressWarnings("unused")
    void onEntryPointAdded(String entryPoint, boolean success) {
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onEntryPointAdded(entryPoint, success);
        }
    }

    @SuppressWarnings("unused")
    public void onEntryPointRemoved(String entryPoint, boolean success) {
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.isEmpty())
                for (EntryPointsEventsCb cb : entryPointsEventsCbList)
                    cb.onEntryPointRemoved(entryPoint, success);
        }
    }

    public void addMediaCb(MediaCb mediaUpdatedCb) {
        synchronized (mMediaCbs) {
            mMediaCbs.add(mediaUpdatedCb);
        }
    }

    public void removeMediaCb(MediaCb mediaUpdatedCb) {
        synchronized (mMediaCbs) {
            mMediaCbs.remove(mediaUpdatedCb);
        }
    }

    public void addGenreCb(GenresCb GenreCb) {
        synchronized (mGenreCbs) {
            this.mGenreCbs.add(GenreCb);
        }
    }

    public void removeGenreCb(GenresCb GenreCb) {
        synchronized (mGenreCbs) {
            this.mGenreCbs.remove(GenreCb);
        }
    }

    public void addPlaylistCb(PlaylistsCb playlistCb) {
        synchronized (mPlaylistCbs) {
            this.mPlaylistCbs.add(playlistCb);
        }
    }

    public void removePlaylistCb(PlaylistsCb playlistCb) {
        synchronized (mPlaylistCbs) {
            this.mPlaylistCbs.remove(playlistCb);
        }
    }

    public void addHistoryCb(HistoryCb historyCb) {
        synchronized (mHistoryCbs) {
            this.mHistoryCbs.add(historyCb);
        }
    }

    public void removeHistoryCb(HistoryCb historyCb) {
        synchronized (mHistoryCbs) {
            this.mHistoryCbs.remove(historyCb);
        }
    }

    public void addMediaGroupCb(MediaGroupCb mediaGroupCb) {
        synchronized (mMediaGroupCbs) {
            this.mMediaGroupCbs.add(mediaGroupCb);
        }
    }

    public void removeMediaGroupCb(MediaGroupCb mediaGroupCb) {
        synchronized (mMediaGroupCbs) {
            this.mMediaGroupCbs.remove(mediaGroupCb);
        }
    }

    public void addDeviceDiscoveryCb(DevicesDiscoveryCb cb) {
        synchronized (devicesDiscoveryCbList) {
            if (!devicesDiscoveryCbList.contains(cb))
                devicesDiscoveryCbList.add(cb);
        }
    }

    public void removeDeviceDiscoveryCb(DevicesDiscoveryCb cb) {
        synchronized (devicesDiscoveryCbList) {
            devicesDiscoveryCbList.remove(cb);
        }
    }

    public void addOnMedialibraryReadyListener(OnMedialibraryReadyListener cb) {
        synchronized (onMedialibraryReadyListeners) {
            if (!onMedialibraryReadyListeners.contains(cb))
                onMedialibraryReadyListeners.add(cb);
        }
    }

    public void removeOnMedialibraryReadyListener(OnMedialibraryReadyListener cb) {
        synchronized (onMedialibraryReadyListeners) {
            onMedialibraryReadyListeners.remove(cb);
        }
    }

    public void addEntryPointsEventsCb(EntryPointsEventsCb cb) {
        synchronized (entryPointsEventsCbList) {
            if (!entryPointsEventsCbList.contains(cb))
                entryPointsEventsCbList.add(cb);
        }
    }

    public void removeEntryPointsEventsCb(EntryPointsEventsCb cb) {
        synchronized (entryPointsEventsCbList) {
            entryPointsEventsCbList.remove(cb);
        }
    }

    public void addOnDeviceChangeListener(OnDeviceChangeListener listener) {
        synchronized (onDeviceChangeListeners) {
            if (!onDeviceChangeListeners.contains(listener))
                onDeviceChangeListeners.add(listener);
        }
    }

    public void removeOnDeviceChangeListener(OnDeviceChangeListener listener) {
        synchronized (onDeviceChangeListeners) {
            onDeviceChangeListeners.remove(listener);
        }
    }

    abstract public boolean construct(Context context);
    abstract public int init(Context context);
    abstract public void start();
    abstract public void setLibVLCInstance(long libVLC);

    abstract public void pauseBackgroundOperations();
    abstract public void resumeBackgroundOperations();
    abstract public void reload();
    abstract public void reload(String entrypoint);
    abstract public void forceParserRetry();
    abstract public void forceRescan();
    abstract public MediaWrapper getMedia(long id);
    abstract public MediaWrapper getMedia(Uri uri);
    abstract public MediaWrapper getMedia(String mrl);
    abstract public MediaWrapper addMedia(String mrl, long duration);
    abstract public boolean removeExternalMedia(long id);
    abstract public int setLastTime(long mediaId, long time);
    abstract public boolean setLastPosition(long mediaId, float position);
}
