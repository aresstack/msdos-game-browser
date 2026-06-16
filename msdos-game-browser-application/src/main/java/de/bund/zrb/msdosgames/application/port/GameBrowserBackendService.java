package de.bund.zrb.msdosgames.application.port;

import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameIdentifier;
import de.bund.zrb.msdosgames.domain.GameImage;
import de.bund.zrb.msdosgames.domain.GamePage;
import de.bund.zrb.msdosgames.domain.GameSearchCriteria;
import de.bund.zrb.msdosgames.domain.GameSummary;

import java.io.IOException;
import java.util.List;

public interface GameBrowserBackendService {

    GamePage browse(GameSearchCriteria criteria) throws IOException;

    GamePage search(GameSearchCriteria criteria) throws IOException;

    GameDetails loadDetailsNow(GameIdentifier identifier) throws Exception;

    void preloadDetails(List<GameSummary> games, int firstIndex, int lastIndex);

    byte[] loadPreviewImageNow(GameImage image) throws Exception;

    void cancelLowPriorityImagePreloading();

    void shutdown();
}
