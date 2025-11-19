package org.example.sharqi;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.TilePane;
import javafx.scene.layout.VBox;

public class ClonePageManager {

    // Ez a metódus adja vissza a kész oldalt (VBox), amit a Controller csak megjelenít
    public VBox buildClonePage() {
        VBox clonePageVBox = new VBox(20);
        clonePageVBox.setPadding(new Insets(30));
        clonePageVBox.setStyle("-fx-background-color: transparent;");

        Label title = new Label("Budget Friendly Alternatives (Clones)");
        title.getStyleClass().add("title-label");
        title.setStyle("-fx-text-fill: #333; -fx-font-weight: bold; -fx-font-size: 24px;");

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
                }
        };

        TilePane gridContainer = new TilePane();
        gridContainer.setHgap(20);
        gridContainer.setVgap(20);
        gridContainer.setPrefColumns(2);
        gridContainer.setAlignment(Pos.TOP_CENTER);
        gridContainer.setStyle("-fx-background-color: transparent;");

        for (String[] pair : clonePairs) {
            HBox row = createCloneRow(pair[0], pair[1], pair[2], pair[3]);
            gridContainer.getChildren().add(row);
        }

        ScrollPane scrollPane = new ScrollPane(gridContainer);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background-color: transparent; -fx-background: transparent;");

        clonePageVBox.getChildren().addAll(title, scrollPane);
        return clonePageVBox;
    }

    private HBox createCloneRow(String cloneName, String cloneImgUrl, String originalName, String originalImgUrl) {
        HBox row = new HBox(25);
        row.setAlignment(Pos.CENTER);
        row.setPadding(new Insets(20));

        row.setPrefWidth(560);
        row.setMaxWidth(560);
        row.setStyle("-fx-background-color: white; -fx-background-radius: 20; -fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.1), 10, 0, 0, 5);");

        // Bal oldal: Clone
        VBox cloneCard = createBigPerfumeCard(cloneName, cloneImgUrl);
        Label cloneLabel = new Label("CLONE");
        cloneLabel.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");

        VBox leftBox = new VBox(10, cloneLabel, cloneCard);
        leftBox.setAlignment(Pos.CENTER);

        // Közép
        Label vsLabel = new Label("dupe of");
        vsLabel.setStyle("-fx-text-fill: #999; -fx-font-style: italic; -fx-font-size: 16px; -fx-font-weight: bold;");

        // Jobb oldal: Original
        VBox originalCard = createBigPerfumeCard(originalName, originalImgUrl);
        Label origLabel = new Label("ORIGINAL");
        origLabel.setStyle("-fx-background-color: #333; -fx-text-fill: white; -fx-padding: 4 10; -fx-background-radius: 6; -fx-font-weight: bold; -fx-font-size: 12px;");

        VBox rightBox = new VBox(10, origLabel, originalCard);
        rightBox.setAlignment(Pos.CENTER);

        row.getChildren().addAll(leftBox, vsLabel, rightBox);
        row.setCursor(javafx.scene.Cursor.HAND);

        return row;
    }

    private VBox createBigPerfumeCard(String name, String imageUrl) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.TOP_CENTER);
        card.setPrefWidth(170);
        card.getStyleClass().add("perfume-card");

        ImageView imageView = new ImageView();
        imageView.setFitHeight(150);
        imageView.setFitWidth(150);
        imageView.setPreserveRatio(true);

        try {
            imageView.setImage(new Image(imageUrl, 150, 150, true, true, true));
        } catch (Exception e) { }

        Label nameLabel = new Label(name);
        nameLabel.getStyleClass().add("card-label");
        nameLabel.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #333;");
        nameLabel.setWrapText(true);
        nameLabel.setTextAlignment(javafx.scene.text.TextAlignment.CENTER);

        card.getChildren().addAll(imageView, nameLabel);
        return card;
    }
}