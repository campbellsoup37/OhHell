package ml;

public abstract class SplitLossFunction implements LossFunction {
    public double f(double yhat, double y) {
        return 0;
    }
    
    public double df(double yhat, double y) {
        return 0;
    }

    @Override
    public double loss(Vector predicted, Vector truth) {
        if (predicted.size() != truth.size()) {
            throw new MLException("Attempted to compute loss on vectors of sizes " + predicted.size() + " and " + truth.size() + ".");
        }
        
        double[] yhatVec = predicted.toArray();
        double[] yVec = truth.toArray();
        double ans = 0;
        for (int i = 0; i < yhatVec.length; i++) {
            ans += f(yhatVec[i], yVec[i]);
        }
        return ans;
    }

    @Override
    public Vector dloss(Vector predicted, Vector truth) {
        if (predicted.size() != truth.size()) {
            throw new MLException("Attempted to compute loss on vectors of sizes " + predicted.size() + " and " + truth.size() + ".");
        }
        
        double[] yhatVec = predicted.toArray();
        double[] yVec = truth.toArray();
        double[] ans = new double[yhatVec.length];
        for (int i = 0; i < yhatVec.length; i++) {
            ans[i] = df(yhatVec[i], yVec[i]);
        }
        return new BasicVector(ans);
    }
}
