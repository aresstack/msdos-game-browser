package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.domain.GameDetails;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;

final class GameDescriptionPanel extends JPanel {

    private final JTextArea descriptionArea = new JTextArea();

    GameDescriptionPanel() {
        super(new BorderLayout());
        setBorder(BorderFactory.createTitledBorder("Beschreibung"));
        descriptionArea.setEditable(false);
        descriptionArea.setLineWrap(true);
        descriptionArea.setWrapStyleWord(true);
        descriptionArea.setRows(7);
        add(new JScrollPane(descriptionArea), BorderLayout.CENTER);
        clear();
    }

    void clear() {
        descriptionArea.setText("Wähle links ein Spiel aus.");
        descriptionArea.setCaretPosition(0);
    }

    void showDetails(GameDetails details) {
        descriptionArea.setText(details.getDescriptionText());
        descriptionArea.setCaretPosition(0);
    }
}
