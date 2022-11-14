package com.quantumdevlopment.musicplayer;


public class SongData {
  public String title;
  public String path;
  public String album;
  public String artist;
  public long id;
  public boolean isFav;
  public static MusicService musicService;
    public SongData(String title, String path, String album, String artist, long id, boolean isFav) {
        this.title = title;
        this.path = path;
        this.id = id;
        this.album = album;
        this.artist = artist;
        this.isFav = isFav;
    }

    public SongData() {
        musicService = new MusicService();
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getAlbum() {
        return album;
    }

    public void setAlbum(String album) {
        this.album = album;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }




}
