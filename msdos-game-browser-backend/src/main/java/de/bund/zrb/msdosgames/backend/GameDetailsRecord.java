package de.bund.zrb.msdosgames.backend;

import java.util.ArrayList;
import java.util.List;

final class GameDetailsRecord {

    String identifier;
    String title;
    String description;
    String licenseUrl;
    String rights;
    String pageUrl;
    long itemSize;
    List<GameFileRecord> files = new ArrayList<GameFileRecord>();
    List<GameImageRecord> images = new ArrayList<GameImageRecord>();

    static final class GameFileRecord {
        String name;
        String format;
        long size;
        String md5;
        String sha1;
    }

    static final class GameImageRecord {
        String title;
        String url;
    }
}
