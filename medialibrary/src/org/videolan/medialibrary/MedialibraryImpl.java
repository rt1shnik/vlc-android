/*****************************************************************************
 * MedialibraryImpl.java
 *****************************************************************************
 * Copyright © 2017-2019 VLC authors and VideoLAN
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

package org.videolan.medialibrary;

import android.content.Context;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.util.VLCUtil;
import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

import java.io.File;

public class MedialibraryImpl extends Medialibrary {
    private static final String TAG = "VLC/JMedialibrary";

    public boolean construct(Context context) {
        if (context == null) throw new IllegalStateException("context cannot be null");
        if (mIsInitiated) return false;
        sContext = context;
        final File extFilesDir = context.getExternalFilesDir(null);
        File dbDirectory = context.getDir("db", Context.MODE_PRIVATE);
        if (extFilesDir == null || !extFilesDir.exists()
                || dbDirectory == null || !dbDirectory.canWrite())
            return false;
        LibVLC.loadLibraries();
        try {
            System.loadLibrary("c++_shared");
            System.loadLibrary("mla");
        } catch (UnsatisfiedLinkError ule) {
            Log.e(TAG, "Can't load mla: " + ule);
            return false;
        }
        final File oldDir = new File(extFilesDir + THUMBS_FOLDER_NAME);
        if (oldDir.isDirectory()) {
            //remove old thumbnails directory
            new Thread(() -> {

                String[] children = oldDir.list();
                if (children != null) {
                    for (String child : children) {
                        new File(oldDir, child).delete();
                    }
                }
                oldDir.delete();
            }).start();
        }
        nativeConstruct(dbDirectory + VLC_MEDIA_DB_NAME, extFilesDir + MEDIALIB_FOLDER_NAME);
        return true;
    }

    public int init(Context context) {
        if (context == null) return ML_INIT_FAILED;
        if (mIsInitiated) return ML_INIT_ALREADY_INITIALIZED;
        if (sContext == null) throw new IllegalStateException("Medialibrary construct has to be called before init");
        File dbDirectory = context.getDir("db", Context.MODE_PRIVATE);
        int initCode = nativeInit(dbDirectory + VLC_MEDIA_DB_NAME);
        if (initCode == ML_INIT_DB_CORRUPTED) {
            Log.e(TAG, "Medialib database is corrupted. Clearing it and try to restore playlists");
        }

        mIsInitiated = initCode != ML_INIT_FAILED;
        return initCode;
    }

    @Override
    public void start() {
        if (isStarted()) return;
        isMedialibraryStarted = true;
        synchronized (onMedialibraryReadyListeners) {
            for (OnMedialibraryReadyListener listener : onMedialibraryReadyListeners) listener.onMedialibraryReady();
        }
    }

    @Override
    public void setLibVLCInstance(long libVLC) {
        if (mIsInitiated) nativeSetLibVLCInstance(libVLC);
    }

    @Override
    protected void finalize() throws Throwable {
        if (mIsInitiated) nativeRelease();
        super.finalize();
    }

    public void pauseBackgroundOperations() {
        if (mIsInitiated) nativePauseBackgroundOperations();
    }

    public void resumeBackgroundOperations() {
        if (mIsInitiated) nativeResumeBackgroundOperations();
    }

    public void reload() {
        if (mIsInitiated) nativeReload();
    }

    public void reload(String entryPoint) {
        if (mIsInitiated && !TextUtils.isEmpty(entryPoint))
            nativeReload(Tools.encodeVLCMrl(entryPoint));
    }

    public void forceParserRetry() {
        if (mIsInitiated) nativeForceParserRetry();
    }

    public void forceRescan() {
        if (mIsInitiated) nativeForceRescan();
    }

    @Nullable
    public MediaWrapper getMedia(long id) {
        return mIsInitiated ? nativeGetMedia(id) : null;
    }

    @Nullable
    public MediaWrapper getMedia(Uri uri) {
        if ("content".equals(uri.getScheme())) return null;
        final String vlcMrl = Tools.encodeVLCMrl(uri.toString());
        return mIsInitiated && !TextUtils.isEmpty(vlcMrl) ? nativeGetMediaFromMrl(vlcMrl) : null;
    }

    @Nullable
    public MediaWrapper getMedia(String mrl) {
        if (mrl != null && mrl.startsWith("content:")) return null;
        final String vlcMrl = Tools.encodeVLCMrl(mrl);
        return mIsInitiated && !TextUtils.isEmpty(vlcMrl) ? nativeGetMediaFromMrl(vlcMrl) : null;
    }

    @Nullable
    public MediaWrapper addMedia(String mrl, long duration) {
        final String vlcMrl = Tools.encodeVLCMrl(mrl);
        return mIsInitiated && !TextUtils.isEmpty(vlcMrl) ? nativeAddMedia(vlcMrl, duration) : null;
    }

    public boolean removeExternalMedia(long id) {
        return mIsInitiated && nativeRemoveExternalMedia(id);
    }

    public int setLastTime(long mediaId, long lastTime) {
        if (!mIsInitiated || mediaId < 1) {
            return ML_SET_TIME_ERROR;
        }
        return nativeSetLastTime(mediaId, lastTime);
    }

    public boolean setLastPosition(long mediaId, float position) {
        return mIsInitiated && mediaId > 0 && nativeSetLastPosition(mediaId, position);
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

    // Native methods
    private native void nativeConstruct(String dbPath, String thumbsPath);
    private native int nativeInit(String dbPath);
    private native void nativeRelease();

    private native void nativeSetLibVLCInstance(long libVLC);
    private native MediaWrapper nativeGetMedia(long id);
    private native MediaWrapper nativeGetMediaFromMrl(String mrl);
    private native MediaWrapper nativeAddMedia(String mrl, long duration);
    private native boolean nativeRemoveExternalMedia(long id);

    private native void nativePauseBackgroundOperations();
    private native void nativeResumeBackgroundOperations();
    private native void nativeReload();
    private native void nativeReload(String entryPoint);
    private native void nativeForceParserRetry();
    private native void nativeForceRescan();
    private native int nativeSetLastTime(long mediaId, long progress);
    private native boolean nativeSetLastPosition(long mediaId, float position);
}
