package org.example.sharqi;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class Parfum {
    @SerializedName("Name")
    public String name;
    @SerializedName("Brand")
    public String brand;
    @SerializedName("Price")
    public String price;
    @SerializedName("Image URL")
    public String imageUrl;
    @SerializedName("rating")
    public String rating;
    @SerializedName("Year")
    public String year;
    @SerializedName("General Notes")
    public List<String> generalNotes;
    @SerializedName("Notes")
    public ParfumNotes notes;

    public static class ParfumNotes {
        @SerializedName("Top")
        public List<NoteDetail> top;
        @SerializedName("Middle")
        public List<NoteDetail> middle;
        @SerializedName("Base")
        public List<NoteDetail> base;
    }

    public static class NoteDetail {
        @SerializedName("name")
        public String name;
        @SerializedName("imageUrl")
        public String imageUrl;
    }
    
    @Override
    public String toString() {
        return brand + " - " + name;
    }
}


