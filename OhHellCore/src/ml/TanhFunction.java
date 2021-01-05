package ml;

public class TanhFunction extends SplitActivationFunction {
    @Override
    public double f(double x) {
        return Math.tanh(x);
    }
    
    @Override
    public double df(double x) {
        return Math.pow(Math.cosh(x), -2);
    }
}
