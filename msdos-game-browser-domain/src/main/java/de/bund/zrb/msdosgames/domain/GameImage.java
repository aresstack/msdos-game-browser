package de.bund.zrb.msdosgames.domain;

public final class GameImage {

    private final String title;
    private final String url;

    public GameImage(String title, String url) {
        this.title = title == null || title.trim().length() == 0 ? "Bild" : title.trim();
        if (url == null || url.trim().length() == 0) {
            throw new IllegalArgumentException("url must not be blank");
        }
        this.url = url.trim();
    }

    public String getTitle() {
        return title;
    }

    public String getUrl() {
        return url;
    }
}
