package org.example.sharqi;

//Szarmaztatott osztaly
public class DesignerFragrance extends Parfum {
    public DesignerFragrance(String name, String brand) {
        super(name, brand, "High End");
    }

    @Override
    public String getTypeCategory() {
        return "Designer";
    }
}