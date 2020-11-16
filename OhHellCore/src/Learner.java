import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class Learner {
	Layer inputLayer, outputLayer;
	int depth;
	String[] labels;
	
	public Learner(int[] ds, ActivationFunction af, String[] labels) {
        inputLayer = new Layer(ds[0], af);
        for(int i=1;i<ds.length;i++) {
            Layer layer = new Layer(ds[i], af);
            inputLayer.addLayer(layer);
            if(i==ds.length-1) outputLayer = layer;
        }
        depth = ds.length-1;
        this.labels = labels;
    }
	
	public Learner(int[] ds, ActivationFunction[] afList, String[] labels) {
        inputLayer = new Layer(ds[0], null);
        for(int i=1;i<ds.length;i++) {
            Layer layer = new Layer(ds[i], afList[i - 1]);
            inputLayer.addLayer(layer);
            if(i==ds.length-1) outputLayer = layer;
        }
        depth = ds.length-1;
        this.labels = labels;
    }
	
	public int labelToInt(String s) {
		for(int i=0;i<labels.length;i++) if(labels[i].equals(s)) return i+1;
		return -1;
	}
	
	public LinkedList<LayerVector> getDatum(LayerVector in) {
		return null;
	}
	
	public void addDatum(boolean backprop, double wEta, double bEta, int m, LayerVector in) {
		LinkedList<LayerVector> inout = getDatum(in);
        inputLayer.addInput(inout.getFirst());
        outputLayer.addTruth(inout.getLast());
		inputLayer.compute(backprop, wEta, bEta, m);
	}
	
	public double doEpoch(double wEta, double bEta, int m, boolean computeError) {
		for (int p = 0; p < m; p++) {
		    addDatum(true, wEta, bEta, m, null);
		}
		outputLayer.applyChanges(outputLayer.wgrads, outputLayer.bgrads, depth - 1);
		double error = computeError ? outputLayer.getError() : 0;
		inputLayer.clearData();
		return error;
	}
	
	public void printLayer(int l) {
		inputLayer.print(l);
	}
	
	public void saveToFile(File file) {
	    try {
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            inputLayer.writeToFile(writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public void openFromFile(String file) {
	    try {
            InputStream in = getClass().getResourceAsStream("/" + file);
            BufferedReader reader;
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new FileReader(file));
            }
	        List<double[][]> ws = new LinkedList<>();
	        List<double[]> bs = new LinkedList<>();
	        boolean addToWs = true;
	        List<String> lines = new LinkedList<>();
	        for (String line = reader.readLine(); line != null; line = reader.readLine()) {
	            if (line.length() > 0) {
	                lines.add(line);
	            } else {
	                if (addToWs) {
	                    ws.add(MLTools.stringToMatrix(lines.stream().reduce("", (a, b) -> a + b)));
	                } else {
	                    bs.add(MLTools.stringToVector(lines.stream().reduce("", (a, b) -> a + b)));
	                }
	                addToWs = !addToWs;
	                lines = new LinkedList<>();
	            }
	        }
            reader.close();
	        inputLayer.setWeights(ws, bs);
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public LinkedList<LayerVector> testValue(LayerVector in) {
	    addDatum(false, 0, 0, 0, in);
	    LinkedList<LayerVector> output = new LinkedList<>(Arrays.asList(new LayerVector[] {
            inputLayer.value, 
            outputLayer.value, 
            outputLayer.truth}));
        inputLayer.clearData();
	    return output;
	}
	
	public void printTest(boolean classifier) {
		addDatum(false, 0, 0, 0, null);
		double[] in = inputLayer.value.toArray();
		double[] out = outputLayer.value.toArray();
		double[] truth = outputLayer.truth.toArray();
		System.out.println(MLTools.vectorToString(in));
		if(classifier) {
	        System.out.println(labels[MLTools.classify(truth)-1]);
	        System.out.println(labels[MLTools.classify(out)-1]);
		} else {
		    System.out.println(MLTools.vectorToString(truth));
		    System.out.println(MLTools.vectorToString(out));
		}
		System.out.println();
		inputLayer.clearData();
	}
}
