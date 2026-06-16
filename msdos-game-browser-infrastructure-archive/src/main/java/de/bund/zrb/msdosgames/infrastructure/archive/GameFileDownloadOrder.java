package de.bund.zrb.msdosgames.infrastructure.archive;

import de.bund.zrb.msdosgames.domain.GameFile;

import java.util.Comparator;

final class GameFileDownloadOrder implements Comparator<GameFile> {

    @Override
    public int compare(GameFile left, GameFile right) {
        int leftScore = score(left);
        int rightScore = score(right);
        if (leftScore != rightScore) {
            return leftScore - rightScore;
        }
        return left.getName().compareToIgnoreCase(right.getName());
    }

    private int score(GameFile file) {
        String name = file.getName().toLowerCase();
        String format = file.getFormat().toLowerCase();
        if (name.endsWith(".zip") || format.contains("zip")) {
            return 0;
        }
        if (name.endsWith(".7z") || name.endsWith(".rar")) {
            return 1;
        }
        if (name.endsWith(".exe") || name.endsWith(".com")) {
            return 2;
        }
        return 10;
    }
}
