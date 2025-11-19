package org.example.sharqi;

// 8. pont: Származtatott osztály (2/3)
public class Clone extends Parfum {
    private String inspiration;

    public Clone(String name, String brand, String inspiration) {
        super(name, brand, "Budget Friendly");
        this.inspiration = inspiration;
    }

    @Override
    public String getTypeCategory() {
        return "Dupe / Clone";
    }
}