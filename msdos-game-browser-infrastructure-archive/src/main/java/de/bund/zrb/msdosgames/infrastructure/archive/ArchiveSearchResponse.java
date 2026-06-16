package de.bund.zrb.msdosgames.infrastructure.archive;

import com.google.gson.JsonElement;

import java.util.List;

final class ArchiveSearchResponse {

    List<ArchiveSearchItem> items;
    String cursor;

    static final class ArchiveSearchItem {
        JsonElement identifier;
        JsonElement title;
        JsonElement description;
        JsonElement creator;
        JsonElement date;
        JsonElement publicdate;
        JsonElement downloads;
        JsonElement item_size;
    }
}
