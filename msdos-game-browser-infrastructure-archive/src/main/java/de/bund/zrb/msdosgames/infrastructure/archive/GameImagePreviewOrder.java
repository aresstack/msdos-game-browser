package de.bund.zrb.msdosgames.infrastructure.archive;

import de.bund.zrb.msdosgames.domain.GameImage;

import java.util.Comparator;

final class GameImagePreviewOrder implements Comparator<GameImage> {

    @Override
    public int compare(GameImage left, GameImage right) {
        int leftScore = score(left.getTitle());
        int rightScore = score(right.getTitle());
        if (leftScore != rightScore) {
            return leftScore - rightScore;
        }
        return left.getTitle().compareToIgnoreCase(right.getTitle());
    }

    private int score(String title) {
        String lowerTitle = title.toLowerCase();
        if (lowerTitle.contains("thumb") || lowerTitle.contains("title")) {
            return 0;
        }
        if (lowerTitle.contains("screen") || lowerTitle.contains("screenshot")) {
            return 1;
        }
        return 5;
    }
}
