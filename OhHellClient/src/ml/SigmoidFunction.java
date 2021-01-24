package ml;

public class SigmoidFunction extends SplitActivationFunction {
    @Override
    public double f(double x) {
        return 1 / (1 + Math.exp(-x));
    }
    
    @Override
    public double df(double x) {
        return 1 / (2 + Math.exp(x) + Math.exp(-x));
    }
}
