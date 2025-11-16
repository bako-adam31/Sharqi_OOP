package org.example.sharqi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.concurrent.Task;
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
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.util.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

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

public class ParfumController {

    // -----------------------------------------------------------------------
    // API KULCS
    // -----------------------------------------------------------------------
    private static final String API_KEY = "fd8094dfde51e2499d3f92a850057230d6bc59e890a431e31cd42997c55e4930";

    // API URL-ek
    private static final String API_BASE_URL_SEARCH = "https://api.fragella.com/api/v1/fragrances?limit=20&search=";
    private static final String API_BASE_URL_SIMILAR = "https://api.fragella.com/api/v1/fragrances/similar?limit=5&name=";

    // --- FXML Hivatkozások ---
    @FXML private SplitPane mainSplitPane;
    @FXML private ScrollPane contentScrollPane;
    @FXML private AnchorPane contentPane;
    @FXML private AnchorPane leftMenuPane;
    @FXML private Button homeButton;
    @FXML private Button searchButton;
    @FXML private Button filterButton;
    @FXML private Button cloneButton;

    // --- API és Segédváltozók ---
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();
    private PauseTransition searchDebouncer;
    private Task<List<Parfum>> currentApiTask;

    // Külön szálkezelő a bonyolultabb, párhuzamos API hívásokhoz
    private final ExecutorService executorService = Executors.newFixedThreadPool(5);

    @FXML
    public void initialize() {
        loadHomePage();
    }

    // --- Gomb Eseménykezelők ---
    @FXML private void onHomeClick() { loadHomePage(); }
    @FXML private void onSearchClick() { loadSearchPage(); }
    @FXML private void onFilterClick() { loadPlaceholderPage("Filteres keresés nézet (Hamarosan...)"); }
    @FXML private void onCloneClick() { loadPlaceholderPage("Klonok nézet (Hamarosan...)"); }

    // --- Oldal Betöltő Metódusok ---

    private void loadHomePage() {
        VBox homeView = ViewFactory.buildHomePage();
        contentPane.getChildren().setAll(homeView);
        AnchorPane.setTopAnchor(homeView, 0.0);
        AnchorPane.setLeftAnchor(homeView, 0.0);
    }

    private void loadSearchPage() {
        VBox searchPageVBox = new VBox(15);
        searchPageVBox.setPadding(new Insets(20));
        Label title = new Label("Parfüm Keresése");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
        TextField pageSearchField = new TextField();
        pageSearchField.setPromptText("Keresés parfümre vagy márkára...");
        pageSearchField.setStyle("-fx-font-size: 16px;");
        ListView<Parfum> pageResultsListView = new ListView<>();
        VBox.setVgrow(pageResultsListView, Priority.ALWAYS);
        searchPageVBox.getChildren().addAll(title, pageSearchField, pageResultsListView);

        searchDebouncer = new PauseTransition(Duration.millis(400));
        searchDebouncer.setOnFinished(e -> {
            String searchTerm = pageSearchField.getText();
            if (searchTerm != null && searchTerm.trim().length() > 2) {
                if (currentApiTask != null && currentApiTask.isRunning()) {
                    currentApiTask.cancel();
                }
                currentApiTask = startApiSearchTask(searchTerm, pageResultsListView);
                new Thread(currentApiTask).start();
            }
        });
        pageSearchField.textProperty().addListener((obs, oldVal, newVal) -> {
            searchDebouncer.stop();
            searchDebouncer.playFromStart();
        });

        pageResultsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if (newSelection != null) {
                        displayParfumDetails(newSelection);
                    }
                }
        );

        contentPane.getChildren().setAll(searchPageVBox);
        AnchorPane.setTopAnchor(searchPageVBox, 0.0);
        AnchorPane.setBottomAnchor(searchPageVBox, 0.0);
        AnchorPane.setLeftAnchor(searchPageVBox, 0.0);
        AnchorPane.setRightAnchor(searchPageVBox, 0.0);
    }

    /**
     * Betölti a RÉSZLETES nézetet 3 oszlopban.
     */
    private void displayParfumDetails(Parfum parfum) {
        VBox column1 = buildDetailsColumn(parfum);
        VBox column2 = buildAccordsAndNotesColumn(parfum);
        VBox column3 = buildSimilarColumn(parfum); // A szűrés nélküli verzió

        HBox masterLayout = new HBox(20);
        masterLayout.setPadding(new Insets(20));
        masterLayout.getChildren().addAll(column1, column2, column3);

        contentPane.getChildren().setAll(masterLayout);
        AnchorPane.setTopAnchor(masterLayout, 0.0);
        AnchorPane.setLeftAnchor(masterLayout, 0.0);
    }

    /**
     * Felépíti az 1. Oszlopot (Kép, Infók, Sillage, Ranking).
     */
    private VBox buildDetailsColumn(Parfum parfum) {
        // --- 1. Kép + Overlay ---
        StackPane imageStack = new StackPane();
        ImageView imageView = new ImageView();
        try {
            if (parfum.imageUrl != null && !parfum.imageUrl.isEmpty()) {
                String originalUrl = parfum.imageUrl;
                Image image = new Image(originalUrl, true);
                image.errorProperty().addListener((obs, oldError, newError) -> {
                    if (newError) image.getException().printStackTrace();
                });
                imageView.setImage(image);
            } else {
                imageView.setImage(null);
            }
        } catch (Exception e) {
            e.printStackTrace();
            imageView.setImage(null);
        }
        imageView.setFitHeight(300);
        imageView.setFitWidth(300);
        imageView.setPreserveRatio(true);
        VBox overlayVBox = new VBox();
        overlayVBox.setAlignment(Pos.BOTTOM_LEFT);
        overlayVBox.setStyle("-fx-background-color: linear-gradient(from 0% 50% to 0% 100%, transparent, #000000); -fx-padding: 15;");
        Label nameLabel = new Label(parfum.name);
        nameLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label brandLabel = new Label("by " + parfum.brand);
        brandLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #cccccc;");
        HBox tagHBox = new HBox(10);
        tagHBox.setPadding(new Insets(10, 0, 0, 0));
        if (parfum.oilType != null && !parfum.oilType.isEmpty()) {
            Label oilLabel = new Label(parfum.oilType);
            oilLabel.setStyle("-fx-background-color: rgba(255,255,255,0.3); -fx-text-fill: white; -fx-padding: 4 8 4 8; -fx-background-radius: 5;");
            tagHBox.getChildren().add(oilLabel);
        }
        if (parfum.price != null && !parfum.price.isEmpty()) {
            Label priceLabel = new Label("$" + parfum.price);
            priceLabel.setStyle("-fx-background-color: #e64a19; -fx-text-fill: white; -fx-font-weight: bold; -fx-padding: 4 8 4 8; -fx-background-radius: 5;");
            tagHBox.getChildren().add(priceLabel);
        }
        overlayVBox.getChildren().addAll(nameLabel, brandLabel, tagHBox);
        imageStack.getChildren().addAll(imageView, overlayVBox);
        GridPane infoGrid = new GridPane();
        infoGrid.setHgap(10); infoGrid.setVgap(5);
        infoGrid.add(new Label("For..."), 0, 0);
        infoGrid.add(createStyledInfoLabel(parfum.gender != null ? parfum.gender : "-"), 0, 1);
        infoGrid.add(new Label("Year"), 1, 0);
        infoGrid.add(createStyledInfoLabel(parfum.year != null && !parfum.year.isEmpty() ? parfum.year : "-"), 1, 1);
        infoGrid.add(new Label("Rating"), 2, 0);
        infoGrid.add(createStyledInfoLabel(parfum.rating != null && !parfum.rating.isEmpty() ? "⭐ " + parfum.rating : "-"), 2, 1);
        infoGrid.add(new Label("Country"), 3, 0);
        infoGrid.add(createStyledInfoLabel(parfum.country != null && !parfum.country.isEmpty() ? parfum.country : "-"), 3, 1);
        infoGrid.getChildren().forEach(node -> {
            if (node instanceof Label && GridPane.getRowIndex(node) == 0) {
                node.setStyle("-fx-font-size: 12px; -fx-text-fill: #777;");
            }
        });
        GridPane sillageGrid = new GridPane();
        sillageGrid.setHgap(10); sillageGrid.setVgap(5);
        sillageGrid.add(new Label("Sillage"), 0, 0);
        Label sillageLabel = createStyledInfoLabel(parfum.sillage);
        sillageLabel.setStyle(sillageLabel.getStyle() + "-fx-text-fill: #43A047;");
        sillageGrid.add(sillageLabel, 0, 1);
        sillageGrid.add(new Label("Longevity"), 1, 0);
        Label longevityLabel = createStyledInfoLabel(parfum.longevity);
        longevityLabel.setStyle(longevityLabel.getStyle() + "-fx-text-fill: #43A047;");
        sillageGrid.add(longevityLabel, 1, 1);
        sillageGrid.getChildren().forEach(node -> {
            if (node instanceof Label && GridPane.getRowIndex(node) == 0) node.setStyle("-fx-font-size: 12px; -fx-text-fill: #777;");
        });
        FlowPane seasonPane = new FlowPane(5, 5);
        populateRankingPane(seasonPane, parfum.seasonRanking);
        FlowPane occasionPane = new FlowPane(5, 5);
        populateRankingPane(occasionPane, parfum.occasionRanking);
        VBox rankingVBox = new VBox(10, seasonPane, occasionPane);
        VBox detailsColumnVBox = new VBox(15, imageStack, infoGrid, sillageGrid, rankingVBox);
        return detailsColumnVBox;
    }

    /**
     * Felépíti a 2. Oszlopot (Main Accords, Notes).
     */
    private VBox buildAccordsAndNotesColumn(Parfum parfum) {
        VBox columnVBox = new VBox(20);
        VBox accordsVBox = new VBox(5);
        Label accordsTitle = new Label("Main Accords");
        accordsTitle.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");
        VBox accordBarsVBox = new VBox(5);
        populateAccordsPane(accordBarsVBox, parfum.mainAccordsPercentage);
        accordsVBox.getChildren().addAll(accordsTitle, accordBarsVBox);
        VBox notesVBox = new VBox(5);
        Label topNotesTitle = new Label("Top Notes");
        topNotesTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        FlowPane topNotesPane = new FlowPane(8, 8);
        populateNotePane(topNotesPane, parfum.notes != null ? parfum.notes.top : null);
        Label middleNotesTitle = new Label("Middle Notes");
        middleNotesTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        middleNotesTitle.setPadding(new Insets(10, 0, 0, 0));
        FlowPane middleNotesPane = new FlowPane(8, 8);
        populateNotePane(middleNotesPane, parfum.notes != null ? parfum.notes.middle : null);
        Label baseNotesTitle = new Label("Base Notes");
        baseNotesTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        baseNotesTitle.setPadding(new Insets(10, 0, 0, 0));
        FlowPane baseNotesPane = new FlowPane(8, 8);
        populateNotePane(baseNotesPane, parfum.notes != null ? parfum.notes.base : null);
        notesVBox.getChildren().addAll(topNotesTitle, topNotesPane, middleNotesTitle, middleNotesPane, baseNotesTitle, baseNotesPane);
        columnVBox.getChildren().addAll(accordsVBox, notesVBox);
        return columnVBox;
    }

    /**
     * Felépíti a 3. Oszlopot (Hasonló Parfümök), szűrés NÉLKÜL.
     */
    private VBox buildSimilarColumn(Parfum parfum) {
        VBox columnVBox = new VBox(10);
        columnVBox.setPadding(new Insets(5, 0, 0, 0));
        columnVBox.setPrefWidth(300);

        Label title = new Label("Similar Fragrances");
        title.setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        FlowPane similarPane = new FlowPane(10, 10);
        similarPane.setPrefWrapLength(300);

        columnVBox.getChildren().addAll(title, similarPane);

        // Elindítjuk az új, párhuzamosított hívást
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

    /**
     * Létrehoz egy háttérfeladatot (Task) az API híváshoz (a "Search" oldalhoz).
     */
    private Task<List<Parfum>> startApiSearchTask(String searchTerm, ListView<Parfum> resultsListView) {
        Task<List<Parfum>> apiTask = new Task<>() {
            @Override
            protected List<Parfum> call() throws Exception {
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
                    return gson.fromJson(response.body(), parfumListType);
                } else {
                    throw new Exception("API hiba: " + response.statusCode());
                }
            }
        };
        apiTask.setOnSucceeded(e -> Platform.runLater(() -> resultsListView.getItems().setAll(apiTask.getValue())));
        apiTask.setOnFailed(e -> {
            System.err.println("Hiba az API hívás során:");
            apiTask.getException().printStackTrace();
        });
        apiTask.setOnCancelled(e -> System.out.println("Keresés megszakítva: " + searchTerm));
        return apiTask;
    }

    // --- Segédfüggvények ---

    private Label createStyledInfoLabel(String text) {
        if (text == null) return new Label("-");
        Label label = new Label(text);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        return label;
    }

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
        label.setStyle(
                "-fx-background-color: #e0eafc;" +
                        "-fx-text-fill: #3366cc;" +
                        "-fx-font-weight: bold;" +
                        "-fx-padding: 4 8 4 8;" +
                        "-fx-background-radius: 15;"
        );
        return label;
    }

    // --- API HÍVÁSOK A 3. OSZLOPHOZ (PÁRHUZAMOSÍTVA) ---

    /**
     * Elindít egy többlépcsős, PÁRHUZAMOSÍTOTT API hívást a 3. oszlop feltöltéséhez.
     */
    private void loadSimilarFragrancesParallel(String parfumName, FlowPane resultsPane) {
        // 1. Lépés: Hívjuk a /similar végpontot (aszinkron)
        fetchSimilarFragranceNames(parfumName)
                .thenApply(similarNames -> {
                    // 2. Lépés: Létrehozunk egy listát a párhuzamos feladatokból
                    List<CompletableFuture<Parfum>> detailTasks = similarNames.stream()
                            .map(name -> CompletableFuture.supplyAsync(
                                    () -> fetchFragranceDetailsByName(name), executorService
                            ))
                            .collect(Collectors.toList());

                    // 3. Lépés: Megvárjuk, amíg MINDEN hívás befejeződik
                    CompletableFuture.allOf(detailTasks.toArray(new CompletableFuture[0])).join();

                    // 4. Lépés: Begyűjtjük az eredményeket
                    return detailTasks.stream()
                            .map(CompletableFuture::join)
                            .filter(parfum -> parfum != null)
                            .collect(Collectors.toList());
                })
                .thenAccept(fullParfumList -> {
                    // 5. Lépés: Visszatérés a JavaFX szálra az UI frissítéséhez
                    Platform.runLater(() -> {
                        resultsPane.getChildren().clear();
                        if (fullParfumList.isEmpty()) {
                            resultsPane.getChildren().add(new Label("Nincs hasonló parfüm."));
                        } else {
                            for (Parfum p : fullParfumList) {
                                resultsPane.getChildren().add(createSmallParfumChip(p));
                            }
                        }
                    });
                });
    }


    /**
     * (Háttérszálon futó) 1. Lépés: Lekéri a hasonló parfümök listáját (csak neveket).
     */
    private CompletableFuture<List<String>> fetchSimilarFragranceNames(String parfumName) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String encodedName = URLEncoder.encode(parfumName, StandardCharsets.UTF_8);
                String url = API_BASE_URL_SIMILAR + encodedName;
                HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("x-api-key", API_KEY).build();
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    Parfum.SimilarApiResponse apiResponse = gson.fromJson(response.body(), Parfum.SimilarApiResponse.class);
                    if (apiResponse != null && apiResponse.similarFragrances != null) {
                        return apiResponse.similarFragrances.stream()
                                .map(sim -> sim.name)
                                .collect(Collectors.toList());
                    }
                }
                return new ArrayList<String>();
            } catch (Exception e) {
                e.printStackTrace();
                return new ArrayList<String>();
            }
        }, executorService);
    }

    /**
     * (Háttérszálon futó) 2. Lépés: Lekéri egyetlen parfüm teljes adatlapját a neve alapján.
     */
    private Parfum fetchFragranceDetailsByName(String parfumName) {
        try {
            String encodedName = URLEncoder.encode(parfumName, StandardCharsets.UTF_8);
            String url = API_BASE_URL_SEARCH.replace("limit=20", "limit=1") + encodedName;

            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).header("x-api-key", API_KEY).build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                Type typeToken = new TypeToken<ArrayList<Parfum>>(){}.getType();
                List<Parfum> parfums = gson.fromJson(response.body(), typeToken);
                if (parfums != null && !parfums.isEmpty()) {
                    return parfums.get(0);
                }
            }
            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /** Létrehoz egy kis parfüm "chipet" a 3. oszlophoz. (JAVÍTVA) */
    private Node createSmallParfumChip(Parfum parfum) {
        VBox chip = new VBox(5);
        chip.setAlignment(Pos.TOP_CENTER);
        chip.setPrefWidth(90);
        chip.setStyle("-fx-cursor: hand;");

        ImageView icon = new ImageView();
        icon.setFitHeight(80);
        icon.setFitWidth(80);
        icon.setPreserveRatio(true);

        // --- JAVÍTÁS (NULL-BIZTOS ÉS HIBATŰRŐ KÉPBETÖLTÉS) ---
        try {
            if (parfum.imageUrl != null && !parfum.imageUrl.isEmpty()) {
                Image img = new Image(parfum.imageUrl.replace(".jpg", ".webp"), true);
                img.errorProperty().addListener((obs, old, err) -> {
                    if (err) System.err.println("Hiba a 'similar' chip kép betöltésekor: " + parfum.imageUrl);
                });
                icon.setImage(img);
            }
        } catch (Exception e) {
            System.err.println("Hiba a 'similar' chip URL feldolgozásakor: " + e.getMessage());
        }
        // --- JAVÍTÁS VÉGE ---

        Label nameLabel = new Label(parfum.name);
        nameLabel.setStyle("-fx-font-size: 11px; -fx-text-alignment: center; -fx-wrap-text: true;");
        nameLabel.setPrefWidth(90);

        chip.getChildren().addAll(icon, nameLabel);

        chip.setOnMouseClicked((e) -> {
            displayParfumDetails(parfum);
        });

        return chip;
    }


    // --- Segédfüggvények a 2. Oszlophoz ---

    private void populateAccordsPane(VBox pane, Map<String, String> accords) {
        if (accords != null) {
            Map<String, Integer> strengthOrder = Map.of("Dominant", 1, "Prominent", 2, "Moderate", 3, "Subtle", 4, "Trace", 5);
            String[] colors = {"#d6bcf0", "#c9899a", "#b0a0b0", "#d3a080", "#a0c0b0", "#f0a0a0", "#b0d0d0", "#f0e0a0"};
            AtomicInteger colorIndex = new AtomicInteger(0);
            List<Map.Entry<String, String>> sortedAccords = accords.entrySet().stream()
                    .sorted(Comparator.comparing(entry -> strengthOrder.getOrDefault(entry.getValue(), 99)))
                    .collect(Collectors.toList());
            for (Map.Entry<String, String> entry : sortedAccords) {
                String color = colors[colorIndex.getAndIncrement() % colors.length];
                pane.getChildren().add(createAccordBar(entry.getKey(), entry.getValue(), color));
            }
        }
    }

    private Node createAccordBar(String name, String strength, String color) {
        double barWidth = getWidthForStrength(strength);
        Pane bar = new Pane();
        bar.setPrefSize(barWidth, 30);
        bar.setStyle("-fx-background-color: " + color + "; -fx-background-radius: 15;");
        Label label = new Label(name);
        label.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: white; -fx-padding: 0 0 0 10;");
        StackPane stack = new StackPane(bar, label);
        stack.setAlignment(Pos.CENTER_LEFT);
        stack.setMaxWidth(barWidth);
        return stack;
    }

    private double getWidthForStrength(String strength) {
        if (strength == null) return 100;
        switch (strength) {
            case "Dominant":  return 300;
            case "Prominent": return 250;
            case "Moderate":  return 200;
            case "Subtle":    return 150;
            case "Trace":     return 100;
            default:          return 100;
        }
    }

    private void populateNotePane(FlowPane pane, List<Parfum.NoteDetail> notes) {
        if (notes != null) {
            for (Parfum.NoteDetail note : notes) {
                pane.getChildren().add(createNoteChip(note));
            }
        }
    }

    private Node createNoteChip(Parfum.NoteDetail note) {
        Image img = new Image(note.imageUrl, true);
        ImageView icon = new ImageView(img);
        icon.setFitHeight(30); icon.setFitWidth(30);
        Label nameLabel = new Label(note.name);
        nameLabel.setStyle("-fx-font-size: 14px;");
        HBox chip = new HBox(8, icon, nameLabel);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle("-fx-background-color: #f0f0f0; -fx-padding: 8px 12px 8px 12px; -fx-background-radius: 20px;");
        return chip;
    }

    // --- ViewFactory (Belső osztályként) ---
    private static class ViewFactory {
        public static VBox buildHomePage() {
            VBox homeVBox = new VBox(25);
            homeVBox.setPadding(new Insets(20));
            Label welcomeLabel = new Label("Üdvözlünk a Sharqi alkalmazásban!");
            welcomeLabel.setStyle("-fx-font-size: 24px; -fx-font-weight: bold;");
            Label subLabel = new Label("A tökéletes parfüm kereső.");
            subLabel.setStyle("-fx-font-size: 16px; -fx-text-fill: #555555;");
            VBox maleBox = createTop3Box("Top 3 Male Fragrance", new String[][]{
                    {"Dior Homme Intense", "https://d2k6fvhyk5xgx.cloudfront.net/images/dior-homme-intense.jpg"},
                    {"Bleu de Chanel", "https://d2k6fvhyk5xgx.cloudfront.net/images/chanel-bleu-de-chanel.jpg"},
                    {"Creed Aventus", "https://d2k6fvhyk5xgx.cloudfront.net/images/creed-aventus.jpg"}
            });
            VBox unisexBox = createTop3Box("Top 3 Unisex Fragrance", new String[][]{
                    {"MFK Baccarat Rouge 540", "https://d2k6fvhyk5xgx.cloudfront.net/images/maison-francis-kurkdjian-baccarat-rouge-540.jpg"},
                    {"Tom Ford Tobacco Vanille", "https://d2k6fvhyk5xgx.cloudfront.net/images/tom-ford-tobacco-vanille.jpg"},
                    {"YSL Babycat", "https://d2k6fvhyk5xgx.cloudfront.net/images/babycat-yves-saint-laurent-unisex.jpg"}
            });
            VBox topBoxesVBox = new VBox(20, maleBox, unisexBox);
            homeVBox.getChildren().addAll(welcomeLabel, subLabel, topBoxesVBox);
            return homeVBox;
        }

        private static VBox createTop3Box(String title, String[][] fragrances) {
            VBox box = new VBox(10);
            Label titleLabel = new Label(title);
            titleLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
            HBox cardsHBox = new HBox(15);
            for (String[] parfum : fragrances) {
                cardsHBox.getChildren().add(createPerfumeCard(parfum[0], parfum[1]));
            }
            box.getChildren().addAll(titleLabel, cardsHBox);
            return box;
        }

        private static Node createPerfumeCard(String name, String imageUrl) {
            VBox card = new VBox(5);
            card.setAlignment(Pos.TOP_CENTER);
            card.setPrefWidth(120);
            card.setStyle("-fx-padding: 5px; -fx-border-color: #eee; -fx-border-width: 1; -fx-border-radius: 5; -fx-background-radius: 5;");
            ImageView imageView = new ImageView(new Image(imageUrl, true));
            imageView.setFitHeight(100);
            imageView.setFitWidth(100);
            imageView.setPreserveRatio(true);
            Label nameLabel = new Label(name);
            nameLabel.setStyle("-fx-font-size: 13px; -fx-text-alignment: center;");
            nameLabel.setWrapText(true);
            nameLabel.setPrefHeight(40);
            card.getChildren().addAll(imageView, nameLabel);
            return card;
        }
    }
}