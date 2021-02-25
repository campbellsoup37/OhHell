package ml;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class GradientBooster {
    private boolean empty = true;
    private int dIn;
    private int dOut;
    private List<List<Tree>> trees;

    public GradientBooster(int dIn, int dOut, int size) {
        this.dIn = dIn;
        this.dOut = dOut;
        trees = new ArrayList<>(dOut);
        for (int i = 0; i < dOut; i++) {
            trees.add(new ArrayList<>(size));
        }
    }
    
    public GradientBooster(String file) {
        try {
            InputStream in = getClass().getResourceAsStream("/" + file);
            BufferedReader reader;
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new FileReader(file));
            }
            empty = false;
            trees = new ArrayList<>();
            int i = -1;
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                if (line.length() >= 1 && line.charAt(0) == '/') {
                    i++;
                    if (i >= 1) {
                        trees.set(i - 1, new ArrayList<>(trees.get(i - 1)));
                    }
                    trees.add(new LinkedList<>());
                } else {
                    trees.get(i).add(new Tree(line));
                }
            }
            trees = new ArrayList<>(trees);
            dIn = trees.get(0).get(0).getDIn();
            dOut = trees.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveToFile(File file) {
        try {
            file.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (int i = 0; i < dOut; i++) {
                writer.write("/\n");
                for (Tree tree : trees.get(i)) {
                    writer.write(tree + "\n");
                }
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public List<Vector> testValue(Vector in) {
        if (empty) {
            throw new MLException("Attempted to evaluate an empty gradient booster.");
        }
        if (in.size() != dIn) {
            throw new MLException("Attempted to input a vector of size " + in.size() + " into a model that takes vectors of size " + dIn + ".");
        }
        double[] out = new double[dOut];
        for (int i = 0; i < dOut; i++) {
            for (Tree tree : trees.get(i)) {
                out[i] += tree.testValue(in).get(1).get(0);
            }
        }
        return Arrays.asList(in, new SoftmaxFunction().a(new BasicVector(out)));
    }
    
    public double testError(DataSet data, LossFunction L) {
        double loss = 0;
        for (List<Vector> datum : data) {
            loss += L.loss(testValue(datum.get(0)).get(1), datum.get(1)) / data.size();
        }
        return loss;
    }
    
    public void train(DataSet data, int treeSize, int printEvery) {
//        if (!empty) {
//            throw new MLException("Attempted to train a nonempty gradient booster.");
//        }
//        empty = false;
//        
//        DataSet dataCopy = data.deepCopy();
//        
//        for (int i = 0; i < size; i++) {
//            long time = System.currentTimeMillis();
//            Tree tree = new Tree(dIn, dOut);
//            tree.trainTopDown(dataCopy, treeSize);
//            trees.add(tree);
//            
//            dataCopy.map(inOut -> Arrays.asList(
//                    inOut.get(0),
//                    inOut.get(1).add(tree.testValue(inOut.get(0)).get(1), -1)
//                    ));
//
//            if (printEvery > 0 && i % printEvery == 0) {
//                System.out.println(i + " / " + size);
//                System.out.println("   Error: " + testError(data.complement(), new MeanSquaredError()));
//                System.out.println("   In time: " + (System.currentTimeMillis() - time) + " ms");
//            }
//        }
    }
}
