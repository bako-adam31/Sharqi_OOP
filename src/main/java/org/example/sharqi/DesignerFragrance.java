package org.example.sharqi;

// 8. pont: Származtatott osztály (3/3)
public class DesignerFragrance extends Parfum {
    public DesignerFragrance(String name, String brand) {
        super(name, brand, "High End");
    }

    @Override
    public String getTypeCategory() {
        return "Designer";
    }
}