package de.bund.zrb.msdosgames.launcher;

import de.bund.zrb.msdosgames.application.usecase.AcceptLicenseUseCase;
import de.bund.zrb.msdosgames.application.usecase.BrowseGamesUseCase;
import de.bund.zrb.msdosgames.application.usecase.DownloadGameUseCase;
import de.bund.zrb.msdosgames.application.usecase.LoadGameDetailsUseCase;
import de.bund.zrb.msdosgames.application.usecase.SearchGamesUseCase;
import de.bund.zrb.msdosgames.backend.LuceneH2GameBrowserBackendService;
import de.bund.zrb.msdosgames.frontend.swing.GameBrowserFrame;
import de.bund.zrb.msdosgames.infrastructure.archive.InternetArchiveCatalogClient;
import de.bund.zrb.msdosgames.infrastructure.archive.InternetArchiveDownloadClient;
import de.bund.zrb.msdosgames.infrastructure.archive.JdkHttpGateway;
import de.bund.zrb.msdosgames.infrastructure.local.ApplicationDirectories;
import de.bund.zrb.msdosgames.infrastructure.local.FileBasedLicenseAcceptanceStore;

import javax.swing.SwingUtilities;

public final class MsdosGameBrowserApplication {

    private MsdosGameBrowserApplication() {
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                createFrame().showBrowser();
            }
        });
    }

    private static GameBrowserFrame createFrame() {
        JdkHttpGateway httpGateway = new JdkHttpGateway();
        InternetArchiveCatalogClient archiveCatalogClient = new InternetArchiveCatalogClient(httpGateway);
        InternetArchiveDownloadClient downloadClient = new InternetArchiveDownloadClient();
        FileBasedLicenseAcceptanceStore licenseAcceptanceStore = new FileBasedLicenseAcceptanceStore(ApplicationDirectories.defaultLicenseStoreFile());
        LuceneH2GameBrowserBackendService backendService = new LuceneH2GameBrowserBackendService(archiveCatalogClient, archiveCatalogClient);

        return new GameBrowserFrame(
                backendService,
                new AcceptLicenseUseCase(licenseAcceptanceStore),
                new DownloadGameUseCase(licenseAcceptanceStore, downloadClient),
                ApplicationDirectories.defaultDownloadDirectory());
    }
}
