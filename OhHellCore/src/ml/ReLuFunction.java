package ml;

public class ReLuFunction extends SplitActivationFunction {
    @Override
    public double f(double x) {
        return x < 0 ? 0 : x;
    }
    
    @Override
    public double df(double x) {
        return x < 0 ? 0 : 1;
    }
}
