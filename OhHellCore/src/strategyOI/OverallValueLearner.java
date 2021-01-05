package strategyOI;
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

public class OverallValueLearner extends Learner {
    Hashtable<Card, List<Vector>> ins = new Hashtable<>();
    Hashtable<Card, Double> outs = new Hashtable<>();
    private LinkedList<LinkedList<Vector>> dataAsList;
    
    public OverallValueLearner(int[] ds, ActivationFunction[] actFuncs) {
        super(ds, actFuncs, null);
    }
    
    public OverallValueLearner(String file) {
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
    
    public void putIn(Card c, Vector in) {
        if (ins.get(c) == null) {
            ins.put(c, new LinkedList<>());
        }
        ins.get(c).add(in);
    }
    
    public void putOut(Card c, int out) {
        if (ins.get(c) != null) {
            outs.put(c, (double) out);
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
    
    public void makeDataList() {
        dataAsList = new LinkedList<>();
        for (Card c : ins.keySet()) {
            for (Vector in : ins.get(c)) {
                dataAsList.add(new LinkedList<>(Arrays.asList(
                        in, 
                        (Vector) new BasicVector(new double[] {outs.get(c)})
                        )));
            }
        }
        ins = new Hashtable<>();
        outs = new Hashtable<>();
    }
    
    public List<double[]> doEpoch(double wEta, double bEta, boolean computeSizes) {
        makeDataList();
        if (dataAsList.isEmpty()) {
            return null;
        } else {
            return super.doEpoch(wEta, bEta, dataAsList.size(), new MeanSquaredError(), computeSizes, computeSizes);
        }
    }
    
    @Override
    public String toString() {
        return "OI/OVL";
    }
}
