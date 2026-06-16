package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.application.port.GameBrowserBackendService;
import de.bund.zrb.msdosgames.application.usecase.AcceptArchiveNoticeUseCase;
import de.bund.zrb.msdosgames.application.usecase.DownloadGameUseCase;
import de.bund.zrb.msdosgames.application.usecase.FavoriteGamesUseCase;
import de.bund.zrb.msdosgames.domain.DownloadProgress;
import de.bund.zrb.msdosgames.domain.FavoriteGame;
import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameFile;
import de.bund.zrb.msdosgames.domain.GameImage;
import de.bund.zrb.msdosgames.domain.GamePage;
import de.bund.zrb.msdosgames.domain.GameSearchCriteria;
import de.bund.zrb.msdosgames.domain.GameSummary;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public final class GameBrowserFrame extends JFrame {

    private static final int PAGE_SIZE = 100;

    private final GameBrowserBackendService backendService;
    private final AcceptArchiveNoticeUseCase acceptArchiveNoticeUseCase;
    private final DownloadGameUseCase downloadGameUseCase;
    private final FavoriteGamesUseCase favoriteGamesUseCase;
    private final File applicationDirectory;
    private final File downloadDirectory;
    private final DownloadDirectoryOpener downloadDirectoryOpener = new DownloadDirectoryOpener();

    private final JTextField searchField = new JTextField(30);
    private final JButton searchButton = new JButton("Suchen");
    private final JButton browseButton = new JButton("Alle anzeigen");
    private final JButton nextPageButton = new JButton("Weitere laden");
    private final JButton favoriteButton = new JButton("☆ Zu Favoriten");
    private final JButton downloadButton = new JButton("Download");
    private final JButton downloadOptionsButton = new JButton("▾");
    private final JButton openDownloadFolderButton = new JButton("Download-Ordner öffnen");
    private final GameTableModel gameTableModel = new GameTableModel();
    private final GameTableModel favoriteTableModel = new GameTableModel();
    private final JTable gameTable = new JTable(gameTableModel);
    private final JTable favoriteTable = new JTable(favoriteTableModel);
    private final GameDetailsView gameDetailsView;
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel("Bereit");

    private String activeSearchQuery;
    private String nextCursor;
    private GameSummary currentSummary;
    private GameDetails currentDetails;

    public GameBrowserFrame(
            GameBrowserBackendService backendService,
            AcceptArchiveNoticeUseCase acceptArchiveNoticeUseCase,
            DownloadGameUseCase downloadGameUseCase,
            FavoriteGamesUseCase favoriteGamesUseCase,
            File applicationDirectory,
            File downloadDirectory) {
        super("MS-DOS Game Browser");
        this.backendService = backendService;
        this.acceptArchiveNoticeUseCase = acceptArchiveNoticeUseCase;
        this.downloadGameUseCase = downloadGameUseCase;
        this.favoriteGamesUseCase = favoriteGamesUseCase;
        this.gameDetailsView = new GameDetailsView(new GameImagePreviewPanel.PreviewImageLoader() {
            @Override
            public byte[] loadImage(GameImage image) throws Exception {
                return backendService.loadPreviewImageNow(image);
            }
        });
        this.applicationDirectory = applicationDirectory;
        this.downloadDirectory = downloadDirectory;
        configureFrame();
        bindActions();
    }

    public void showBrowser() {
        setVisible(true);
        loadFavorites();
        loadFirstBrowsePage();
    }

    private void configureFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setJMenuBar(createMenuBar());
        setLayout(new BorderLayout(8, 8));
        add(createSearchPanel(), BorderLayout.NORTH);
        add(createContentPane(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);
        gameDetailsView.clear(downloadDirectory);
        getRootPane().setDefaultButton(searchButton);
        updateFavoriteButtonState();
        updateDownloadButtonState();
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                backendService.shutdown();
            }
        });
        setSize(1250, 800);
        setLocationRelativeTo(null);
    }

    private JMenuBar createMenuBar() {
        JMenuBar menuBar = new JMenuBar();
        JMenu fileMenu = new JMenu("Datei");

        JMenuItem openSettingsFolderItem = new JMenuItem("Einstellungsordner öffnen");
        openSettingsFolderItem.addActionListener(event -> openSettingsFolder());

        JMenuItem clearDatabaseItem = new JMenuItem("Datenbank-Cache löschen");
        clearDatabaseItem.addActionListener(event -> clearDatabaseCache());

        JMenuItem exitItem = new JMenuItem("Beenden");
        exitItem.addActionListener(event -> dispose());

        fileMenu.add(openSettingsFolderItem);
        fileMenu.add(clearDatabaseItem);
        fileMenu.addSeparator();
        fileMenu.add(exitItem);
        menuBar.add(fileMenu);
        return menuBar;
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 0));
        JPanel searchPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        JPanel actionPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        searchPanel.add(new JLabel("Spiel:"));
        searchPanel.add(searchField);
        searchPanel.add(searchButton);
        searchPanel.add(browseButton);
        searchPanel.add(nextPageButton);
        actionPanel.add(favoriteButton);
        actionPanel.add(downloadButton);
        actionPanel.add(downloadOptionsButton);
        nextPageButton.setEnabled(false);
        panel.add(searchPanel, BorderLayout.WEST);
        panel.add(actionPanel, BorderLayout.EAST);
        return panel;
    }

    private JSplitPane createContentPane() {
        JScrollPane tableScrollPane = new JScrollPane(gameTable);
        tableScrollPane.getViewport().addChangeListener(event -> preloadVisibleGames());

        JScrollPane favoriteScrollPane = new JScrollPane(favoriteTable);
        JTabbedPane leftTabs = new JTabbedPane();
        leftTabs.addTab("Spiele", tableScrollPane);
        leftTabs.addTab("Favoriten", favoriteScrollPane);

        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, leftTabs, gameDetailsView);
        splitPane.setResizeWeight(0.40d);
        return splitPane;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
        progressBar.setStringPainted(true);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        buttonPanel.add(openDownloadFolderButton);

        JPanel actionPanel = new JPanel(new BorderLayout(6, 0));
        actionPanel.add(buttonPanel, BorderLayout.WEST);
        actionPanel.add(progressBar, BorderLayout.EAST);

        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.EAST);
        return panel;
    }

    private void bindActions() {
        searchField.addActionListener(event -> searchFirstPage());
        searchButton.addActionListener(event -> searchFirstPage());
        browseButton.addActionListener(event -> loadFirstBrowsePage());
        nextPageButton.addActionListener(event -> loadNextPage());
        downloadButton.addActionListener(event -> downloadSelectedFile());
        downloadOptionsButton.addActionListener(event -> showDownloadOptionsMenu());
        favoriteButton.addActionListener(event -> toggleCurrentFavorite());
        openDownloadFolderButton.addActionListener(event -> openCurrentDownloadDirectory());
        installFavoriteContextMenu();
        gameTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    loadSelectedGameDetails();
                }
            }
        });
        favoriteTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    loadSelectedFavoriteDetails();
                }
            }
        });
    }

    private void showDownloadOptionsMenu() {
        List<GameFile> files = gameDetailsView.getDownloadableFiles();
        if (files.isEmpty()) {
            return;
        }

        JPopupMenu menu = new JPopupMenu();
        GameFile selectedFile = gameDetailsView.getSelectedFile();
        for (final GameFile file : files) {
            String title = file == selectedFile ? "✓ " + file.toString() : file.toString();
            JMenuItem item = new JMenuItem(title);
            item.addActionListener(event -> selectDownloadFile(file));
            menu.add(item);
        }
        menu.show(downloadOptionsButton, 0, downloadOptionsButton.getHeight());
    }

    private void selectDownloadFile(GameFile file) {
        gameDetailsView.selectDownloadFile(file);
        updateSelectedTargetPath();
        if (file != null) {
            statusLabel.setText("Download-Datei ausgewählt: " + file.getName());
        }
    }

    private void installFavoriteContextMenu() {
        favoriteTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent event) {
                maybeShowFavoriteContextMenu(event);
            }

            @Override
            public void mouseReleased(MouseEvent event) {
                maybeShowFavoriteContextMenu(event);
            }
        });
    }

    private void maybeShowFavoriteContextMenu(MouseEvent event) {
        if (!event.isPopupTrigger()) {
            return;
        }

        int row = favoriteTable.rowAtPoint(event.getPoint());
        if (row < 0) {
            return;
        }
        favoriteTable.setRowSelectionInterval(row, row);

        JPopupMenu menu = new JPopupMenu();
        JMenuItem removeItem = new JMenuItem("Favorit entfernen");
        removeItem.addActionListener(actionEvent -> removeSelectedFavorite());
        menu.add(removeItem);
        menu.show(favoriteTable, event.getX(), event.getY());
    }

    private void removeSelectedFavorite() {
        int selectedRow = favoriteTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        int modelRow = favoriteTable.convertRowIndexToModel(selectedRow);
        GameSummary favoriteSummary = favoriteTableModel.getGameAt(modelRow);
        try {
            favoriteGamesUseCase.removeFavorite(favoriteSummary.getIdentifier());
            statusLabel.setText("Favorit entfernt: " + favoriteSummary.getTitle());
            loadFavorites();
            updateFavoriteButtonState();
        } catch (Exception exception) {
            showError("Favorit konnte nicht entfernt werden.", exception);
        }
    }

    private void loadFirstBrowsePage() {
        activeSearchQuery = null;
        loadPage(false, new PageLoader() {
            @Override
            public GamePage load() throws Exception {
                return backendService.browse(GameSearchCriteria.browseFirstPage(PAGE_SIZE));
            }
        });
    }

    private void searchFirstPage() {
        String query = searchField.getText().trim();
        if (query.length() == 0) {
            loadFirstBrowsePage();
            return;
        }

        activeSearchQuery = query;
        loadPage(false, new PageLoader() {
            @Override
            public GamePage load() throws Exception {
                return backendService.search(GameSearchCriteria.searchFirstPage(activeSearchQuery, PAGE_SIZE));
            }
        });
    }

    private void loadNextPage() {
        if (nextCursor == null || nextCursor.length() == 0) {
            return;
        }

        loadPage(true, new PageLoader() {
            @Override
            public GamePage load() throws Exception {
                if (activeSearchQuery == null) {
                    return backendService.browse(GameSearchCriteria.browseNextPage(nextCursor, PAGE_SIZE));
                }
                return backendService.search(GameSearchCriteria.searchNextPage(activeSearchQuery, nextCursor, PAGE_SIZE));
            }
        });
    }

    private void loadPage(final boolean append, final PageLoader pageLoader) {
        setBusy("Lade Spiele ...");
        new SwingWorker<GamePage, Void>() {
            @Override
            protected GamePage doInBackground() throws Exception {
                return pageLoader.load();
            }

            @Override
            protected void done() {
                try {
                    GamePage page = get();
                    showPage(page, append);
                } catch (Exception exception) {
                    showError("Spiele konnten nicht geladen werden.", exception);
                } finally {
                    setReady();
                }
            }
        }.execute();
    }

    private void showPage(GamePage page, boolean append) {
        List<GameSummary> games = page.getGames();
        if (append) {
            gameTableModel.appendGames(games);
        } else {
            gameTableModel.replaceGames(games);
            clearDetails();
        }
        nextCursor = page.getNextCursor();
        nextPageButton.setEnabled(page.hasNextPage());
        statusLabel.setText(games.size() + " Spiele geladen" + (page.hasNextPage() ? " · weitere verfügbar" : ""));
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                preloadVisibleGames();
            }
        });
    }

    private void preloadVisibleGames() {
        if (gameTableModel.getRowCount() == 0) {
            return;
        }

        Rectangle visibleRect = gameTable.getVisibleRect();
        int firstViewRow = gameTable.rowAtPoint(visibleRect.getLocation());
        if (firstViewRow < 0) {
            firstViewRow = 0;
        }

        int visibleRows = Math.max(1, visibleRect.height / Math.max(1, gameTable.getRowHeight())) + 12;
        int lastViewRow = Math.min(gameTable.getRowCount() - 1, firstViewRow + visibleRows);
        int firstModelRow = gameTable.convertRowIndexToModel(firstViewRow);
        int lastModelRow = gameTable.convertRowIndexToModel(lastViewRow);
        backendService.preloadDetails(gameTableModel.getGames(), Math.min(firstModelRow, lastModelRow), Math.max(firstModelRow, lastModelRow));
    }

    private void loadSelectedGameDetails() {
        int selectedRow = gameTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        int modelRow = gameTable.convertRowIndexToModel(selectedRow);
        loadDetailsForSummary(gameTableModel.getGameAt(modelRow));
    }

    private void loadSelectedFavoriteDetails() {
        int selectedRow = favoriteTable.getSelectedRow();
        if (selectedRow < 0) {
            return;
        }

        int modelRow = favoriteTable.convertRowIndexToModel(selectedRow);
        loadDetailsForSummary(favoriteTableModel.getGameAt(modelRow));
    }

    private void loadDetailsForSummary(final GameSummary summary) {
        currentSummary = summary;
        setBusy("Lade Details für " + summary.getTitle() + " ...");
        new SwingWorker<GameDetails, Void>() {
            @Override
            protected GameDetails doInBackground() throws Exception {
                return backendService.loadDetailsNow(summary.getIdentifier());
            }

            @Override
            protected void done() {
                try {
                    showDetails(get());
                } catch (Exception exception) {
                    showError("Details konnten nicht geladen werden.", exception);
                } finally {
                    setReady();
                }
            }
        }.execute();
    }

    private void showDetails(GameDetails details) throws Exception {
        currentDetails = details;
        gameDetailsView.showDetails(details, downloadDirectory);
        updateFavoriteButtonState();
        updateDownloadButtonState();
    }

    private void clearDetails() {
        currentSummary = null;
        currentDetails = null;
        gameDetailsView.clear(downloadDirectory);
        updateFavoriteButtonState();
        updateDownloadButtonState();
    }

    private void loadFavorites() {
        new SwingWorker<List<FavoriteGame>, Void>() {
            @Override
            protected List<FavoriteGame> doInBackground() throws Exception {
                return favoriteGamesUseCase.listFavorites();
            }

            @Override
            protected void done() {
                try {
                    showFavorites(get());
                } catch (Exception exception) {
                    showError("Favoriten konnten nicht geladen werden.", exception);
                }
            }
        }.execute();
    }

    private void showFavorites(List<FavoriteGame> favorites) {
        List<GameSummary> summaries = new ArrayList<GameSummary>();
        for (FavoriteGame favorite : favorites) {
            summaries.add(favorite.toGameSummary());
        }
        favoriteTableModel.replaceGames(summaries);
    }

    private void toggleCurrentFavorite() {
        if (currentDetails == null) {
            return;
        }

        try {
            if (favoriteGamesUseCase.isFavorite(currentDetails.getIdentifier())) {
                favoriteGamesUseCase.removeFavorite(currentDetails.getIdentifier());
                statusLabel.setText("Favorit entfernt: " + currentDetails.getTitle());
            } else {
                favoriteGamesUseCase.addFavorite(currentSummary, currentDetails);
                statusLabel.setText("Favorit hinzugefügt: " + currentDetails.getTitle());
            }
            loadFavorites();
            updateFavoriteButtonState();
        } catch (Exception exception) {
            showError("Favorit konnte nicht gespeichert werden.", exception);
        }
    }

    private void updateFavoriteButtonState() {
        if (currentDetails == null) {
            favoriteButton.setEnabled(false);
            favoriteButton.setText("☆ Zu Favoriten");
            return;
        }

        favoriteButton.setEnabled(true);
        try {
            if (favoriteGamesUseCase.isFavorite(currentDetails.getIdentifier())) {
                favoriteButton.setText("★ Favorit entfernen");
            } else {
                favoriteButton.setText("☆ Zu Favoriten");
            }
        } catch (Exception exception) {
            favoriteButton.setText("☆ Zu Favoriten");
        }
    }

    private boolean ensureArchiveNoticeAcceptedForDownload() {
        try {
            if (acceptArchiveNoticeUseCase.isAccepted(currentDetails)) {
                return true;
            }
        } catch (Exception exception) {
            showError("Die gespeicherte Bestätigung konnte nicht gelesen werden.", exception);
            return false;
        }

        int result = JOptionPane.showConfirmDialog(
                this,
                createArchiveNoticePromptText(),
                "Archive.org-Hinweise bestätigen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            statusLabel.setText("Download abgebrochen: Archive.org-Hinweise nicht bestätigt");
            return false;
        }

        try {
            acceptArchiveNoticeUseCase.accept(currentDetails);
            statusLabel.setText("Archive.org-Hinweise bestätigt für " + currentDetails.getTitle());
            return true;
        } catch (Exception exception) {
            showError("Die Bestätigung konnte nicht gespeichert werden.", exception);
            return false;
        }
    }

    private String createArchiveNoticePromptText() {
        StringBuilder text = new StringBuilder();
        text.append("Bitte bestätige vor dem Download die Archive.org-Hinweise.\n\n");
        text.append("Spiel: ").append(currentDetails.getTitle()).append('\n');
        text.append("Verfügbarkeit: ").append(currentDetails.getArchiveItemNotice().getAvailabilityText()).append('\n');
        text.append("Quelle: ").append(currentDetails.getArchiveItemNotice().getSourceUrl()).append("\n\n");
        text.append(currentDetails.getArchiveItemNotice().getAccessText()).append("\n\n");
        text.append("Fortfahren und Download starten?");
        return text.toString();
    }

    private void downloadSelectedFile() {
        if (currentDetails == null) {
            return;
        }

        final GameFile selectedFile = gameDetailsView.getSelectedFile();
        if (selectedFile == null) {
            return;
        }

        if (!ensureArchiveNoticeAcceptedForDownload()) {
            return;
        }

        final File targetFile = gameDetailsView.getSelectedTargetFile();
        setBusy("Lade " + selectedFile.getName() + " nach " + targetFile.getParentFile().getAbsolutePath() + " ...");
        new SwingWorker<Void, DownloadProgress>() {
            @Override
            protected Void doInBackground() throws Exception {
                downloadGameUseCase.downloadAcceptedGame(
                        currentDetails.getIdentifier(),
                        currentDetails.getArchiveItemNotice(),
                        selectedFile,
                        downloadDirectory,
                        progress -> publish(progress));
                return null;
            }

            @Override
            protected void process(List<DownloadProgress> chunks) {
                if (!chunks.isEmpty()) {
                    showProgress(chunks.get(chunks.size() - 1));
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Download abgeschlossen: " + targetFile.getAbsolutePath());
                } catch (Exception exception) {
                    showError("Download fehlgeschlagen.", exception);
                } finally {
                    setReady();
                }
            }
        }.execute();
    }

    private void openSettingsFolder() {
        try {
            downloadDirectoryOpener.openDirectory(applicationDirectory);
            statusLabel.setText("Einstellungsordner geöffnet: " + applicationDirectory.getAbsolutePath());
        } catch (Exception exception) {
            showError("Einstellungsordner konnte nicht geöffnet werden: " + applicationDirectory.getAbsolutePath(), exception);
        }
    }

    private void clearDatabaseCache() {
        int result = JOptionPane.showConfirmDialog(
                this,
                "Soll der Datenbank-Cache wirklich gelöscht werden?\nGeladene Details und Bildvorschauen werden danach neu geladen.",
                "Datenbank-Cache löschen",
                JOptionPane.YES_NO_OPTION,
                JOptionPane.WARNING_MESSAGE);
        if (result != JOptionPane.YES_OPTION) {
            return;
        }

        setBusy("Lösche Datenbank-Cache ...");
        new SwingWorker<Void, Void>() {
            @Override
            protected Void doInBackground() throws Exception {
                backendService.clearDatabaseCache();
                return null;
            }

            @Override
            protected void done() {
                try {
                    get();
                    statusLabel.setText("Datenbank-Cache gelöscht");
                } catch (Exception exception) {
                    showError("Datenbank-Cache konnte nicht gelöscht werden.", exception);
                } finally {
                    setReady();
                }
            }
        }.execute();
    }

    private void openCurrentDownloadDirectory() {
        File directory = gameDetailsView.getCurrentDirectory();
        try {
            downloadDirectoryOpener.openDirectory(directory);
            statusLabel.setText("Download-Ordner geöffnet: " + directory.getAbsolutePath());
        } catch (Exception exception) {
            showError("Download-Ordner konnte nicht geöffnet werden: " + directory.getAbsolutePath(), exception);
        }
    }

    private void updateSelectedTargetPath() {
        gameDetailsView.updateSelectedTargetPath();
        updateDownloadButtonState();
    }

    private void showProgress(DownloadProgress progress) {
        progressBar.setValue(progress.getPercent());
        statusLabel.setText("Download " + progress.getFileName() + ": " + progress.getPercent() + "%");
    }

    private void setBusy(String message) {
        statusLabel.setText(message);
        progressBar.setIndeterminate(true);
        searchButton.setEnabled(false);
        browseButton.setEnabled(false);
        favoriteButton.setEnabled(false);
        downloadButton.setEnabled(false);
        downloadOptionsButton.setEnabled(false);
    }

    private void setReady() {
        progressBar.setIndeterminate(false);
        searchButton.setEnabled(true);
        browseButton.setEnabled(true);
        gameDetailsView.updateSelectedTargetPath();
        updateFavoriteButtonState();
        updateDownloadButtonState();
    }

    private void updateDownloadButtonState() {
        boolean hasDownload = currentDetails != null && gameDetailsView.getSelectedFile() != null;
        downloadButton.setEnabled(hasDownload);
        downloadOptionsButton.setEnabled(hasDownload && gameDetailsView.getDownloadableFiles().size() > 1);
    }

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText(message);
    }

    private interface PageLoader {
        GamePage load() throws Exception;
    }
}
