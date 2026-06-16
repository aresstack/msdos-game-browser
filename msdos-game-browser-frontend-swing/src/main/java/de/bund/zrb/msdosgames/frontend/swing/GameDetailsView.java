package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameFile;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.File;

final class GameDetailsView extends JPanel {

    private final GameMetadataPanel metadataPanel = new GameMetadataPanel();
    private final GameImagePreviewPanel imagePreviewPanel;
    private final GameDescriptionPanel descriptionPanel = new GameDescriptionPanel();
    private final ArchiveItemNoticePanel archiveItemNoticePanel = new ArchiveItemNoticePanel();
    private final DownloadControlsPanel downloadControlsPanel = new DownloadControlsPanel();

    GameDetailsView(GameImagePreviewPanel.PreviewImageLoader imageLoader) {
        super(new BorderLayout(6, 6));
        this.imagePreviewPanel = new GameImagePreviewPanel(imageLoader);

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 6, 6));
        topPanel.add(metadataPanel);
        topPanel.add(imagePreviewPanel);

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        bottomPanel.add(archiveItemNoticePanel);
        bottomPanel.add(downloadControlsPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, descriptionPanel, bottomPanel);
        splitPane.setResizeWeight(0.45d);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(splitPane), BorderLayout.CENTER);
    }

    void clear(File downloadDirectory) {
        metadataPanel.clear();
        imagePreviewPanel.clear();
        descriptionPanel.clear();
        archiveItemNoticePanel.clear();
        downloadControlsPanel.clear(downloadDirectory);
    }

    void showDetails(GameDetails details, File downloadDirectory) {
        metadataPanel.showDetails(details);
        imagePreviewPanel.showDetails(details);
        descriptionPanel.showDetails(details);
        archiveItemNoticePanel.showDetails(details);
        downloadControlsPanel.showDetails(details, downloadDirectory);
    }

    GameFile getSelectedFile() {
        return downloadControlsPanel.getSelectedFile();
    }

    java.util.List<GameFile> getDownloadableFiles() {
        return downloadControlsPanel.getDownloadableFiles();
    }

    void selectDownloadFile(GameFile file) {
        downloadControlsPanel.selectFile(file);
    }

    File getCurrentDirectory() {
        return downloadControlsPanel.getCurrentDirectory();
    }

    File getSelectedTargetFile() {
        return downloadControlsPanel.getSelectedTargetFile();
    }

    void updateSelectedTargetPath() {
        downloadControlsPanel.updateTargetFileLabel();
    }
}
