package org.example.sharqi;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Parfum {

    // --- Fő mezők ---
    @SerializedName("Name")
    public String name;
    @SerializedName("Brand")
    public String brand;
    @SerializedName("Image URL")
    public String imageUrl;
    @SerializedName("Price")
    public String price;
    @SerializedName("OilType")
    public String oilType;
    @SerializedName("Gender")
    public String gender;
    @SerializedName("Year")
    public String year;
    @SerializedName("rating")
    public String rating;
    @SerializedName("Country")
    public String country;
    @SerializedName("Longevity")
    public String longevity;
    @SerializedName("Sillage")
    public String sillage;

    // --- Listák és Térképek ---
    @SerializedName("Season Ranking")
    public List<RankingItem> seasonRanking;
    @SerializedName("Occasion Ranking")
    public List<RankingItem> occasionRanking;
    @SerializedName("Main Accords Percentage")
    public Map<String, String> mainAccordsPercentage;
    @SerializedName("Notes")
    public ParfumNotes notes;

    // Ez a mező a te korábbi kódodból származik, visszatettem
    @SerializedName("General Notes")
    public List<String> generalNotes;

    // --- Belső osztályok (Nested Classes) ---

    public static class ParfumNotes {
        @SerializedName("Top") public List<NoteDetail> top;
        @SerializedName("Middle") public List<NoteDetail> middle;
        @SerializedName("Base") public List<NoteDetail> base;
    }

    public static class NoteDetail {
        @SerializedName("name") public String name;
        @SerializedName("imageUrl") public String imageUrl;
    }

    public static class RankingItem {
        @SerializedName("name") public String name;
        @SerializedName("score") public double score;
    }

    // --- EZ A RÉSZ HIÁNYZOTT: ÚJ OSZTÁLYOK A /similar VÁLASZHOZ ---
    /**
     * A /fragrances/similar API hívás teljes válaszát írja le.
     */
    public static class SimilarApiResponse {
        @SerializedName("similar_to")
        public String similarTo;

        @SerializedName("similar_fragrances")
        public List<SimilarFragrance> similarFragrances;
    }

    /**
     * Egyetlen hasonló parfüm nevét írja le a /similar válaszban.
     */
    public static class SimilarFragrance {
        @SerializedName("Name")
        public String name;
    }
    // --- ÚJ OSZTÁLYOK VÉGE ---

    // --- Segédfüggvények (Ezeket használja a Controller) ---
    public String getDominantSeason() {
        if (seasonRanking != null && !seasonRanking.isEmpty()) {
            return seasonRanking.get(0).name;
        }
        return "";
    }

    public String getDominantAccord() {
        if (mainAccordsPercentage != null && !mainAccordsPercentage.isEmpty()) {
            Optional<Map.Entry<String, String>> dominantEntry = mainAccordsPercentage.entrySet()
                    .stream()
                    .filter(entry -> "Dominant".equalsIgnoreCase(entry.getValue()))
                    .findFirst();
            if (dominantEntry.isPresent()) {
                return dominantEntry.get().getKey();
            }
        }
        return "";
    }

    @Override
    public String toString() {
        return brand + " - " + name;
    }
}