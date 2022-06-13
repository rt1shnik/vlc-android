package org.videolan.medialibrary.media;

import android.os.Parcel;

import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.interfaces.media.Artist;
import org.videolan.medialibrary.interfaces.media.Genre;
import org.videolan.medialibrary.interfaces.media.MediaWrapper;

@SuppressWarnings("JniMissingFunction")
public class GenreImpl extends Genre {

    public GenreImpl(long id, String title, int nbTracks, int nbPresentTracks) {
        super(id, title, nbTracks, nbPresentTracks);
    }

    public Artist[] getArtists(int sort, boolean desc, boolean includeMissing) {
        final Medialibrary ml = Medialibrary.getInstance();
        return ml.isInitiated() ? nativeGetArtists(ml, mId, sort, desc, includeMissing) : new Artist[0];
    }

    public MediaWrapper[] getTracks(boolean withThumbnail, int sort, boolean desc, boolean includeMissing) {
        final Medialibrary ml = Medialibrary.getInstance();
        return ml.isInitiated() ? nativeGetTracks(ml, mId, withThumbnail, sort, desc, includeMissing) : Medialibrary.EMPTY_COLLECTION;
    }

    public MediaWrapper[] getPagedTracks(boolean withThumbnail, int sort, boolean desc, boolean includeMissing, int nbItems, int offset) {
        final Medialibrary ml = Medialibrary.getInstance();
        return ml.isInitiated() ? nativeGetPagedTracks(ml, mId, withThumbnail, sort, desc, includeMissing, nbItems, offset) : Medialibrary.EMPTY_COLLECTION;
    }

    public MediaWrapper[] searchTracks(String query, int sort, boolean desc, boolean includeMissing, int nbItems, int offset) {
        final Medialibrary ml = Medialibrary.getInstance();
        return ml.isInitiated() ? nativeSearch(ml, mId, query, sort, desc, includeMissing, nbItems, offset) : Medialibrary.EMPTY_COLLECTION;
    }

    public int searchTracksCount(String query) {
        final Medialibrary ml = Medialibrary.getInstance();
        return ml.isInitiated() ? nativeGetSearchCount(ml, mId, query) : 0;
    }

    private native Artist[] nativeGetArtists(Medialibrary ml, long mId, int sort, boolean desc, boolean includeMissing);
    private native MediaWrapper[] nativeGetTracks(Medialibrary ml, long mId, boolean withThumbnail, int sort, boolean desc, boolean includeMissing);

    private native Artist[] nativeGetPagedArtists(Medialibrary ml, long mId, int sort, boolean desc, boolean includeMissing, int nbItems, int offset);
    private native MediaWrapper[] nativeGetPagedTracks(Medialibrary ml, long mId, boolean withThumbnail, int sort, boolean desc, boolean includeMissing, int nbItems, int offset);
    private native int nativeGetAlbumsCount(Medialibrary ml, long mId);
    private native int nativeGetArtistsCount(Medialibrary ml, long mId);
    private native MediaWrapper[] nativeSearch(Medialibrary ml, long mId, String query, int sort, boolean desc, boolean includeMissing, int nbItems, int offset);
    private native int nativeGetSearchCount(Medialibrary ml, long mId, String query);
    private native int nativeGetSearchAlbumCount(Medialibrary ml, long mId, String query);

    public GenreImpl(Parcel in) {
        super(in);
    }
}
