package ml;

public class MeanSquaredError extends SplitLossFunction {
    @Override
    public double f(double yhat, double y) {
        return Math.pow(yhat - y, 2);
    }

    @Override
    public double df(double yhat, double y) {
        return (double) 2 * (yhat - y);
    }
}
