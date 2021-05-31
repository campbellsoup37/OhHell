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
    Map<Card, Map<Integer, Map<Card, Vector>>> insLevel1 = new HashMap<>();
    Map<Integer, Map<Card, Vector>> insLevel2 = new HashMap<>();
    Map<Card, List<Vector>> insLevel3 = new HashMap<>();
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
    
    public double evaluate(Card card1, int index, Card card2, Vector in) {
        putIn(card1, index, card2, in);
        return testValue(in).get(1).get(0);
    }
    
    public void putIn(Card card1, int index, Card card2, Vector in) {
        if (!insLevel1.containsKey(card1)) {
            insLevel1.put(card1, new HashMap<>());
        }
        Map<Integer, Map<Card, Vector>> nextLevel = insLevel1.get(card1);
        if (!nextLevel.containsKey(index)) {
            nextLevel.put(index, new HashMap<>());
        }
        nextLevel.get(index).put(card2, in);
    }
    
    public void deleteIns() {
        insLevel1 = new HashMap<>();
    }
    
    public void elevateIns1(Card card) {
        /*System.out.println("---------------------");
        for (Card card1 : insLevel1.keySet()) {
            for (Card card2 : insLevel1.get(card1).keySet()) {
                System.out.println(card1 + " " + card2);
            }
        }
        System.out.println("---------------------");*/
        for (Map.Entry<Integer, Map<Card, Vector>> entry : insLevel1.get(card).entrySet()) {
            int index = entry.getKey();
            if (!insLevel2.containsKey(index)) {
                insLevel2.put(index, new HashMap<>());
            }
            insLevel2.get(index).putAll(entry.getValue());
        }
        insLevel1 = new HashMap<>();
    }
    
    public void elevateIns2(int index) {
        /*System.out.println("---------------------");
        for (int i : insLevel2.keySet()) {
            for (Card card2 : insLevel2.get(i).keySet()) {
                System.out.println(i + " " + card2);
            }
        }
        System.out.println("---------------------");*/
        if (!insLevel2.isEmpty()) {
            for (Map.Entry<Card, Vector> entry : insLevel2.get(index).entrySet()) {
                Card card2 = entry.getKey();
                if (!insLevel3.containsKey(card2)) {
                    insLevel3.put(card2, new LinkedList<>());
                }
                insLevel3.get(card2).add(entry.getValue());
            }
            insLevel2 = new HashMap<>();
        }
    }
    
    public void flushIns(Card card, boolean won) {
        if (!insLevel3.isEmpty()) {
            for (Vector in : insLevel3.get(card)) {
                Vector out = new BasicVector(new double[] {won ? 1 : 0});
                dataAsList.add(new LinkedList<>(Arrays.asList(in, out)));
            }
            insLevel3.remove(card);
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
}
