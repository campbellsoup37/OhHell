package ml;

public abstract class SplitActivationFunction implements ActivationFunction {
    public double f(double x) {
        return 0;
    }
    
    public double df(double x) {
        return 0;
    }
    
    @Override
    public Vector a(Vector v) {
        double[] vec = v.toArray();
        double[] ans = new double[vec.length];
        for (int i = 0; i < vec.length; i++) {
            ans[i] = f(vec[i]);
        }
        return new BasicVector(ans);
    }

    @Override
    public Matrix da(Vector v) {
        double[] ans = new double[v.size()];
        
        if (v.isSparse()) {
            for (SparseVectorEntry sve : v.getEntries()) {
                ans[sve.key()] = df(sve.value());
            }
        } else {
            double[] vec = v.toArray();
            for (int i = 0; i < vec.length; i++) {
                ans[i] = df(vec[i]);
            }
        }
        
        return new DiagonalMatrix(ans);
    }
}
