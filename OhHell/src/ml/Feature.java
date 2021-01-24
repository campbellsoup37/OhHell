package ml;

import java.util.Arrays;

public class Feature {
    private String name = "";
    private int dimension = 0;
    private String[] subNames;
    
    public Feature(String name, String[] subNames) {
        this.setName(name);
        dimension = subNames.length;
        this.setSubNames(subNames);
    }
    
    public Feature(String name, int l0, int l1) {
        if (l1 - l0 <= 0) {
            throw new MLException("Invalid range from " + l0 + " to " + l1 + ".");
        }
        this.setName(name);
        dimension = l1 - l0;
        subNames = new String[dimension];
        for (int l = l0; l < l1; l++) {
            subNames[l - l0] = l + "";
        }
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getDimension() {
        return dimension;
    }

    public void setDimension(int dimension) {
        this.dimension = dimension;
    }

    public String[] getSubNames() {
        return subNames;
    }

    public void setSubNames(String[] subNames) {
        this.subNames = subNames;
    }
    
    @Override
    public String toString() {
        return name + " (" + dimension + ") : " + Arrays.toString(subNames);
    }
}
