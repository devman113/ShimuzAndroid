package com.theshmuz.app.streamer;

import android.os.Parcel;
import android.os.Parcelable;

public class NowPlaying implements Parcelable {

    private String type;
    private String id;
    private String url;
    private String title;
    private int startPosition;
    private long downloadId;

    public NowPlaying(String type, String id, String url, String title, int start, long downloadId) {
        this.type = type;
        this.id = id;
        this.url = url;
        this.title = title;
        this.startPosition = start;
        this.downloadId = downloadId;
    }

    public String getType() {
        return type;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        NowPlaying other = (NowPlaying) obj;
        if (id == null) {
            if (other.id != null)
                return false;
        } else if (!id.equals(other.id))
            return false;
        if (type == null) {
            if (other.type != null)
                return false;
        } else if (!type.equals(other.type))
            return false;
        return true;
    }

    public String getId() {
        return id;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public int getStartPosition() {
        return startPosition;
    }

    public long getDownloadId() {
        return downloadId;
    }

    public boolean isSame(String type, String id){
        return type.equals(this.type) && id.equals(this.id);
    }


    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(type);
        dest.writeString(id);
        dest.writeString(url);
        dest.writeString(title);
        dest.writeInt(startPosition);
        dest.writeLong(downloadId);
    }

    private NowPlaying(Parcel in) {
        type = in.readString();
        id = in.readString();
        url = in.readString();
        title = in.readString();
        startPosition = in.readInt();
        downloadId = in.readLong();
    }

    public static final Parcelable.Creator<NowPlaying> CREATOR = new Parcelable.Creator<NowPlaying>() {
        public NowPlaying createFromParcel(Parcel in) {
            return new NowPlaying(in);
        }

        public NowPlaying[] newArray(int size) {
            return new NowPlaying[size];
        }
    };

}
