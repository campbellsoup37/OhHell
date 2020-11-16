
public class ReLuFunction implements ActivationFunction {
    public double a(double x) {
        if(x<0) return 0;
        else return x;
    }
    
    public double da(double x) {
        if(x<0) return 0;
        else return 1;
    }
}
