package bookrecommender.ui;

import bookrecommender.model.Book;
import bookrecommender.model.Library;
import bookrecommender.model.Review;
import bookrecommender.model.Suggestion;
import bookrecommender.model.User;
import bookrecommender.repo.LibriRepository;
import bookrecommender.service.AggregationService;
import bookrecommender.service.AuthService;
import bookrecommender.service.LibraryService;
import bookrecommender.service.ReviewService;
import bookrecommender.service.SearchService;
import bookrecommender.service.SuggestionService;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.Separator;
import javafx.scene.control.Spinner;
import javafx.scene.control.SpinnerValueFactory;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.KeyCode;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * GUI JavaFX per il progetto Book Recommender (Lab A).
 */
public class BookRecommenderFX extends Application {

    // --- servizi ---
    private LibriRepository libriRepo;
    private SearchService searchService;
    private AuthService authService;
    private LibraryService libraryService;
    private ReviewService reviewService;
    private SuggestionService suggestionService;
    private AggregationService aggregationService;

    // --- UI base ---
    private BorderPane root;
    private TableView<Book> tblBooks;
    private ObservableList<Book> booksData;

    private TextField tfSearch;
    private ComboBox<SearchMode> cbSearchMode;
    private Label lblUser;
    private Label lblStatus;

    private Button btnLogin;
    private Button btnRegister;
    private Button btnLogout;

    // dettaglio libro
    private Label lblDetTitle;
    private Label lblDetAuthors;
    private Label lblDetMeta;
    private Label lblDetCategory;
    private Label lblRatingHeader;
    private Label lblRatingAverages;
    private Label lblRatingDistribution;
    private VBox  boxSuggestions;

    private enum SearchMode {
        TITLE("Titolo"),
        AUTHOR("Autore"),
        AUTHOR_YEAR("Autore + anno");
        private final String label;
        SearchMode(String l){ this.label = l; }
        @Override public String toString(){ return label; }
    }

    private final DecimalFormat DF1 = new DecimalFormat("0.0");

    @Override
    public void start(Stage stage) throws Exception {
        // === init dati ===
        Path dataDir = Paths.get("data");
        Files.createDirectories(dataDir);

        libriRepo = new LibriRepository(dataDir.resolve("Libri.dati"));
        try {
            libriRepo.load();
        } catch (Exception e) {
            showFatal("Errore nel caricamento di Libri.dati.\n" +
                    "Assicurati che il file (o BooksDatasetClean.csv) esista nella cartella data/.\n\n" +
                    e.getMessage());
            return;
        }

        searchService      = new SearchService(libriRepo);
        authService        = new AuthService(dataDir.resolve("UtentiRegistrati.dati"));
        libraryService     = new LibraryService(dataDir.resolve("Librerie.dati"), libriRepo);
        reviewService      = new ReviewService(dataDir.resolve("ValutazioniLibri.dati"), dataDir.resolve("Librerie.dati"));
        suggestionService  = new SuggestionService(dataDir.resolve("ConsigliLibri.dati"), dataDir.resolve("Librerie.dati"));
        aggregationService = new AggregationService(dataDir.resolve("ValutazioniLibri.dati"), dataDir.resolve("ConsigliLibri.dati"));

        // === UI skeleton ===
        root = new BorderPane();
        root.setTop(buildHeader());
        root.setCenter(buildCenter());
        root.setBottom(buildStatusBar());

        Scene scene = new Scene(root, 1200, 720);

        URL css = getClass().getResource("app.css");
        if (css != null) {
            scene.getStylesheets().add(css.toExternalForm());
        } else {
            scene.getStylesheets().add("file:src/bookrecommender/ui/app.css");
        }

        // tasto F5 = refresh dati da file
        scene.setOnKeyPressed(e -> {
            if (e.getCode() == KeyCode.F5) {
                e.consume();
                refreshAll();
            }
        });

        stage.setTitle("Book Recommender — Lab A");
        stage.setScene(scene);
        stage.show();

        tfSearch.requestFocus();
        refreshUserUi();
        setStatus("Libri caricati: " + libriRepo.size());
    }

    // ========================= HEADER =========================

    private Node buildHeader() {
        // titoli
        Label title = new Label("Book Recommender");
        title.getStyleClass().add("title");

        Label subtitle = new Label("Cerca, valuta e suggerisci libri dal dataset del progetto");
        subtitle.getStyleClass().add("subtitle");

        VBox titleBox = new VBox(2, title, subtitle);

        // user info + pulsanti
        lblUser = new Label("Ospite");
        lblUser.getStyleClass().add("muted");

        // Accedi: testo nero (nessuna classe "primary")
        btnLogin = new Button("Accedi");
        btnLogin.setOnAction(e -> doLogin());
        btnLogin.setTooltip(new Tooltip("Accedi con un account esistente"));

        btnRegister = new Button("Registrati");
        btnRegister.setOnAction(e -> doRegister());
        btnRegister.setTooltip(new Tooltip("Crea un nuovo account"));

        btnLogout = new Button("Logout");
        btnLogout.getStyleClass().add("ghost");
        btnLogout.setOnAction(e -> {
            authService.logout();
            refreshUserUi();
            setStatus("Logout effettuato.");
        });
        btnLogout.setTooltip(new Tooltip("Esci dall'account corrente"));

        HBox userBox = new HBox(8, lblUser, btnLogin, btnRegister, btnLogout);
        userBox.setAlignment(Pos.CENTER_RIGHT);

        HBox topRow = new HBox(20, titleBox);
        HBox.setHgrow(titleBox, Priority.ALWAYS);
        topRow.getChildren().add(userBox);
        topRow.setAlignment(Pos.CENTER_LEFT);

        // search row
        tfSearch = new TextField();
        tfSearch.setPromptText("Titolo (es: Il signore degli anelli)");
        tfSearch.setPrefWidth(420);
        tfSearch.setOnAction(e -> performSearch());

        cbSearchMode = new ComboBox<>();
        cbSearchMode.getItems().addAll(SearchMode.TITLE, SearchMode.AUTHOR, SearchMode.AUTHOR_YEAR);
        cbSearchMode.getSelectionModel().select(SearchMode.TITLE);
        cbSearchMode.valueProperty().addListener((obs,oldv,newv) -> updateSearchPrompt());

        Button btnSearch = new Button("Cerca");
        btnSearch.getStyleClass().add("primary");
        btnSearch.setOnAction(e -> performSearch());
        btnSearch.setTooltip(new Tooltip("Esegui la ricerca (Invio)"));

        HBox searchRow = new HBox(8,
                new Label("Cerca per:"),
                cbSearchMode,
                tfSearch,
                btnSearch
        );
        searchRow.setAlignment(Pos.CENTER_LEFT);
        HBox.setHgrow(tfSearch, Priority.ALWAYS);

        VBox header = new VBox(12, topRow, searchRow);
        header.getStyleClass().add("appbar");
        header.setPadding(new Insets(12, 16, 12, 16));
        return header;
    }

    private void updateSearchPrompt() {
        SearchMode m = cbSearchMode.getValue();
        if (m == SearchMode.TITLE) {
            tfSearch.setPromptText("Titolo (es: Il signore degli anelli)");
        } else if (m == SearchMode.AUTHOR) {
            tfSearch.setPromptText("Autore (es: Stephen King)");
        } else {
            tfSearch.setPromptText("Autore e anno (es: Stephen King; 1986)");
        }
    }

    // ========================= CENTER =========================

    private Node buildCenter() {
        // tabella sinistra
        tblBooks = new TableView<>();
        booksData = FXCollections.observableArrayList(libriRepo.all());
        tblBooks.setItems(booksData);

        TableColumn<Book,Integer> colId = new TableColumn<>("ID");
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colId.setPrefWidth(60);

        TableColumn<Book,String> colTitolo = new TableColumn<>("Titolo");
        colTitolo.setCellValueFactory(new PropertyValueFactory<>("titolo"));
        colTitolo.setPrefWidth(280);

        TableColumn<Book,String> colAutori = new TableColumn<>("Autori");
        colAutori.setCellValueFactory(c -> Bindings.createStringBinding(
                () -> String.join(", ", c.getValue().getAutori())
        ));
        colAutori.setPrefWidth(260);

        TableColumn<Book,Integer> colAnno = new TableColumn<>("Anno");
        colAnno.setCellValueFactory(new PropertyValueFactory<>("anno"));
        colAnno.setPrefWidth(70);

        tblBooks.getColumns().addAll(colId, colTitolo, colAutori, colAnno);
        tblBooks.setPlaceholder(new Label("Nessun libro trovato. Modifica i criteri di ricerca."));
        tblBooks.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSel, newSel) -> updateDetail(newSel)
        );

        VBox tableBox = new VBox(
                new Label("Risultati ricerca"),
                tblBooks
        );
        VBox.setVgrow(tblBooks, Priority.ALWAYS);
        tableBox.setPadding(new Insets(12));
        tableBox.setSpacing(8);

        // pannello di dettaglio a destra
        Node detail = buildDetailPanel();

        HBox center = new HBox(12, tableBox, detail);
        HBox.setHgrow(tableBox, Priority.ALWAYS);
        HBox.setHgrow(detail, Priority.ALWAYS);
        center.setPadding(new Insets(0, 12, 12, 12));
        return center;
    }

    private Node buildDetailPanel() {
        lblDetTitle = new Label("Seleziona un libro dalla tabella");
        lblDetTitle.getStyleClass().add("title2");

        lblDetAuthors = new Label();
        lblDetMeta = new Label();
        lblDetCategory = new Label();
        lblDetAuthors.getStyleClass().add("muted");
        lblDetMeta.getStyleClass().add("muted");
        lblDetCategory.getStyleClass().add("muted");

        VBox head = new VBox(4, lblDetTitle, lblDetAuthors, lblDetMeta, lblDetCategory);

        // sezione valutazioni
        Label lblRatingTitle = new Label("Valutazioni");
        lblRatingTitle.getStyleClass().add("title2");

        lblRatingHeader = new Label("Nessuna valutazione ancora presente.");
        lblRatingAverages = new Label();
        lblRatingDistribution = new Label();
        lblRatingAverages.getStyleClass().add("muted");
        lblRatingDistribution.getStyleClass().add("muted");

        VBox ratingBox = new VBox(4, lblRatingHeader, lblRatingAverages, lblRatingDistribution);
        ratingBox.setPadding(new Insets(4, 0, 0, 0));

        // sezione suggerimenti
        Label lblSugTitle = new Label("Libri suggeriti dagli utenti");
        lblSugTitle.getStyleClass().add("title2");
        boxSuggestions = new VBox(4);
        Label placeholder = new Label("Nessun suggerimento ancora presente.");
        placeholder.getStyleClass().add("muted");
        boxSuggestions.getChildren().add(placeholder);

        // pulsanti azione
        Button btnAddLib = new Button("Aggiungi a libreria…");
        btnAddLib.setOnAction(e -> onAddToLibrary());

        Button btnReview = new Button("Valuta questo libro…");
        btnReview.setOnAction(e -> onAddReview());

        Button btnSuggest = new Button("Suggerisci libri correlati…");
        btnSuggest.setOnAction(e -> onAddSuggestion());

        btnAddLib.disableProperty().bind(tblBooks.getSelectionModel().selectedItemProperty().isNull());
        btnReview.disableProperty().bind(tblBooks.getSelectionModel().selectedItemProperty().isNull());
        btnSuggest.disableProperty().bind(tblBooks.getSelectionModel().selectedItemProperty().isNull());

        HBox actions = new HBox(8, btnAddLib, btnReview, btnSuggest);
        actions.setAlignment(Pos.CENTER_LEFT);
        actions.setPadding(new Insets(8, 0, 0, 0));

        VBox content = new VBox(
                head,
                new Separator(),
                lblRatingTitle,
                ratingBox,
                new Separator(),
                lblSugTitle,
                boxSuggestions,
                new Separator(),
                actions
        );
        content.setSpacing(8);
        content.setPadding(new Insets(12));
        content.getStyleClass().add("elevated");

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFitToWidth(true);
        scroll.setPrefWidth(420);
        scroll.setStyle("-fx-background-color: transparent;");

        return scroll;
    }

    private Node buildStatusBar() {
        lblStatus = new Label("Pronto.");
        HBox bar = new HBox(lblStatus);
        bar.getStyleClass().add("statusbar");
        bar.setPadding(new Insets(6, 12, 6, 12));
        return bar;
    }

    // ========================= ACTIONS =========================

    private void refreshAll() {
        try {
            libriRepo.load();
            booksData.setAll(libriRepo.all());
            tblBooks.getSelectionModel().clearSelection();
            clearDetail();
            setStatus("Dati aggiornati (" + libriRepo.size() + " libri).");
        } catch (Exception ex) {
            showError("Errore durante l'aggiornamento dei dati: " + ex.getMessage());
        }
    }

    private void performSearch() {
        String q = tfSearch.getText() == null ? "" : tfSearch.getText().trim();
        SearchMode m = cbSearchMode.getValue();
        List<Book> result;
        try {
            switch (m) {
                case TITLE:
                    result = searchService.cercaLibroPerTitolo(q);
                    break;
                case AUTHOR:
                    result = searchService.cercaLibroPerAutore(q);
                    break;
                case AUTHOR_YEAR:
                    String[] parts = q.split("[;,]", 2);
                    if (parts.length < 2) {
                        showError("Per la ricerca Autore + anno usa il formato:\n\"Autore; 2005\"");
                        return;
                    }
                    String autore = parts[0].trim();
                    String annoStr = parts[1].trim();
                    int anno = Integer.parseInt(annoStr);
                    result = searchService.cercaLibroPerAutoreEAnno(autore, anno);
                    break;
                default:
                    result = libriRepo.all();
            }
            booksData.setAll(result);
            if (result.isEmpty()) {
                setStatus("Nessun libro trovato.");
                clearDetail();
            } else {
                setStatus("Trovati " + result.size() + " libri.");
            }
        } catch (NumberFormatException nfe) {
            showError("Anno non valido. Inserisci un numero intero, es. 1999.");
        } catch (Exception ex) {
            showError("Errore durante la ricerca: " + ex.getMessage());
        }
    }

    private void updateDetail(Book b) {
        if (b == null) {
            clearDetail();
            return;
        }
        lblDetTitle.setText(b.getTitolo());
        lblDetAuthors.setText(String.join(", ", b.getAutori()));

        String meta = "ID " + b.getId();
        if (b.getAnno() != null) meta += " • Anno " + b.getAnno();
        if (b.getEditore() != null && !b.getEditore().isBlank()) meta += " • " + b.getEditore();
        lblDetMeta.setText(meta);

        if (b.getCategoria() != null && !b.getCategoria().isBlank()) {
            lblDetCategory.setText("Categoria: " + b.getCategoria());
        } else {
            lblDetCategory.setText("");
        }

        try {
            // --- Valutazioni ---
            AggregationService.ReviewStats rs = aggregationService.getReviewStats(b.getId());
            if (rs.count == 0) {
                lblRatingHeader.setText("Nessuna valutazione ancora presente.");
                lblRatingAverages.setText("");
                lblRatingDistribution.setText("");
            } else {
                lblRatingHeader.setText("Valutazioni degli utenti (" + rs.count + ")");

                // prima il voto finale medio, poi le medie per criterio
                StringBuilder sbAvg = new StringBuilder();
                sbAvg.append("Voto finale medio: ").append(DF1.format(rs.mediaVotoFinale)).append(" / 5\n\n");
                sbAvg.append("Medie per criterio (1–5):\n");
                sbAvg.append("• Stile: ").append(DF1.format(rs.mediaStile)).append(" / 5\n");
                sbAvg.append("• Contenuto: ").append(DF1.format(rs.mediaContenuto)).append(" / 5\n");
                sbAvg.append("• Gradevolezza: ").append(DF1.format(rs.mediaGradevolezza)).append(" / 5\n");
                sbAvg.append("• Originalità: ").append(DF1.format(rs.mediaOriginalita)).append(" / 5\n");
                sbAvg.append("• Edizione: ").append(DF1.format(rs.mediaEdizione)).append(" / 5");
                lblRatingAverages.setText(sbAvg.toString());

                String dist = rs.distribuzioneVoti.entrySet().stream()
                        .map(e -> {
                            int c = e.getValue();
                            String label = (c == 1) ? "voto" : "voti";
                            return e.getKey() + "★ — " + c + " " + label;
                        })
                        .collect(Collectors.joining("   "));
                lblRatingDistribution.setText("Distribuzione voti finali:\n" + dist);
            }

            // --- Suggerimenti ---
            AggregationService.SuggestionsStats ss = aggregationService.getSuggestionsStats(b.getId());
            boxSuggestions.getChildren().clear();
            if (ss.suggeritiCount.isEmpty()) {
                Label noSug = new Label("Nessun suggerimento ancora presente.");
                noSug.getStyleClass().add("muted");
                boxSuggestions.getChildren().add(noSug);
            } else {
                for (Map.Entry<Integer,Integer> e : ss.suggeritiCount.entrySet()) {
                    Book sug = libriRepo.findById(e.getKey());
                    String titolo = sug != null ? sug.getTitolo() : ("Libro ID " + e.getKey());
                    int count = e.getValue();
                    String labelUtenti = (count == 1) ? "utente" : "utenti";
                    Label row = new Label("• " + titolo + " (ID " + e.getKey() + ") — suggerito da " + count + " " + labelUtenti);
                    boxSuggestions.getChildren().add(row);
                }
            }
        } catch (Exception ex) {
            showError("Errore nel caricamento delle statistiche: " + ex.getMessage());
        }
    }

    private void clearDetail() {
        lblDetTitle.setText("Seleziona un libro dalla tabella");
        lblDetAuthors.setText("");
        lblDetMeta.setText("");
        lblDetCategory.setText("");
        lblRatingHeader.setText("Nessuna valutazione ancora presente.");
        lblRatingAverages.setText("");
        lblRatingDistribution.setText("");
        boxSuggestions.getChildren().clear();
        Label noSug = new Label("Nessun suggerimento ancora presente.");
        noSug.getStyleClass().add("muted");
        boxSuggestions.getChildren().add(noSug);
    }

    // ========================= USER / DIALOG =========================

    private void refreshUserUi() {
        String user = authService.getCurrentUserid();
        if (user == null) {
            lblUser.setText("Ospite");
            if (!lblUser.getStyleClass().contains("muted")) {
                lblUser.getStyleClass().add("muted");
            }
            btnLogin.setVisible(true);
            btnLogin.setManaged(true);
            btnRegister.setVisible(true);
            btnRegister.setManaged(true);
            btnLogout.setVisible(false);
            btnLogout.setManaged(false);
        } else {
            lblUser.setText("Utente: " + user);
            lblUser.getStyleClass().remove("muted");
            btnLogin.setVisible(false);
            btnLogin.setManaged(false);
            btnRegister.setVisible(false);
            btnRegister.setManaged(false);
            btnLogout.setVisible(true);
            btnLogout.setManaged(true);
        }
    }

    private String ensureLoggedIn() {
        String user = authService.getCurrentUserid();
        if (user == null) {
            showError("Per usare questa funzione è necessario essere registrati ed effettuare il login.");
            return null;
        }
        return user;
    }

    /** Registrazione + login automatico. */
    private void doRegister() {
        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Registrazione nuovo utente");
        dlg.setHeaderText("Compila tutti i campi richiesti");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        TextField tfUser   = new TextField();
        PasswordField pfPw = new PasswordField();
        TextField tfNome   = new TextField();
        TextField tfCogn   = new TextField();
        TextField tfCF     = new TextField();
        TextField tfMail   = new TextField();

        tfUser.setPromptText("Scegli uno userid");
        tfNome.setPromptText("Nome");
        tfCogn.setPromptText("Cognome");
        tfCF.setPromptText("Codice fiscale");
        tfMail.setPromptText("Email");

        grid.add(new Label("Userid"), 0, 0); grid.add(tfUser, 1, 0);
        grid.add(new Label("Password"), 0, 1); grid.add(pfPw, 1, 1);
        grid.add(new Label("Nome"), 0, 2); grid.add(tfNome, 1, 2);
        grid.add(new Label("Cognome"), 0, 3); grid.add(tfCogn, 1, 3);
        grid.add(new Label("Codice fiscale"), 0, 4); grid.add(tfCF, 1, 4);
        grid.add(new Label("Email"), 0, 5); grid.add(tfMail, 1, 5);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            String u  = tfUser.getText().trim();
            String pw = pfPw.getText();
            String n  = tfNome.getText().trim();
            String c  = tfCogn.getText().trim();
            String cf = tfCF.getText().trim();
            String em = tfMail.getText().trim();
            if (u.isEmpty() || pw.isEmpty()) {
                showError("Userid e password sono obbligatori.");
                return null;
            }
            return new String[]{u, pw, n, c, cf, em};
        });

        String[] data = dlg.showAndWait().orElse(null);
        if (data == null) return;

        String userid  = data[0];
        String pwPlain = data[1];
        String nome    = data[2];
        String cognome = data[3];
        String cf      = data[4];
        String email   = data[5];

        try {
            String hash = AuthService.sha256(pwPlain);
            User u = new User(userid, hash, nome, cognome, cf, email);
            boolean ok = authService.registrazione(u);
            if (!ok) {
                showError("Registrazione non riuscita.\nLo userid potrebbe essere già esistente.");
                return;
            }
            boolean loginOk = authService.login(userid, pwPlain);
            if (loginOk) {
                refreshUserUi();
                setStatus("Registrazione completata e login effettuato come " + userid);
            } else {
                setStatus("Registrazione completata. Effettua il login con le tue credenziali.");
            }
        } catch (Exception e) {
            showError("Errore in registrazione: " + e.getMessage());
        }
    }

    private void doLogin() {
        Dialog<String[]> dlg = new Dialog<>();
        dlg.setTitle("Login");
        dlg.setHeaderText("Inserisci userid e password");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        TextField tfUser   = new TextField();
        PasswordField pfPw = new PasswordField();

        grid.add(new Label("Userid"), 0, 0); grid.add(tfUser, 1, 0);
        grid.add(new Label("Password"), 0, 1); grid.add(pfPw, 1, 1);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);
        dlg.setResultConverter(bt -> bt == ButtonType.OK
                ? new String[]{ tfUser.getText().trim(), pfPw.getText() }
                : null);

        String[] creds = dlg.showAndWait().orElse(null);
        if (creds == null) return;

        try {
            boolean ok = authService.login(creds[0], creds[1]);
            if (!ok) {
                showError("Credenziali errate.");
            } else {
                refreshUserUi();
                setStatus("Login effettuato come " + authService.getCurrentUserid());
            }
        } catch (Exception e) {
            showError("Errore nel login: " + e.getMessage());
        }
    }

    private void onAddToLibrary() {
        Book sel = tblBooks.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String user = ensureLoggedIn();
        if (user == null) return;

        try {
            List<Library> libs = libraryService.listUserLibraries(user);

            Dialog<ButtonType> dlg = new Dialog<>();
            if (libs.isEmpty()) {
                dlg.setTitle("Crea la tua prima libreria");
                dlg.setHeaderText("Non hai ancora nessuna libreria.\n" +
                        "Scrivi un nome per crearne una nuova e aggiungere il libro selezionato.");
            } else {
                dlg.setTitle("Aggiungi a libreria");
                dlg.setHeaderText("Seleziona una libreria esistente o creane una nuova.");
            }

            GridPane grid = new GridPane();
            grid.setHgap(8);
            grid.setVgap(8);
            grid.setPadding(new Insets(12));

            ComboBox<String> cbLibs = new ComboBox<>();
            cbLibs.getItems().addAll(
                    libs.stream().map(Library::getNome).collect(Collectors.toList())
            );
            if (libs.isEmpty()) {
                cbLibs.setPromptText("Nessuna libreria esistente");
                cbLibs.setDisable(true);
            } else {
                cbLibs.setPromptText("Scegli libreria esistente");
            }

            TextField tfNewName = new TextField();
            tfNewName.setPromptText("Nome nuova libreria");

            grid.add(new Label("Libreria esistente"), 0, 0);
            grid.add(cbLibs, 1, 0);
            grid.add(new Label("Nuova libreria"), 0, 1);
            grid.add(tfNewName, 1, 1);

            dlg.getDialogPane().setContent(grid);
            dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

            Optional<ButtonType> res = dlg.showAndWait();
            if (!res.isPresent() || res.get() != ButtonType.OK) {
                return;
            }

            String rawNewName = tfNewName.getText().trim();
            String chosenName = rawNewName.isEmpty() ? cbLibs.getValue() : rawNewName;

            if (chosenName == null || chosenName.isBlank()) {
                showError("Devi scegliere o creare una libreria.");
                return;
            }

            Library existing = libs.stream()
                    .filter(l -> l.getNome().equals(chosenName))
                    .findFirst()
                    .orElse(null);

            Set<Integer> ids = new LinkedHashSet<>();
            if (existing != null) {
                ids.addAll(existing.getBookIds());
            }
            ids.add(sel.getId());

            Library lib = new Library(user, chosenName, ids);
            boolean ok = libraryService.saveLibrary(lib);
            if (ok) {
                setStatus("Libro aggiunto alla libreria \"" + lib.getNome() + "\"");
            } else {
                showError("Impossibile salvare la libreria.");
            }

        } catch (Exception e) {
            showError("Errore nel salvataggio libreria: " + e.getMessage());
        }
    }

    private void onAddReview() {
        Book sel = tblBooks.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String user = ensureLoggedIn();
        if (user == null) return;

        Dialog<Review> dlg = new Dialog<>();
        dlg.setTitle("Valuta libro");
        dlg.setHeaderText("Inserisci una valutazione da 1 a 5 per ogni criterio");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        Spinner<Integer> spS = spinner15();
        Spinner<Integer> spC = spinner15();
        Spinner<Integer> spG = spinner15();
        Spinner<Integer> spO = spinner15();
        Spinner<Integer> spE = spinner15();
        TextField tfComm = new TextField();
        tfComm.setPromptText("Commento (max 256 caratteri)");

        grid.add(new Label("Stile"), 0, 0); grid.add(spS, 1, 0);
        grid.add(new Label("Contenuto"), 0, 1); grid.add(spC, 1, 1);
        grid.add(new Label("Gradevolezza"), 0, 2); grid.add(spG, 1, 2);
        grid.add(new Label("Originalità"), 0, 3); grid.add(spO, 1, 3);
        grid.add(new Label("Edizione"), 0, 4); grid.add(spE, 1, 4);
        grid.add(new Label("Commento"), 0, 5); grid.add(tfComm, 1, 5);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            int stile = spS.getValue();
            int cont = spC.getValue();
            int grad = spG.getValue();
            int orig = spO.getValue();
            int ed   = spE.getValue();
            String comm = tfComm.getText();
            if (comm != null && comm.length() > 256) {
                showError("Il commento supera i 256 caratteri.");
                return null;
            }
            int votoFinale = Review.calcolaVotoFinale(stile, cont, grad, orig, ed);
            return new Review(user, sel.getId(), stile, cont, grad, orig, ed, votoFinale, comm);
        });

        Review r = dlg.showAndWait().orElse(null);
        if (r == null) return;

        try {
            boolean ok = reviewService.inserisciValutazione(r);
            if (!ok) {
                showError("Valutazione non accettata.\n" +
                        "Controlla che il libro sia presente in almeno una tua libreria e che tu non lo abbia già valutato.");
            } else {
                setStatus("Valutazione salvata.");
                updateDetail(sel);
            }
        } catch (Exception e) {
            showError("Errore nel salvataggio della valutazione: " + e.getMessage());
        }
    }

    private void onAddSuggestion() {
        Book sel = tblBooks.getSelectionModel().getSelectedItem();
        if (sel == null) return;
        String user = ensureLoggedIn();
        if (user == null) return;

        Dialog<List<Integer>> dlg = new Dialog<>();
        dlg.setTitle("Suggerisci libri collegati");
        dlg.setHeaderText(
                "Inserisci fino a 3 ID di libri presenti nelle tue librerie.\n" +
                "(Usa l'ID numerico del libro, NON il titolo.)");

        GridPane grid = new GridPane();
        grid.setHgap(8);
        grid.setVgap(8);
        grid.setPadding(new Insets(12));

        TextField tf1 = new TextField();
        TextField tf2 = new TextField();
        TextField tf3 = new TextField();
        tf1.setPromptText("ID libro 1 (obbligatorio)");
        tf2.setPromptText("ID libro 2 (opzionale)");
        tf3.setPromptText("ID libro 3 (opzionale)");

        grid.add(new Label("Suggerimento 1"), 0, 0); grid.add(tf1, 1, 0);
        grid.add(new Label("Suggerimento 2"), 0, 1); grid.add(tf2, 1, 1);
        grid.add(new Label("Suggerimento 3"), 0, 2); grid.add(tf3, 1, 2);

        dlg.getDialogPane().setContent(grid);
        dlg.getDialogPane().getButtonTypes().addAll(ButtonType.CANCEL, ButtonType.OK);

        dlg.setResultConverter(bt -> {
            if (bt != ButtonType.OK) return null;
            List<Integer> ids = new ArrayList<>();
            for (TextField tf : List.of(tf1, tf2, tf3)) {
                String s = tf.getText().trim();
                if (s.isEmpty()) continue;
                try {
                    ids.add(Integer.parseInt(s));
                } catch (NumberFormatException nfe) {
                    showError("Gli ID devono essere numeri interi.");
                    return null;
                }
            }
            if (ids.isEmpty()) {
                showError("Inserisci almeno un ID di libro da suggerire.");
                return null;
            }
            return ids;
        });

        List<Integer> ids = dlg.showAndWait().orElse(null);
        if (ids == null) return;

        try {
            Suggestion sug = new Suggestion(user, sel.getId(), ids);
            boolean ok = suggestionService.inserisciSuggerimento(sug);
            if (!ok) {
                showError("Suggerimento non accettato.\n" +
                        "Puoi indicare al massimo 3 libri diversi, presenti nelle tue librerie e differenti da questo libro.");
            } else {
                setStatus("Suggerimento registrato.");
                updateDetail(sel);
            }
        } catch (Exception e) {
            showError("Errore nel salvataggio del suggerimento: " + e.getMessage());
        }
    }

    // ========================= UTIL =========================

    private Spinner<Integer> spinner15() {
        Spinner<Integer> sp = new Spinner<>();
        sp.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 5, 3));
        sp.setEditable(false);
        return sp;
    }

    private void setStatus(String s) {
        if (lblStatus != null) lblStatus.setText(s);
    }

    private void showError(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.OK);
        a.showAndWait();
    }

    private void showFatal(String msg) {
        Alert a = new Alert(Alert.AlertType.ERROR, msg, ButtonType.CLOSE);
        a.showAndWait();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
