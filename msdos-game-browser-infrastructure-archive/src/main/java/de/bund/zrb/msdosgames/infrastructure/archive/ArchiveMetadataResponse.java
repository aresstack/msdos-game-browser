package de.bund.zrb.msdosgames.infrastructure.archive;

import com.google.gson.JsonElement;

import java.util.List;

final class ArchiveMetadataResponse {

    ArchiveMetadata metadata;
    List<ArchiveFile> files;
    JsonElement item_size;

    static final class ArchiveMetadata {
        JsonElement identifier;
        JsonElement title;
        JsonElement description;
        JsonElement creator;
        JsonElement date;
        JsonElement licenseurl;
        JsonElement rights;
    }

    static final class ArchiveFile {
        JsonElement name;
        JsonElement format;
        JsonElement size;
        JsonElement md5;
        JsonElement sha1;
        JsonElement source;
    }
}
