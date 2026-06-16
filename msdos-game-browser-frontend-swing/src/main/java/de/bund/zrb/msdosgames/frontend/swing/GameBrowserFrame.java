package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.application.usecase.AcceptLicenseUseCase;
import de.bund.zrb.msdosgames.application.port.GameBrowserBackendService;
import de.bund.zrb.msdosgames.application.usecase.DownloadGameUseCase;
import de.bund.zrb.msdosgames.domain.DownloadProgress;
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
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Rectangle;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.File;
import java.util.List;

public final class GameBrowserFrame extends JFrame {

    private static final int PAGE_SIZE = 100;

    private final GameBrowserBackendService backendService;
    private final AcceptLicenseUseCase acceptLicenseUseCase;
    private final DownloadGameUseCase downloadGameUseCase;
    private final File downloadDirectory;
    private final DownloadDirectoryOpener downloadDirectoryOpener = new DownloadDirectoryOpener();

    private final JTextField searchField = new JTextField(30);
    private final JButton searchButton = new JButton("Suchen");
    private final JButton browseButton = new JButton("Alle anzeigen");
    private final JButton nextPageButton = new JButton("Weitere laden");
    private final JButton openDownloadFolderButton = new JButton("Download-Ordner öffnen");
    private final GameTableModel gameTableModel = new GameTableModel();
    private final JTable gameTable = new JTable(gameTableModel);
    private final GameDetailsView gameDetailsView;
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel("Bereit");

    private String activeSearchQuery;
    private String nextCursor;
    private GameDetails currentDetails;

    public GameBrowserFrame(
            GameBrowserBackendService backendService,
            AcceptLicenseUseCase acceptLicenseUseCase,
            DownloadGameUseCase downloadGameUseCase,
            File downloadDirectory) {
        super("MS-DOS Game Browser");
        this.backendService = backendService;
        this.acceptLicenseUseCase = acceptLicenseUseCase;
        this.gameDetailsView = new GameDetailsView(new GameImagePreviewPanel.PreviewImageLoader() {
            @Override
            public byte[] loadImage(GameImage image) throws Exception {
                return backendService.loadPreviewImageNow(image);
            }
        });
        this.downloadGameUseCase = downloadGameUseCase;
        this.downloadDirectory = downloadDirectory;
        configureFrame();
        bindActions();
    }

    public void showBrowser() {
        setVisible(true);
        loadFirstBrowsePage();
    }

    private void configureFrame() {
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setLayout(new BorderLayout(8, 8));
        add(createSearchPanel(), BorderLayout.NORTH);
        add(createContentPane(), BorderLayout.CENTER);
        add(createStatusPanel(), BorderLayout.SOUTH);
        gameDetailsView.clear(downloadDirectory);
        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent event) {
                backendService.shutdown();
            }
        });
        setSize(1250, 800);
        setLocationRelativeTo(null);
    }

    private JPanel createSearchPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 6, 0, 6));
        panel.add(new JLabel("Spiel:"));
        panel.add(searchField);
        panel.add(searchButton);
        panel.add(browseButton);
        panel.add(nextPageButton);
        nextPageButton.setEnabled(false);
        return panel;
    }

    private JSplitPane createContentPane() {
        JScrollPane tableScrollPane = new JScrollPane(gameTable);
        tableScrollPane.getViewport().addChangeListener(event -> preloadVisibleGames());
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, gameDetailsView);
        splitPane.setResizeWeight(0.40d);
        return splitPane;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
        progressBar.setStringPainted(true);

        JPanel actionPanel = new JPanel(new BorderLayout(6, 0));
        actionPanel.add(openDownloadFolderButton, BorderLayout.WEST);
        actionPanel.add(progressBar, BorderLayout.EAST);

        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(actionPanel, BorderLayout.EAST);
        return panel;
    }

    private void bindActions() {
        searchButton.addActionListener(event -> searchFirstPage());
        browseButton.addActionListener(event -> loadFirstBrowsePage());
        nextPageButton.addActionListener(event -> loadNextPage());
        openDownloadFolderButton.addActionListener(event -> openCurrentDownloadDirectory());
        gameDetailsView.bindActions(
                () -> acceptCurrentLicenseWhenSelected(),
                () -> downloadSelectedFile(),
                () -> updateSelectedTargetPath());
        gameTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent event) {
                if (!event.getValueIsAdjusting()) {
                    loadSelectedGameDetails();
                }
            }
        });
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
        final GameSummary summary = gameTableModel.getGameAt(modelRow);
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
        gameDetailsView.showDetails(details, acceptLicenseUseCase.isAccepted(details), downloadDirectory);
    }

    private void clearDetails() {
        currentDetails = null;
        gameDetailsView.clear(downloadDirectory);
    }

    private void acceptCurrentLicenseWhenSelected() {
        if (currentDetails == null || !gameDetailsView.isNoticeAccepted()) {
            gameDetailsView.updateSelectedTargetPath();
            return;
        }

        try {
            acceptLicenseUseCase.accept(currentDetails);
            gameDetailsView.setNoticeAccepted(true);
            statusLabel.setText("Rechtehinweis akzeptiert für " + currentDetails.getTitle());
        } catch (Exception exception) {
            gameDetailsView.setNoticeAccepted(false);
            showError("Die Bestätigung konnte nicht gespeichert werden.", exception);
        }
    }

    private void downloadSelectedFile() {
        if (currentDetails == null) {
            return;
        }

        final GameFile selectedFile = gameDetailsView.getSelectedFile();
        if (selectedFile == null) {
            return;
        }

        final File targetFile = gameDetailsView.getSelectedTargetFile();
        setBusy("Lade " + selectedFile.getName() + " nach " + targetFile.getParentFile().getAbsolutePath() + " ...");
        new SwingWorker<Void, DownloadProgress>() {
            @Override
            protected Void doInBackground() throws Exception {
                downloadGameUseCase.downloadAcceptedGame(
                        currentDetails.getIdentifier(),
                        currentDetails.getLicenseNotice(),
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
    }

    private void setReady() {
        progressBar.setIndeterminate(false);
        searchButton.setEnabled(true);
        browseButton.setEnabled(true);
        gameDetailsView.updateSelectedTargetPath();
    }

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText(message);
    }

    private interface PageLoader {
        GamePage load() throws Exception;
    }
}
