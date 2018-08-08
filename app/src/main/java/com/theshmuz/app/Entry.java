package com.theshmuz.app;

public class Entry {

    public String title;
    public String content;

    public String urlArtwork;
    public String urlAudio;
    public String urlVideo;
    public String urlPdf;

    public int startPosition;
    public int duration;

    public long downloadId;

    @Override
    public String toString() {
        if(!D.D) return "";
        StringBuilder sb = new StringBuilder("Entry[");
        sb.append("title=").append(title).append(", ");
        sb.append("urlArtwork=").append(urlArtwork).append(", ");
        sb.append("urlAudio=").append(urlAudio).append(", ");
        sb.append("urlVideo=").append(urlVideo).append(", ");
        sb.append("urlPdf=").append(urlPdf).append(", ");
        sb.append(']');
        return sb.toString();
    }
}
