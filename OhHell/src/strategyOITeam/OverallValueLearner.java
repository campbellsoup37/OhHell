package strategyOITeam;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

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
    Map<Card, AnnotatedVector> insLevel1 = new HashMap<>();
    List<AnnotatedVector> insLevel2 = new LinkedList<>();
    private LinkedList<LinkedList<Vector>> dataAsList = new LinkedList<>();
    
    public OverallValueLearner(int N, int T, int D, int d1) {
        int maxH = Math.min(10, (52 * D - 1) / N);
        
        List<Feature> features = new ArrayList<>();
        features.add(new Feature("Initial hand size", 1, maxH));
        features.add(new Feature("Current hand size", 0, maxH));
        features.add(new Feature("Void count", 0, 3));
        features.add(new Feature("Trump unseen", 0, 13 * D - 1));
        features.add(new Feature("Card's suit unseen", 0, 13 * D - 1));
        features.add(new Feature("Card is trump", 0, 1));
        features.add(new Feature("Card's adjusted number", 0, 13 * D));
        features.add(new Feature("Card's matches unseen", 0, D - 1));
        for (int j = 0; j < N; j++) {
            features.add(new Feature(j + " Team number", 0, T - 1));
            features.add(new Feature(j + " Bid", -1, maxH));
            features.add(new Feature(j + " Taken", 0, maxH - 1));
            features.add(new Feature(j + " Team wants", 0, maxH));
            features.add(new Feature(j + " Trump void", 0, 1));
            features.add(new Feature(j + " Card's suit void", 0, 1));
            //features.add(new Feature(j + " On lead", 0, 1));
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
    
    public double evaluate(Card card1, Card card2, Vector in) {
        putIn(card1, card2, in);
        return testValue(in).get(1).get(0);
    }
    
    public void putIn(Card card1, Card card2, Vector in) {
        insLevel1.put(card1, new AnnotatedVector(in, card1));
    }
    
    public void elevateIns(Card card) {
        /*System.out.println("---------------------");
        for (Card card1 : insLevel1.keySet()) {
            for (Card card2 : insLevel1.get(card1).keySet()) {
                System.out.println(card1 + " " + card2);
            }
        }
        System.out.println("---------------------");*/
        insLevel2.add(insLevel1.get(card));
        insLevel1 = new HashMap<>();
    }
    
    public void flushIns(Card winner) {
        if (!insLevel2.isEmpty()) {
            for (AnnotatedVector av : insLevel2) {
                Vector out = new BasicVector(new double[] {av.card == winner ? 1 : 0});
                dataAsList.add(new LinkedList<>(Arrays.asList(av.in, out)));
            }
            insLevel2 = new LinkedList<>();
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
            List<double[]> ans = super.doEpoch(wEta, bEta, dataAsList.size(), new MeanSquaredError(), computeSizes, computeSizes);
            dataAsList = new LinkedList<>();
            return ans;
        }
    }
    
    @Override
    public String toString() {
        return "OIT/OVL";
    }
    
    private class AnnotatedVector {
        Vector in;
        Card card;
        
        public AnnotatedVector(Vector in, Card card) {
            this.in = in;
            this.card = card;
        }
    }
}
