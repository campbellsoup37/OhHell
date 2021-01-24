package ml;

import java.util.List;

public interface Vector {
    public int size();
    
    public boolean isSparse();
    
    public List<SparseVectorEntry> getEntries();
    
    public double[] toArray();
    
    public Vector applyMatrix(Matrix M);
    
    public double dot(Vector vector);
    
    public void add(Vector vector);
    
    public void add(Vector vector, double scale);
    
    public double get(int i);
    
    public double norm();
    
    public void print();
}
