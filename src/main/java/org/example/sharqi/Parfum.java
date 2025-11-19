package org.example.sharqi;

import com.google.gson.annotations.SerializedName;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

//Szarmaztatott osztaly
public class Parfum extends Fragrance {

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


    @SerializedName("Main Accords")
    public List<String> mainAccords;

    //overlaoding ures konstruktor
    public Parfum() {
    }

    // overloading parameteres
    public Parfum(String name, String brand, String price) {
        this.name = name;
        this.brand = brand;
        this.price = price;
    }

    // overriding from abstract class
    @Override
    public String getTypeCategory() {
        return (oilType != null) ? oilType : "Unknown Type";
    }

    // to string
    @Override
    public String toString() {
        return (brand != null ? brand : "Unknown") + " - " + (name != null ? name : "Unknown");
    }

    // equals metodus
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

    // szures, ha evszak>0.6


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

// ellenorzes, megfelel e az occasionre?

    public boolean isAppropriateForOccasion(String occasionName) {
        if (occasionRanking == null || occasionName == null || occasionName.equals("Any")) return true;
        for (RankingItem item : occasionRanking) {
            if (item.name.equalsIgnoreCase(occasionName) && item.score > 0.6) {
                return true;
            }
        }
        return false;
    }

//ellenorzes; egyenlo a keresettel?
    public boolean matchesGender(String genderFilter) {
        if (gender == null || genderFilter == null || genderFilter.equals("Any")) return true;
        return gender.equalsIgnoreCase(genderFilter);
    }

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