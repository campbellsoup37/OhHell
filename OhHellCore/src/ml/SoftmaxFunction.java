package ml;

public class SoftmaxFunction implements ActivationFunction {
    private Vector memo;
    private double denom;
    
    public void memoize(Vector v) {
        if (memo != v) {
            double[] vec = v.toArray();
            memo = v;
            denom = 0;
            for (double x : vec) {
                denom += Math.exp(x);
            }
        }
    }
    
    @Override
    public Vector a(Vector v) {
        memoize(v);
        double[] vec = v.toArray();
        double[] ans = new double[vec.length];
        for (int i = 0; i < vec.length; i++) {
            ans[i] = Math.exp(vec[i]) / denom;
        }
        return new BasicVector(ans);
    }

    @Override
    public Matrix da(Vector v) {
        memoize(v);
        double[] vec = v.toArray();
        double[][] ans = new double[vec.length][vec.length];
        for (int i = 0; i < vec.length; i++) {
            for (int j = 0; j < vec.length; j++) {
                if (i == j) {
                    ans[i][j] = Math.exp(vec[i]) / denom - Math.exp(2 * vec[i]) / Math.pow(denom, 2);
                } else {
                    ans[i][j] = -Math.exp(vec[i] + vec[j]) / Math.pow(denom, 2);
                }
            }
        }
        return new BasicMatrix(ans);
    }
}
