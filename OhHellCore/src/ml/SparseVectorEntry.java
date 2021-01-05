package ml;

public class SparseVectorEntry {
    private int key;
    private double value;
    
    public SparseVectorEntry(int key, double value) {
        this.key = key;
        this.value = value;
    }
    
    public int key() {
        return key;
    }
    
    public double value() {
        return value;
    }
}