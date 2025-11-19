package org.example.sharqi;

import java.util.ArrayList;
import java.util.List;

public class FragranceManager {

    public static final String APP_VERSION = "1.0.0";

    public static void printAppVersion() {
        System.out.println("Sharqi App Version: " + APP_VERSION);
    }

    public static void processFragrances(List<Parfum> fragranceList) {
        System.out.println("Processing " + fragranceList.size() + " fragrances...");
        for (Parfum p : fragranceList) {
            System.out.println("Processing: " + p.getDisplayName() + " | Type: " + p.getTypeCategory());
        }
    }
}