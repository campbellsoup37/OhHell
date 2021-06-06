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
import ml.Learner;
import ml.MeanSquaredError;
import ml.ReLuFunction;
import ml.SoftmaxFunction;
import ml.SparseVector;
import ml.Vector;

public class TeammateTakesLearner extends Learner {
    int maxH;
    Map<Card, Map<Integer, AnnotatedVector>> insLevel1 = new HashMap<>();
    Map<Integer, List<AnnotatedVector>> insLevel2 = new HashMap<>();
    List<AnnotatedVector> insLevel3 = new LinkedList<>();
    LinkedList<LinkedList<Vector>> dataAsList = new LinkedList<>();
    
    public TeammateTakesLearner(int N, int T, int D, int d1) {
        maxH = Math.min(10, (52 * D - 1) / N);
        
        List<Feature> features = new ArrayList<>();
        features.add(new Feature("Initial hand size", 1, maxH));
        features.add(new Feature("Current hand size", 0, maxH));
        features.add(new Feature("Void count", 0, 3));
        features.add(new Feature("Trump unseen", 0, 13 * D - 1));
        for (int j = 0; j < maxH; j++) {
            features.add(new Feature(j + " Card played", 0, 1));
            features.add(new Feature(j + " Card strength", 0, 1));
        }
        for (int j = 0; j < N; j++) {
            features.add(new Feature(j + " Team number", 0, T - 1));
            features.add(new Feature(j + " Bid", 0, maxH));
            features.add(new Feature(j + " Taken", 0, maxH - 1));
            features.add(new Feature(j + " Team wants", 0, maxH));
            features.add(new Feature(j + " Trump void", 0, 1));
            features.add(new Feature(j + " On lead", 0, 1));
        }
        setFeatures(features);
        
        int[] ds = {
                features.stream().map(Feature::getDimension).reduce(0, Integer::sum),
                d1,
                maxH + 1
        };
        
        ActivationFunction[] actFuncs = new ActivationFunction[ds.length - 1];
        for (int i = 0; i < actFuncs.length - 1; i++) {
            actFuncs[i] = new ReLuFunction();
        }
        actFuncs[actFuncs.length - 1] = new SoftmaxFunction();
        
        buildLayers(ds, actFuncs);
    }
    
    public double[] evaluate(Card card, int index1, int index2, Vector in, int taken) {
        if (!insLevel1.containsKey(card)) {
            insLevel1.put(card, new HashMap<>());
        }
        Map<Integer, AnnotatedVector> nextLevel = insLevel1.get(card);
        nextLevel.put(index1, new AnnotatedVector(in, index2, taken));
        return testValue(in).get(1).toArray();
    }
    
    public void elevateIns1(Card card) {
        if (!insLevel1.containsKey(card)) {
            return;
        }
        
        for (Map.Entry<Integer, AnnotatedVector> entry : insLevel1.get(card).entrySet()) {
            if (!insLevel2.containsKey(entry.getKey())) {
                insLevel2.put(entry.getKey(), new LinkedList<>());
            }
            insLevel2.get(entry.getKey()).add(entry.getValue());
        }
        /*System.out.println("---------------------");
        for (Card card1 : insLevel1.keySet()) {
            for (int index1 : insLevel1.get(card1).keySet()) {
                System.out.println(card1 + " " + index1 + " " + insLevel1.get(card1).get(index1).index);
            }
        }
        
        System.out.println();
        
        for (int index1 : insLevel2.keySet()) {
            System.out.print(card + " " + index1 + " [");
            for (AnnotatedVector vec : insLevel2.get(index1)) {
                System.out.print(" " + vec.index);
            }
            System.out.println(" ]");
        }
        System.out.println("---------------------");*/
        insLevel1 = new HashMap<>();
    }
    
    public void deleteIns() {
        insLevel1 = new HashMap<>();
    }
    
    public void elevateIns2(int winner) {
        if (!insLevel2.containsKey(winner)) {
            return;
        }
        
        if (!insLevel2.isEmpty()) {
            insLevel3.addAll(insLevel2.get(winner));
            /*System.out.println("---------------------");
            for (int index1 : insLevel2.keySet()) {
                System.out.print(index1 + " [");
                for (AnnotatedVector vec : insLevel2.get(index1)) {
                    System.out.print(" " + vec.index);
                }
                System.out.println(" ]");
            }
            
            System.out.println();
            
            System.out.print(winner + " [");
            for (AnnotatedVector vec : insLevel3) {
                System.out.print(" " + vec.index);
            }
            System.out.println(" ]");
            System.out.println("---------------------");*/
            insLevel2 = new HashMap<>();
        }
    }
    
    public void flushIns(Map<Integer, Integer> takens) {
        if (!insLevel3.isEmpty()) {
            /*System.out.println("---------------------");
            for (AnnotatedVector vec : insLevel3) {
                System.out.println(vec.index);
            }
            System.out.println("---------------------");*/
            
            for (AnnotatedVector av : insLevel3) {
                SparseVector out = new SparseVector();
                out.addOneHot("Taken", takens.get(av.index) - av.taken, -1, maxH);
                dataAsList.add(new LinkedList<>(Arrays.asList(av.in, out)));
            }
            insLevel3 = new LinkedList<>();
        }
    }
    
    public boolean insFlushed() {
        return insLevel3.isEmpty();
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
        return "OIT/TTL";
    }
    
    private class AnnotatedVector {
        Vector in;
        int index;
        int taken;
        
        public AnnotatedVector(Vector in, int index, int taken) {
            this.in = in;
            this.index = index;
            this.taken = taken;
        }
    }
}
