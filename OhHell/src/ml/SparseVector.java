package ml;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class SparseVector implements Vector {
    private int totalSize = 0;
    
    private List<SparseVectorEntry> entries = new LinkedList<>();
    private Map<String, SparseVectorChunk> chunks = new LinkedHashMap<>();

    public SparseVector() {}

    public SparseVector(List<SparseVectorEntry> entries, int totalSize) {
        this.totalSize = totalSize;
        this.entries = entries;
        chunks.put("", new SparseVectorChunk("", 0, 0, 0, totalSize, false));
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
        addOneHot("", j, 0, d);
    }
    
    public void addOneHot(String feature, int val, int min, int max) {
        if (val < min || val > max) {
            throw new MLException("Invalid value " + val + " for one-hot in [" + min + ", " + max + "].");
        }
        if (val != min) {
            entries.add(new SparseVectorEntry(totalSize + val - min - 1, 1));
        }
        chunks.put(feature, new SparseVectorChunk(feature, totalSize, val, min, max, true));
        totalSize += max - min;
    }
    
    public void modifyChunk(String feature, int newVal) {
        SparseVectorChunk chunk = chunks.get(feature);
        if (chunk == null) {
            throw new MLException("Attempted to modify the nonexisting feature " + feature + ".");
        }
        if (chunk.isDiscrete) {
            if (newVal < chunk.min || newVal > chunk.max) {
                throw new MLException("Invalid value " + newVal + " for one-hot in [" + chunk.min + ", " + chunk.max + "].");
            }
            if (chunk.val > chunk.min) {
                int index = chunk.offset + chunk.val - chunk.min - 1;
                for (SparseVectorEntry sve : entries) {
                    if (sve.key() == index) {
                        sve.setKey(chunk.offset + newVal - chunk.min - 1);
                        break;
                    }
                }
            } else {
                if (newVal > chunk.min) {
                    entries.add(new SparseVectorEntry(chunk.offset + newVal - chunk.min - 1, 1));
                }
            }
            chunk.val = newVal;
        } else {
            throw new MLException("Modifying nondiscrete chunks not yet supported.");
        }
    }
    
    public void addBinaryVector(List<Integer> js, int d) {
        addBinaryVector("", js, d);
    }
    
    public void addBinaryVector(String feature, List<Integer> js, int d) {
        for (int j : js) {
            if (j <= 0 || j > d) {
                throw new MLException("Invalid index " + j + " for sparse vector of size " + d + ".");
            }
            entries.add(new SparseVectorEntry(totalSize + j - 1, 1));
        }
        chunks.put(feature, new SparseVectorChunk(feature, totalSize, 0, 0, d, false));
        totalSize += d;
    }
    
    public void addZeros(int d) {
        addOneHot("", 0, 0, d);
    }
    
    @Override
    public void scale(double s) {
        for (SparseVectorEntry sve : entries) {
            sve.setValue(sve.value() * s);
        }
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
    public Vector add(Vector vector) {
        return add(vector, 1);
    }
    
    @Override
    public Vector add(Vector vector, double scale) {
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
        for (SparseVectorEntry sve : entries) {
            if (sve.key() == i) {
                return sve.value();
            }
        }
        return 0;
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
        List<SparseVectorEntry> entriesCopy = new LinkedList<>();
        for (SparseVectorEntry sve : entries) {
            entriesCopy.add(new SparseVectorEntry(sve.key(), sve.value()));
        }
        return new SparseVector(entriesCopy, totalSize);
    }

    @Override
    public void print() {
        double[] vec = toArray();
        for (SparseVectorChunk chunk : chunks.values()) {
            System.out.print(chunk.feature + ": ");
            if (chunk.isDiscrete) {
                System.out.print(chunk.val + " (min " + chunk.min + ", max " + chunk.max + ") ");
            }
            for (int j = 0; j < chunk.size; j++) {
                System.out.print((int) vec[chunk.offset + j] + " ");
            }
            
            System.out.println();
        }
        System.out.println();
    }
    
    private class SparseVectorChunk {
        public String feature;
        public int offset;
        public int val;
        public int min;
        public int max;
        public int size;
        public boolean isDiscrete;
        
        public SparseVectorChunk(String feature, int offset, int val, int min, int max, boolean isDiscrete) {
            this.feature = feature;
            this.offset = offset;
            this.val = val;
            this.min = min;
            this.max = max;
            size = max - min;
            this.isDiscrete = isDiscrete;
        }
    }
}
