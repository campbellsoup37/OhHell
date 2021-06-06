package strategyOITeam;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import core.Card;
import ml.ActivationFunction;
import ml.CrossEntropy;
import ml.Feature;
import ml.Vector;
import ml.Learner;
import ml.MeanSquaredError;
import ml.ReLuFunction;
import ml.SoftmaxFunction;
import ml.SparseVector;

public class ImmediateValueLearner extends Learner {
    int N;
    private Map<Card, Vector> insLevel1 = new HashMap<>();
    private Map<Card, Vector> insLevel2 = new HashMap<>();
    private LinkedList<LinkedList<Vector>> dataAsList = new LinkedList<>();
    
    public ImmediateValueLearner(int N, int T, int D, int d1) {
        this.N = N;
        int maxH = Math.min(10, (52 * D - 1) / N);
        int maxCancels = (N - 1) / 2;
        
        List<Feature> features = new ArrayList<>();
        features.add(new Feature("Initial hand size", 1, maxH));
        features.add(new Feature("Current hand size", 0, maxH - 1));
        features.add(new Feature("Trump unseen", 0, 13 * D - 1));
        features.add(new Feature("Led suit unseen", 0, 13 * D - 1));
        features.add(new Feature("Lead is trump", 0, 1));
        for (int j = 0; j < N; j++) {
            features.add(new Feature(j + " Team number", 0, T - 1));
            features.add(new Feature(j + " Bid", 0, maxH));
            features.add(new Feature(j + " Taken", 0, maxH - 1));
            features.add(new Feature(j + " Team wants", 0, maxH));
            features.add(new Feature(j + " Trump void", 0, 1));
            features.add(new Feature(j + " Lead void", 0, 1));
            features.add(new Feature(j + " Is trump", 0, 1));
            features.add(new Feature(j + " Adjusted number", 0, 13 * D));
            features.add(new Feature(j + " Matches unseen", 0, D - 1));
            features.add(new Feature(j + " Required cancels", -2, maxCancels));
            features.add(new Feature(j + " Led", 0, 1));
        }
        setFeatures(features);
        
        int[] ds = {
                features.stream().map(Feature::getDimension).reduce(0, Integer::sum),
                d1,
                N
        };
        
        ActivationFunction[] actFuncs = new ActivationFunction[ds.length - 1];
        for (int i = 0; i < actFuncs.length - 1; i++) {
            actFuncs[i] = new ReLuFunction();
        }
        actFuncs[actFuncs.length - 1] = new SoftmaxFunction();
        
        buildLayers(ds, actFuncs);
    }
    
    public double[] evaluate(Card card, Vector in) {
        insLevel1.put(card, in);
        return testValue(in).get(1).toArray();
    }
    
    public void elevateIns(Card card) {
        insLevel2.put(card, insLevel1.get(card));
        insLevel1 = new HashMap<>();
    }
    
    public void deleteIns() {
        insLevel1 = new HashMap<>();
    }
    
    public void flushIns(int winner) {
        if (!insLevel2.isEmpty()) {
            for (Vector in : insLevel2.values()) {
                SparseVector out = new SparseVector();
                out.addOneHot("Winner", winner, -1, N - 1);
                dataAsList.add(new LinkedList<>(Arrays.asList(in, out)));
            }
            insLevel2 = new HashMap<>();
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
        if (dataAsList.isEmpty()) {
            return null;
        } else {
            List<double[]> ans = super.doEpoch(wEta, bEta, dataAsList.size(), new CrossEntropy(), computeSizes, computeSizes);
            dataAsList = new LinkedList<>();
            return ans;
        }
    }
    
    @Override
    public String toString() {
        return "OIT/IVL";
    }
}
