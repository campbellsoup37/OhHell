package ml;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

public class Tree {
    private class Region {
        public Region sup;
        // If this region is a leaf, it takes constant value c.
        public boolean isLeaf = true;
        public DataSet data;
        public Vector c;
        public boolean isPureInput = true;
        public boolean isPureOutput = true;
        // If this region is not a leaf, sub1 is the region X_j <= s and sub2 is the region X_j > s.
        public Region sub1;
        public Region sub2;
        public int j;
        public double s;

        public boolean doNotSplit = true;
        
        public double loss = -1;
        public double totalLoss = -1;
        public int size = -1;
        public double gain = -1;
        
        public Region() {}
        
        public Region(Region sup) {
            this.sup = sup;
        }
        
        public int loadFromCode(String[] nodeCodes, int index) {
            String[] fields = nodeCodes[index].split(":");
            if (fields[0].equals("l")) {
                c = MLTools.stringToVector(fields[1]);
                return index + 1;
            } else {
                j = Integer.parseInt(fields[1]);
                s = Double.parseDouble(fields[2]);
                isLeaf = false;
                sub1 = new Region();
                sub2 = new Region();
                return sub2.loadFromCode(nodeCodes, 
                        sub1.loadFromCode(nodeCodes, index + 1));
            }
        }
        
        public void setData(DataSet data) {
            this.data = data;
            c = new BasicVector(new double[dOut]);

            Vector inPurityTest = data.randomSample().get(0);
            Vector outPurityTest = data.randomSample().get(1);
            
            for (List<Vector> inOut : data) {
                c.add(inOut.get(1), 1D / data.size());
                if (isPureInput && !inOut.get(0).entrywiseEquals(inPurityTest)) {
                    isPureInput = false;
                }
                if (isPureOutput && !inOut.get(1).entrywiseEquals(outPurityTest)) {
                    isPureOutput = false;
                }
            }
            
            loss = 0;
            for (int k = 0; k < dOut; k++) {
                loss -= c.get(k) * log(c.get(k));
            }
            loss *= data.size();
        }
        
        public double computeSplit() {
            double bestLoss = Integer.MAX_VALUE;
            for (int j = 0; j < dIn; j++) {
                data.sortByInputEntry(j);
                Vector S1 = new BasicVector(new double[dOut]);
                int N1 = 0;
                double loss1 = 0;
                Vector S2 = c.copy();
                int N2 = data.size();
                S2.scale(N2);
                double loss2 = loss;
                
                int i = 0;
                Iterator<List<Vector>> it = data.iterator();
                List<Vector> datum = it.next();
                while (i < data.size()) {
                    double Xj = datum.get(0).get(j);
                    while (i < data.size() && datum.get(0).get(j) == Xj) {
                        int k;
                        for (k = 0; datum.get(1).get(k) == 0; k++);
                        
                        loss1 = loss1 
                                + S1.get(k) * log(S1.get(k)) 
                                - (S1.get(k) + 1) * log(S1.get(k) + 1)
                                - N1 * log(N1)
                                + (N1 + 1) * log(N1 + 1);
                        S1.add(datum.get(1));
                        N1++;
                        
                        loss2 = loss2 
                                + S2.get(k) * log(S2.get(k))
                                - (S2.get(k) - 1) * log(S2.get(k) - 1)
                                - N2 * log(N2)
                                + (N2 - 1) * log(N2 - 1);
                        S2.add(datum.get(1), -1);
                        N2--;
                        
                        i++;
                        if (it.hasNext()) {
                            datum = it.next();
                        }
                    }
                    if (loss1 + loss2 < bestLoss && i !=0 && i < data.size()) {
                        this.j = j;
                        s = (Xj + datum.get(0).get(j)) / 2;
                        bestLoss = loss1 + loss2;
                        doNotSplit = false;
                    }
                }
            }
            return bestLoss;
        }
        
        public void makeSplit() {
            isLeaf = false;
            DataSet[] split = data.split(j, s);
            sub1 = new Region(this);
            sub1.setData(split[0]);
            sub2 = new Region(this);
            sub2.setData(split[1]);
        }
        
        public void grow() {
            if (!isPureInput && !isPureOutput) {
                computeSplit();
                if (!doNotSplit) {
                    makeSplit();
                    sub1.grow();
                    sub2.grow();
                }
            }
        }
        
        private double log(double x) {
            return x <= 0 ? 0 : Math.log(x);
        }
        
        public double totalLoss() {
            if (isLeaf) {
                return loss;
            }
            if (totalLoss == -1) {
                totalLoss = sub1.totalLoss() + sub2.totalLoss();
            }
            return totalLoss;
        }
        
        public int size() {
            if (isLeaf) {
                return 1;
            }
            if (size == -1) {
                size = sub1.size() + sub2.size();
            }
            return size;
        }
        
        public void computeGainDown() {
            if (isLeaf) {
                totalLoss = loss;
                size = 1;
                gain = Double.POSITIVE_INFINITY;
            } else {
                sub1.computeGainDown();
                sub2.computeGainDown();
                totalLoss = sub1.totalLoss + sub2.totalLoss;
                size = sub1.size + sub2.size;
                gain = (loss - totalLoss) / (size - 1);
            }
        }
        
        public void heapify(PriorityQueue<Region> heap) {
            if (!isLeaf) {
                heap.add(this);
                sub1.heapify(heap);
                sub2.heapify(heap);
            }
        }
        
        public void unheapDown(PriorityQueue<Region> heap) {
            if (!isLeaf) {
                heap.remove(this);
                sub1.unheapDown(heap);
                sub2.unheapDown(heap);
            }
        }
        
        public void computeGainUp() {
            if (isLeaf) {
                totalLoss = loss;
                size = 1;
                gain = Double.POSITIVE_INFINITY;
            } else {
                totalLoss = sub1.totalLoss + sub2.totalLoss;
                size = sub1.size + sub2.size;
                gain = (loss - totalLoss) / (size - 1);
            }
            if (sup != null) {
                sup.computeGainUp();
            }
        }
        
        public void reheapUp(PriorityQueue<Region> heap) {
            if (!isLeaf) {
                heap.remove(this);
                heap.add(this);
            }
            if (sup != null) {
                sup.reheapUp(heap);
            }
        }
        
        public void prune() {
            isLeaf = true;
            sub1 = null;
            sub2 = null;
            computeGainUp();
        }
        
        public Vector evaluate(Vector in) {
            if (isLeaf) {
                return c;
            } else if (in.get(j) <= s) {
                return sub1.evaluate(in);
            } else {
                return sub2.evaluate(in);
            }
        }
        
        public void print(int depth) {
            for (int i = 0; i < depth; i++) {
                System.out.print(" ");
            }
            if (!isLeaf) {
                System.out.println("X_" + j + " split at " + s);
                sub1.print(depth + 1);
                sub2.print(depth + 1);
            } else {
                System.out.println(c);
            }
        }
        
        public void getString(StringBuilder prev) {
            if (isLeaf) {
                prev.append("|l:" + MLTools.vectorToString(c.toArray()));
            } else {
                prev.append("|b:" + j + ":" + s);
                sub1.getString(prev);
                sub2.getString(prev);
            }
        }
    }
    
    private boolean empty = true;
    private int dIn;
    private int dOut;
    private Region root;
    private PriorityQueue<Region> branches = 
            new PriorityQueue<>((R1, R2) -> (int) Math.signum(R1.gain - R2.gain));
    
    public Tree(int dIn, int dOut) {
        this.dIn = dIn;
        this.dOut = dOut;
        root = new Region();
    }
    
    public Tree(String code) {
        empty = false;
        String[] fields = code.split(";");
        dIn = Integer.parseInt(fields[0]);
        dOut = Integer.parseInt(fields[1]);
        root = new Region();
        root.loadFromCode(fields[2].split("\\|"), 1);
    }
    
    public int getDIn() {
        return dIn;
    }
    
    public int getDOut() {
        return dOut;
    }
    
    public List<Vector> testValue(Vector in) {
        if (empty) {
            throw new MLException("Attempted to evaluate an empty tree.");
        }
        if (in.size() != dIn) {
            throw new MLException("Attempted to input a vector of size " + in.size() + " into a tree that takes vectors of size " + dIn + ".");
        }
        return Arrays.asList(in, root.evaluate(in));
    }
    
    public double testError(DataSet data, LossFunction L) {
        double loss = 0;
        for (List<Vector> datum : data) {
            loss += L.loss(testValue(datum.get(0)).get(1), datum.get(1)) / data.size();
        }
        return loss;
    }
    
    public void trainTopDown(DataSet data, int numNodes) {
        if (!empty) {
            throw new MLException("Attempted to train a nonempty tree.");
        }
        root.setData(data);
        
        Hashtable<Region, Double> diffs = new Hashtable<>();
        PriorityQueue<Region> leaves = new PriorityQueue<>((R1, R2) -> (int) Math.signum(diffs.get(R1) - diffs.get(R2)));
        diffs.put(root, -root.loss + root.computeSplit());
        leaves.add(root);
        int j = 1;
        for (Region toSplit = leaves.poll(); 
                j < numNodes 
                && toSplit != null
                && !toSplit.doNotSplit
                && !toSplit.isPureInput
                && !toSplit.isPureOutput; 
                toSplit = leaves.poll(), j++) {
            toSplit.makeSplit();
            diffs.put(toSplit.sub1, -toSplit.sub1.loss + toSplit.sub1.computeSplit());
            leaves.add(toSplit.sub1);
            diffs.put(toSplit.sub2, -toSplit.sub2.loss + toSplit.sub2.computeSplit());
            leaves.add(toSplit.sub2);
        }
        
        empty = false;
    }
    
    public void trainBottomUp(DataSet data, double alpha) {
        if (!empty) {
            throw new MLException("Attempted to train a nonempty tree.");
        }
        root.setData(data);
        
        root.grow();
        root.computeGainDown();
        root.heapify(branches);
        
        while (minAlpha() < alpha) {
            prune(minAlpha());
        }
        
        empty = false;
    }
    
    public double minAlpha() {
        return branches.peek().gain;
    }
    
    public void prune(double alpha) {
        while (minAlpha() <= alpha) {
            Region toPrune = branches.peek();
            toPrune.unheapDown(branches);
            toPrune.prune();
            toPrune.reheapUp(branches);
        }
    }
    
    public double totalLoss() {
        return root.totalLoss();
    }
    
    public double numLeaves() {
        return root.size();
    }
    
    public void print() {
        root.print(0);
    }
    
    @Override
    public String toString() {
        StringBuilder out = new StringBuilder();
        out.append(dIn + ";" + dOut + ";");
        root.getString(out);
        return out.toString();
    }
    
    public static void errorVsAlpha(int dIn, int dOut, DataSet data, int testSize, double alpha1, double alpha2, double alphaStep) {
        data.allocateTestData(testSize);
        Tree tree = new Tree(dIn, dOut);
        tree.trainBottomUp(data, 0);
        
        for (double alpha = alpha1; alpha < alpha2; alpha += alphaStep) {
            tree.prune(alpha);
            double averageError =  tree.testError(data.complement(), new MeanSquaredError());
            System.out.println("[" + alpha + ", " + averageError + ", " + tree.numLeaves() + "],");
        }
    }
    
    public static void errorVsAlphaCv(int dIn, int dOut, DataSet data, int foldsK, double alpha1, double alpha2, double alphaStep) {
        List<DataSet> partition = data.cvPartition(foldsK);
        List<Tree> trees = new ArrayList<>(foldsK);
        
        
        for (int k = 0; k < foldsK; k++) {
            System.out.println(k + " / " + foldsK);
            Tree tree = new Tree(dIn, dOut);
            tree.trainBottomUp(partition.get(k), 0);
            trees.add(tree);
        }
        
        for (double alpha = alpha1; alpha < alpha2; alpha += alphaStep) {
            double averageError = 0;
            for (int k = 0; k < foldsK; k++) {
                trees.get(k).prune(alpha);
                averageError += trees.get(k).testError(partition.get(k).complement(), new MeanSquaredError()) / foldsK;
            }
            System.out.print("[" + alpha + ", " + averageError);
            for (int k = 0; k < foldsK; k++) {
                System.out.print(", " + trees.get(k).numLeaves());
            }
            System.out.println("],");
        }
    }
}
