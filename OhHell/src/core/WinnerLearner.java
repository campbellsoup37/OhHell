package core;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import ml.ActivationFunction;
import ml.CrossEntropy;
import ml.Learner;
import ml.ReLuFunction;
import ml.SoftmaxFunction;
import ml.SparseVector;
import ml.Vector;

public class WinnerLearner extends Learner {
    private List<Vector> ins = new LinkedList<>();
    private LinkedList<LinkedList<Vector>> dataAsList = new LinkedList<>();
    
    public WinnerLearner(int[] ds, ActivationFunction[] actFuncs) {
        super(ds, actFuncs, null);
    }
    
    public WinnerLearner(String file) {
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
    
    public void putIn(Vector in) {
        ins.add(in);
    }
    
    public void putOut(int out) {
        for (Vector in : ins) {
            SparseVector outVector = new SparseVector();
            outVector.addOneHot(out + 1, in.size() - 1);
            dataAsList.add(new LinkedList<>(Arrays.asList(in, outVector)));
        }
        ins = new LinkedList<>();
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
    
    public void dumpData(BufferedWriter writer) {
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
        return "WL";
    }
}
