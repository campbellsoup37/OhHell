package ml;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

public class Layer {
	public int d;
	public Matrix w;
	public Vector b;
	public ActivationFunction af;
	
	public Vector lin;
	public Vector value;
	public Matrix afJac;
	
	//public ArrayList<Map<Long, Vector>> dw;
	//public ArrayList<Map<Integer, Vector>> db;
	public ArrayList<Vector[][]> dw;
	public ArrayList<Vector[]> db;
	
	private Layer prev;
	private Layer next;
	private boolean isInput = true;
	private boolean isOutput = true;
	
	public Vector truth;
	public double[] delta;
	public double error = 0;
    public ArrayList<Matrix> wGrad;
    public ArrayList<Vector> bGrad;
    public ArrayList<Matrix> prevWGrad;
    public ArrayList<Vector> prevBGrad;
	
	public Layer(int d, ActivationFunction af) {
		this.d = d;
		this.af = af;
	}
	
	public int getDepth() {
	    if (isOutput) {
	        return 0;
	    } else {
	        return 1 + next.getDepth();
	    }
	}
	
	public void setPrev(Layer prev) {
		this.prev = prev;
		isInput = false;
		
		double[][] w = new double[d][prev.d];
		double[] b = new double[d];
		
		Random r = new Random();
		for (int i = 0; i < d; i++) {
		    for (int j = 0; j < prev.d; j++) {
		        w[i][j] = r.nextGaussian();
		    }
		    //b[i] = r.nextGaussian();
		}
		
		this.w = new BasicMatrix(w);
		this.b = new BasicVector(b);
	}
	
	public Layer setWeights(List<Matrix> ws, List<Vector> bs) {
	    if (!isInput) {
	        w = ws.remove(0);
	        b = bs.remove(0);
	        d = w.numRows();
	    }
	    if (!ws.isEmpty()) {
	        if (next == null) {
	            Layer newNext = new Layer(0, null);
	            addLayer(newNext);
	        }
	        return next.setWeights(ws, bs);
	    }
	    return this;
	}
	
	public void setActFuncs(List<ActivationFunction> afs) {
	    if (!isInput) {
            af = afs.remove(0);
        }
        if (!isOutput) {
            next.setActFuncs(afs);
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
	
	public void addInput(Vector vec) {
		if (isInput) {
		    value = vec;
		}
	}
	
	public void addTruth(Vector y) {
		if (isOutput) {
		    truth = y;
		}
	}
	
	/*public void compute(double m, LossFunction lf) {
        if (!isInput) {
            // Compute new values
            Vector oldValue = prev.value;
            lin = oldValue.applyMatrix(w);
            lin.add(b);
            
            value = af.a(lin);
            
            if (lf != null) {
                // Compute differentials
                afJac = af.da(lin);
                
                boolean firstCompLayer = prev.dw != null && !prev.dw.isEmpty();
                int newSize = 1;
                if (firstCompLayer) {
                    newSize += prev.dw.size();
                }
                dw = new ArrayList<>(newSize);
                db = new ArrayList<>(newSize);
                
                if (firstCompLayer) {
                    for (int p = 0; p < prev.dw.size(); p++) {
                        Map<Long, Vector> oldDw = prev.dw.get(p);
                        Map<Integer, Vector> oldDb = prev.db.get(p);
                        dw.add(new Hashtable<>());
                        db.add(new Hashtable<>());
                        dw.get(p).put(-1L, oldDw.get(-1L));
                        db.get(p).put(-1, oldDb.get(-1));
                        for (long ij : oldDw.keySet()) {
                            if (ij == -1) {
                                continue;
                            }
                            dw.get(p).put(ij, oldDw.get(ij).applyMatrix(w).applyMatrix(afJac));
                        }
                        for (int i : oldDb.keySet()) {
                            if (i == -1) {
                                continue;
                            }
                            db.get(p).put(i, oldDb.get(i).applyMatrix(w).applyMatrix(afJac));
                        }
                    }
                }
                dw.add(new Hashtable<>());
                dw.get(newSize - 1).put(-1L, new BasicVector(new double[] {d, prev.d}));
                db.add(new Hashtable<>());
                db.get(newSize - 1).put(-1, new BasicVector(new double[] {d}));
                for (int i = 0; i < d; i++) {
                    if (prev.value.isSparse()) {
                        for (Integer j : prev.value.getOnes()) {
                            dw.get(newSize - 1).put(pairToLong(i, j), afJac.scaledCol(i, 1));
                        }
                    } else {
                        for (int j = 0; j < prev.d; j++) {
                            dw.get(newSize - 1).put(pairToLong(i, j), afJac.scaledCol(i, prev.value.get(j)));
                        }
                    }
                    db.get(newSize - 1).put(i, afJac.scaledCol(i, 1));
                }
                
                if (isOutput) {
                    computeLossAndGradient(m, lf);
                }
            }
        }
        if (!isOutput) {
            next.compute(m, lf);
        }
    }
    
    public void computeLossAndGradient(double m, LossFunction lf) {
        // Loss
        error += lf.loss(value, truth) / m;
        
        // Gradient
        if (wGrad == null) {
            wGrad = new ArrayList<>(dw.size());
            bGrad = new ArrayList<>(db.size());
        }
        for (int p = 0; p < dw.size(); p++) {
            Map<Long, Vector> dwp = dw.get(p);
            Map<Integer, Vector> dbp = db.get(p);
            
            double[][] newWGrad = new double[(int) dwp.get(-1L).get(0)][(int) dwp.get(-1L).get(1)];
            double[] newBGrad = new double[(int) dbp.get(-1).get(0)];
            for (long ij : dwp.keySet()) {
                if (ij == -1) {
                    continue;
                }
                int[] IJ = longToPair(ij);
                newWGrad[IJ[0]][IJ[1]] = lf.dloss(value, truth).dot(dwp.get(ij)) / m;
            }
            for (int i : dbp.keySet()) {
                if (i == -1) {
                    continue;
                }
                newBGrad[i] = lf.dloss(value, truth).dot(dbp.get(i)) / m;
            }
            
            if (wGrad.size() < p + 1) {
                wGrad.add(new BasicMatrix(newWGrad));
                bGrad.add(new BasicVector(newBGrad));
            } else {
                wGrad.get(p).add(new BasicMatrix(newWGrad));
                bGrad.get(p).add(new BasicVector(newBGrad));
            }
        }
    }
    
    public static long pairToLong(int i, int j) {
        return ((long) i << 32) + j;
    }
    
    public static int[] longToPair(long ij) {
        int j = (int) (ij % (1 << 31));
        int i = (int) (ij >> 32);
        return new int[] {i, j};
    }*/
	
	public void compute(double m, LossFunction lf) {
        if (!isInput) {
            // Compute new values
            Vector oldValue = prev.value;
            lin = oldValue.applyMatrix(w);
            lin.add(b);
            
            value = af.a(lin);
            
            if (lf != null) {
                // Compute differentials
                afJac = af.da(lin);
                
                boolean firstCompLayer = prev.dw != null && !prev.dw.isEmpty();
                int newSize = 1;
                if (firstCompLayer) {
                    newSize += prev.dw.size();
                }
                dw = new ArrayList<>(newSize);
                db = new ArrayList<>(newSize);
                
                if (firstCompLayer) {
                    for (int p = 0; p < prev.dw.size(); p++) {
                        Vector[][] oldDw = prev.dw.get(p);
                        Vector[] oldDb = prev.db.get(p);
                        dw.add(new Vector[oldDw.length][oldDw[0].length]);
                        db.add(new Vector[oldDb.length]);
                        for (int i = 0; i < oldDw.length; i++) {
                            for (int j = 0; j < oldDw[0].length; j++) {
                                if (oldDw[i][j] != null) {
                                    dw.get(p)[i][j] = oldDw[i][j].applyMatrix(w).applyMatrix(afJac);
                                }
                            }
                            db.get(p)[i] = oldDb[i].applyMatrix(w).applyMatrix(afJac);
                        }
                    }
                }
                dw.add(new Vector[d][prev.d]);
                db.add(new Vector[d]);
                for (int i = 0; i < d; i++) {
                    if (prev.value.isSparse()) {
                        for (SparseVectorEntry sve : prev.value.getEntries()) {
                            dw.get(newSize - 1)[i][sve.key()] = afJac.scaledCol(i, sve.value());
                        }
                    } else {
                        for (int j = 0; j < prev.d; j++) {
                            dw.get(newSize - 1)[i][j] = afJac.scaledCol(i, prev.value.get(j));
                        }
                    }
                    db.get(newSize - 1)[i] = afJac.scaledCol(i, 1);
                }
                
                if (isOutput) {
                    computeLossAndGradient(m, lf);
                }
            }
        }
        if (!isOutput) {
            next.compute(m, lf);
        }
    }
    
    public void computeLossAndGradient(double m, LossFunction lf) {
        // Loss
        error += lf.loss(value, truth) / m;
        
        // Gradient
        if (wGrad == null) {
            wGrad = new ArrayList<>(dw.size());
            bGrad = new ArrayList<>(db.size());
        }
        for (int p = 0; p < dw.size(); p++) {
            Vector[][] dwp = dw.get(p);
            Vector[] dbp = db.get(p);
            
            double[][] newWGrad = new double[dwp.length][dwp[0].length];
            double[] newBGrad = new double[dbp.length];
            for (int i = 0; i < dwp.length; i++) {
                for (int j = 0; j < dwp[0].length; j++) {
                    if (dw.get(p)[i][j] != null) {
                        newWGrad[i][j] = lf.dloss(value, truth).dot(dw.get(p)[i][j]) / m;
                    }
                }
                newBGrad[i] = lf.dloss(value, truth).dot(db.get(p)[i]) / m;
            }
            
            if (wGrad.size() < p + 1) {
                wGrad.add(new BasicMatrix(newWGrad));
                bGrad.add(new BasicVector(newBGrad));
            } else {
                wGrad.get(p).add(new BasicMatrix(newWGrad));
                bGrad.get(p).add(new BasicVector(newBGrad));
            }
        }
    }
	
	public List<double[]> applyChanges(ArrayList<Matrix> wGrads, ArrayList<Vector> bGrads, double wEta, double bEta, int l, boolean computeSizes) {
		if (l >= 0) {
		    double[] sizes = {};
		    if (computeSizes) {
		        sizes = new double[] {
		                w.norm(),
		                wGrads.get(l).norm(),
		                b.norm(),
		                bGrads.get(l).norm()
		        };
		    }
		    w.add(wGrads.get(l), -wEta);
		    b.add(bGrads.get(l), -wEta);
		    
		    if (l == 0) {
		        return new LinkedList<>(Arrays.asList(sizes));
		    } else {
		        List<double[]> prevSizes = prev.applyChanges(wGrads, bGrads, wEta, bEta, l - 1, computeSizes);
		        prevSizes.add(sizes);
		        return prevSizes;
		    }
		} else {
		    return null;
		}
	}
	
	public double getError() {
		if (!isOutput) {
		    return next.getError();
		} else {
		    return error;
		}
	}
	
	public double getAngle() {
	    if (!isOutput) {
	        return next.getAngle();
	    } else if (prevWGrad == null) {
	        return 0;
	    } else {
	        double dot = 0;
	        double size = 0;
	        double prevSize = 0;
	        /*for (int i = 0; i < wgrads.size(); i++) {
	            for (int j = 0; j < wgrads.get(i).length; j++) {
	                for (int k = 0; k < wgrads.get(i)[j].length; k++) {
	                    dot += wgrads.get(i)[j][k] * prevWgrads.get(i)[j][k];
	                    size += wgrads.get(i)[j][k] * wgrads.get(i)[j][k];
	                    prevSize += prevWgrads.get(i)[j][k] * prevWgrads.get(i)[j][k];
	                }
	            }
	            for (int j = 0; j < bgrads.get(i).length; j++) {
                    dot += bgrads.get(i)[j] * prevBgrads.get(i)[j];
                    size += bgrads.get(i)[j] * bgrads.get(i)[j];
                    prevSize += prevBgrads.get(i)[j] * prevBgrads.get(i)[j];
	            }
	        }*/
	        
	        return Math.acos(dot / Math.sqrt(size * prevSize));
	    }
	}
	
	public void clearData() {
		if (!isOutput) {
		    next.clearData();
		} else {
		    if (wGrad != null) {
	            prevWGrad = wGrad;
	            prevBGrad = bGrad;
		    }
		    wGrad = null;
		    bGrad = null;
		    error = 0;
		}
	}
	
	public void print(int l) {
        if (l == 0) {
            System.out.println(w.toString());
            System.out.println(b.toString());
            System.out.println();
        } else {
            next.print(l - 1);
        }
    }
	
	public void writeToFile(BufferedWriter writer) throws IOException {
	    if (!isInput) {
	        writer.write(MLTools.matrixToString(w.toArray()) + "\n\n");
	        writer.write(MLTools.vectorToString(b.toArray()) + "\n\n");
        }
        if (!isOutput) {
            next.writeToFile(writer);
        }
    }
}
