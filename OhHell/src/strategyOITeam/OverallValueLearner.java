package strategyOITeam;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
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
    Hashtable<Card, List<Vector>> ins = new Hashtable<>();
    Hashtable<Card, Double> outs = new Hashtable<>();
    private LinkedList<LinkedList<Vector>> dataAsList;
    
    public OverallValueLearner(int N, int D, int d1) {
        int maxH = Math.min(10, (52 * D - 1) / N);
        
        List<Feature> features = new ArrayList<>();
        features.add(new Feature("Initial hand size", 1, maxH));
        features.add(new Feature("Current hand size", 0, maxH - 1));
        features.add(new Feature("Trump unseen", 0, 13 * D - 1));
        features.add(new Feature("Card's suit unseen", 0, 13 * D - 1));
        features.add(new Feature("Card is trump", 0, 1));
        for (int j = 0; j < N; j++) {
            features.add(new Feature(j + " Bid", 0, maxH));
            features.add(new Feature(j + " Taken", 0, maxH - 1));
            features.add(new Feature(j + " Team wants", 0, maxH));
            features.add(new Feature(j + " Trump void", 0, 1));
            features.add(new Feature(j + " Card's suit void", 0, 1));
            features.add(new Feature(j + " On lead", 0, 1));
        }
        setFeatures(features);
        
        int[] ds = {
                features.stream().map(Feature::getDimension).reduce(0, Integer::sum),
                d1,
                1
        };
        
        ActivationFunction[] actFuncs = new ActivationFunction[ds.length - 1];
        for (int i = 0; i < actFuncs.length - 1; i++) {
            actFuncs[i] = new ReLuFunction();
        }
        actFuncs[actFuncs.length - 1] = new SigmoidFunction();
        
        buildLayers(ds, actFuncs);
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
    
    public void dumpData(BufferedWriter writer) {
        makeDataList();
        try {
            for (List<Vector> datum : dataAsList) {
                for (Vector v : datum) {
                    writer.write(v.toString() + "\n");
                }
                writer.write("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    @Override
    public String toString() {
        return "OIT/OVL";
    }
}
