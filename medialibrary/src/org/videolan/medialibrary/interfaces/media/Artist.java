package org.videolan.medialibrary.interfaces.media;

import android.os.Parcel;
import android.os.Parcelable;

import org.videolan.libvlc.util.VLCUtil;
import org.videolan.medialibrary.R;
import org.videolan.medialibrary.MLServiceLocator;
import org.videolan.medialibrary.interfaces.Medialibrary;
import org.videolan.medialibrary.media.MediaLibraryItem;

abstract public class Artist extends MediaLibraryItem {

    private String shortBio;
    private String artworkMrl;
    private String musicBrainzId;
    private int tracksCount;
    private int albumsCount;
    private int presentTracksCount;

    public static class SpecialRes {
        public static String UNKNOWN_ARTIST = Medialibrary.getContext().getString(R.string.unknown_artist);
        public static String VARIOUS_ARTISTS = Medialibrary.getContext().getString(R.string.various_artists);
    }

    public Artist(long id, String name, String shortBio, String artworkMrl, String musicBrainzId, int albumsCount, int tracksCount, int presentTracksCount) {
        super(id, name);
        this.shortBio = shortBio;
        this.artworkMrl = artworkMrl != null ? VLCUtil.UriFromMrl(artworkMrl).getPath() : null;
        this.musicBrainzId = musicBrainzId;
        this.tracksCount = tracksCount;
        this.albumsCount = albumsCount;
        this.presentTracksCount = presentTracksCount;
        if (id == 1L) {
            mTitle = SpecialRes.UNKNOWN_ARTIST;
        } else if (id == 2L) {
            mTitle = SpecialRes.VARIOUS_ARTISTS;
        }
    }

    abstract public int searchAlbumsCount(String query);
    abstract public MediaWrapper[] searchTracks(String query, int sort, boolean desc, boolean includeMissing, int nbItems, int offset);
    abstract public int searchTracksCount(String query);
    abstract public MediaWrapper[] getTracks(int sort, boolean desc, boolean includeMissing);
    abstract public MediaWrapper[] getPagedTracks(int sort, boolean desc, boolean includeMissing, int nbItems, int offset);

    public String getShortBio() {
        return shortBio;
    }

    @Override
    public String getArtworkMrl() {
        return artworkMrl;
    }

    public String getMusicBrainzId() {
        return musicBrainzId;
    }

    public void setShortBio(String shortBio) {
        this.shortBio = shortBio;
    }

    public void setArtworkMrl(String artworkMrl) {
        this.artworkMrl = artworkMrl;
    }

    @Override
    public int getTracksCount() {
        return tracksCount;
    }

    public int getAlbumsCount() {
        return albumsCount;
    }

    public int getPresentTracksCount() {
        return presentTracksCount;
    }

    @Override
    public MediaWrapper[] getTracks() {
        return getTracks(Medialibrary.SORT_ARTIST, false, true);
    }

    @Override
    public int getItemType() {
        return TYPE_ARTIST;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        super.writeToParcel(parcel, i);
        parcel.writeString(shortBio);
        parcel.writeString(artworkMrl);
        parcel.writeString(musicBrainzId);
        parcel.writeInt(tracksCount);
        parcel.writeInt(albumsCount);
    }

    public Artist(Parcel in) {
        super(in);
        this.shortBio = in.readString();
        this.artworkMrl = in.readString();
        this.musicBrainzId = in.readString();
        this.tracksCount = in.readInt();
        this.albumsCount = in.readInt();
    }
}
