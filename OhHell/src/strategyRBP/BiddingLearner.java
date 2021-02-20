package strategyRBP;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import ml.ActivationFunction;
import ml.BasicVector;
import ml.Vector;
import ml.Learner;
import ml.MeanSquaredError;
import ml.ReLuFunction;
import ml.SigmoidFunction;

public class BiddingLearner extends Learner {
    Hashtable<Integer, List<Vector>> playerIns = new Hashtable<>();
    private LinkedList<LinkedList<Vector>> dataAsList = new LinkedList<>();
    
    public BiddingLearner(int[] ds, ActivationFunction[] actFuncs) {
        super(ds, actFuncs, null);
    }
    
    public BiddingLearner(String file) {
        super(new int[] {0}, new ActivationFunction[] {null}, null);
        openFromFile(file);
        getInputLayer().setActFuncs(new LinkedList<>(Arrays.asList(getActFuncs(getDepth()))));
    }
    
    public static ActivationFunction[] getActFuncs(int length) {
        ActivationFunction[] actFuncs = new ActivationFunction[length];
        for (int i = 0; i < length - 1; i++) {
            actFuncs[i] = new ReLuFunction();
        }
        actFuncs[length - 1] = new SigmoidFunction();
        //actFuncs[length - 1] = new IdentityFunction();
        return actFuncs;
    }
    
    public void putIn(int playerIndex, List<Vector> ins) {
        playerIns.put(playerIndex, ins);
    }
    
    public void putOut(int playerIndex, double points) {
        for (Vector in : playerIns.get(playerIndex)) {
            BasicVector out = new BasicVector(new double[] {points});
            dataAsList.add(new LinkedList<>(Arrays.asList(in, out)));
        }
    }
    
    @Override
    public LinkedList<Vector> getDatum(Vector in) {
        if (in == null) {
            return dataAsList.pop();
        } else {
            return new LinkedList<>(Arrays.asList(in, null));
        }
    }
    
    public int dataSize() {
        return dataAsList.size();
    }
    
    public List<double[]> doEpoch(double wEta, double bEta, boolean computeSizes) {
        List<double[]> ans = super.doEpoch(wEta, bEta, dataAsList.size(), new MeanSquaredError(), computeSizes, computeSizes);
        dataAsList = new LinkedList<>();
        return ans;
    }
    
    @Override
    public String toString() {
        return "RBP/BL";
    }
}
