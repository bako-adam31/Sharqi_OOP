package org.example.sharqi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.SplitPane;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import javafx.scene.layout.TilePane;
import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import javafx.scene.control.Separator;

public class ParfumController {

    // API Keys, links
    private static final String API_KEY = "fd8094dfde51e2499d3f92a850057230d6bc59e890a431e31cd42997c55e4930";
    private static final String API_BASE_URL_SEARCH = "https://api.fragella.com/api/v1/fragrances?limit=20&search=";
    private static final String API_BASE_URL_SIMILAR = "https://api.fragella.com/api/v1/fragrances/similar?limit=5&name=";
    private static final String API_BASE_URL_MATCH = "https://api.fragella.com/api/v1/fragrances/match?limit=6";

    @FXML private SplitPane mainSplitPane;
    @FXML private ScrollPane contentScrollPane;
    @FXML private AnchorPane contentPane;
    @FXML private AnchorPane leftMenuPane;
    @FXML private Button homeButton;
    @FXML private Button searchButton;
    @FXML private Button filterButton;
    @FXML private Button cloneButton;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .version(HttpClient.Version.HTTP_2)
            .build();
    private final Gson gson = new Gson();
    private PauseTransition searchDebouncer;
    private Task<List<Parfum>> currentApiTask;
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @FXML
    public void initialize() {
        loadHomePage();
    }

    @FXML private void onHomeClick() { loadHomePage(); }
    @FXML private void onSearchClick() { loadSearchPage(); }
    @FXML private void onFilterClick() { loadFilterPage(); }
    @FXML private void onCloneClick() { loadClonePage(); }

    private void loadHomePage() {
        VBox homeView = ViewFactory.buildHomePage();
        contentPane.getChildren().setAll(homeView);
        AnchorPane.setTopAnchor(homeView, 0.0);
        AnchorPane.setLeftAnchor(homeView, 0.0);
    }

    private void loadSearchPage() {
        VBox searchPageLayout = new VBox(20);
        searchPageLayout.setPadding(new Insets(30));
        searchPageLayout.setStyle("-fx-background-color: transparent;"); // Hogy látszódjon a háttérkép

        // cim es kereso sav
        Label title = new Label("Parfüm Keresése");
        title.getStyleClass().add("title-label");

        // kereso
        TextField pageSearchField = new TextField();
        pageSearchField.setPromptText("Írj be egy parfüm nevet vagy márkát...");
        pageSearchField.getStyleClass().add("modern-input");
        pageSearchField.setPrefHeight(45);

        // Icon a keresohoz
        Button searchActionBtn = new Button("Keresés");
        searchActionBtn.getStyleClass().add("action-button");

        // box for buttons and search
        HBox searchBar = new HBox(15, pageSearchField, searchActionBtn);
        HBox.setHgrow(pageSearchField, Priority.ALWAYS);
        searchBar.getStyleClass().add("search-container");

        // ScrollPane
        ScrollPane scrollWrapper = new ScrollPane();
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollWrapper, Priority.ALWAYS); // Töltse ki a képernyő alját

        TilePane resultsGrid = new TilePane();
        resultsGrid.setHgap(20);
        resultsGrid.setVgap(20);
        resultsGrid.setPrefColumns(3); // 3 oszlop
        resultsGrid.setAlignment(Pos.TOP_CENTER); // Középre igazítva
        resultsGrid.setStyle("-fx-padding: 10; -fx-background-color: transparent;");

        // Alapértelmezett üzenet, ha még nincs keresés
        Label placeholder = new Label("Kezdj el gépelni a kereséshez...");
        placeholder.getStyleClass().add("placeholder-label");
        resultsGrid.getChildren().add(placeholder);

        scrollWrapper.setContent(resultsGrid);

        // 3. KERESÉSI LOGIKA (Debounce - várakozás gépelés közben)
        searchDebouncer = new PauseTransition(Duration.millis(500)); // Fél másodperc szünet
        searchDebouncer.setOnFinished(e -> {
            String term = pageSearchField.getText();
            if (term.length() > 2) {
                // Itt hívjuk meg a keresést a RÁCSRA (Grid)
                performGridSearch(term, resultsGrid);
            }
        });

        // Gépelés figyelése
        pageSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebouncer.stop();
            searchDebouncer.playFromStart();
        });

        // Gombnyomásra azonnal keres
        searchActionBtn.setOnAction(e -> {
            searchDebouncer.stop();
            performGridSearch(pageSearchField.getText(), resultsGrid);
        });

        searchPageLayout.getChildren().addAll(title, searchBar, scrollWrapper);

        contentPane.getChildren().setAll(searchPageLayout);
        AnchorPane.setTopAnchor(searchPageLayout, 0.0);
        AnchorPane.setBottomAnchor(searchPageLayout, 0.0);
        AnchorPane.setLeftAnchor(searchPageLayout, 0.0);
        AnchorPane.setRightAnchor(searchPageLayout, 0.0);
    }

    // Ez végzi a keresést és frissíti a rácsot
    private void performGridSearch(String searchTerm, TilePane resultsGrid) {
        if (searchTerm == null || searchTerm.trim().isEmpty()) return;

        // Töltés jelzése
        resultsGrid.getChildren().clear();
        resultsGrid.getChildren().add(new Label("Keresés folyamatban..."));

        Task<List<Parfum>> searchTask = new Task<>() {
            @Override
            protected List<Parfum> call() throws Exception {
                // Ugyanaz az API hívás, mint eddig
                String encodedSearchTerm = URLEncoder.encode(searchTerm.trim(), StandardCharsets.UTF_8);
                String finalApiUrl = API_BASE_URL_SEARCH + encodedSearchTerm;

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(finalApiUrl))
                        .header("x-api-key", API_KEY)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Type listType = new TypeToken<ArrayList<Parfum>>(){}.getType();
                    return gson.fromJson(response.body(), listType);
                }
                return new ArrayList<>();
            }
        };

        searchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            resultsGrid.getChildren().clear();
            List<Parfum> results = searchTask.getValue();

            if (results == null || results.isEmpty()) {
                Label noRes = new Label("Nincs találat.");
                noRes.getStyleClass().add("placeholder-label");
                resultsGrid.getChildren().add(noRes);
            } else {
                for (Parfum p : results) {
                    // ITT A LÉNYEG: A szép kártyákat használjuk!
                    resultsGrid.getChildren().add(createDetailedResultCard(p));
                }
            }
        }));

        searchTask.setOnFailed(e -> {
            resultsGrid.getChildren().clear();
            resultsGrid.getChildren().add(new Label("Hiba történt a keresés során."));
            searchTask.getException().printStackTrace();
        });

        executorService.submit(searchTask);
    }


// --- MODERN RÉSZLETES NÉZET ---

    private void displayParfumDetails(Parfum parfum) {
        // 1. BAL OSZLOP: Kép és Alapadatok
        VBox col1 = buildLeftCard(parfum);
        HBox.setHgrow(col1, Priority.ALWAYS);

        // 2. KÖZÉPSŐ OSZLOP: Illatjegyek (Accords & Notes)
        VBox col2 = buildMiddleCard(parfum);
        HBox.setHgrow(col2, Priority.ALWAYS);

        // 3. JOBB OSZLOP: Hasonló parfümök
        VBox col3 = buildRightCard(parfum);
        HBox.setHgrow(col3, Priority.ALWAYS);

        // Fő elrendezés
        HBox masterLayout = new HBox(25, col1, col2, col3); // 25px távolság az oszlopok között
        masterLayout.setPadding(new Insets(30));
        masterLayout.setAlignment(Pos.TOP_CENTER);
        masterLayout.setStyle("-fx-background-color: transparent;");

        // Görgethetővé tesszük az egészet, ha nem férne ki
        ScrollPane scrollPane = new ScrollPane(masterLayout);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        contentPane.getChildren().setAll(scrollPane);
        AnchorPane.setTopAnchor(scrollPane, 0.0);
        AnchorPane.setBottomAnchor(scrollPane, 0.0);
        AnchorPane.setLeftAnchor(scrollPane, 0.0);
        AnchorPane.setRightAnchor(scrollPane, 0.0);
    }

    // 1. Kártya: Kép, Ár, Statisztikák
    private VBox buildLeftCard(Parfum p) {
        VBox card = new VBox(20);
        card.getStyleClass().add("details-card");
        card.setMaxWidth(350);
        card.setMinWidth(300);

        // Kép
        ImageView img = new ImageView();
        try {
            String url = (p.imageUrl != null && !p.imageUrl.isEmpty()) ? p.imageUrl : "file:placeholder.png";
            img.setImage(new Image(url, 280, 280, true, true, true));
        } catch (Exception e) {}
        img.setFitWidth(280);
        img.setPreserveRatio(true);

        StackPane imgContainer = new StackPane(img);
        imgContainer.setAlignment(Pos.CENTER);

        // Név és Márka
        Label nameLbl = new Label(p.name);
        nameLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111;");
        nameLbl.setWrapText(true);

        Label brandLbl = new Label("by " + p.brand);
        brandLbl.setStyle("-fx-font-size: 14px; -fx-text-fill: #666; -fx-font-style: italic;");

        // Statisztika Rács (Grid) - Modern dobozokkal
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(10); statsGrid.setVgap(10);

        addStatBox(statsGrid, 0, 0, "Gender", p.gender != null ? p.gender : "-");
        addStatBox(statsGrid, 1, 0, "Year", p.year != null ? p.year : "-");
        addStatBox(statsGrid, 0, 1, "Rating", p.rating != null ? "★ " + p.rating : "-");
        addStatBox(statsGrid, 1, 1, "Country", p.country != null ? p.country : "-");

        // Sillage & Longevity
        VBox perfBox = new VBox(12); // Nagyobb távolság a sorok között
        perfBox.setPadding(new Insets(15, 0, 0, 0)); // Térköz a felső vonaltól

        Label perfTitle = new Label("Performance");
        perfTitle.getStyleClass().add("details-section-title");
        // Biztosítjuk, hogy fekete legyen és nagyobb
        perfTitle.setStyle("-fx-text-fill: #222; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Egyedi sorok létrehozása (Segédfüggvénnyel)
        HBox sillageRow = createPerformanceRow("🌬️ Sillage", p.sillage);
        HBox longevityRow = createPerformanceRow("⏱️ Longevity", p.longevity);

        VBox usageBox = new VBox(15); // 15px távolság a csoportok között

        Label usageTitle = new Label("Best for...");
        usageTitle.getStyleClass().add("details-section-title");
        usageTitle.setStyle("-fx-text-fill: #222; -fx-font-size: 18px; -fx-font-weight: bold;");

        // Season (Évszak) Szekció
        VBox seasonContainer = new VBox(5); // 5px távolság a sávok között
        Label seasonLabel = new Label("Season");
        seasonLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 13px;");

        // ITT HÍVJUK MEG AZ ÚJ FÜGGVÉNYT:
        populateRankingBars(seasonContainer, p.seasonRanking);

        // Occasion (Alkalom) Szekció
        VBox occasionContainer = new VBox(5);
        Label occasionLabel = new Label("Occasion");
        occasionLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #555; -fx-font-size: 13px;");

        // ITT HÍVJUK MEG AZ ÚJ FÜGGVÉNYT:
        populateRankingBars(occasionContainer, p.occasionRanking);


        perfBox.getChildren().addAll(perfTitle, sillageRow, longevityRow);
        usageBox.getChildren().addAll(usageTitle, seasonLabel, seasonContainer, occasionLabel, occasionContainer);

        card.getChildren().addAll(
                imgContainer,
                nameLbl, brandLbl,
                new Separator(), statsGrid,
                new Separator(), perfBox,
                new Separator(), usageBox
        );

        return card;
    }



    // Új segédfüggvény a rangsoroló sávokhoz (Season/Occasion)

// --- ÚJ METÓDUSOK A SZÍNES SÁVOKHOZ (Season/Occasion) ---

    // 1. Ez számolja ki az arányokat és rajzolja ki a sávokat
    private void populateRankingBars(VBox container, List<Parfum.RankingItem> items) {
        if (items == null || items.isEmpty()) return;

        // Rendezés: a legnagyobb pontszámú legyen elöl
        items.sort((a, b) -> Double.compare(b.score, a.score));

        // Megkeressük a legmagasabb pontszámot a listában (ez lesz a 100% szélesség alapja)
        double maxScore = items.stream()
                .mapToDouble(i -> i.score)
                .max()
                .orElse(1.0);

        for (Parfum.RankingItem item : items) {
            if (item.score > 0) { // Csak ami kapott szavazatot
                String color = getSeasonOrOccasionColor(item.name);

                // Kiszámoljuk az arányos szélességet (max 280px)
                // Képlet: (aktuális / maximum) * 280
                double MAX_WIDTH = 280.0;
                double calculatedWidth = (item.score / maxScore) * MAX_WIDTH;

                Node bar = createSingleRankingBar(item.name, calculatedWidth, color);
                container.getChildren().add(bar);
            }
        }
    }

    // 2. Ez hoz létre egyetlen színes sávot a megadott szélességgel
    private Node createSingleRankingBar(String name, double width, String color) {
        // Minimum 70px szélesség, hogy a szöveg kiférjen akkor is, ha kicsi a pontszám
        if (width < 70) width = 70;

        Pane bar = new Pane();
        bar.setPrefSize(width, 28); // 28px magas sáv
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 8;");

        Label label = new Label(name);
        // Alapból fehér szöveg
        label.setStyle("-fx-text-fill: white; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 0 0 0 10;");

        // Ha nagyon világos a sáv (pl. sárga), legyen sötét a betű, hogy olvasható legyen
        if (color.equals("#facc15") || color.equals("#fef08a")) {
            label.setStyle("-fx-text-fill: #444; -fx-font-weight: bold; -fx-font-size: 13px; -fx-padding: 0 0 0 10;");
        }

        StackPane stack = new StackPane(bar, label);
        stack.setAlignment(Pos.CENTER_LEFT);
        VBox.setMargin(stack, new Insets(2, 0, 2, 0)); // Kis térköz a sávok között

        return stack;
    }

    // 3. Ez adja meg a színeket a nevek alapján
    private String getSeasonOrOccasionColor(String name) {
        name = name.toLowerCase();
        // Évszakok
        if (name.contains("winter")) return "#3b82f6"; // Kék
        if (name.contains("spring")) return "#84cc16"; // Zöld
        if (name.contains("summer")) return "#facc15"; // Sárga
        if (name.contains("fall") || name.contains("autumn")) return "#d97706"; // Barna

        // Alkalmak
        if (name.contains("day")) return "#fef08a"; // Halványsárga
        if (name.contains("night")) return "#1e293b"; // Sötétkék
        if (name.contains("date")) return "#ec4899"; // Pink
        if (name.contains("office") || name.contains("work")) return "#94a3b8"; // Szürke

        return "#cbd5e1"; // Alapértelmezett szürke
    }



    // ÚJ SEGÉDFÜGGVÉNY A PERFORMANCE SOROKHOZ
    private HBox createPerformanceRow(String labelText, String valueText) {
        // 1. Címke (Bal oldal)
        Label lbl = new Label(labelText);
        // Sötétszürke szín (#444), félkövér, nagyobb betűméret (14px)
        lbl.setStyle("-fx-font-weight: bold; -fx-text-fill: #444; -fx-font-size: 14px;");
        lbl.setMinWidth(100); // Fix szélesség, hogy a plecsnik egymás alá kerüljenek

        // 2. Érték (Jobb oldal - a színes plecsni)
        Label badge = createBadge(valueText);
        // Felülírjuk a badge stílusát, hogy kicsit nagyobb legyen
        badge.setStyle(badge.getStyle() + "-fx-font-size: 13px; -fx-padding: 6 14;");

        // Sor összerakása
        HBox row = new HBox(15, lbl, badge); // 15px távolság a címke és az érték között
        row.setAlignment(Pos.CENTER_LEFT);
        return row;
    }


    // 2. Kártya: Illatprofil
    private VBox buildMiddleCard(Parfum p) {
        VBox card = new VBox(25);
        card.getStyleClass().add("details-card");
        card.setMinWidth(350);
        HBox.setHgrow(card, Priority.ALWAYS); // Ez nyúljon meg, ha van hely

        // Main Accords
        VBox accordsBox = new VBox(10);
        Label accordsTitle = new Label("Main Accords");
        accordsTitle.getStyleClass().add("details-section-title");
        populateAccordsPane(accordsBox, p.mainAccordsPercentage);

        // Illatpiramis (Notes)
        VBox notesBox = new VBox(15);
        Label notesTitle = new Label("Olfactory Pyramid");
        notesTitle.getStyleClass().add("details-section-title");

        notesBox.getChildren().add(createNoteGroup("Top Notes", p.notes != null ? p.notes.top : null));
        notesBox.getChildren().add(createNoteGroup("Middle Notes", p.notes != null ? p.notes.middle : null));
        notesBox.getChildren().add(createNoteGroup("Base Notes", p.notes != null ? p.notes.base : null));

        card.getChildren().addAll(accordsTitle, accordsBox, new Separator(), notesTitle, notesBox);
        return card;
    }

    // 3. Kártya: Hasonlók
    private VBox buildRightCard(Parfum p) {
        VBox card = new VBox(15);
        card.getStyleClass().add("details-card");
        card.setMinWidth(260);
        card.setMaxWidth(280);

        Label title = new Label("Similar Fragrances");
        title.getStyleClass().add("details-section-title");

        FlowPane similarPane = new FlowPane(15, 15);
        similarPane.setAlignment(Pos.TOP_CENTER);

        card.getChildren().addAll(title, similarPane);

        // Aszinkron betöltés indítása
        loadSimilarFragrancesParallel(p.name, similarPane);

        return card;
    }

    // --- Új Segédfüggvények ---

    private void addStatBox(GridPane grid, int col, int row, String title, String value) {
        VBox box = new VBox(3);
        box.getStyleClass().add("stat-box");
        box.setPrefWidth(140);

        Label t = new Label(title); t.getStyleClass().add("stat-label");
        Label v = new Label(value); v.getStyleClass().add("stat-value");

        box.getChildren().addAll(t, v);
        grid.add(box, col, row);
    }

    private Label createBadge(String text) {
        Label l = new Label(text != null ? text : "Moderate");
        l.getStyleClass().add("performance-badge");
        // Egyszerű logika a színezésre
        if (text != null && (text.toLowerCase().contains("strong") || text.toLowerCase().contains("long"))) {
            l.getStyleClass().add("perf-strong");
        } else {
            l.getStyleClass().add("perf-moderate");
        }
        return l;
    }
// Ezt a metódust cseréld le a ParfumController.java-ban:


    private VBox createNoteGroup(String title, List<Parfum.NoteDetail> notes) {
        VBox box = new VBox(8);
        Label l = new Label(title);
        // A szekció címe (pl. "Top Notes") legyen sötétszürke
        l.setStyle("-fx-font-weight: bold; -fx-text-fill: #555555;");

        FlowPane fp = new FlowPane(8, 8);
        if (notes != null) {
            for (Parfum.NoteDetail n : notes) {
                HBox chip = new HBox(6);
                chip.getStyleClass().add("modern-note-chip"); // Ez adja a szürke hátteret
                chip.setAlignment(Pos.CENTER_LEFT);

                ImageView icon = new ImageView();
                // Biztonságos képbetöltés
                try {
                    if(n.imageUrl != null) {
                        icon.setImage(new Image(n.imageUrl, 24, 24, true, true, true));
                    }
                } catch(Exception e) {}

                Label name = new Label(n.name);

                // --- ITT A JAVÍTÁS! ---
                // Kényszerítjük a sötét színt (#333333) és a méretet
                name.setStyle("-fx-font-size: 13px; -fx-text-fill: #333333; -fx-font-weight: bold;");

                chip.getChildren().addAll(icon, name);
                fp.getChildren().add(chip);
            }
        }
        box.getChildren().addAll(l, fp);
        return box;
    }


    // Egy apró javítás: kell a 'Separator' importálása
    // import javafx.scene.control.Separator;

    private VBox buildDetailsColumn(Parfum parfum) {
        StackPane imageStack = new StackPane();
        ImageView imageView = new ImageView();
        try {
            if (parfum.imageUrl != null && !parfum.imageUrl.isEmpty()) {
                Image image = new Image(parfum.imageUrl, true);
                imageView.setImage(image);
            }
        } catch (Exception e) { e.printStackTrace(); }

        imageView.setFitHeight(300);
        imageView.setFitWidth(300);
        imageView.setPreserveRatio(true);

        VBox overlayVBox = new VBox();
        overlayVBox.setAlignment(Pos.BOTTOM_LEFT);
        overlayVBox.getStyleClass().add("overlay-box"); // CSS

        Label nameLabel = new Label(parfum.name);
        nameLabel.getStyleClass().add("detail-name"); // CSS

        Label brandLabel = new Label("by " + parfum.brand);
        brandLabel.getStyleClass().add("detail-brand"); // CSS

        HBox tagHBox = new HBox(10);
        tagHBox.setPadding(new Insets(10, 0, 0, 0));
        if (parfum.oilType != null && !parfum.oilType.isEmpty()) {
            Label oilLabel = new Label(parfum.oilType);
            oilLabel.getStyleClass().addAll("tag-label", "tag-oil"); // CSS
            tagHBox.getChildren().add(oilLabel);
        }
        if (parfum.price != null && !parfum.price.isEmpty()) {
            Label priceLabel = new Label("$" + parfum.price);
            priceLabel.getStyleClass().addAll("tag-label", "tag-price"); // CSS
            tagHBox.getChildren().add(priceLabel);
        }
        overlayVBox.getChildren().addAll(nameLabel, brandLabel, tagHBox);
        imageStack.getChildren().addAll(imageView, overlayVBox);

        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10); infoGrid.setVgap(5);

        // Helper a grid sorokhoz
        addInfoRow(infoGrid, 0, "For...", parfum.gender != null ? parfum.gender : "-");
        addInfoRow(infoGrid, 1, "Year", parfum.year != null ? parfum.year : "-");
        addInfoRow(infoGrid, 2, "Rating", parfum.rating != null ? "⭐ " + parfum.rating : "-");
        addInfoRow(infoGrid, 3, "Country", parfum.country != null ? parfum.country : "-");

        GridPane sillageGrid = new GridPane();
        sillageGrid.setHgap(10); sillageGrid.setVgap(5);

        sillageGrid.add(new Label("Sillage"), 0, 0);
        Label sillageLabel = new Label(parfum.sillage);
        sillageLabel.getStyleClass().add("sillage-value"); // CSS
        sillageGrid.add(sillageLabel, 0, 1);

        sillageGrid.add(new Label("Longevity"), 1, 0);
        Label longevityLabel = new Label(parfum.longevity);
        longevityLabel.getStyleClass().add("longevity-value"); // CSS
        sillageGrid.add(longevityLabel, 1, 1);

        // Stílus a címkéknek
        for (Node node : sillageGrid.getChildren()) {
            if (node instanceof Label && GridPane.getRowIndex(node) != null && GridPane.getRowIndex(node) == 0) {
                node.getStyleClass().add("info-label-key");
            }
        }

        FlowPane seasonPane = new FlowPane(5, 5);
        populateRankingPane(seasonPane, parfum.seasonRanking);
        FlowPane occasionPane = new FlowPane(5, 5);
        populateRankingPane(occasionPane, parfum.occasionRanking);
        VBox rankingVBox = new VBox(10, seasonPane, occasionPane);

        return new VBox(15, imageStack, infoGrid, sillageGrid, rankingVBox);
    }

    private void addInfoRow(GridPane grid, int col, String key, String value) {
        Label keyLabel = new Label(key);
        keyLabel.getStyleClass().add("info-label-key"); // CSS
        Label valueLabel = new Label(value);
        valueLabel.getStyleClass().add("info-label-value"); // CSS
        grid.add(keyLabel, col, 0);
        grid.add(valueLabel, col, 1);
    }

    private VBox buildAccordsAndNotesColumn(Parfum parfum) {
        VBox columnVBox = new VBox(20);

        VBox accordsVBox = new VBox(5);
        Label accordsTitle = new Label("Main Accords");
        accordsTitle.getStyleClass().add("section-title"); // CSS
        VBox accordBarsVBox = new VBox(5);
        populateAccordsPane(accordBarsVBox, parfum.mainAccordsPercentage);
        accordsVBox.getChildren().addAll(accordsTitle, accordBarsVBox);

        VBox notesVBox = new VBox(5);

        addNotesSection(notesVBox, "Top Notes", parfum.notes != null ? parfum.notes.top : null);
        addNotesSection(notesVBox, "Middle Notes", parfum.notes != null ? parfum.notes.middle : null);
        addNotesSection(notesVBox, "Base Notes", parfum.notes != null ? parfum.notes.base : null);

        columnVBox.getChildren().addAll(accordsVBox, notesVBox);
        return columnVBox;
    }

    private void addNotesSection(VBox parent, String title, List<Parfum.NoteDetail> notes) {
        Label label = new Label(title);
        label.getStyleClass().add("subsection-title"); // CSS
        label.setPadding(new Insets(10, 0, 0, 0));
        FlowPane pane = new FlowPane(8, 8);
        populateNotePane(pane, notes);
        parent.getChildren().addAll(label, pane);
    }

    private VBox buildSimilarColumn(Parfum parfum) {
        VBox columnVBox = new VBox(10);
        columnVBox.setPadding(new Insets(5, 0, 0, 0));
        columnVBox.setPrefWidth(300);

        Label title = new Label("Similar Fragrances");
        title.getStyleClass().add("section-title"); // CSS

        FlowPane similarPane = new FlowPane(10, 10);
        similarPane.setPrefWrapLength(300);

        columnVBox.getChildren().addAll(title, similarPane);
        loadSimilarFragrancesParallel(parfum.name, similarPane);
        return columnVBox;
    }

    private void loadPlaceholderPage(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 22px; -fx-padding: 20px;");
        contentPane.getChildren().setAll(label);
        AnchorPane.setTopAnchor(label, 20.0);
        AnchorPane.setLeftAnchor(label, 20.0);
    }
    private Task<List<Parfum>> startApiSearchTask(String searchTerm, ListView<Parfum> resultsListView) {
        Task<List<Parfum>> apiTask = new Task<>() {
            @Override
            protected List<Parfum> call() throws Exception { // Itt a throws fontos
                String encodedSearchTerm = URLEncoder.encode(searchTerm, StandardCharsets.UTF_8);
                String finalApiUrl = API_BASE_URL_SEARCH + encodedSearchTerm;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(finalApiUrl))
                        .header("x-api-key", API_KEY)
                        .GET()
                        .build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (isCancelled()) return null;

                if (response.statusCode() == 200) {
                    Type parfumListType = new TypeToken<ArrayList<Parfum>>(){}.getType();
                    List<Parfum> results = gson.fromJson(response.body(), parfumListType);

                    // 16. pont: Saját kivétel dobása
                    if (results == null || results.isEmpty()) {
                        throw new InvalidFragranceException("Nem található parfüm ezzel a névvel: " + searchTerm);
                    }
                    return results;
                } else {
                    throw new Exception("API hiba: " + response.statusCode());
                }
            }
        };
        // ... a többi része a metódusnak változatlan (setOnSucceeded, setOnFailed) ...
        apiTask.setOnSucceeded(e -> Platform.runLater(() -> resultsListView.getItems().setAll(apiTask.getValue())));

        apiTask.setOnFailed(e -> {
            // 16. pont: Kivétel elkapása és kiírása
            Throwable error = apiTask.getException();
            if (error instanceof InvalidFragranceException) {
                System.out.println("Saját hiba elkapva: " + error.getMessage());
            } else {
                error.printStackTrace();
            }
        });
        apiTask.setOnCancelled(e -> System.out.println("Keresés megszakítva: " + searchTerm));
        return apiTask;
    }




    // ... (A ParfumController többi része változatlan) ...

    // --- MÓDOSÍTOTT FILTER PAGE (CSAK 4 MEZŐ) ---
    private void loadFilterPage() {
        // 1. A BAL OLDALI SZŰRŐ PANEL LÉTREHOZÁSA
        VBox filterCard = new VBox(20); // 20px távolság a blokkok között
        filterCard.getStyleClass().add("filter-box"); // Fehér kártya stílus

        // Cím
        Label titleLabel = new Label("Filter by Notes & Accords");
        titleLabel.getStyleClass().add("filter-title");

        // Input mezők létrehozása (Segédfüggvényt használunk a tisztaságért)
        TextField accordField = new TextField();
        VBox accordBox = createInputGroup("Main Accord", accordField, "Pl. leather");

        TextField topField = new TextField();
        VBox topBox = createInputGroup("Top Note", topField, "Pl. bergamot");

        TextField middleField = new TextField();
        VBox middleBox = createInputGroup("Middle Note", middleField, "Pl. jasmine");

        TextField baseField = new TextField();
        VBox baseBox = createInputGroup("Base Note", baseField, "Pl. amber");

        // Gomb
        Button findButton = new Button("Find Matches");
        findButton.getStyleClass().add("action-button");
        findButton.setMaxWidth(Double.MAX_VALUE); // Teljes szélesség

        // Összerakjuk a kártyát
        filterCard.getChildren().addAll(titleLabel, accordBox, topBox, middleBox, baseBox, findButton);

        // 2. JOBB OLDALI EREDMÉNY PANEL (TilePane - ahogy már megcsináltuk)
        VBox resultsContainer = new VBox(15);
        resultsContainer.setPadding(new Insets(20));
        HBox.setHgrow(resultsContainer, Priority.ALWAYS); // Kitölti a maradék helyet

        Label resultsTitle = new Label("Matching Perfumes");
        resultsTitle.getStyleClass().add("section-title");

        TilePane resultsPane = new TilePane();
        resultsPane.setHgap(20);
        resultsPane.setVgap(20);
        resultsPane.setPrefColumns(3);
        resultsPane.setAlignment(Pos.TOP_LEFT);
        resultsPane.setStyle("-fx-padding: 20; -fx-background-color: transparent;");

        // Fontos: ScrollPane kell a TilePane köré, ha sok a találat
        ScrollPane scrollWrapper = new ScrollPane(resultsPane);
        scrollWrapper.setFitToWidth(true);
        scrollWrapper.setStyle("-fx-background-color: transparent; -fx-background: transparent;");
        scrollWrapper.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        VBox.setVgrow(scrollWrapper, Priority.ALWAYS); // A görgető töltse ki a helyet

        resultsContainer.getChildren().addAll(resultsTitle, scrollWrapper);

        // 3. A TELJES OLDAL ÖSSZEÁLLÍTÁSA
        HBox filterPageLayout = new HBox(30, filterCard, resultsContainer);
        filterPageLayout.setPadding(new Insets(30)); // Margó az ablak szélétől
        filterPageLayout.setStyle("-fx-background-color: transparent;"); // Az egész oldal háttere halványszürke

        // Eseménykezelő
        findButton.setOnAction(e -> startApiMatchTask(
                accordField.getText(),
                topField.getText(),
                middleField.getText(),
                baseField.getText(),
                resultsPane
        ));

        contentPane.getChildren().setAll(filterPageLayout);
        AnchorPane.setTopAnchor(filterPageLayout, 0.0);
        AnchorPane.setBottomAnchor(filterPageLayout, 0.0);
        AnchorPane.setLeftAnchor(filterPageLayout, 0.0);
        AnchorPane.setRightAnchor(filterPageLayout, 0.0);
    }

    // Segédfüggvény a modern input mezők létrehozásához
    private VBox createInputGroup(String labelText, TextField textField, String prompt) {
        VBox group = new VBox(8); // 8px távolság a címke és a mező között
        Label label = new Label(labelText);
        label.getStyleClass().add("input-label");

        textField.setPromptText(prompt);
        textField.getStyleClass().add("modern-input");

        group.getChildren().addAll(label, textField);
        return group;
    }

    // --- MÓDOSÍTOTT API MATCH TASK (Csak API hívás, nincs utólagos szűrés) ---
    private Task<List<Parfum>> startApiMatchTask(String accord, String top, String middle, String base, TilePane resultsPane) {
        resultsPane.getChildren().clear();
        resultsPane.getChildren().add(new Label("Keresés..."));
        Task<List<Parfum>> matchTask = new Task<>() {
            @Override
            protected List<Parfum> call() throws Exception {
                StringBuilder query = new StringBuilder();
                if (accord != null && !accord.trim().isEmpty()) {
                    String raw = accord.trim();
                    // Ha nincs megadva százalék, hozzáadjuk a :100-at (vagy :10-et)
                    query.append("&accords=").append(URLEncoder.encode(!raw.contains(":") ? raw.replaceAll(" *, *", ":100,") + ":100" : raw, StandardCharsets.UTF_8));
                }
                if (top != null && !top.trim().isEmpty()) query.append("&top=").append(URLEncoder.encode(top, StandardCharsets.UTF_8));
                if (middle != null && !middle.trim().isEmpty()) query.append("&middle=").append(URLEncoder.encode(middle, StandardCharsets.UTF_8));
                if (base != null && !base.trim().isEmpty()) query.append("&base=").append(URLEncoder.encode(base, StandardCharsets.UTF_8));

                if (query.length() == 0) return new ArrayList<>();

                String url = API_BASE_URL_MATCH + query.toString();
                HttpRequest req = HttpRequest.newBuilder().uri(URI.create(url)).header("x-api-key", API_KEY).GET().build();
                HttpResponse<String> res = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
                if (res.statusCode() == 200) {
                    // Nincs utólagos szűrés, közvetlenül visszaadjuk a listát (az API már szűrt)
                    return gson.fromJson(res.body(), new TypeToken<ArrayList<Parfum>>(){}.getType());
                } else if (res.statusCode() == 404) return new ArrayList<>();
                throw new Exception("API Error: " + res.statusCode());
            }
        };
        matchTask.setOnSucceeded(e -> Platform.runLater(() -> {
            resultsPane.getChildren().clear();
            List<Parfum> m = matchTask.getValue();
            if (m == null || m.isEmpty()) {
                resultsPane.getChildren().add(new Label("Nincs találat."));
            } else {
                for (Parfum p : m) {
                    // ITT A VÁLTOZÁS: Az új, részletes kártyát hívjuk meg!
                    resultsPane.getChildren().add(createDetailedResultCard(p));
                }
            }
        }));
        matchTask.setOnFailed(e -> {
            Platform.runLater(() -> { resultsPane.getChildren().clear(); resultsPane.getChildren().add(new Label("Hiba.")); });
            matchTask.getException().printStackTrace();
        });
        executorService.submit(matchTask);
        return matchTask;
    }



    private Node createDetailedResultCard(Parfum p) {
        // 1. A FŐ KÁRTYA DOBOZ
        VBox card = new VBox();
        card.getStyleClass().add("result-card"); // Ez adja a fehér hátteret és árnyékot



        StackPane imageContainer = new StackPane();
        imageContainer.setPrefHeight(200);
        imageContainer.setMinHeight(200);
        imageContainer.setStyle("-fx-background-color: transparent;");

        ImageView imageView = new ImageView();
        try {
            String imgUrl = (p.imageUrl != null && !p.imageUrl.isEmpty()) ? p.imageUrl : "file:placeholder.png";

            // --- JAVÍTÁS ITT ---
            // A paraméterek: url, width, height, preserveRatio, smooth, BACKGROUNDLOADING
            // Az utolsó 'true' teszi lehetővé, hogy ne akadjon meg a program!
            Image image = new Image(imgUrl, 200, 180, true, true, true);

            imageView.setImage(image);
        } catch (Exception e) {
            // Hiba esetén ne történjen semmi, vagy placeholder
        }
        imageView.setFitHeight(180);
        imageView.setPreserveRatio(true);
        // Ár címke (Balra fent)
        Label priceBadge = new Label(p.price != null ? "$" + p.price : "$--");
        priceBadge.getStyleClass().add("price-badge");
        StackPane.setAlignment(priceBadge, Pos.TOP_LEFT);
        StackPane.setMargin(priceBadge, new Insets(10)); // 10px margó a szélektől

        // Nem címke (Jobbra fent)
        Label genderBadge = new Label(p.gender != null ? p.gender : "Unisex");
        genderBadge.getStyleClass().add("gender-badge");
        StackPane.setAlignment(genderBadge, Pos.TOP_RIGHT);
        StackPane.setMargin(genderBadge, new Insets(10));

        imageContainer.getChildren().addAll(imageView, priceBadge, genderBadge);

        // 3. SZÖVEGES TARTALOM (Alsó rész)
        VBox contentBox = new VBox(8); // 8px távolság a sorok között
        contentBox.setPadding(new Insets(15)); // Hogy ne érjen a szöveg a kártya széléhez
        contentBox.setAlignment(Pos.TOP_LEFT);

        Label nameLabel = new Label(p.name);
        nameLabel.getStyleClass().add("card-title");
        nameLabel.setWrapText(true);
        nameLabel.setMinHeight(40); // Fix magasság, hogy ha 2 soros a név, akkor se ugráljon a layout

        Label brandLabel = new Label("by " + p.brand);
        brandLabel.getStyleClass().add("card-brand");

        // Évszám, Rating, Ország sor
        HBox infoRow = new HBox(8);
        infoRow.setAlignment(Pos.CENTER_LEFT);

        if (p.year != null && !p.year.equals("0")) {
            Label yearLabel = new Label(p.year);
            yearLabel.getStyleClass().add("info-tag");
            infoRow.getChildren().add(yearLabel);
        }
        if (p.rating != null) {
            Label ratingLabel = new Label("★ " + p.rating);
            ratingLabel.getStyleClass().add("rating-tag");
            infoRow.getChildren().add(ratingLabel);
        }

        // Illatjegyek (Max 3 db)
        FlowPane accordsPane = new FlowPane(5, 5);
        if (p.mainAccordsPercentage != null) {
            int count = 0;
            for (Map.Entry<String, String> entry : p.mainAccordsPercentage.entrySet()) {
                if (count >= 3) break;
                Label accordLabel = new Label(entry.getKey());
                accordLabel.getStyleClass().add("accord-pill");

                // Dinamikus színek
                String style = "-fx-background-color: #F3F4F6; -fx-text-fill: #555;";
                String key = entry.getKey().toLowerCase();
                if (key.contains("citrus")) style = "-fx-background-color: #ECFCCB; -fx-text-fill: #3F6212;";
                else if (key.contains("wood")) style = "-fx-background-color: #E0E7FF; -fx-text-fill: #3730A3;";
                else if (key.contains("floral") || key.contains("rose")) style = "-fx-background-color: #FCE7F3; -fx-text-fill: #9D174D;";
                else if (key.contains("spicy") || key.contains("warm")) style = "-fx-background-color: #FFF7ED; -fx-text-fill: #9A3412;";
                else if (key.contains("sweet") || key.contains("vanilla")) style = "-fx-background-color: #FEF9C3; -fx-text-fill: #854D0E;";
                else if (key.contains("leather")) style = "-fx-background-color: #4a3b32; -fx-text-fill: #ffffff;";

                accordLabel.setStyle(style);
                accordsPane.getChildren().add(accordLabel);
                count++;
            }
        }

        contentBox.getChildren().addAll(nameLabel, brandLabel, infoRow, accordsPane);

        // Összerakás
        card.getChildren().addAll(imageContainer, contentBox);

        // Kattintás esemény
        card.setOnMouseClicked(e -> displayParfumDetails(p));

        return card;
    }






    // --- Segédfüggvények (CSS osztályokat használnak) ---

    private void populateRankingPane(FlowPane pane, List<Parfum.RankingItem> rankings) {
        if (rankings != null) {
            AtomicInteger i = new AtomicInteger(1);
            for (Parfum.RankingItem item : rankings) {
                if (item.score > 0) {
                    pane.getChildren().add(createRankingChip(i.getAndIncrement() + ". " + item.name));
                }
            }
        }
    }

    private Node createRankingChip(String text) {
        Label label = new Label(text);
        label.getStyleClass().add("ranking-chip"); // CSS
        return label;
    }



    private void loadSimilarFragrancesParallel(String parfumName, FlowPane resultsPane) {
        // Show loading indicator
        Platform.runLater(() -> {
            resultsPane.getChildren().clear();
            Label loadingLabel = new Label("Loading...");
            loadingLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");
            resultsPane.getChildren().add(loadingLabel);
        });

        // Fetch similar names AND details in parallel batches
        fetchSimilarFragranceNames(parfumName).thenAccept(similarNames -> {
            // Clear loading indicator
            Platform.runLater(() -> resultsPane.getChildren().clear());

            if (similarNames.isEmpty()) {
                Platform.runLater(() -> {
                    Label noResultsLabel = new Label("No similar fragrances found");
                    noResultsLabel.setStyle("-fx-text-fill: #999;");
                    resultsPane.getChildren().add(noResultsLabel);
                });
                return;
            }

            // OPTIMIZATION: Fetch all details in parallel and collect results
            List<CompletableFuture<Parfum>> futures = similarNames.stream()
                    .map(name -> CompletableFuture.supplyAsync(
                            () -> fetchFragranceDetailsByName(name),
                            executorService
                    ))
                    .collect(Collectors.toList());

            // Wait for ALL to complete, then display in order
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .thenRun(() -> {
                        // Collect completed results
                        List<Parfum> parfums = futures.stream()
                                .map(CompletableFuture::join)
                                .filter(p -> p != null)
                                .collect(Collectors.toList());

                        // Display all at once on UI thread
                        Platform.runLater(() -> {
                            for (Parfum parfum : parfums) {
                                resultsPane.getChildren().add(createSmallParfumChip(parfum));
                            }
                        });
                    })
                    .exceptionally(ex -> {
                        ex.printStackTrace();
                        Platform.runLater(() -> {
                            Label errorLabel = new Label("Error loading similar fragrances");
                            errorLabel.setStyle("-fx-text-fill: #c00;");
                            resultsPane.getChildren().add(errorLabel);
                        });
                        return null;
                    });
        }).exceptionally(ex -> {
            ex.printStackTrace();
            Platform.runLater(() -> {
                resultsPane.getChildren().clear();
                Label errorLabel = new Label("Error fetching similar names");
                errorLabel.setStyle("-fx-text-fill: #c00;");
                resultsPane.getChildren().add(errorLabel);
            });
            return null;
        });
    }



    private CompletableFuture<List<String>> fetchSimilarFragranceNames(String parfumName) {
        return CompletableFuture.supplyAsync(new java.util.function.Supplier<List<String>>() {
            @Override
            public List<String> get() {
                try {
                    String url = API_BASE_URL_SIMILAR + URLEncoder.encode(parfumName, StandardCharsets.UTF_8);
                    HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder().uri(URI.create(url)).header("x-api-key", API_KEY).build(), HttpResponse.BodyHandlers.ofString());
                    if (response.statusCode() == 200) {
                        Parfum.SimilarApiResponse api = gson.fromJson(response.body(), Parfum.SimilarApiResponse.class);
                        if (api != null && api.similarFragrances != null) {
                            List<String> names = new ArrayList<>();
                            for (Parfum.SimilarFragrance sf : api.similarFragrances) {
                                names.add(sf.name);
                            }
                            return names;
                        }
                    }
                    return new ArrayList<>();
                } catch (Exception e) {
                    return new ArrayList<>();
                }
            }
        }, executorService);
    }

    private Parfum fetchFragranceDetailsByName(String parfumName) {
        try {
            String url = API_BASE_URL_SEARCH.replace("limit=20", "limit=1") + URLEncoder.encode(parfumName, StandardCharsets.UTF_8);
            HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder().uri(URI.create(url)).header("x-api-key", API_KEY).build(), HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                List<Parfum> l = gson.fromJson(response.body(), new TypeToken<ArrayList<Parfum>>(){}.getType());
                if (l != null && !l.isEmpty()) return l.get(0);
            }
            return null;
        } catch (Exception e) { return null; }
    }

    private Node createSmallParfumChip(Parfum parfum) {
        VBox chip = new VBox(5);
        chip.setAlignment(Pos.TOP_CENTER);
        chip.setPrefWidth(90);
        chip.getStyleClass().add("perfume-card"); // CSS

        ImageView icon = new ImageView();
        icon.setFitHeight(80); icon.setFitWidth(80); icon.setPreserveRatio(true);
        try {
            if (parfum.imageUrl != null) icon.setImage(new Image(parfum.imageUrl, true));
        } catch (Exception e) {}

        Label nameLabel = new Label(parfum.name);
        nameLabel.getStyleClass().add("card-label"); // CSS
        nameLabel.setWrapText(true);
        nameLabel.setPrefWidth(90);

        chip.getChildren().addAll(icon, nameLabel);
        chip.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent e) {
                displayParfumDetails(parfum);
            }
        });
        return chip;
    }

    private void populateAccordsPane(VBox pane, Map<String, String> accords) {
        if (accords != null) {
            Map<String, Integer> strengthOrder = Map.of("Dominant", 1, "Prominent", 2, "Moderate", 3, "Subtle", 4, "Trace", 5);
            String[] colors = {"#d6bcf0", "#c9899a", "#b0a0b0", "#d3a080", "#a0c0b0", "#f0a0a0", "#b0d0d0", "#f0e0a0"};

            List<Map.Entry<String, String>> sortedEntries = new ArrayList<>(accords.entrySet());
            sortedEntries.sort(new Comparator<Map.Entry<String, String>>() {
                @Override
                public int compare(Map.Entry<String, String> e1, Map.Entry<String, String> e2) {
                    int order1 = strengthOrder.getOrDefault(e1.getValue(), 99);
                    int order2 = strengthOrder.getOrDefault(e2.getValue(), 99);
                    return Integer.compare(order1, order2);
                }
            });

            int colorIndex = 0;
            for (Map.Entry<String, String> entry : sortedEntries) {
                String color = colors[colorIndex % colors.length];
                pane.getChildren().add(createAccordBar(entry.getKey(), entry.getValue(), color));
                colorIndex++;
            }
        }
    }

    private Node createAccordBar(String name, String strength, String color) {
        double width = 100;
        if ("Dominant".equals(strength)) width = 300;
        else if ("Prominent".equals(strength)) width = 250;
        else if ("Moderate".equals(strength)) width = 200;
        else if ("Subtle".equals(strength)) width = 150;

        Pane bar = new Pane();
        bar.setPrefSize(width, 30);
        bar.setStyle("-fx-background-color: " + color + ";");
        bar.getStyleClass().add("accord-bar"); // CSS

        Label label = new Label(name);
        label.getStyleClass().add("accord-label"); // CSS

        StackPane stack = new StackPane(bar, label);
        stack.setAlignment(Pos.CENTER_LEFT);
        stack.setMaxWidth(width);
        return stack;
    }

    private void populateNotePane(FlowPane pane, List<Parfum.NoteDetail> notes) {
        if (notes != null) {
            for (Parfum.NoteDetail n : notes) {
                pane.getChildren().add(createNoteChip(n));
            }
        }
    }

    private Node createNoteChip(Parfum.NoteDetail note) {
        ImageView icon = new ImageView(new Image(note.imageUrl, true));
        icon.setFitHeight(30); icon.setFitWidth(30);
        Label nameLabel = new Label(note.name);
        nameLabel.getStyleClass().add("note-label"); // CSS
        HBox chip = new HBox(8, icon, nameLabel);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.getStyleClass().add("note-chip"); // CSS
        return chip;
    }

    // --- ViewFactory (Tiszta CSS-es verzió) ---
    private static class ViewFactory {

        public static VBox buildHomePage() {
            VBox homeVBox = new VBox(30); // Nagyobb térköz a blokkok között
            homeVBox.setPadding(new Insets(40)); // Nagyobb margó az oldaltól

            // 1. Hero Szekció (Címek)
            Label welcomeLabel = new Label("Explore the World of Scents");
            welcomeLabel.getStyleClass().add("hero-title"); // Új CSS osztály

            Label subLabel = new Label("Your journey to the perfect fragrance starts here.");
            subLabel.getStyleClass().add("hero-subtitle"); // Új CSS osztály

            VBox headerBox = new VBox(5, welcomeLabel, subLabel);

            // ... (többi része változatlan) ...
            VBox maleBox = createTop3Box("Top 3 Male Fragrance", new String[][]{
                    {"Dior Homme Intense", "https://d2k6fvhyk5xgx.cloudfront.net/images/dior-homme-intense.jpg"},
                    {"YSL La Nuit De L'Homme", "https://d2k6fvhyk5xgx.cloudfront.net/images/la-nuit-de-lhomme-yves-saint-laurent.jpg"},
                    {"Creed Aventus", "https://d2k6fvhyk5xgx.cloudfront.net/images/creed-aventus.jpg"}
            });
            VBox unisexBox = createTop3Box("Top 3 Unisex Fragrance", new String[][]{
                    {"MFK Baccarat Rouge 540", "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-francis-kurkdjian-baccarat-rouge-540.jpg"},
                    {"Tom Ford Tobacco Vanille", "https://d2k6fvhyk5xgx.cloudfront.net/images/tom-ford-tobacco-vanille.jpg"},
                    {"YSL Babycat", "https://d2k6fvhyk5xgx.cloudfront.net/images/babycat-yves-saint-laurent-unisex.jpg"}
            });
            homeVBox.getChildren().addAll(headerBox, maleBox, unisexBox);
            return homeVBox;
        }

        private static VBox createTop3Box(String title, String[][] fragrances) {
            VBox box = new VBox(15);
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("category-header"); // Új CSS osztály

            HBox cardsHBox = new HBox(25); // Térköz a kártyák között
            for (String[] parfum : fragrances) {
                cardsHBox.getChildren().add(createGlassCard(parfum[0], parfum[1]));
            }
            box.getChildren().addAll(titleLabel, cardsHBox);
            return box;
        }

        // Ez gyártja az üvegkártyákat
        private static Node createGlassCard(String name, String imageUrl) {
            VBox card = new VBox(12); // Térköz a kép és név között
            card.getStyleClass().add("glass-card"); // Az üveg stílus

            // Kép létrehozása
            ImageView imageView = new ImageView();
            try {
                imageView.setImage(new Image(imageUrl, 150, 150, true, true, true));
            } catch (Exception e) {}

            imageView.setFitHeight(130);
            imageView.setFitWidth(130);
            imageView.setPreserveRatio(true);

            // Kép sarkainak lekerekítése (Clip)
            javafx.scene.shape.Rectangle clip = new javafx.scene.shape.Rectangle(130, 130);
            clip.setArcWidth(20);
            clip.setArcHeight(20);
            imageView.setClip(clip);

            // Név
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("glass-card-label"); // Fehér szöveg
            nameLabel.setWrapText(true);
            nameLabel.setMinHeight(40); // Hogy a 2 soros nevek ne ugráljanak

            card.getChildren().addAll(imageView, nameLabel);
            return card;
        }

        // A másik osztályoknak (pl. Clone oldal) meg kell hagyni a régi metódust is,
        // vagy át kell írni őket, hogy ezt használják.
        // Ha a Clone oldalnak kell a régi createPerfumeCard, hagyd meg itt,
        // de nevezd át vagy hagyd békén, és a fenti GlassCard-ot használd a főoldalon.
        public static Node createPerfumeCard(String name, String imageUrl) {
            // ... ez maradhat a régi a kompatibilitás miatt, ha kell ...
            return createGlassCard(name, imageUrl); // Vagy irányítsd át az újra
        }
    }




    private void loadClonePage() {
        VBox clonePageVBox = new VBox(20);
        clonePageVBox.setPadding(new Insets(20));

        Label title = new Label("Budget Friendly Alternatives (Clones)");
        title.getStyleClass().add("title-label");

        // A párok listája: {Klón Név, Klón Kép, Eredeti Név, Eredeti Kép}
        String[][] clonePairs = {
                {
                        "French Avenue Royal Blend",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/royal-blend-fragrance-world-unisex.jpg",
                        "Kilian Angels' Share",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/kilian-angels-share.jpg"
                },
                {
                        "Liquid Brun",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/liquid-brun-fragrance-world-for-women.jpg",
                        "PdM Alhair",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/parfums-de-marly-althair.jpg"
                },
                {
                        "After Effect",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/after-effect-fragrance-world-unisex.jpg",
                        "Side Effect",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/side-effect.jpg"
                },
                {
                        "Spectre Ghost",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/spectre-ghost-fragrance-world-for-men.jpg",
                        "Nishane Ani",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/nishane-ani.jpg"
                },
                {
                        "Spectre Wraith",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/spectre-wraith-fragrance-world-for-men.jpg",
                        "Black Phantom",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/kilian-black-phantom.jpg"
                },
                {
                        "Miraj Absolu",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/miraj-absolu-fragrance-world-for-women.jpg",
                        "Layton Exclusif",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/parfums-de-marly-layton-exclusif.jpg"
                },
                {
                        "Sweet Paradise",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/sweet-paradise-fragrance-world-for-women.jpg",
                        "Scarlet Poppy",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/jo-malone-scarlet-poppy.jpg"

                },
                {
                        "Pinnace",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/pinnace-fragrance-world-for-women.jpg",
                        "Pacific Chill",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/pacific-chill-louis-vuitton-unisex.jpg"
                },
                {
                        "Fierte",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/fierte-fragrance-world-for-women.jpg",
                        "Babycat",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/babycat-yves-saint-laurent-unisex.jpg"
                },
                {
                        "Azzure Oud",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/azzure-oud-fragrance-world-for-women.jpg",
                        "Oud Maracuja",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/oud-maracuja-maison-crivelli-unisex.jpg"
                },
                {
                        "Aether",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/aether-fragrance-world-unisex.jpg",
                        "Greenley",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/parfums-de-marly-greenley.jpg"

                },
                {
                        "Essence de Blanc",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/essence-de-blanc-fragrance-world-unisex.jpg",
                        "Imagination",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/imagination-louis-vuitton-for-men.jpg"
                },
                {
                        "Arsh",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/arsh-fragrance-world-unisex.jpg",
                        "L'Homme Ideal Platin Prive",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/lhomme-ideal-platine-prive-guerlain-for-men.jpg"
                },
                {
                        "Tropical Kiss",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/tropical-kiss-fragrance-world-for-women.jpg",
                        "Soleil de Jeddah",
                        "https://media.parfumo.com/perfumes/2e/2e4fd8-soleil-de-jeddah-mango-kiss-stephane-humbert-lucas_1200.jpg?width=720&aspect_ratio=1:1"
                },
                {
                        "Lumiere Garcon",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/lumiere-garcon-fragrance-world-unisex.jpg",
                        "The One Luminous Night",
                        "https://media.parfumo.com/perfumes/49/4980a8-the-one-luminous-night-dolce-gabbana_1200.jpg?width=720&aspect_ratio=1:1"
                },
                {
                        "Spectre",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/spectre-fragrance-world-for-men.jpg",
                        "Falcon Leather",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/matiere-premiere-falcon-leather.jpg"
                },
                {
                        "Hercules",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-hercules.jpg",
                        "Herod",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/parfums-de-marly-herod.jpg"
                },
                {
                        "Cassius",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-cassius.jpg",
                        "Carlise",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/parfums-de-marly-carlisle.jpg"
                },
                {
                        "Galatea",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-galatea.jpg",
                        "Godolphino",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/parfums-de-marly-godolphin.jpg"
                },
                {
                        "Perseus",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-perseus.jpg",
                        "Pegasus",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/parfums-de-marly-pegasus.jpg"
                },
                {
                        "Forbidden Love",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-forbidden-love.jpg",
                        "Lost Cherry",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/tom-ford-lost-cherry.jpg"
                },
                {
                        "Flaming Elixir",
                        "https://media.parfumo.com/perfumes/3b/3ba8cd_flaming-elixir-maison-alhambra_1200.jpg?width=720&aspect_ratio=1:1",
                        "Cherry Smoke",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/tom-ford-cherry-smoke.jpg"
                },
                {
                        "Dark Aoud",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-dark-oud.jpg",
                        "Oud Wood",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/tom-ford-oud-wood.jpg"
                },
                {
                        "Fusion Intense",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-fabulo-intense.jpg",
                        "Fucking Fabulous",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/tom-ford-fucking-fabulous.jpg"
                },
                {
                        "Pacific Blue",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-alhambra-pacific-blue.jpg",
                        "Neroli Portofino",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/tom-ford-neroli-portofino.jpg"
                },
                {
                        "Lattafa Khamrah",
                        "https://d2k6fvhyk5xgx.cloudfront.net/images/lattafa-khamrah.jpg",
                        "Gissah Sava",
                        "https://media.parfumo.com/perfumes/d4/d425de-sava-gissah_1200.jpg?width=720&aspect_ratio=1:1"
                },
                {
                        "Oud For Glory",
                        "https://media.parfumo.com/perfumes/f7/f7f6d1_oud-for-glory-lattafa_1200.jpg?width=720&aspect_ratio=1:1",
                        "Oud for greatness",
                        "https://media.parfumo.com/perfumes/0e/0ee873-oud-for-greatness-eau-de-parfum-initio_1200.jpg?width=720&aspect_ratio=1:1"
                },
                {
                        "Asad",
                        "https://media.parfumo.com/perfumes/fa/fa86a6-asad-lattafa_1200.jpg?width=720&aspect_ratio=1:1",
                        "Sauvage Elixir",
                        "https://media.parfumo.com/perfumes/32/322a8e-sauvage-elixir-dior_1200.jpg?width=720&aspect_ratio=1:1"
                },
                {
                        "Liam",
                        "https://media.parfumo.com/perfumes/dd/dd4a74-liam-lattafa_1200.jpg?width=720&aspect_ratio=1:1",
                        "Gris Charnel",
                        "https://media.parfumo.com/perfumes/8a/8a97cc-gris-charnel-eau-de-parfum-bdk-parfums_1200.jpg?width=720&aspect_ratio=1:1"
                },
                {
                        "Ameer Al Oudh Intense Oud",
                        "https://media.parfumo.com/perfumes/31/312dd9_ameer-al-oudh-intense-oud-lattafa_1200.jpg?width=720&aspect_ratio=1:1",
                        "By the Fireplace",
                        "https://media.parfumo.com/perfumes/bb/bb736c-by-the-fireplace-maison-margiela_1200.jpg?width=720&aspect_ratio=1:1"
                }


        };
// --- ITT A VÁLTOZÁS: VBox HELYETT TilePane ---
        TilePane gridContainer = new TilePane();
        gridContainer.setHgap(20); // Vízszintes távolság a dobozok között
        gridContainer.setVgap(20); // Függőleges távolság
        gridContainer.setPrefColumns(2); // 2 oszlop legyen
        gridContainer.setAlignment(Pos.TOP_CENTER); // Középre igazítva
        gridContainer.setStyle("-fx-background-color: transparent;");

        for (String[] pair : clonePairs) {
            // A sort létrehozó függvény most egy szélesebb dobozt ad vissza
            HBox row = createCloneRow(pair[0], pair[1], pair[2], pair[3]);
            gridContainer.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(gridContainer);
        scrollPane.setFitToWidth(true);
        // Fontos: Átlátszó háttér a görgetőnek is
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        clonePageVBox.getChildren().addAll(title, scrollPane);

        contentPane.getChildren().setAll(clonePageVBox);
        AnchorPane.setTopAnchor(clonePageVBox, 0.0);
        AnchorPane.setBottomAnchor(clonePageVBox, 0.0);
        AnchorPane.setLeftAnchor(clonePageVBox, 0.0);
        AnchorPane.setRightAnchor(clonePageVBox, 0.0);
    }

    private VBox createBigPerfumeCard(String name, String imageUrl) {
        VBox card = new VBox(8); // Nagyobb térköz a kép és a szöveg között
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(230); // Szélesebb kártya (régi: 120)
        card.getStyleClass().add("perfume-card"); // Megtartjuk a fehér keretet

        ImageView imageView = new ImageView();
        // KÉP MÉRETÉNEK NÖVELÉSE (100 -> 150)
        imageView.setFitHeight(200);
        imageView.setFitWidth(200);
        imageView.setPreserveRatio(true);

        try {
            // Background loading true, hogy ne akadjon
            imageView.setImage(new Image(imageUrl, 150, 150, true, true, true));
        } catch (Exception e) { }

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("card-label");
        // Felülírjuk a betűméretet nagyobbra (13px -> 15px)
        nameLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: #333;");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        card.getChildren().addAll(imageView, nameLabel);
        return card;
    }
    private HBox createCloneRow(String cloneName, String cloneImgUrl, String originalName, String originalImgUrl) {
        HBox row = new HBox(25); // Nagyobb távolság a két parfüm között (15 -> 25)
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(20)); // Nagyobb belső margó

        // --- MÉRET ÉS STÍLUS MÓDOSÍTÁS ---
        // Szélesség növelése: 480 -> 560 (hogy elférjenek a nagyobb képek)
        // Így még pont kifér 2 oszlopba az 1200px széles ablakban
        row.setPrefWidth(560);
        row.setMaxWidth(560);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Bal oldal: Clone (ÚJ Nagy Kártyával)
        VBox cloneCard = createBigPerfumeCard(cloneName, cloneImgUrl);

        Label cloneLabel = new Label("CLONE");
        // Nagyobb címke
        cloneLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");

        VBox leftBox = new VBox(10, cloneLabel, cloneCard);
        leftBox.setAlignment(Pos.CENTER);

        // Közép: "dupe of" szöveg
        Label vsLabel = new Label("dupe of");
        // Nagyobb betűméret (14 -> 16)
        vsLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Jobb oldal: Original (ÚJ Nagy Kártyával)
        VBox originalCard = createBigPerfumeCard(originalName, originalImgUrl);

        Label origLabel = new Label("ORIGINAL");
        // Nagyobb címke
        origLabel.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");

        VBox rightBox = new VBox(10, origLabel, originalCard);
        rightBox.setAlignment(Pos.CENTER);

        row.getChildren().addAll(leftBox, vsLabel, rightBox);

        row.setCursor(javafx.scene.Cursor.HAND);
        return row;
    }



}