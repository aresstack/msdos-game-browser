package de.bund.zrb.msdosgames.backend;

import de.bund.zrb.msdosgames.application.port.GameBrowserBackendService;
import de.bund.zrb.msdosgames.application.port.GameCatalog;
import de.bund.zrb.msdosgames.application.port.GameDetailsProvider;
import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameIdentifier;
import de.bund.zrb.msdosgames.domain.GameImage;
import de.bund.zrb.msdosgames.domain.GamePage;
import de.bund.zrb.msdosgames.domain.GameSearchCriteria;
import de.bund.zrb.msdosgames.domain.GameSummary;

import java.io.IOException;
import java.sql.SQLException;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class LuceneH2GameBrowserBackendService implements GameBrowserBackendService {

    private static final int BACKGROUND_DETAIL_THREADS = 4;

    private final GameCatalog gameCatalog;
    private final GameDetailsProvider gameDetailsProvider;
    private final H2InMemoryGameDetailsStore store;
    private final LuceneGameDetailsIndex index;
    private final ExecutorService userExecutor;
    private final ThreadPoolExecutor detailExecutor;
    private final ExecutorService imageExecutor;
    private final Set<GameIdentifier> queuedDetailLoads = Collections.synchronizedSet(new HashSet<GameIdentifier>());
    private final AtomicInteger activeDetailLoads = new AtomicInteger();

    private volatile Future<?> lowPriorityImageFuture;

    public LuceneH2GameBrowserBackendService(GameCatalog gameCatalog, GameDetailsProvider gameDetailsProvider) {
        if (gameCatalog == null) {
            throw new IllegalArgumentException("gameCatalog must not be null");
        }
        if (gameDetailsProvider == null) {
            throw new IllegalArgumentException("gameDetailsProvider must not be null");
        }
        this.gameCatalog = gameCatalog;
        this.gameDetailsProvider = gameDetailsProvider;
        this.store = new H2InMemoryGameDetailsStore();
        this.index = new LuceneGameDetailsIndex();
        this.userExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("msdos-user-load"));
        this.detailExecutor = (ThreadPoolExecutor) Executors.newFixedThreadPool(BACKGROUND_DETAIL_THREADS, new NamedThreadFactory("msdos-preload-details"));
        this.imageExecutor = Executors.newSingleThreadExecutor(new NamedThreadFactory("msdos-preload-images"));
    }

    @Override
    public GamePage browse(GameSearchCriteria criteria) throws IOException {
        return gameCatalog.browse(criteria);
    }

    @Override
    public GamePage search(GameSearchCriteria criteria) throws IOException {
        return gameCatalog.search(criteria);
    }

    @Override
    public GameDetails loadDetailsNow(final GameIdentifier identifier) throws Exception {
        Future<GameDetails> future = userExecutor.submit(new Callable<GameDetails>() {
            @Override
            public GameDetails call() throws Exception {
                GameDetails details = findCachedDetails(identifier);
                if (details == null) {
                    details = gameDetailsProvider.loadDetails(identifier);
                    cacheDetails(details);
                }
                loadImagesNow(details);
                return details;
            }
        });
        return future.get();
    }

    @Override
    public void preloadDetails(List<GameSummary> games, int firstIndex, int lastIndex) {
        cancelLowPriorityImagePreloading();
        if (games == null || games.isEmpty()) {
            return;
        }

        int safeFirstIndex = Math.max(0, firstIndex);
        int safeLastIndex = Math.min(games.size() - 1, lastIndex);
        for (int index = safeFirstIndex; index <= safeLastIndex; index++) {
            queueBackgroundDetailsLoad(games.get(index).getIdentifier());
        }
    }

    @Override
    public byte[] loadPreviewImageNow(final GameImage image) throws Exception {
        Future<byte[]> future = userExecutor.submit(new Callable<byte[]>() {
            @Override
            public byte[] call() throws Exception {
                return store.loadImage(image.getUrl());
            }
        });
        return future.get();
    }

    @Override
    public void cancelLowPriorityImagePreloading() {
        Future<?> future = lowPriorityImageFuture;
        if (future != null && !future.isDone()) {
            future.cancel(true);
        }
    }

    @Override
    public void shutdown() {
        cancelLowPriorityImagePreloading();
        userExecutor.shutdownNow();
        detailExecutor.shutdownNow();
        imageExecutor.shutdownNow();
        store.close();
        index.close();
    }

    private void queueBackgroundDetailsLoad(final GameIdentifier identifier) {
        if (!queuedDetailLoads.add(identifier)) {
            return;
        }

        detailExecutor.submit(new Runnable() {
            @Override
            public void run() {
                activeDetailLoads.incrementAndGet();
                try {
                    GameDetails cachedDetails = findCachedDetails(identifier);
                    if (cachedDetails == null) {
                        GameDetails loadedDetails = gameDetailsProvider.loadDetails(identifier);
                        cacheDetails(loadedDetails);
                        scheduleLowPriorityImagePreload(loadedDetails);
                    } else {
                        scheduleLowPriorityImagePreload(cachedDetails);
                    }
                } catch (Exception ignored) {
                } finally {
                    activeDetailLoads.decrementAndGet();
                }
            }
        });
    }

    private GameDetails findCachedDetails(GameIdentifier identifier) throws SQLException {
        return store.findDetails(identifier);
    }

    private void cacheDetails(GameDetails details) throws SQLException, IOException {
        store.saveDetails(details);
        index.index(details);
    }

    private void loadImagesNow(GameDetails details) throws Exception {
        for (GameImage image : details.getPreviewImages()) {
            store.loadImage(image.getUrl());
        }
    }

    private void scheduleLowPriorityImagePreload(final GameDetails details) {
        if (activeDetailLoads.get() > 0 || detailExecutor.getQueue().size() > 0) {
            return;
        }

        lowPriorityImageFuture = imageExecutor.submit(new Runnable() {
            @Override
            public void run() {
                for (GameImage image : details.getPreviewImages()) {
                    if (Thread.currentThread().isInterrupted()) {
                        return;
                    }
                    try {
                        store.loadImage(image.getUrl());
                    } catch (Exception ignored) {
                    }
                }
            }
        });
    }
}
