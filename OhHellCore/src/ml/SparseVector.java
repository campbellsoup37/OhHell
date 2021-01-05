package ml;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class SparseVector implements Vector {
    private int totalSize = 0;
    
    private List<SparseVectorEntry> entries = new LinkedList<>();
    private List<Integer> sizes = new LinkedList<>();

    public SparseVector() {}

    public SparseVector(List<SparseVectorEntry> entries, int totalSize) {
        this.totalSize = totalSize;
        this.entries = entries;
        sizes.add(totalSize);
    }
    
    public int size() {
        return totalSize;
    }
    
    public double[] toArray() {
        double[] vec = new double[totalSize];
        for (SparseVectorEntry sve : entries) {
            vec[sve.key()] = sve.value();
        }
        return vec;
    }
    
    @Override
    public boolean isSparse() {
        return true;
    }
    
    @Override
    public List<SparseVectorEntry> getEntries() {
        return entries;
    }
    
    @Override
    public String toString() {
        return Arrays.toString(toArray());
    }
    
    public void addOneHot(int j, int d) {
        if (j <= 0 || j > d) {
            throw new MLException("Invalid index " + j + " for one-hot of size " + d + ".");
        }
        entries.add(new SparseVectorEntry(totalSize + j - 1, 1));
        sizes.add(d);
        totalSize += d;
    }
    
    public void addBinaryVector(List<Integer> js, int d) {
        for (int j : js) {
            if (j <= 0 || j > d) {
                throw new MLException("Invalid index " + j + " for sparse vector of size " + d + ".");
            }
            entries.add(new SparseVectorEntry(totalSize + j - 1, 1));
        }
        sizes.add(d);
        totalSize += d;
    }
    
    public void addZeros(int d) {
        sizes.add(d);
        totalSize += d;
    }
    
    @Override
    public Vector applyMatrix(Matrix M) {
        return M.applyVector(this);
    }
    
    @Override
    public double dot(Vector vector) {
        double[] vec = vector.toArray();
        double ans = 0;
        for (SparseVectorEntry sve : entries) {
            ans += vec[sve.key()] * sve.value();
        }
        return ans;
    }
    
    @Override
    public void add(Vector vector) {
        add(vector, 1);
    }
    
    @Override
    public void add(Vector vector, double scale) {
        if (size() != vector.size()) {
            throw new MLException("Attempted to add vectors of sizes " + size() + " and " + vector.size() + ".");
        } else if (!vector.isSparse()) {
            throw new MLException("Adding a non-sparse vector to a sparse vector is not supported.");
        } else {
            throw new MLException("Adding a sparse vector to a sparse vector is not yet supported.");
        }
    }

    @Override
    public double get(int i) {
        double[] vec = toArray();
        return vec[i];
    }
    
    @Override
    public double norm() {
        double square = 0;
        for (SparseVectorEntry sve : entries) {
            square += Math.pow(sve.value(), 2);
        }
        return Math.sqrt(square);
    }

    @Override
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
