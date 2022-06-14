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

import android.net.Uri;
import android.text.TextUtils;

import androidx.annotation.Nullable;

import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

public class MedialibraryImpl extends Medialibrary {
    private static final String TAG = "VLC/JMedialibrary";

    @Override
    protected void finalize() throws Throwable {
        if (mIsInitiated) nativeRelease();
        super.finalize();
    }

    public void reload() {
        if (mIsInitiated) nativeReload();
    }

    public void reload(String entryPoint) {
        if (mIsInitiated && !TextUtils.isEmpty(entryPoint))
            nativeReload(Tools.encodeVLCMrl(entryPoint));
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
    private native void nativeRelease();

    private native MediaWrapper nativeGetMedia(long id);
    private native MediaWrapper nativeGetMediaFromMrl(String mrl);
    private native MediaWrapper nativeAddMedia(String mrl, long duration);

    private native void nativeReload();
    private native void nativeReload(String entryPoint);
    private native int nativeSetLastTime(long mediaId, long progress);
    private native boolean nativeSetLastPosition(long mediaId, float position);
}
