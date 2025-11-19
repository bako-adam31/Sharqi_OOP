package org.example.sharqi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
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
import javafx.scene.control.ComboBox;
import java.util.function.Consumer;
import java.util.function.Function;

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

    // ... (Konstansok és FXML változók változatlanok) ...
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
        VBox searchPageVBox = new VBox(15);
        searchPageVBox.setPadding(new Insets(20));

        Label title = new Label("Parfüm Keresése");
        title.getStyleClass().add("title-label"); // CSS

        TextField pageSearchField = new TextField();
        pageSearchField.setPromptText("Keresés parfümre vagy márkára...");
        pageSearchField.getStyleClass().add("search-field"); // CSS

        ListView<Parfum> pageResultsListView = new ListView<>();
        VBox.setVgrow(pageResultsListView, Priority.ALWAYS);

        searchPageVBox.getChildren().addAll(title, pageSearchField, pageResultsListView);

        searchDebouncer = new PauseTransition(Duration.millis(400));
        searchDebouncer.setOnFinished(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent e) {
                String searchTerm = pageSearchField.getText();
                if (searchTerm != null && searchTerm.trim().length() > 2) {
                    if (currentApiTask != null && currentApiTask.isRunning()) {
                        currentApiTask.cancel();
                    }
                    currentApiTask = startApiSearchTask(searchTerm, pageResultsListView);
                    executorService.submit(currentApiTask);
                }
            }
        });
        pageSearchField.textProperty().addListener(new ChangeListener<String>() {
            @Override
            public void changed(ObservableValue<? extends String> obs, String oldVal, String newVal) {
                searchDebouncer.stop();
                searchDebouncer.playFromStart();
            }
        });

        pageResultsListView.getSelectionModel().selectedItemProperty().addListener(
                new ChangeListener<Parfum>() {
                    @Override
                    public void changed(ObservableValue<? extends Parfum> obs, Parfum oldSelection, Parfum newSelection) {
                        if (newSelection != null) {
                            displayParfumDetails(newSelection);
                        }
                    }
                }
        );

        contentPane.getChildren().setAll(searchPageVBox);
        AnchorPane.setTopAnchor(searchPageVBox, 0.0);
        AnchorPane.setBottomAnchor(searchPageVBox, 0.0);
        AnchorPane.setLeftAnchor(searchPageVBox, 0.0);
        AnchorPane.setRightAnchor(searchPageVBox, 0.0);
    }


    private void displayParfumDetails(Parfum parfum) {
        VBox column1 = buildDetailsColumn(parfum);
        VBox column2 = buildAccordsAndNotesColumn(parfum);
        VBox column3 = buildSimilarColumn(parfum);

        HBox masterLayout = new HBox(20);
        masterLayout.setPadding(new Insets(20));
        masterLayout.getChildren().addAll(column1, column2, column3);

        contentPane.getChildren().setAll(masterLayout);
        AnchorPane.setTopAnchor(masterLayout, 0.0);
        AnchorPane.setLeftAnchor(masterLayout, 0.0);
    }

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
        GridPane filterGrid = new GridPane();
        filterGrid.setHgap(10); filterGrid.setVgap(10); filterGrid.setPadding(new Insets(10));
        filterGrid.setMinWidth(300);

        // 1. Main Accord
        Label accordLabel = new Label("Main Accord:"); accordLabel.setStyle("-fx-font-weight: bold;");
        TextField accordField = new TextField(); accordField.setPromptText("Pl. leather");
        filterGrid.add(accordLabel, 0, 0); filterGrid.add(accordField, 1, 0);

        // 2. Top Note
        Label topLabel = new Label("Top Note:"); topLabel.setStyle("-fx-font-weight: bold;");
        TextField topField = new TextField(); topField.setPromptText("Pl. bergamot");
        filterGrid.add(topLabel, 0, 1); filterGrid.add(topField, 1, 1);

        // 3. Middle Note
        Label middleLabel = new Label("Middle Note:"); middleLabel.setStyle("-fx-font-weight: bold;");
        TextField middleField = new TextField(); middleField.setPromptText("Pl. jasmine");
        filterGrid.add(middleLabel, 0, 2); filterGrid.add(middleField, 1, 2);

        // 4. Base Note
        Label baseLabel = new Label("Base Note:"); baseLabel.setStyle("-fx-font-weight: bold;");
        TextField baseField = new TextField(); baseField.setPromptText("Pl. amber");
        filterGrid.add(baseLabel, 0, 3); filterGrid.add(baseField, 1, 3);

        Button findButton = new Button("Find Matches");
        findButton.setMaxWidth(Double.MAX_VALUE);
        filterGrid.add(findButton, 0, 4, 2, 1);

        VBox filterVBox = new VBox(15, new Label("Filter by Notes & Accords"), filterGrid);
        filterVBox.setPadding(new Insets(20));
        ((Label)filterVBox.getChildren().get(0)).setStyle("-fx-font-size: 20px; -fx-font-weight: bold;");

        VBox resultsVBox = new VBox(10);
        resultsVBox.setPadding(new Insets(20));
        Label resultsTitle = new Label("Matching Perfumes (max 6)");
        resultsTitle.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");

        FlowPane resultsPane = new FlowPane(10, 10);
        resultsPane.setPrefWrapLength(500);
        resultsVBox.getChildren().addAll(resultsTitle, resultsPane);

        HBox filterPageLayout = new HBox(20, filterVBox, resultsVBox);

        // MÓDOSÍTOTT HÍVÁS (Kivettük a Gender/Season/Occasion argumentumokat)
        findButton.setOnAction(e -> startApiMatchTask(
                accordField.getText(),
                topField.getText(),
                middleField.getText(),
                baseField.getText(),
                resultsPane
        ));

        contentPane.getChildren().setAll(filterPageLayout);
        AnchorPane.setTopAnchor(filterPageLayout, 0.0);
        AnchorPane.setLeftAnchor(filterPageLayout, 0.0);
    }

    // --- MÓDOSÍTOTT API MATCH TASK (Csak API hívás, nincs utólagos szűrés) ---
    private Task<List<Parfum>> startApiMatchTask(String accord, String top, String middle, String base, FlowPane resultsPane) {
        resultsPane.getChildren().clear(); resultsPane.getChildren().add(new Label("Keresés..."));
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
            if (m == null || m.isEmpty()) resultsPane.getChildren().add(new Label("Nincs találat."));
            else m.forEach(p -> resultsPane.getChildren().add(createSmallParfumChip(p)));
        }));
        matchTask.setOnFailed(e -> {
            Platform.runLater(() -> { resultsPane.getChildren().clear(); resultsPane.getChildren().add(new Label("Hiba.")); });
            matchTask.getException().printStackTrace();
        });
        executorService.submit(matchTask);
        return matchTask;
    }

    // ... (A ParfumController többi része változatlan) ...




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
            VBox homeVBox = new VBox(25);
            homeVBox.setPadding(new Insets(20));
            Label welcomeLabel = new Label("Üdvözlünk a Sharqi alkalmazásban!");
            welcomeLabel.getStyleClass().add("title-label"); // CSS
            Label subLabel = new Label("A tökéletes parfüm kereső.");
            subLabel.getStyleClass().add("subtitle-label"); // CSS

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
            VBox topBoxesVBox = new VBox(20, maleBox, unisexBox);
            homeVBox.getChildren().addAll(welcomeLabel, subLabel, topBoxesVBox);
            return homeVBox;
        }

        private static VBox createTop3Box(String title, String[][] fragrances) {
            VBox box = new VBox(10);
            Label titleLabel = new Label(title);
            titleLabel.getStyleClass().add("subsection-title"); // CSS
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
            card.getStyleClass().add("perfume-card"); // CSS
            ImageView imageView = new ImageView(new Image(imageUrl, true));
            imageView.setFitHeight(100); imageView.setFitWidth(100); imageView.setPreserveRatio(true);
            Label nameLabel = new Label(name);
            nameLabel.getStyleClass().add("card-label"); // CSS
            nameLabel.setWrapText(true); nameLabel.setPrefHeight(40);
            card.getChildren().addAll(imageView, nameLabel);
            return card;
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
                        "Afnan 9pm",
                        "https://p1.prod.trwd.eu/exported/products/43644/afnan-9-pm-parfum-ferfiaknak-d2k6fvhyk5xgx-a99a9906-668f-46ef-b45e-8c42e6d102a5.jpg",
                        "JPG Ultra Male",
                        "https://p1.prod.trwd.eu/exported/products/8073/jean-paul-gaultier-ultra-male-eau-de-toilette-ferfiaknak-d2k6fvhyk5xgx-668e753d-b72b-428e-8c10-641552f6b212.jpg"
                },
                {
                        "Lattafa The Kingdom",
                        "https://m.media-amazon.com/images/I/61+k+2y+4yL._SL1000_.jpg",
                        "JPG Le Male Le Parfum",
                        "https://p1.prod.trwd.eu/exported/products/17853/jean-paul-gaultier-le-male-le-parfum-parfum-ferfiaknak-d2k6fvhyk5xgx-0627928a-1f6a-43a9-9972-91053235e263.jpg"
                },
                {
                        "FA After Effect",
                        "https://www.fragrantica.com/designers/Fragrance-World.png",
                        "Initio Side Effect",
                        "https://p1.prod.trwd.eu/exported/products/22956/initio-side-effect-parfum-unisex-d2k6fvhyk5xgx-e81f8d65-9c6a-4324-9798-34547135c504.jpg"
                },
                {
                        "FA Liquid Brun",
                        "https://www.fragrantica.com/designers/Fragrance-World.png",
                        "PdM Althair",
                        "https://p1.prod.trwd.eu/exported/products/60702/parfums-de-marly-althair-parfum-ferfiaknak-d2k6fvhyk5xgx-3d983b3a-1208-4d14-872a-411244071002.jpg"
                }
        };

        VBox listContainer = new VBox(15); // Ebben lesznek a sorok

        for (String[] pair : clonePairs) {
            HBox row = createCloneRow(pair[0], pair[1], pair[2], pair[3]);
            listContainer.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(listContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        clonePageVBox.getChildren().addAll(title, scrollPane);

        contentPane.getChildren().setAll(clonePageVBox);
        AnchorPane.setTopAnchor(clonePageVBox, 0.0);
        AnchorPane.setBottomAnchor(clonePageVBox, 0.0);
        AnchorPane.setLeftAnchor(clonePageVBox, 0.0);
        AnchorPane.setRightAnchor(clonePageVBox, 0.0);
    }

    // Segédmetódus egy sor létrehozásához
    private HBox createCloneRow(String cloneName, String cloneImgUrl, String originalName, String originalImgUrl) {
        HBox row = new HBox(20);
        row.setAlignment(Pos.CENTER_LEFT);
        row.setPadding(new Insets(10));
        row.setStyle("-fx-background-color: white; -fx-background-radius: 10; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 5, 0, 0, 2);");

        // Bal oldal: Clone
        VBox cloneCard = (VBox) ViewFactory.createPerfumeCard(cloneName, cloneImgUrl);
        Label cloneLabel = new Label("CLONE");
        cloneLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 2 5 2 5; -fx-background-radius: 3; -fx-font-size: 10px;");

        VBox leftBox = new VBox(5, cloneLabel, cloneCard);
        leftBox.setAlignment(Pos.CENTER);

        // Közép: Nyíl vagy "Vs." szöveg
        Label vsLabel = new Label("dupe of");
        vsLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic;");

        // Jobb oldal: Original
        VBox originalCard = (VBox) ViewFactory.createPerfumeCard(originalName, originalImgUrl);
        Label origLabel = new Label("ORIGINAL");
        origLabel.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 2 5 2 5; -fx-background-radius: 3; -fx-font-size: 10px;");

        VBox rightBox = new VBox(5, origLabel, originalCard);
        rightBox.setAlignment(Pos.CENTER);

        row.getChildren().addAll(leftBox, vsLabel, rightBox);
        return row;
    }


}