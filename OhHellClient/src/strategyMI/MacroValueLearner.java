package strategyMI;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;

import ml.ActivationFunction;
import ml.SparseVector;
import ml.CrossEntropy;
import ml.Vector;
import ml.Learner;
import ml.ReLuFunction;
import ml.SigmoidFunction;
import ml.SoftmaxFunction;

public class MacroValueLearner extends Learner {
    private class InData {
        public Vector in;
        public int taken;
        
        public InData(Vector in, int taken) {
            this.in = in;
            this.taken = taken;
        }
    }
    Hashtable<Integer, List<InData>> playerIns = new Hashtable<>();
    Hashtable<Vector, Vector> outs = new Hashtable<>();
    private LinkedList<LinkedList<Vector>> dataAsList;
    
    public MacroValueLearner(int[] ds, ActivationFunction[] actFuncs) {
        super(ds, actFuncs, null);
    }
    
    public MacroValueLearner(String file) {
        super(new int[] {0}, new ActivationFunction[] {null}, null);
        openFromFile(file);
        getInputLayer().setActFuncs(new LinkedList<>(Arrays.asList(getActFuncs(getDepth()))));
    }
    
    public static ActivationFunction[] getActFuncs(int length) {
        ActivationFunction[] actFuncs = new ActivationFunction[length];
        for (int i = 0; i < length - 1; i++) {
            actFuncs[i] = new ReLuFunction();
        }
        actFuncs[length - 1] = new SoftmaxFunction();
        return actFuncs;
    }
    
    public void putIn(int playerIndex, List<Vector> ins, int taken) {
        if (ins != null) {
            if (playerIns.get(playerIndex) == null) {
                playerIns.put(playerIndex, new LinkedList<>());
            }
            for (Vector in : ins) {
                if (in != null) {
                    playerIns.get(playerIndex).add(new InData(in, taken));
                }
            }
        }
    }
    
    public void putOut(int playerIndex, int taken, int maxH) {
        for (InData inData : playerIns.get(playerIndex)) {
            SparseVector out = new SparseVector();
            out.addOneHot(taken - inData.taken + 1, maxH + 1);
            outs.put(inData.in, out);
        }
        playerIns.put(playerIndex, new LinkedList<>());
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
        for (Vector in : outs.keySet()) {
            dataAsList.add(new LinkedList<>(Arrays.asList(in, outs.get(in))));
        }
        outs = new Hashtable<>();
    }
    
    public List<double[]> doEpoch(double wEta, double bEta, boolean computeSizes) {
        makeDataList();
        if (dataAsList.isEmpty()) {
            return null;
        } else {
            return super.doEpoch(wEta, bEta, dataAsList.size(), new CrossEntropy(), computeSizes, computeSizes);
        }
    }
    
    @Override
    public String toString() {
        return "MI/MVL";
    }
}
