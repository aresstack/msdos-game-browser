package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameFile;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;

final class DownloadControlsPanel extends JPanel {

    private final JComboBox<GameFile> fileComboBox = new JComboBox<GameFile>();
    private final JCheckBox noticeCheckBox = new JCheckBox("Ich habe die angezeigten Lizenz- und Rechtehinweise gelesen und akzeptiere sie.");
    private final JButton downloadButton = new JButton("Herunterladen");
    private final JLabel targetFileLabel = new JLabel("Ziel: -");

    private File baseDirectory;
    private GameDetails currentDetails;

    DownloadControlsPanel() {
        super(new BorderLayout(6, 6));
        setBorder(BorderFactory.createTitledBorder("Download"));
        fileComboBox.setRenderer(new GameFileComboBoxRenderer());

        JPanel centerPanel = new JPanel(new BorderLayout(4, 4));
        centerPanel.add(targetFileLabel, BorderLayout.NORTH);
        centerPanel.add(noticeCheckBox, BorderLayout.CENTER);

        add(fileComboBox, BorderLayout.NORTH);
        add(centerPanel, BorderLayout.CENTER);
        add(downloadButton, BorderLayout.SOUTH);
    }

    void bindActions(Runnable acceptAction, Runnable downloadAction, Runnable selectionChangedAction) {
        noticeCheckBox.addActionListener(event -> acceptAction.run());
        downloadButton.addActionListener(event -> downloadAction.run());
        fileComboBox.addActionListener(event -> selectionChangedAction.run());
    }

    void clear(File baseDirectory) {
        this.baseDirectory = baseDirectory;
        currentDetails = null;
        fileComboBox.setModel(new DefaultComboBoxModel<GameFile>());
        noticeCheckBox.setSelected(false);
        noticeCheckBox.setEnabled(false);
        targetFileLabel.setText("Ziel: " + baseDirectory.getAbsolutePath());
        updateDownloadButtonState();
    }

    void showDetails(GameDetails details, boolean accepted, File baseDirectory) {
        this.baseDirectory = baseDirectory;
        currentDetails = details;
        fileComboBox.setModel(new DefaultComboBoxModel<GameFile>(details.getDownloadableFiles().toArray(new GameFile[details.getDownloadableFiles().size()])));
        if (fileComboBox.getItemCount() > 0) {
            fileComboBox.setSelectedIndex(0);
        }
        noticeCheckBox.setEnabled(true);
        noticeCheckBox.setSelected(accepted);
        updateTargetFileLabel();
        updateDownloadButtonState();
    }

    GameFile getSelectedFile() {
        return (GameFile) fileComboBox.getSelectedItem();
    }

    boolean isNoticeAccepted() {
        return noticeCheckBox.isSelected();
    }

    void setNoticeAccepted(boolean accepted) {
        noticeCheckBox.setSelected(accepted);
        updateDownloadButtonState();
    }

    File getCurrentDirectory() {
        if (currentDetails == null) {
            return baseDirectory;
        }
        return new File(baseDirectory, sanitizeDirectoryName(currentDetails.getIdentifier().getValue()));
    }

    File getSelectedTargetFile() {
        GameFile selectedFile = getSelectedFile();
        if (selectedFile == null) {
            return getCurrentDirectory();
        }
        return new File(getCurrentDirectory(), sanitizeRelativePath(selectedFile.getName()));
    }

    void updateTargetFileLabel() {
        targetFileLabel.setText("Ziel: " + getSelectedTargetFile().getAbsolutePath());
    }

    void updateDownloadButtonState() {
        downloadButton.setEnabled(currentDetails != null && getSelectedFile() != null && noticeCheckBox.isSelected());
    }

    private String sanitizeDirectoryName(String value) {
        return value.replaceAll("[^a-zA-Z0-9._-]", "_");
    }

    private String sanitizeRelativePath(String value) {
        return value.replace('\\', '/').replace("..", "_");
    }
}
