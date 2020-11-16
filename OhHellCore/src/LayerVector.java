
public interface LayerVector {
    public int size();
    
    public double[] toArray();
    
    public LayerVector applyMatrix(double[][] M);
    
    public LayerVector applyActivation(ActivationFunction af);
    
    public double[] applyDActivation(ActivationFunction af);
    
    public double[][][] makeNewDw(double[] newDValue);
    
    public LayerVector add(double[] vec);
    
    public double[] minus(LayerVector vec);
    
    public double get(int i);
    
    public void print();
}
