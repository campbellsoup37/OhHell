import java.util.LinkedList;
import java.util.List;

public class BinaryLayerVector implements LayerVector {
    private int totalSize = 0;
    private List<Integer> ones = new LinkedList<>();
    private List<Integer> sizes = new LinkedList<>();
    
    public BinaryLayerVector() {}
    
    public int size() {
        return totalSize;
    }
    
    public double[] toArray() {
        double[] vec = new double[totalSize];
        for (Integer j : ones) {
            vec[j] = 1;
        }
        return vec;
    }
    
    public void addOneHot(int j, int d) {
        ones.add(totalSize + j - 1);
        sizes.add(d);
        totalSize += d;
    }
    
    public void addZeros(int d) {
        sizes.add(d);
        totalSize += d;
    }
    
    public LayerVector applyMatrix(double[][] M) {
        double[] newVec = new double[M.length];
        for (int i = 0; i < M.length; i++) {
            for (Integer j : ones) {
                newVec[i] += M[i][j];
            }
        }
        return new BasicLayerVector(newVec);
    }
    
    public LayerVector applyActivation(ActivationFunction af) {
        return null;
    }
    
    public double[] applyDActivation(ActivationFunction af) {
        return null;
    }
    
    public double[][][] makeNewDw(double[] newDValue) {
        double[][][] newDw = new double[newDValue.length][newDValue.length][totalSize];
        for (int i = 0; i < newDValue.length; i++) {
            for (Integer j : ones) {
                newDw[i][i][j] = newDValue[i];
            }
        }
        return newDw;
    }
    
    public LayerVector add(double[] vec) {
        double[] newVec = new double[vec.length];
        int i = 0;
        for (Integer j : ones) {
            for (; i < j; i++) {
                newVec[i] = vec[i];
            }
            newVec[i] = 1 + vec[i];
            i++;
        }
        for (; i < totalSize; i++) {
            newVec[i] = vec[i];
        }
        return new BasicLayerVector(newVec);
    }
    
    public double[] minus(LayerVector vec) {
        double[] vecArr = vec.toArray();
        double[] newVec = new double[vecArr.length];
        int i = 0;
        for (Integer j : ones) {
            for (; i < j; i++) {
                newVec[i] = -vecArr[i];
            }
            newVec[i] = 1 - vecArr[i];
            i++;
        }
        for (; i < totalSize; i++) {
            newVec[i] = -vecArr[i];
        }
        return newVec;
    }
    
    public double get(int i) {
        return 0;
    }
    
    public void print() {
        double[] vec = toArray();
        int i = 0;
        for (Integer size : sizes) {
            for (int j = 0; j < size; j++) {
                System.out.print((int) vec[i + j] + " ");
            }
            i += size;
            System.out.println();
        }
        System.out.println();
    }
}
