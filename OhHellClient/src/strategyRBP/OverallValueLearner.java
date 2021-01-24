package strategyRBP;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import core.Card;
import ml.ActivationFunction;
import ml.BasicVector;
import ml.Feature;
import ml.Vector;
import ml.Learner;
import ml.MeanSquaredError;
import ml.ReLuFunction;
import ml.SigmoidFunction;

public class OverallValueLearner extends Learner {
    Hashtable<Card, List<List<Vector>>> ins = new Hashtable<>();
    private LinkedList<LinkedList<Vector>> dataAsList = new LinkedList<>();
    
    public OverallValueLearner(int[] ds, ActivationFunction[] actFuncs) {
        super(ds, actFuncs, null);
    }
    
    public OverallValueLearner(String file) {
        super(new int[] {0}, new ActivationFunction[] {null}, null);
        openFromFile(file);
        getInputLayer().setActFuncs(new LinkedList<>(Arrays.asList(getActFuncs(getDepth()))));
        List<Feature> features = new LinkedList<>(Arrays.asList(
                new Feature("Card adjusted number", new String[] {"2", "3", "4", "5", "6", "7", "8", "9", "T", "J", "Q", "K", "A"}),
                new Feature("Suit", new String[] {"Nontrump 1", "Nontrump 2", "Nontrump 3", "Trump"}),
                new Feature("My nontrump 1 suit length", 0, 11),
                new Feature("My nontrump 2 suit length", 0, 11),
                new Feature("My nontrump 3 suit length", 0, 11),
                new Feature("My trump suit length", 0, 11),
                new Feature("Unseen nontrump 1 suit length", 0, 14),
                new Feature("Unseen nontrump 2 suit length", 0, 14),
                new Feature("Unseen nontrump 3 suit length", 0, 14),
                new Feature("Unseen trump suit length", 0, 13)));
        for (int i = 0; i < 5; i++) {
            features.add(new Feature("Player " + i + " wants", 0, 11));
        }
        setFeatures(features);
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
        return "RBP/OVL";
    }
}
