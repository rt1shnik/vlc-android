package org.videolan.medialibrary;

import android.net.Uri;
import android.os.Parcel;

import org.videolan.libvlc.interfaces.IMedia;
import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.interfaces.media.Bookmark;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;
import org.videolan.medialibrary.media.BookmarkImpl;
import org.videolan.medialibrary.media.MediaWrapperImpl;

public class MLServiceLocator {

    private static volatile Medialibrary instance;

    public static synchronized Medialibrary getAbstractMedialibrary() {
        if (instance == null) {
            instance = new MedialibraryImpl();
        }
        return instance;
    }

    public static MediaWrapper getAbstractMediaWrapper(Uri uri) {
        return new MediaWrapperImpl(uri);
    }

    public static MediaWrapper getAbstractMediaWrapper(IMedia media) {
        return new MediaWrapperImpl(media);
    }

    public static MediaWrapper getAbstractMediaWrapper(Parcel in) {
        return new MediaWrapperImpl(in);
    }

    public static Bookmark getAbstractBookmark(Parcel in) {
        return new BookmarkImpl(in);
    }
}
