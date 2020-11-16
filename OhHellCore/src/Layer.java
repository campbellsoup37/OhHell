import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Layer {
	public int d;
	public double[][] w;
	public double[] b;
	public ActivationFunction af;
	
	public LayerVector lin;
	public LayerVector value;
	public double[] dvalue;
	
	public ArrayList<double[][][]> dws;
	public ArrayList<double[][]> dbs;
	
	public ArrayList<double[][]> wgrads;
	public ArrayList<double[]> bgrads;
	
	Layer prev, next;
	boolean isInput = true;
	boolean isOutput = true;
	public LayerVector truth;
	public double[] delta;
	public double error = 0;
	
	public Layer(int d, ActivationFunction af) {
		this.d = d;
		this.af = af;
	}
	
	public void setPrev(Layer prev) {
		this.prev = prev;
		isInput = false;
		
		w = new double[d][prev.d];
		b = new double[d];
		
		Random r = new Random();
		for (int i = 0; i < d; i++) {
		    for (int j = 0; j < prev.d; j++) {
		        w[i][j] = r.nextGaussian();
		    }
		    //b[i] = r.nextGaussian();
		}
	}
	
	public void setWeights(List<double[][]> ws, List<double[]> bs) {
	    if (!isInput) {
	        w = ws.remove(0);
	        b = bs.remove(0);
	    }
	    if (!isOutput) {
	        next.setWeights(ws, bs);
	    }
	}
	
	public void addLayer(Layer layer) {
		if(!isOutput) next.addLayer(layer);
		else {
			next = layer;
			isOutput = false;
			layer.setPrev(this);
		}
	}
	
	public void addInput(LayerVector vec) {
		if (isInput) {
		    value = vec;
		}
	}
	
	public void addTruth(LayerVector y) {
		if (isOutput) {
		    truth = y;
		}
	}
	
	public void compute(boolean backprop, double wEta, double bEta, double m) {
		if (!isInput) {
			// Compute new values
			LayerVector oldValue = prev.value;
			lin = oldValue.applyMatrix(w).add(b);
			
			value = lin.applyActivation(af);
			
			if (backprop) {
				// Compute differentials
	            dvalue = lin.applyDActivation(af);
	            if (isOutput) {
	                delta = value.minus(truth);
	                error += MLTools.vectorSize(delta) / m;
	            }
	            
			    int newSize = 1;
			    if (prev.dws != null && !prev.dws.isEmpty()) {
			        newSize += prev.dws.size();
			    }
				dws = new ArrayList<>(newSize);
				dbs = new ArrayList<>(newSize);
				
				if (isOutput && wgrads == null) {
				    wgrads = new ArrayList<>(newSize);
				    bgrads = new ArrayList<>(newSize);
				}
				
				if (prev.dws != null && !prev.dws.isEmpty()) {
					for (int p = 0; p < prev.dws.size(); p++) {
					    double[][][] oldDw = prev.dws.get(p);
					    double[][] oldDb = prev.dbs.get(p);
					    double[][][] newDw = new double[d][oldDw[0].length][oldDw[0][0].length];
					    double[][] newDb = new double[d][oldDb[0].length];
					    if (isOutput && wgrads.size() < p + 1) {
					        wgrads.add(new double[oldDw[0].length][oldDw[0][0].length]);
					        bgrads.add(new double[oldDb[0].length]);
					    }
                        for (int i = 0; i < d; i++) {
    						for (int j = 0; j < oldDw[0].length; j++) {
    						    for (int k = 0; k < oldDw[0][0].length; k++) {
    								if (p == prev.dws.size() - 1) {
    								    newDw[i][j][k] = dvalue[i] * w[i][j] * oldDw[j][j][k];
    								} else {
    								    for (int q = 0; q < prev.d; q++) {
    								        newDw[i][j][k] += dvalue[i] * w[i][q] * oldDw[q][j][k];
    								    }
    								}
    								if (isOutput) {
    								    wgrads.get(p)[j][k] += wEta / m * 2 * delta[i] * newDw[i][j][k];
    								}
    							}
    						    if (p == prev.dws.size() - 1) {
                                    newDb[i][j] = dvalue[i] * w[i][j] * oldDb[j][j];
                                } else {
                                    for (int q = 0; q < prev.d; q++) {
                                        newDb[i][j] += dvalue[i] * w[i][q] * oldDb[q][j];
                                    }
                                }
    						    if (isOutput) {
    						        bgrads.get(p)[j] += bEta / m * 2 * delta[i] * newDb[i][j];
    						    }
    						}
						}
						dws.add(newDw);
						dbs.add(newDb);
					}
				}
				/*double[][][] newDw = new double[d][d][prev.d];
				for (int i = 0; i < d; i++) {
			        for (int k = 0; k < prev.d; k++) {
    					newDw[i][i][k] = newDValue[i] * prev.values.getLast()[k];
    				}
				}*/
				dws.add(prev.value.makeNewDw(dvalue));
				double[][] newDb = new double[d][d];
				for (int i = 0; i < d; i++) {
				    newDb[i][i] = dvalue[i];
				}
				dbs.add(newDb);
				if (isOutput && wgrads.size() < newSize) {
				    wgrads.add(new double[d][prev.d]);
				    bgrads.add(new double[d]);
                    for (int j = 0; j < d; j++) {
                        for (int k = 0; k < prev.d; k++) {
                            wgrads.get(newSize - 1)[j][k] += wEta / m * 2 * delta[j] * dws.get(newSize - 1)[j][j][k];
                        }
                        bgrads.get(newSize - 1)[j] += bEta / m * 2 * delta[j] * dbs.get(newSize - 1)[j][j];
                    }
                }
			}
		}
		if (!isOutput) {
		    next.compute(backprop, wEta, bEta, m);
		}
	}
	
	public void applyChanges(ArrayList<double[][]> wgrads, ArrayList<double[]> bgrads, int l) {
		if (l >= 0) {
		    /*System.out.println("Layer " + l + ": ");
		    System.out.println("Size of w: " + MLTools.matrixSize(w));
            System.out.println("Size of dw: " + MLTools.matrixSize(wgrads.get(l)));
		    System.out.println("Size of b: " + MLTools.vectorSize(b));
            System.out.println("Size of db: " + MLTools.vectorSize(bgrads.get(l)));*/
		    for (int j = 0; j < d; j++) {
		        for (int k = 0; k < prev.d; k++) {
		            w[j][k] -= wgrads.get(l)[j][k];
		        }
		        b[j] -= bgrads.get(l)[j];
		    }
			prev.applyChanges(wgrads, bgrads, l - 1);
		}
	}
	
	public double getError() {
		if (!isOutput) {
		    return next.getError();
		} else {
		    return error;
		}
	}
	
	public void clearData() {
		if (!isOutput) {
		    next.clearData();
		} else {
		    wgrads = null;
		    bgrads = null;
		    error = 0;
		}
	}
	
	public void print(int l) {
        if (l == 0) {
            System.out.println(MLTools.matrixToString(w));
            System.out.println(MLTools.vectorToString(b));
            System.out.println();
        } else {
            next.print(l - 1);
        }
    }
	
	public void writeToFile(BufferedWriter writer) throws IOException {
	    if (!isInput) {
	        writer.write(MLTools.matrixToString(w) + "\n\n");
	        writer.write(MLTools.vectorToString(b) + "\n\n");
        }
        if (!isOutput) {
            next.writeToFile(writer);
        }
    }
}
