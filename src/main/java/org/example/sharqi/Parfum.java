package org.example.sharqi;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

// 8. pont: Származtatott osztály (1/3) - Parfum örököl a Fragrance-ból
public class Parfum extends Fragrance {

    @SerializedName("Name")
    public String name;  // Ez felülírja (shadow) az ősosztály name mezőjét, hogy a GSON lássa

    @SerializedName("Brand")
    public String brand; // Ez felülírja az ősosztály brand mezőjét

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
    @SerializedName("Season Ranking")
    public List<RankingItem> seasonRanking;
    @SerializedName("Occasion Ranking")
    public List<RankingItem> occasionRanking;
    @SerializedName("Main Accords Percentage")
    public Map<String, String> mainAccordsPercentage;
    @SerializedName("Notes")
    public ParfumNotes notes;
    @SerializedName("General Notes")
    public List<String> generalNotes;
    @SerializedName("Occasion")
    public String occasion;

    // --- EZT A RÉSZT JAVÍTSD ÁT: ---
    @SerializedName("Main Accords")
    public List<String> mainAccords; // String helyett List<String> legyen!

    // 10. pont: Túlterhelt konstruktor (Overloading) - 1. Üres konstruktor
    public Parfum() {
    }

    // 10. pont: Túlterhelt konstruktor (Overloading) - 2. Paraméteres
    public Parfum(String name, String brand, String price) {
        this.name = name;
        this.brand = brand;
        this.price = price;
    }

    // 12. pont: Felülírt metódus (Overriding) az absztrakt osztályból
    @Override
    public String getTypeCategory() {
        return (oilType != null) ? oilType : "Unknown Type";
    }

    // 14. pont: Felülírt toString()
    @Override
    public String toString() {
        return (brand != null ? brand : "Unknown") + " - " + (name != null ? name : "Unknown");
    }

    // 15. pont: equals() metódusra példa
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Parfum parfum = (Parfum) o;
        return Objects.equals(name, parfum.name) && Objects.equals(brand, parfum.brand);
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, brand);
    }

    // --- Belső osztályok ---
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

    public static class SimilarApiResponse {
        @SerializedName("similar_to") public String similarTo;
        @SerializedName("similar_fragrances") public List<SimilarFragrance> similarFragrances;
    }

    public static class SimilarFragrance {
        @SerializedName("Name") public String name;
    }
// --- SEGÉDFÜGGVÉNYEK A SZŰRÉSHEZ ---

    /**
     * Ellenőrzi, hogy a parfüm megfelelő-e a megadott évszakra.
     * Akkor tekintjük megfelelőnek, ha az adott évszak pontszáma magas (> 0.6).
     */
    public boolean isAppropriateForSeason(String seasonName) {
        if (seasonRanking == null || seasonName == null || seasonName.equals("Any")) return true;
        for (RankingItem item : seasonRanking) {
            // Ha megtaláljuk a keresett évszakot és a pontszáma jó
            if (item.name.equalsIgnoreCase(seasonName) && item.score > 0.6) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ellenőrzi, hogy a parfüm megfelelő-e a megadott alkalomra.
     */
    public boolean isAppropriateForOccasion(String occasionName) {
        if (occasionRanking == null || occasionName == null || occasionName.equals("Any")) return true;
        for (RankingItem item : occasionRanking) {
            if (item.name.equalsIgnoreCase(occasionName) && item.score > 0.6) {
                return true;
            }
        }
        return false;
    }

    /**
     * Ellenőrzi, hogy a parfüm neme egyezik-e a keresettel.
     */
    public boolean matchesGender(String genderFilter) {
        if (gender == null || genderFilter == null || genderFilter.equals("Any")) return true;
        return gender.equalsIgnoreCase(genderFilter);
    }

    // --- Egyéb segédek ---
    public String getDominantSeason() {
        if (seasonRanking != null && !seasonRanking.isEmpty()) return seasonRanking.get(0).name;
        return "";
    }

    public String getDominantAccord() {
        if (mainAccordsPercentage != null && !mainAccordsPercentage.isEmpty()) {
            Optional<Map.Entry<String, String>> dominantEntry = mainAccordsPercentage.entrySet()
                    .stream().filter(entry -> "Dominant".equalsIgnoreCase(entry.getValue())).findFirst();
            if (dominantEntry.isPresent()) return dominantEntry.get().getKey();
        }
        return "";
    }
}