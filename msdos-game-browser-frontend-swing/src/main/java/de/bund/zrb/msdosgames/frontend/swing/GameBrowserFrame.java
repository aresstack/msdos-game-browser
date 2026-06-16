package de.bund.zrb.msdosgames.frontend.swing;

import de.bund.zrb.msdosgames.application.usecase.AcceptLicenseUseCase;
import de.bund.zrb.msdosgames.application.usecase.BrowseGamesUseCase;
import de.bund.zrb.msdosgames.application.usecase.DownloadGameUseCase;
import de.bund.zrb.msdosgames.application.usecase.LoadGameDetailsUseCase;
import de.bund.zrb.msdosgames.application.usecase.SearchGamesUseCase;
import de.bund.zrb.msdosgames.domain.DownloadProgress;
import de.bund.zrb.msdosgames.domain.GameDetails;
import de.bund.zrb.msdosgames.domain.GameFile;
import de.bund.zrb.msdosgames.domain.GamePage;
import de.bund.zrb.msdosgames.domain.GameSummary;

import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.WindowConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.File;
import java.util.List;

public final class GameBrowserFrame extends JFrame {

    private static final int PAGE_SIZE = 100;

    private final BrowseGamesUseCase browseGamesUseCase;
    private final SearchGamesUseCase searchGamesUseCase;
    private final LoadGameDetailsUseCase loadGameDetailsUseCase;
    private final AcceptLicenseUseCase acceptLicenseUseCase;
    private final DownloadGameUseCase downloadGameUseCase;
    private final File downloadDirectory;

    private final JTextField searchField = new JTextField(30);
    private final JButton searchButton = new JButton("Suchen");
    private final JButton browseButton = new JButton("Alle anzeigen");
    private final JButton nextPageButton = new JButton("Weitere laden");
    private final GameTableModel gameTableModel = new GameTableModel();
    private final JTable gameTable = new JTable(gameTableModel);
    private final JLabel titleLabel = new JLabel("Kein Spiel ausgewählt");
    private final JEditorPane detailsPane = new JEditorPane();
    private final JComboBox<GameFile> fileComboBox = new JComboBox<GameFile>();
    private final JCheckBox licenseCheckBox = new JCheckBox("Ich habe die Lizenz- und Rechtehinweise gelesen und akzeptiere sie.");
    private final JButton downloadButton = new JButton("Herunterladen");
    private final JProgressBar progressBar = new JProgressBar(0, 100);
    private final JLabel statusLabel = new JLabel("Bereit");

    private String activeSearchQuery;
    private String nextCursor;
    private GameDetails currentDetails;

    public GameBrowserFrame(
            BrowseGamesUseCase browseGamesUseCase,
            SearchGamesUseCase searchGamesUseCase,
            LoadGameDetailsUseCase loadGameDetailsUseCase,
            AcceptLicenseUseCase acceptLicenseUseCase,
            DownloadGameUseCase downloadGameUseCase,
            File downloadDirectory) {
        super("MS-DOS Game Browser");
        this.browseGamesUseCase = browseGamesUseCase;
        this.searchGamesUseCase = searchGamesUseCase;
        this.loadGameDetailsUseCase = loadGameDetailsUseCase;
        this.acceptLicenseUseCase = acceptLicenseUseCase;
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
        setSize(1200, 760);
        setLocationRelativeTo(null);
        updateDownloadButtonState();
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
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, tableScrollPane, createDetailsPanel());
        splitPane.setResizeWeight(0.45d);
        return splitPane;
    }

    private JPanel createDetailsPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(6, 0, 6, 6));

        titleLabel.setBorder(BorderFactory.createEmptyBorder(0, 0, 4, 0));
        detailsPane.setEditable(false);
        detailsPane.setContentType("text/html");
        detailsPane.setText("<html><body><p>Wähle links ein Spiel aus.</p></body></html>");
        fileComboBox.setRenderer(new GameFileComboBoxRenderer());

        JPanel downloadPanel = new JPanel(new BorderLayout(6, 6));
        downloadPanel.add(fileComboBox, BorderLayout.NORTH);
        downloadPanel.add(licenseCheckBox, BorderLayout.CENTER);
        downloadPanel.add(downloadButton, BorderLayout.SOUTH);

        panel.add(titleLabel, BorderLayout.NORTH);
        panel.add(new JScrollPane(detailsPane), BorderLayout.CENTER);
        panel.add(downloadPanel, BorderLayout.SOUTH);
        return panel;
    }

    private JPanel createStatusPanel() {
        JPanel panel = new JPanel(new BorderLayout(6, 6));
        panel.setBorder(BorderFactory.createEmptyBorder(0, 6, 6, 6));
        progressBar.setStringPainted(true);
        panel.add(statusLabel, BorderLayout.CENTER);
        panel.add(progressBar, BorderLayout.EAST);
        return panel;
    }

    private void bindActions() {
        searchButton.addActionListener(event -> searchFirstPage());
        browseButton.addActionListener(event -> loadFirstBrowsePage());
        nextPageButton.addActionListener(event -> loadNextPage());
        licenseCheckBox.addActionListener(event -> acceptCurrentLicenseWhenSelected());
        downloadButton.addActionListener(event -> downloadSelectedFile());
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
                return browseGamesUseCase.browseFirstPage(PAGE_SIZE);
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
                return searchGamesUseCase.search(activeSearchQuery, PAGE_SIZE);
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
                    return browseGamesUseCase.browseNextPage(nextCursor, PAGE_SIZE);
                }
                return searchGamesUseCase.searchNextPage(activeSearchQuery, nextCursor, PAGE_SIZE);
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
        }
        nextCursor = page.getNextCursor();
        nextPageButton.setEnabled(page.hasNextPage());
        statusLabel.setText(games.size() + " Spiele geladen" + (page.hasNextPage() ? " · weitere verfügbar" : ""));
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
                return loadGameDetailsUseCase.loadDetails(summary.getIdentifier());
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
        titleLabel.setText(details.getTitle());
        detailsPane.setText(createDetailsHtml(details));
        detailsPane.setCaretPosition(0);
        fileComboBox.setModel(new DefaultComboBoxModel<GameFile>(details.getDownloadableFiles().toArray(new GameFile[details.getDownloadableFiles().size()])));
        licenseCheckBox.setSelected(acceptLicenseUseCase.isAccepted(details));
        updateDownloadButtonState();
    }

    private String createDetailsHtml(GameDetails details) {
        StringBuilder html = new StringBuilder();
        html.append("<html><body>");
        html.append(details.getDescriptionHtml());
        html.append("<hr>");
        html.append("<h3>Lizenz- und Rechtehinweise</h3>");
        if (details.getLicenseNotice().hasLicenseInformation()) {
            html.append("<p><b>Lizenz:</b> ").append(escape(details.getLicenseNotice().getLicenseUrl())).append("</p>");
            html.append("<p><b>Rechte:</b> ").append(escape(details.getLicenseNotice().getRights())).append("</p>");
        } else {
            html.append("<p>Für dieses Item liefert Archive.org keine ausdrückliche Lizenzinformation im Metadatenfeld.</p>");
        }
        html.append("<p><b>Quelle:</b> ").append(escape(details.getLicenseNotice().getSourceUrl())).append("</p>");
        html.append("<p>Bitte prüfe die Detailseite und verwende die Dateien nur, wenn deine Nutzung zulässig ist.</p>");
        html.append("</body></html>");
        return html.toString();
    }

    private void acceptCurrentLicenseWhenSelected() {
        if (currentDetails == null || !licenseCheckBox.isSelected()) {
            updateDownloadButtonState();
            return;
        }

        try {
            acceptLicenseUseCase.accept(currentDetails);
            statusLabel.setText("Lizenzhinweis akzeptiert für " + currentDetails.getTitle());
        } catch (Exception exception) {
            licenseCheckBox.setSelected(false);
            showError("Lizenzannahme konnte nicht gespeichert werden.", exception);
        }
        updateDownloadButtonState();
    }

    private void downloadSelectedFile() {
        if (currentDetails == null) {
            return;
        }

        final GameFile selectedFile = (GameFile) fileComboBox.getSelectedItem();
        if (selectedFile == null) {
            return;
        }

        setBusy("Lade " + selectedFile.getName() + " herunter ...");
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
                    statusLabel.setText("Download abgeschlossen: " + selectedFile.getName());
                } catch (Exception exception) {
                    showError("Download fehlgeschlagen.", exception);
                } finally {
                    setReady();
                }
            }
        }.execute();
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
        downloadButton.setEnabled(false);
    }

    private void setReady() {
        progressBar.setIndeterminate(false);
        searchButton.setEnabled(true);
        browseButton.setEnabled(true);
        updateDownloadButtonState();
    }

    private void updateDownloadButtonState() {
        boolean hasFile = fileComboBox.getSelectedItem() != null;
        boolean hasAcceptedLicense = licenseCheckBox.isSelected();
        downloadButton.setEnabled(currentDetails != null && hasFile && hasAcceptedLicense);
    }

    private String escape(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(), "Fehler", JOptionPane.ERROR_MESSAGE);
        statusLabel.setText(message);
    }

    private interface PageLoader {
        GamePage load() throws Exception;
    }
}
