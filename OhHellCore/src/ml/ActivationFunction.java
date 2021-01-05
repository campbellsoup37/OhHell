package ml;

public interface ActivationFunction {
    public Vector a(Vector v);
    
    public Matrix da(Vector v);
}