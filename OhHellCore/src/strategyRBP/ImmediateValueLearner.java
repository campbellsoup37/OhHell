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
import ohHellCore.Card;

public class ImmediateValueLearner extends Learner {
    Hashtable<Card, List<List<Vector>>> ins = new Hashtable<>();
    private LinkedList<LinkedList<Vector>> dataAsList = new LinkedList<>();
    
    public ImmediateValueLearner(int[] ds, ActivationFunction[] actFuncs) {
        super(ds, actFuncs, null);
    }
    
    public ImmediateValueLearner(String file) {
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
        return actFuncs;
    }
    
    public void putIn(Card c, List<Vector> ins) {
        if (ins != null) {
            if (this.ins.get(c) == null) {
                this.ins.put(c, new LinkedList<>());
            }
            this.ins.get(c).add(ins);
        }
    }
    
    public void putOut(Card c, int value) {
        if (ins.get(c) != null) {
            Vector out = new BasicVector(new double[] {value});
            for (List<Vector> insList : ins.get(c)) {
                for (Vector in : insList) {
                    dataAsList.add(new LinkedList<>(Arrays.asList(in, out)));
                }
            }
            ins.remove(c);
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
        return "RBP/IVL";
    }
}
