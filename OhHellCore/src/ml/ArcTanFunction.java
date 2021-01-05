package ml;

public class ArcTanFunction extends SplitActivationFunction {
    @Override
    public double f(double x) {
        return Math.atan(x);
    }
    
    @Override
    public double df(double x) {
        return 1 / (1 + x * x);
    }
}
