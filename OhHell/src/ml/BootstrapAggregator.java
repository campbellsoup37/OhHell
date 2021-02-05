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

public class BootstrapAggregator {
    private boolean empty = true;
    private int dIn;
    private int dOut;
    private int size;
    private List<Tree> trees;
    
    public BootstrapAggregator(int dIn, int dOut, int size) {
        this.dIn = dIn;
        this.dOut = dOut;
        this.size = size;
        trees = new ArrayList<>(size);
    }
    
    public BootstrapAggregator(String file) {
        try {
            InputStream in = getClass().getResourceAsStream("/" + file);
            BufferedReader reader;
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new FileReader(file));
            }
            empty = false;
            trees = new LinkedList<>();
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                trees.add(new Tree(line));
            }
            trees = new ArrayList<>(trees);
            dIn = trees.get(0).getDIn();
            dOut = trees.get(0).getDOut();
            size = trees.size();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void saveToFile(File file) {
        try {
            file.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            for (Tree tree : trees) {
                writer.write(tree + "\n");
            }
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public List<Vector> testValue(Vector in) {
        if (empty) {
            throw new MLException("Attempted to evaluate an empty bootstrap aggregator.");
        }
        if (in.size() != dIn) {
            throw new MLException("Attempted to input a vector of size " + in.size() + " into a model that takes vectors of size " + dIn + ".");
        }
        Vector out = new BasicVector(new double[dOut]);
        for (Tree tree : trees) {
            out.add(tree.testValue(in).get(1), 1D / size);
        }
        return Arrays.asList(in, out);
    }
    
    public double testError(DataSet data, LossFunction L) {
        double loss = 0;
        for (List<Vector> datum : data) {
            loss += L.loss(testValue(datum.get(0)).get(1), datum.get(1)) / data.size();
        }
        return loss;
    }
    
    public void train(DataSet data, int sampleSize, double alpha, int printEvery) {
        if (!empty) {
            throw new MLException("Attempted to train a nonempty bootstrap aggregator.");
        }
        
        for (int i = 0; i < size; i++) {
            long time = System.currentTimeMillis();
            Tree tree = new Tree(dIn, dOut);
            tree.trainBottomUp(data.bootstrapSample(sampleSize), alpha);
            trees.add(tree);

            if (printEvery > 0 && i % printEvery == 0) {
                System.out.println(i + " / " + size);
                System.out.println("   Created a tree of size: " + tree.numLeaves());
                System.out.println("   In time: " + (System.currentTimeMillis() - time) + " ms");
            }
        }
        
        empty = false;
    }
}
