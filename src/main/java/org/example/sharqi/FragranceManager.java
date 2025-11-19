package org.example.sharqi;

import java.util.ArrayList;
import java.util.List;

// 5. pont: Ez az 5. saját osztály (Parfum, Controller, App, Launcher mellett)
public class FragranceManager {

    // 9. pont: Statikus attribútum
    public static final String APP_VERSION = "1.0.0";

    // 9. pont: Statikus metódus
    public static void printAppVersion() {
        System.out.println("Sharqi App Version: " + APP_VERSION);
    }

    // 13. pont: Polimorf lista használata és paraméterátadás
    // Ez a metódus elfogad Parfum-öt, Clone-t és DesignerFragrance-t is, mert mindegyik Parfum leszármazott
    public static void processFragrances(List<Parfum> fragranceList) {
        System.out.println("Processing " + fragranceList.size() + " fragrances...");
        for (Parfum p : fragranceList) {
            // Polimorfizmus: a futás közbeni típus határozza meg, melyik getTypeCategory() fut le
            System.out.println("Processing: " + p.getDisplayName() + " | Type: " + p.getTypeCategory());
        }
    }
}