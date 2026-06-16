package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameImage;

import javax.imageio.ImageIO;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import java.awt.BorderLayout;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;

final class GameImagePreviewPanel extends JPanel {

    private static final int PREVIEW_WIDTH = 260;
    private static final int PREVIEW_HEIGHT = 180;

    private final JLabel imageLabel = new JLabel("Keine Vorschau verfügbar", SwingConstants.CENTER);
    private final JLabel imageTitleLabel = new JLabel(" ");

    GameImagePreviewPanel() {
        super(new BorderLayout(4, 4));
        setBorder(BorderFactory.createTitledBorder("Vorschau"));
        imageLabel.setHorizontalAlignment(SwingConstants.CENTER);
        imageLabel.setVerticalAlignment(SwingConstants.CENTER);
        add(imageLabel, BorderLayout.CENTER);
        add(imageTitleLabel, BorderLayout.SOUTH);
    }

    void clear() {
        imageLabel.setIcon(null);
        imageLabel.setText("Keine Vorschau verfügbar");
        imageTitleLabel.setText(" ");
    }

    void showDetails(GameDetails details) {
        List<GameImage> images = details.getPreviewImages();
        if (images.isEmpty()) {
            clear();
            return;
        }

        GameImage firstImage = images.get(0);
        imageLabel.setIcon(null);
        imageLabel.setText("Lade Vorschau ...");
        imageTitleLabel.setText(firstImage.getTitle());
        loadImage(firstImage);
    }

    private void loadImage(final GameImage gameImage) {
        new SwingWorker<ImageIcon, Void>() {
            @Override
            protected ImageIcon doInBackground() throws Exception {
                BufferedImage image = ImageIO.read(new URL(gameImage.getUrl()));
                if (image == null) {
                    return null;
                }
                Image scaledImage = image.getScaledInstance(PREVIEW_WIDTH, PREVIEW_HEIGHT, Image.SCALE_SMOOTH);
                return new ImageIcon(scaledImage);
            }

            @Override
            protected void done() {
                try {
                    ImageIcon icon = get();
                    if (icon == null) {
                        imageLabel.setIcon(null);
                        imageLabel.setText("Vorschau konnte nicht gelesen werden");
                        return;
                    }
                    imageLabel.setText("");
                    imageLabel.setIcon(icon);
                } catch (Exception exception) {
                    imageLabel.setIcon(null);
                    imageLabel.setText("Vorschau konnte nicht geladen werden");
                }
            }
        }.execute();
    }
}
