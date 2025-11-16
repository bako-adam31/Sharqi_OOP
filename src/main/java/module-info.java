// Helyes tartalom a module-info.java fájlhoz

module org.example.sharqi {
    // Standard JavaFX modulok
    requires javafx.controls;
    requires javafx.fxml;

    // Szükséges a beépített HttpClient-hez (API hívások)
    requires java.net.http;

    // Szükséges a Google Gson könyvtárhoz (JSON feldolgozás)
    requires com.google.gson;

    // Megnyitjuk a csomagot az FXML-nek (hogy elérje az @FXML mezőket)
    // és a Gson-nak (hogy elérje a Parfum osztály mezőit)
    opens org.example.sharqi to javafx.fxml, com.google.gson;

    // Exportáljuk a fő osztályt, hogy elindítható legyen
    exports org.example.sharqi;
}