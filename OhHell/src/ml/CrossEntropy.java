package ml;

public class CrossEntropy extends SplitLossFunction {
    @Override
    public double f(double yhat, double y) {
        return -y * Math.log(yhat);
    }

    @Override
    public double df(double yhat, double y) {
        if (y == 0) {
            return 0;
        } else {
            return -y / yhat;
        }
    }
    
    @Override
    public Vector dloss(Vector predicted, Vector truth) {
        if (predicted.size() != truth.size()) {
            throw new MLException("Attempted to compute loss on vectors of sizes " + predicted.size() + " and " + truth.size() + ".");
        }
        
        if (!truth.isSparse()) {
            return super.dloss(predicted, truth);
        } else {
            double[] yhatVec = predicted.toArray();
            double[] ans = new double[yhatVec.length];
            for (SparseVectorEntry sve : truth.getEntries()) {
                ans[sve.key()] = df(yhatVec[sve.key()], sve.value());
            }
            return new BasicVector(ans);
        }
    }
}
