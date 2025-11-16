package org.example.sharqi;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ListView;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;

import java.lang.reflect.Type;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.geometry.Pos;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.Node;

public class ParfumController {
    private static final String API_KEY = "fd8094dfde51e2499d3f92a850057230d6bc59e890a431e31cd42997c55e4930";
    private static final String API_BASE_URL = "https://api.fragella.com/api/v1/fragrances?limit=100&search=";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final Gson gson = new Gson();

    @FXML
    private TextField searchField;
    @FXML
    private Button searchButton;
    @FXML
    private ProgressIndicator loadingIndicator;
    @FXML
    private ListView<Parfum> resultsListView;
    @FXML
    private ImageView detailImageView;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailPriceLabel;
    @FXML
    private FlowPane topNotesPane;
    @FXML
    private FlowPane middleNotesPane;
    @FXML
    private FlowPane baseNotesPane;




    public void initialize() {
        loadingIndicator.setVisible(false);

        resultsListView.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldSelection, newSelection) -> {
                    if(newSelection != null){
                        displayParfumDetails(newSelection);
                    }else{
                        clearParfumDetails();
                    }
                }
        );
        clearParfumDetails();
    }

    @FXML
    protected void handleSearchClick(){
        String searchTerm = searchField.getText();
        if (searchTerm == null || searchTerm.trim().isEmpty()) {
            return;
        }
        startApiCallTask(searchTerm);
    }

    private void startApiCallTask(String saerchTerm){
        Task<List<Parfum>> apiTask = new Task<>() {
            protected List<Parfum> call() throws Exception {
                String encodedSearchTerm = URLEncoder.encode(saerchTerm, StandardCharsets.UTF_8);
                String finalApiUrl = API_BASE_URL + encodedSearchTerm;
                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(finalApiUrl))
                        .header("x-api-key", API_KEY)
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if(response.statusCode() == 200){
                    String jsonBody = response.body();
                    Type parfumListType = new TypeToken<ArrayList<Parfum>>(){}.getType();
                    List<Parfum> parfumok = gson.fromJson(jsonBody, parfumListType);
                    return parfumok;
                } else {
                    throw new Exception("API hiba: " + response.statusCode());
                }
            }
        };

        apiTask.setOnRunning(e -> {
                    loadingIndicator.setVisible(true);
                    searchButton.setDisable(true);
                    resultsListView.getItems().clear();
        });

        apiTask.setOnSucceeded(e -> {
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);
            List<Parfum> talalatok = apiTask.getValue();
            resultsListView.getItems().setAll(talalatok);
        });

        apiTask.setOnFailed(e -> {
            loadingIndicator.setVisible(false);
            searchButton.setDisable(false);

            System.err.println("Hiba az API hivasa soran");
            apiTask.getException().printStackTrace();
        });

        new Thread(apiTask).start();
    }


    private void displayParfumDetails(Parfum parfum){

        System.out.println("------------------------");
        System.out.println("Kivalasztott parfum: + parfum.name");
        System.out.println("Kapott url: " + parfum.imageUrl);
        detailNameLabel.setText(parfum.name);
        detailPriceLabel.setText(parfum.brand + " | " + parfum.price + " USD");
/*
        if(parfum.generalNotes != null && !parfum.generalNotes.isEmpty()){
            StringBuilder notesText = new StringBuilder();
            for (String note : parfum.generalNotes){
                notesText.append("- ").append(note).append("\n");
            }
            detailNotesArea.setText(notesText.toString());
        }else{
            detailNotesArea.setText("Nincsenek elerheto illatjegy adatok.");
        }
*/

        if(parfum.imageUrl != null && !parfum.imageUrl.isEmpty()){
            String originalUrl = parfum.imageUrl;
            Image image = new Image(originalUrl, true);

            image.errorProperty().addListener((obs, oldError, newError) -> {
                if(newError){
                    System.err.println("HIBA A KEP BETOLTESEKOR: " + originalUrl);
                    image.getException().printStackTrace();
                }
            });
            detailImageView.setFitWidth(300);
            detailImageView.setFitHeight(300);
            detailImageView.setPreserveRatio(true);

            detailImageView.setImage(image);
        }else {
            detailImageView.setImage(null);
        }

        clearNotePanes();

        if(parfum.notes != null){
            if(parfum.notes.top != null){
                for(Parfum.NoteDetail note : parfum.notes.top){
                    topNotesPane.getChildren().add(createNoteChip(note));
                }
            }
            if(parfum.notes.middle != null){
                for(Parfum.NoteDetail note : parfum.notes.middle){
                    middleNotesPane.getChildren().add(createNoteChip(note));
                }
            }
            if(parfum.notes.base != null){
                for(Parfum.NoteDetail note : parfum.notes.base){
                    baseNotesPane.getChildren().add(createNoteChip(note));
                }
            }
        }
    }

    private void clearParfumDetails(){
        detailNameLabel.setText("Valassz egy parfumot");
        detailPriceLabel.setText("Marka es Ar");
        detailImageView.setImage(null);

        if(topNotesPane != null) topNotesPane.getChildren().clear();
        if(middleNotesPane != null) middleNotesPane.getChildren().clear();
        if(baseNotesPane != null) baseNotesPane.getChildren().clear();
    }


    private Node createNoteChip(Parfum.NoteDetail note){
        Image img = new Image(note.imageUrl, true);
        ImageView icon = new ImageView(img);
        icon.setFitHeight(30);
        icon.setFitWidth(30);
        Label nameLabel = new Label(note.name);
        nameLabel.setStyle("-fx-font-size: 14px;");

        HBox chip = new HBox(5, icon, nameLabel);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setStyle(
                "-fx-background-color: #f0f0f0;" +
                        "-fx-padding: 8px 12px 8px 12px;" +
                        "-fx-background-radius: 15px;"

        );
        return chip;
    }

    private void clearNotePanes(){
        if(topNotesPane != null) topNotesPane.getChildren().clear();
        if(middleNotesPane != null) middleNotesPane.getChildren().clear();
        if(baseNotesPane != null) baseNotesPane.getChildren().clear();
    }






}
