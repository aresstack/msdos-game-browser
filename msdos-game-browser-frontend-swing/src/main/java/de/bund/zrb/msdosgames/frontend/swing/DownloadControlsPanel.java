package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameFile;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

final class DownloadControlsPanel extends JPanel {

    private final JLabel selectedFileLabel = new JLabel("Datei: -");
    private final JLabel targetFileLabel = new JLabel("Ziel: -");

    private final List<GameFile> downloadableFiles = new ArrayList<GameFile>();
    private File baseDirectory;
    private GameDetails currentDetails;
    private GameFile selectedFile;

    DownloadControlsPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Download-Datei"));
        add(selectedFileLabel, BorderLayout.NORTH);
        add(targetFileLabel, BorderLayout.CENTER);
    }

    void clear(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        currentDetails = null;
        selectedFile = null;
        downloadableFiles.clear();
        selectedFileLabel.setText("Datei: -");
        targetFileLabel.setText("Ziel: " + baseDirectory.getAbsolutePath());
    }

    void showDetails(GameDetails details, File baseDirectory) {
        this.baseDirectory = baseDirectory;
        currentDetails = details;
        downloadableFiles.clear();
        downloadableFiles.addAll(details.getDownloadableFiles());
        selectedFile = downloadableFiles.isEmpty() ? null : downloadableFiles.get(0);
        updateLabels();
    }

    List<GameFile> getDownloadableFiles() {
        return Collections.unmodifiableList(downloadableFiles);
    }

    GameFile getSelectedFile() {
        return selectedFile;
    }

    void selectFile(GameFile file) {
        if (file == null) {
            selectedFile = null;
        } else if (downloadableFiles.contains(file)) {
            selectedFile = file;
        }
        updateLabels();
    }

    File getCurrentDirectory() {
        if (currentDetails == null) {
            return baseDirectory;
        }
        return new File(baseDirectory, sanitizeDirectoryName(currentDetails.getIdentifier().getValue()));
    }

    File getSelectedTargetFile() {
        if (selectedFile == null) {
            return getCurrentDirectory();
        }
        return new File(getCurrentDirectory(), sanitizeRelativePath(selectedFile.getName()));
    }

    void updateTargetFileLabel() {
        updateLabels();
    }

    private void updateLabels() {
        if (selectedFile == null) {
            selectedFileLabel.setText("Datei: -");
        } else {
            selectedFileLabel.setText("Datei: " + selectedFile.getName());
        }
        targetFileLabel.setText("Ziel: " + getSelectedTargetFile().getAbsolutePath());
    }

    private String sanitizeDirectoryName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeRelativePath(String value) {
        return value.replace('\\', '/').replace("..", "_");
    }
}
