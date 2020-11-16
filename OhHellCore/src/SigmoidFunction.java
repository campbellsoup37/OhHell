
public class SigmoidFunction implements ActivationFunction {
    public double a(double x) {
        return 1/(1+Math.exp(-x));
    }
    
    public double da(double x) {
        return Math.exp(-x)/Math.pow(1+Math.exp(-x), 2);
    }
}
