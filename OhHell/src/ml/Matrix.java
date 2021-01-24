package ml;

public interface Matrix {
    public int numRows();
    
    public int numCols();
    
    public double[][] toArray();
    
    public double get(int i, int j);
    
    public Vector applyVector(Vector vector);
    
    public void add(Matrix matrix);
    
    public void add(Matrix matrix, double scale);
    
    public Vector scaledCol(int j, double scale);
    
    public double norm();
}
