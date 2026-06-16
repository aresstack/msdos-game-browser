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
    private final GameImagePreviewPanel imagePreviewPanel = new GameImagePreviewPanel();
    private final GameDescriptionPanel descriptionPanel = new GameDescriptionPanel();
    private final LicenseNoticePanel licenseNoticePanel = new LicenseNoticePanel();
    private final DownloadControlsPanel downloadControlsPanel = new DownloadControlsPanel();

    GameDetailsView() {
        super(new BorderLayout(6, 6));

        JPanel topPanel = new JPanel(new GridLayout(1, 2, 6, 6));
        topPanel.add(metadataPanel);
        topPanel.add(imagePreviewPanel);

        JPanel bottomPanel = new JPanel(new GridLayout(2, 1, 6, 6));
        bottomPanel.add(licenseNoticePanel);
        bottomPanel.add(downloadControlsPanel);

        JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, descriptionPanel, bottomPanel);
        splitPane.setResizeWeight(0.45d);

        add(topPanel, BorderLayout.NORTH);
        add(new JScrollPane(splitPane), BorderLayout.CENTER);
    }

    void bindActions(Runnable acceptAction, Runnable downloadAction, Runnable selectionChangedAction) {
        downloadControlsPanel.bindActions(acceptAction, downloadAction, selectionChangedAction);
    }

    void clear(File downloadDirectory) {
        metadataPanel.clear();
        imagePreviewPanel.clear();
        descriptionPanel.clear();
        licenseNoticePanel.clear();
        downloadControlsPanel.clear(downloadDirectory);
    }

    void showDetails(GameDetails details, boolean accepted, File downloadDirectory) {
        metadataPanel.showDetails(details);
        imagePreviewPanel.showDetails(details);
        descriptionPanel.showDetails(details);
        licenseNoticePanel.showDetails(details);
        downloadControlsPanel.showDetails(details, accepted, downloadDirectory);
    }

    GameFile getSelectedFile() {
        return downloadControlsPanel.getSelectedFile();
    }

    boolean isNoticeAccepted() {
        return downloadControlsPanel.isNoticeAccepted();
    }

    void setNoticeAccepted(boolean accepted) {
        downloadControlsPanel.setNoticeAccepted(accepted);
    }

    File getCurrentDirectory() {
        return downloadControlsPanel.getCurrentDirectory();
    }

    File getSelectedTargetFile() {
        return downloadControlsPanel.getSelectedTargetFile();
    }

    void updateSelectedTargetPath() {
        downloadControlsPanel.updateTargetFileLabel();
        downloadControlsPanel.updateDownloadButtonState();
    }
}
