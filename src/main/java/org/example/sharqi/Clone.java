package org.example.sharqi;

//Szarmaztatott osztaly
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