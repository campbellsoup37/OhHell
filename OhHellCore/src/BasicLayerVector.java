import java.util.LinkedList;
import java.util.List;

public class BasicLayerVector implements LayerVector {
    private int totalSize = 0;
    private List<double[]> subVecs = new LinkedList<>();
    
    public BasicLayerVector() {}
    
    public BasicLayerVector(double[] vec) {
        subVecs.add(vec);
        totalSize = vec.length;
    }
    
    public int size() {
        return totalSize;
    }
    
    public double[] toArray() {
        if (subVecs.size() == 1) {
            return subVecs.get(0);
        } else {
            double[] vec = new double[totalSize];
            int j = 0;
            for (double[] subVec : subVecs) {
                for (double v : subVec) {
                    vec[j] = v;
                    j++;
                }
            }
            return vec;
        }
    }
    
    public void addData(double[] vec) {
        subVecs.add(vec);
        totalSize += vec.length;
    }
    
    public LayerVector applyMatrix(double[][] M) {
        double[] newVec = new double[M.length];
        for (int i = 0; i < M.length; i++) {
            int j = 0;
            for (double[] vec : subVecs) {
                for (double v : vec) {
                    newVec[i] += M[i][j] * v;
                    j++;
                }
            }
        }
        return new BasicLayerVector(newVec);
    }
    
    public LayerVector applyActivation(ActivationFunction af) {
        double[] newVec = new double[totalSize];
        int i = 0;
        for (double[] vec : subVecs) {
            for (double v : vec) {
                newVec[i] = af.a(v);
                i++;
            }
        }
        return new BasicLayerVector(newVec);
    }
    
    public double[] applyDActivation(ActivationFunction af) {
        double[] newVec = new double[totalSize];
        int i = 0;
        for (double[] vec : subVecs) {
            for (double v : vec) {
                newVec[i] = af.da(v);
                i++;
            }
        }
        return newVec;
    }
    
    public double[][][] makeNewDw(double[] newDValue) {
        double[][][] newDw = new double[newDValue.length][newDValue.length][totalSize];
        for (int i = 0; i < newDValue.length; i++) {
            int j = 0;
            for (double[] vec : subVecs) {
                for (double v : vec) {
                    newDw[i][i][j] = newDValue[i] * v;
                    j++;
                }
            }
        }
        return newDw;
    }
    
    public LayerVector add(double[] vec) {
        double[] newVec = new double[vec.length];
        int j = 0;
        for (double[] subVec : subVecs) {
            for (double v : subVec) {
                newVec[j] = v + vec[j];
                j++;
            }
        }
        return new BasicLayerVector(newVec);
    }
    
    public double[] minus(LayerVector vec) {
        double[] vecArr = vec.toArray();
        double[] newVec = new double[vecArr.length];
        int j = 0;
        for (double[] subVec : subVecs) {
            for (double v : subVec) {
                newVec[j] = v - vecArr[j];
                j++;
            }
        }
        return newVec;
    }
    
    public double get(int i) {
        return subVecs.get(0)[i];
    }
    
    public void print() {
        for (double[] vec : subVecs) {
            for (double v : vec) {
                System.out.print(v + " ");
            }
            System.out.println();
        }
        System.out.println();
    }
}
