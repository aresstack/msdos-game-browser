package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.LicenseNotice;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.GridLayout;

final class LicenseNoticePanel extends JPanel {

    private final JTextArea rightsArea = new JTextArea();
    private final JLabel licenseLabel = new JLabel("Lizenz: -");
    private final JLabel sourceLabel = new JLabel("Quelle: -");

    LicenseNoticePanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Lizenz und Rechte"));
        rightsArea.setEditable(false);
        rightsArea.setLineWrap(true);
        rightsArea.setWrapStyleWord(true);
        rightsArea.setRows(4);

        JPanel factsPanel = new JPanel(new GridLayout(2, 1, 2, 2));
        factsPanel.add(licenseLabel);
        factsPanel.add(sourceLabel);

        add(factsPanel, BorderLayout.NORTH);
        add(new JScrollPane(rightsArea), BorderLayout.CENTER);
        clear();
    }

    void clear() {
        licenseLabel.setText("Lizenz: -");
        sourceLabel.setText("Quelle: -");
        rightsArea.setText("Wähle links ein Spiel aus. Danach werden hier die Rechtehinweise des Archive.org-Items angezeigt.");
        rightsArea.setCaretPosition(0);
    }

    void showDetails(GameDetails details) {
        LicenseNotice notice = details.getLicenseNotice();
        licenseLabel.setText("Lizenz: " + textOrFallback(notice.getLicenseUrl(), "keine Angabe im Metadatenfeld"));
        sourceLabel.setText("Quelle: " + textOrFallback(notice.getSourceUrl(), "keine Angabe"));
        if (notice.hasLicenseInformation()) {
            rightsArea.setText(textOrFallback(notice.getRights(), "Keine gesonderten Rechtehinweise im Metadatenfeld."));
        } else {
            rightsArea.setText("Archive.org liefert für dieses Item keine ausdrückliche Lizenzangabe im Metadatenfeld. Prüfe die verlinkte Detailseite selbst. Der Download bedeutet nicht automatisch, dass jede Nutzung erlaubt ist.");
        }
        rightsArea.setCaretPosition(0);
    }

    private String textOrFallback(String value, String fallback) {
        if (value == null || value.trim().length() == 0) {
            return fallback;
        }
        return value.trim();
    }
}
