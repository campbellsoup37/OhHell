package ml;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class BasicVector implements Vector {
    private int totalSize = 0;
    private List<double[]> subVecs = new LinkedList<>();
    
    public BasicVector() {}
    
    public BasicVector(double[] vec) {
        subVecs.add(vec);
        totalSize = vec.length;
    }

    @Override
    public int size() {
        return totalSize;
    }
    
    @Override
    public boolean isSparse() {
        return false;
    }
    
    @Override
    public List<SparseVectorEntry> getEntries() {
        throw new MLException("Attempted to get sparse entries from a non-sparse vector.");
    }
    
    public void flatten() {
        if (subVecs.size() > 1) {
            double[] vec = toArray();
            subVecs = new LinkedList<>();
            subVecs.add(vec);
        }
    }

    @Override
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
    
    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
    
    public void addData(double[] vec) {
        subVecs.add(vec);
        totalSize += vec.length;
    }
    
    @Override
    public void scale(double s) {
        for (double[] vec : subVecs) {
            for (int i = 0; i < vec.length; i++) {
                vec[i] *= s;
            }
        }
    }

    @Override
    public Vector applyMatrix(Matrix M) {
        return M.applyVector(this);
    }
    
    @Override
    public double dot(Vector vector) {
        if (vector.isSparse()) {
            return vector.dot(this);
        } else {
            double[] vec = vector.toArray();
            double ans = 0;
            int j = 0;
            for (double[] subVec : subVecs) {
                for (double v : subVec) {
                    ans += vec[j] * v;
                    j++;
                }
            }
            return ans;
        }
    }

    @Override
    public Vector add(Vector vector) {
        return add(vector, 1);
    }
    
    @Override
    public Vector add(Vector vector, double scale) {
        if (size() != vector.size()) {
            throw new MLException("Attempted to add vectors of sizes " + size() + " and " + vector.size() + ".");
        }
        
        flatten();
        double[] thisVec = toArray();
        double[] vec = vector.toArray();
        for (int j = 0; j < thisVec.length; j++) {
            thisVec[j] += vec[j] * scale;
        }
        return this;
    }

    @Override
    public double get(int i) {
        flatten();
        return subVecs.get(0)[i];
    }
    
    @Override
    public double norm() {
        double ans = 0;
        for (double[] subVec : subVecs) {
            for (double v : subVec) {
                ans += v * v;
            }
        }
        return Math.sqrt(ans);
    }
    
    @Override
    public boolean entrywiseEquals(Vector v) {
        if (size() != v.size()) {
            return false;
        }
        double[] arr = toArray();
        double[] vArr = v.toArray();
        for (int i = 0; i < size(); i++) {
            if (arr[i] != vArr[i]) {
                return false;
            }
        }
        return true;
    }
    
    @Override
    public Vector copy() {
        if (subVecs.size() == 1) {
            double[] arrCopy = new double[totalSize];
            for (int i = 0; i < totalSize; i++) {
                arrCopy[i] = subVecs.get(0)[i];
            }
            return new BasicVector(arrCopy);
        } else {
            return new BasicVector(toArray());
        }
    }

    @Override
    public void print() {
        for (String line : printL()) {
            System.out.println(line);
        }
        System.out.println();
    }
    
    @Override
    public List<String> printL() {
        List<String> ans = new LinkedList<>();
        for (double[] vec : subVecs) {
            String line = "";
            for (double v : vec) {
                line += v + " ";
            }
            ans.add(line);
        }
        return ans;
    }
}
