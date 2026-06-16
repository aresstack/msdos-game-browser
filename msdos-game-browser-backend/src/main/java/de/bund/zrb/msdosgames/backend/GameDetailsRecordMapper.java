package de.bund.zrb.msdosgames.backend;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameFile;
import de.bund.zrb.msdosgames.domain.GameIdentifier;
import de.bund.zrb.msdosgames.domain.GameImage;
import de.bund.zrb.msdosgames.domain.LicenseNotice;

import java.util.ArrayList;
import java.util.List;

final class GameDetailsRecordMapper {

    GameDetailsRecord toRecord(GameDetails details) {
        GameDetailsRecord record = new GameDetailsRecord();
        record.identifier = details.getIdentifier().getValue();
        record.title = details.getTitle();
        record.description = details.getDescriptionText();
        record.licenseUrl = details.getLicenseNotice().getLicenseUrl();
        record.rights = details.getLicenseNotice().getRights();
        record.pageUrl = details.getLicenseNotice().getSourceUrl();
        record.itemSize = details.getItemSize();
        record.files = toFileRecords(details.getDownloadableFiles());
        record.images = toImageRecords(details.getPreviewImages());
        return record;
    }

    GameDetails toDetails(GameDetailsRecord record) {
        return new GameDetails(
                GameIdentifier.of(record.identifier),
                record.title,
                record.description,
                new LicenseNotice(record.licenseUrl, record.rights, record.pageUrl),
                toFiles(record.files),
                toImages(record.images),
                record.itemSize);
    }

    private List<GameDetailsRecord.GameFileRecord> toFileRecords(List<GameFile> files) {
        List<GameDetailsRecord.GameFileRecord> records = new ArrayList<GameDetailsRecord.GameFileRecord>();
        for (GameFile file : files) {
            GameDetailsRecord.GameFileRecord record = new GameDetailsRecord.GameFileRecord();
            record.name = file.getName();
            record.format = file.getFormat();
            record.size = file.getSize();
            record.md5 = file.getMd5();
            record.sha1 = file.getSha1();
            records.add(record);
        }
        return records;
    }

    private List<GameDetailsRecord.GameImageRecord> toImageRecords(List<GameImage> images) {
        List<GameDetailsRecord.GameImageRecord> records = new ArrayList<GameDetailsRecord.GameImageRecord>();
        for (GameImage image : images) {
            GameDetailsRecord.GameImageRecord record = new GameDetailsRecord.GameImageRecord();
            record.title = image.getTitle();
            record.url = image.getUrl();
            records.add(record);
        }
        return records;
    }

    private List<GameFile> toFiles(List<GameDetailsRecord.GameFileRecord> records) {
        List<GameFile> files = new ArrayList<GameFile>();
        if (records == null) {
            return files;
        }
        for (GameDetailsRecord.GameFileRecord record : records) {
            files.add(new GameFile(record.name, record.format, record.size, record.md5, record.sha1));
        }
        return files;
    }

    private List<GameImage> toImages(List<GameDetailsRecord.GameImageRecord> records) {
        List<GameImage> images = new ArrayList<GameImage>();
        if (records == null) {
            return images;
        }
        for (GameDetailsRecord.GameImageRecord record : records) {
            images.add(new GameImage(record.title, record.url));
        }
        return images;
    }
}
