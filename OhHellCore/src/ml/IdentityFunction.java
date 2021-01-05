package ml;

public class IdentityFunction implements ActivationFunction {
    @Override
    public Vector a(Vector v) {
        return new BasicVector(v.toArray());
    }

    @Override
    public Matrix da(Vector v) {
        double[] ones = new double[v.size()];
        for (int i = 0; i < v.size(); i++) {
            ones[i] = 1;
        }
        return new DiagonalMatrix(ones);
    }
}
