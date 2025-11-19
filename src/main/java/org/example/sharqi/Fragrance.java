package org.example.sharqi;

public abstract class Fragrance implements Displayable {
    protected String name;
    protected String brand;

    public void printInfo(){

        System.out.println("Fragrance: " + name + "by" + brand);
    }

    public abstract String getTypeCategory();

    @Override
    public String getDisplayName(){

        return name;
    }

    @Override
    public String getBrandName(){

        return brand;
    }
}
