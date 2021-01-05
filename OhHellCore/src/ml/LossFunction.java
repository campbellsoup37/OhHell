package ml;

public interface LossFunction {
    public double loss(Vector predicted, Vector truth);
    
    public Vector dloss(Vector predicted, Vector truth);
}
