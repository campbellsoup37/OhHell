package ml;

import java.util.List;

public interface Vector {
    public int size();
    
    public boolean isSparse();
    
    public List<SparseVectorEntry> getEntries();
    
    public double[] toArray();
    
    public void scale(double c);
    
    public Vector applyMatrix(Matrix M);
    
    public double dot(Vector vector);
    
    public Vector add(Vector vector);
    
    public Vector add(Vector vector, double scale);
    
    public double get(int i);
    
    public double norm();
    
    public Vector copy();
    
    public boolean entrywiseEquals(Vector v);
    
    public void print();
}
