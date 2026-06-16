package de.bund.zrb.msdosgames.infrastructure.archive;

import com.google.gson.Gson;
import de.bund.zrb.msdosgames.application.port.GameCatalog;
import de.bund.zrb.msdosgames.application.port.GameDetailsProvider;
import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameFile;
import de.bund.zrb.msdosgames.domain.GameIdentifier;
import de.bund.zrb.msdosgames.domain.GameImage;
import de.bund.zrb.msdosgames.domain.GamePage;
import de.bund.zrb.msdosgames.domain.GameSearchCriteria;
import de.bund.zrb.msdosgames.domain.GameSummary;
import de.bund.zrb.msdosgames.domain.LicenseNotice;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class InternetArchiveCatalogClient implements GameCatalog, GameDetailsProvider {

    private final HttpGateway httpGateway;
    private final Gson gson;
    private final InternetArchiveUrlBuilder urlBuilder;
    private final ArchiveSearchQueryBuilder searchQueryBuilder;
    private final ArchiveHtmlSanitizer htmlSanitizer;
    private final ArchiveFileFilter fileFilter;

    public InternetArchiveCatalogClient(HttpGateway httpGateway) {
        this(httpGateway, new Gson(), new InternetArchiveUrlBuilder(), new ArchiveSearchQueryBuilder(), new ArchiveHtmlSanitizer());
    }

    InternetArchiveCatalogClient(
            HttpGateway httpGateway,
            Gson gson,
            InternetArchiveUrlBuilder urlBuilder,
            ArchiveSearchQueryBuilder searchQueryBuilder,
            ArchiveHtmlSanitizer htmlSanitizer) {
        if (httpGateway == null) {
            throw new IllegalArgumentException("httpGateway must not be null");
        }
        this.httpGateway = httpGateway;
        this.gson = gson;
        this.urlBuilder = urlBuilder;
        this.searchQueryBuilder = searchQueryBuilder;
        this.htmlSanitizer = htmlSanitizer;
        this.fileFilter = new ArchiveFileFilter();
    }

    @Override
    public GamePage browse(GameSearchCriteria criteria) throws IOException {
        String query = searchQueryBuilder.buildBrowseQuery();
        return loadSearchPage(query, criteria);
    }

    @Override
    public GamePage search(GameSearchCriteria criteria) throws IOException {
        String query = searchQueryBuilder.buildSearchQuery(criteria.getQuery());
        return loadSearchPage(query, criteria);
    }

    @Override
    public GameDetails loadDetails(GameIdentifier identifier) throws IOException {
        String responseBody = httpGateway.getText(urlBuilder.buildMetadataUrl(identifier));
        ArchiveMetadataResponse response = gson.fromJson(responseBody, ArchiveMetadataResponse.class);
        ArchiveMetadataResponse.ArchiveMetadata metadata = response == null ? null : response.metadata;

        List<ArchiveMetadataResponse.ArchiveFile> archiveFiles = response == null ? Collections.<ArchiveMetadataResponse.ArchiveFile>emptyList() : response.files;
        String title = metadata == null ? identifier.getValue() : ArchiveJsonValues.text(metadata.title);
        String descriptionText = metadata == null ? "" : htmlSanitizer.toPlainText(ArchiveJsonValues.text(metadata.description));
        LicenseNotice licenseNotice = createLicenseNotice(identifier, metadata);
        List<GameFile> files = mapFiles(archiveFiles);
        List<GameImage> images = mapImages(identifier, archiveFiles);
        long itemSize = response == null ? 0L : ArchiveJsonValues.number(response.item_size);

        return new GameDetails(identifier, title, descriptionText, licenseNotice, files, images, itemSize);
    }

    private GamePage loadSearchPage(String query, GameSearchCriteria criteria) throws IOException {
        String responseBody = httpGateway.getText(urlBuilder.buildScrapeUrl(query, criteria));
        ArchiveSearchResponse response = gson.fromJson(responseBody, ArchiveSearchResponse.class);
        if (response == null || response.items == null) {
            return new GamePage(Collections.<GameSummary>emptyList(), null);
        }

        List<GameSummary> games = new ArrayList<GameSummary>();
        for (ArchiveSearchResponse.ArchiveSearchItem item : response.items) {
            if (ArchiveJsonValues.text(item.identifier).length() > 0) {
                games.add(mapSummary(item));
            }
        }
        return new GamePage(games, response.cursor);
    }

    private GameSummary mapSummary(ArchiveSearchResponse.ArchiveSearchItem item) {
        return new GameSummary(
                GameIdentifier.of(ArchiveJsonValues.text(item.identifier)),
                ArchiveJsonValues.text(item.title),
                htmlSanitizer.toPlainText(ArchiveJsonValues.text(item.description)),
                ArchiveJsonValues.text(item.creator),
                ArchiveJsonValues.text(item.date),
                ArchiveJsonValues.text(item.publicdate),
                ArchiveJsonValues.number(item.downloads),
                ArchiveJsonValues.number(item.item_size));
    }

    private LicenseNotice createLicenseNotice(GameIdentifier identifier, ArchiveMetadataResponse.ArchiveMetadata metadata) {
        String licenseUrl = metadata == null ? "" : ArchiveJsonValues.text(metadata.licenseurl);
        String rights = metadata == null ? "" : ArchiveJsonValues.text(metadata.rights);
        return new LicenseNotice(licenseUrl, rights, urlBuilder.buildItemUrl(identifier));
    }

    private List<GameFile> mapFiles(List<ArchiveMetadataResponse.ArchiveFile> archiveFiles) {
        if (archiveFiles == null || archiveFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<GameFile> gameFiles = new ArrayList<GameFile>();
        for (ArchiveMetadataResponse.ArchiveFile archiveFile : archiveFiles) {
            if (fileFilter.accepts(archiveFile) && !isPreviewImage(archiveFile)) {
                gameFiles.add(new GameFile(
                        ArchiveJsonValues.text(archiveFile.name),
                        ArchiveJsonValues.text(archiveFile.format),
                        ArchiveJsonValues.number(archiveFile.size),
                        ArchiveJsonValues.text(archiveFile.md5),
                        ArchiveJsonValues.text(archiveFile.sha1)));
            }
        }
        Collections.sort(gameFiles, new GameFileDownloadOrder());
        return gameFiles;
    }

    private List<GameImage> mapImages(GameIdentifier identifier, List<ArchiveMetadataResponse.ArchiveFile> archiveFiles) {
        if (archiveFiles == null || archiveFiles.isEmpty()) {
            return Collections.emptyList();
        }

        List<GameImage> images = new ArrayList<GameImage>();
        for (ArchiveMetadataResponse.ArchiveFile archiveFile : archiveFiles) {
            if (isPreviewImage(archiveFile)) {
                String fileName = ArchiveJsonValues.text(archiveFile.name);
                images.add(new GameImage(fileName, urlBuilder.buildDownloadUrl(identifier, fileName)));
            }
        }
        Collections.sort(images, new GameImagePreviewOrder());
        return images;
    }

    private boolean isPreviewImage(ArchiveMetadataResponse.ArchiveFile archiveFile) {
        String name = ArchiveJsonValues.text(archiveFile.name).toLowerCase();
        String format = ArchiveJsonValues.text(archiveFile.format).toLowerCase();
        return name.endsWith(".jpg")
                || name.endsWith(".jpeg")
                || name.endsWith(".png")
                || name.endsWith(".gif")
                || format.contains("jpeg")
                || format.contains("png")
                || format.contains("gif");
    }
}
