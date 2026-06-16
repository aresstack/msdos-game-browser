package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.domain.GameDetails;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.GridLayout;

final class GameMetadataPanel extends JPanel {

    private final JLabel titleLabel = new JLabel("Kein Spiel ausgewählt");
    private final JLabel identifierLabel = new JLabel(" ");
    private final JLabel sizeLabel = new JLabel(" ");

    GameMetadataPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Spiel"));
        titleLabel.setFont(titleLabel.getFont().deriveFont(15.0f));

        JPanel factsPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        factsPanel.add(identifierLabel);
        factsPanel.add(sizeLabel);

        add(titleLabel, BorderLayout.NORTH);
        add(factsPanel, BorderLayout.CENTER);
    }

    void clear() {
        titleLabel.setText("Kein Spiel ausgewählt");
        identifierLabel.setText(" ");
        sizeLabel.setText(" ");
    }

    void showDetails(GameDetails details) {
        titleLabel.setText(details.getTitle());
        identifierLabel.setText("Identifier: " + details.getIdentifier().getValue());
        sizeLabel.setText("Item-Größe: " + formatSize(details.getItemSize()));
    }

    private String formatSize(long bytes) {
        if (bytes <= 0L) {
            return "unbekannt";
        }
        long kiloBytes = bytes / 1024L;
        if (kiloBytes < 1024L) {
            return kiloBytes + " KB";
        }
        return kiloBytes / 1024L + " MB";
    }
}
