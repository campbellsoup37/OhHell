package ml;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import common.FileTools;

public class Learner {
	private Layer inputLayer, outputLayer;
	private int depth;
	private String[] labels;
	private Trainer trainer;
	
	private List<Feature> features;
	
	public Learner() {}
	
	public Learner(int[] ds, ActivationFunction af, String[] labels) {
	    ActivationFunction[] afList = new ActivationFunction[ds.length - 1];
	    for (int i = 0; i < afList.length; i++) {
	        afList[i] = af;
	    }
	    buildLayers(ds, afList);
    }
	
	public Learner(int[] ds, ActivationFunction[] afList, String[] labels) {
        buildLayers(ds, afList);
    }
	
	public void buildLayers(int[] ds, ActivationFunction[] afList) {
	    inputLayer = new Layer(ds[0], null);
        for (int i = 1; i < ds.length; i++) {
            Layer layer = new Layer(ds[i], afList[i - 1]);
            inputLayer.addLayer(layer);
            if (i == ds.length - 1) {
                outputLayer = layer;
            }
        }
        depth = ds.length - 1;
	}
	
	public void setTrainer(Trainer trainer) {
	    this.trainer = trainer;
	}
    
    public List<Feature> getFeatures() {
        return features;
    }
	
	public void setFeatures(List<Feature> features) {
	    this.features = features;
	}
	
	public Layer getInputLayer() {
	    return inputLayer;
	}
	
	public int getDepth() {
	    return depth;
	}
	
	public int labelToInt(String s) {
		for (int i = 0; i < labels.length; i++) {
		    if (labels[i].equals(s)) {
		        return i + 1;
		    }
		}
		return -1;
	}
	
	public LinkedList<Vector> getDatum(Vector in) {
		return null;
	}
	
	public void addDatum(boolean backprop, int m, LossFunction lf, Vector in) {
		LinkedList<Vector> inout = getDatum(in);
		
        if (lf != null && inout.getLast() == null) {
            throw new MLException("Attempted to learn with a null truth value.");
        }
		
        inputLayer.addInput(inout.getFirst());
        outputLayer.addTruth(inout.getLast());
        
		inputLayer.compute(m, lf);
	}
	
	public List<double[]> doEpoch(double wEta, double bEta, int m, LossFunction lf, boolean computeSizes, boolean verboseError) {
		for (int p = 0; p < m; p++) {
		    if (trainer != null && p % 10 == (m - 1) % 10) {
		        trainer.notifyDatumNumber(this, p + 1, m);
		    }
		    
		    addDatum(true, m, lf, null);
		}
		List<double[]> ans = outputLayer.applyChanges(outputLayer.wGrad, outputLayer.bGrad, wEta, bEta, depth - 1, computeSizes);
		double error = outputLayer.getError();
		double angle = outputLayer.getAngle();
		ans.add(0, new double[] {error, angle});
		if (verboseError) {
		    printError(ans);
		}
		inputLayer.clearData();
		return ans;
	}
	
	public void printError(List<double[]> info) {
	    StringBuilder log = new StringBuilder();
	    log.append("++++++++++++++++++++++++++++++++++++++++++++++++++\n");
        int layer = -1;
        for (double[] nums : info) {
            if (layer == -1) {
                log.append(this + " ERROR: " + nums[0] + "\n");
                log.append(this + " ANGLE: " + (nums[1] / Math.PI) + "π\n");
            } else {
                log.append("Layer " + layer + ":\n");
                log.append("Size of w: " + nums[0] + "\n");
                log.append("Size of dw: " + nums[1] + "\n");
                log.append("Size of b: " + nums[2] + "\n");
                log.append("Size of db: " + nums[3] + "\n");
            }
            layer++;
        }
        log.append("++++++++++++++++++++++++++++++++++++++++++++++++++\n");
        if (trainer == null) {
            System.out.println(log);
        } else {
            trainer.addLog(this, log + "");
        }
	}
	
	public void printLayer(int l) {
		inputLayer.print(l);
	}
	
	public void saveToFile(File file) {
	    try {
	        file.getParentFile().mkdirs();
            BufferedWriter writer = new BufferedWriter(new FileWriter(file));
            inputLayer.writeToFile(writer);
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
	}
	
	public void openFromFile(String file) {
	    try {
            BufferedReader reader = FileTools.getInternalFile(file, this);
	        List<Matrix> ws = new LinkedList<>();
	        List<Vector> bs = new LinkedList<>();
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
            if (ws.size() == 0 || bs.size() == 0) {
                throw new MLException("File " + file + " either does not exist or was corrupted.");
            }
	        outputLayer = inputLayer.setWeights(ws, bs);
	        depth = inputLayer.getDepth();
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	
	public LinkedList<Vector> testValue(Vector in) {
	    addDatum(false, 0, null, in);
	    LinkedList<Vector> output = new LinkedList<>(Arrays.asList(new Vector[] {
            inputLayer.value, 
            outputLayer.value, 
            outputLayer.truth}));
        inputLayer.clearData();
	    return output;
	}
	
	public void printTest(boolean classifier) {
		addDatum(false, 0, null, null);
		double[] in = inputLayer.value.toArray();
		double[] out = outputLayer.value.toArray();
		double[] truth = outputLayer.truth.toArray();
		System.out.println(MLTools.vectorToString(in));
		if (classifier) {
	        System.out.println(labels[MLTools.classify(truth) - 1]);
	        System.out.println(labels[MLTools.classify(out) - 1]);
		} else {
		    System.out.println(MLTools.vectorToString(truth));
		    System.out.println(MLTools.vectorToString(out));
		}
		System.out.println();
		inputLayer.clearData();
	}
}
