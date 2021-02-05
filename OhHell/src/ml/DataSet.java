package ml;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.function.Function;

public class DataSet implements Iterable<List<Vector>> {
    public class DataSetIterator implements Iterator<List<Vector>> {
        private int index = -1;
        private int next = 0;
        
        private void updateNext() {
            if (next <= index) {
                for (next = index + 1; 
                        next < inOuts.size() 
                            && leftOut.contains(inOuts.get(next)); 
                        next++);
            }
        }
        
        @Override
        public boolean hasNext() {
            updateNext();
            return next < inOuts.size();
        }

        @Override
        public List<Vector> next() {
            updateNext();
            index = next;
            return inOuts.get(index);
        }
    }
    
    private List<List<Vector>> inOuts;
    private Set<List<Vector>> leftOut = new HashSet<>();
    private Random random = new Random();
    
    public DataSet() {};
    
    public DataSet(String file) {
        loadFromFile(file, -1);
    }
    
    public DataSet(String file, int limit) {
        loadFromFile(file, limit);
    }
    
    public DataSet(List<List<Vector>> list) {
        inOuts = list;
    }
    
    public DataSet(List<List<Vector>> list, Set<List<Vector>> leftOut) {
        inOuts = list;
        this.leftOut = leftOut;
    }
    
    public void loadFromFile(String file, int limit) {
        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            List<String> allLines = new LinkedList<>();
            for (String line = br.readLine(); line != null && allLines.size() != 2 * limit; line = br.readLine()) {
                if (!line.isEmpty()) {
                    allLines.add(line);
                }
            }
            inOuts = new ArrayList<>(allLines.size() / 2);
            Vector in = null;
            for (String line : allLines) {
                Vector v = MLTools.stringToVector(line);
                if (in == null) {
                    in = v;
                } else {
                    inOuts.add(Arrays.asList(in, v));
                    in = null;
                }
            }
            br.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public int size() {
        return inOuts.size() - leftOut.size();
    }
    
    /*public List<List<Vector>> asList() {
        return inOuts;
    }*/
    
    public DataSet complement() {
        List<List<Vector>> data = new ArrayList<>(leftOut.size());
        for (List<Vector> datum : leftOut) {
            data.add(datum);
        }
        return new DataSet(data);
    }
    
    public void sortByInputEntry(int j) {
        inOuts.sort((datum1, datum2) -> (int) Math.signum(datum1.get(0).get(j) - datum2.get(0).get(j)));
    }
    
    public DataSet[] split(int j, double s) {
        List<List<Vector>> left = new LinkedList<>();
        List<List<Vector>> right = new LinkedList<>();
        for (List<Vector> datum : this) {
            if (!leftOut.contains(datum)) {
                if (datum.get(0).get(j) <= s) {
                    left.add(datum);
                } else {
                    right.add(datum);
                }
            }
        }
        
        return new DataSet[] {
                new DataSet(new ArrayList<>(left)), 
                new DataSet(new ArrayList<>(right))};
    }
    
    public List<Vector> randomSample() {
        return inOuts.get(random.nextInt(size()));
    }
    
    public DataSet bootstrapSample(int size) {
        if (size < 0) {
            throw new MLException("Attempted to take a subset of size " + size + " from a dataset of size " + size() + ".");
        }
        List<List<Vector>> sub = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            sub.add(inOuts.get(random.nextInt(size())));
        }
        return new DataSet(sub);
    }

    @Override
    public Iterator<List<Vector>> iterator() {
        return new DataSetIterator();
    }
    
    public void shuffle() {
        for (int i = 0; i < size(); i++) {
            int j = random.nextInt(size() - i) + i;
            List<Vector> temp = inOuts.get(i);
            inOuts.set(i, inOuts.get(j));
            inOuts.set(j, temp);
        }
    }
    
    public void allocateTestData(int testSize) {
        shuffle();
        for (int k = 0; k < testSize; k++) {
            leftOut.add(inOuts.get(k));
        }
    }
    
    public List<DataSet> cvPartition(int foldsK) {
        shuffle();
        List<DataSet> partition = new ArrayList<>(foldsK);
        for (int k = 0; k < foldsK; k++) {
            Set<List<Vector>> leftOutK = new HashSet<>();
            for (int i = k * size() / foldsK; i < (k + 1) * size() / foldsK; i++) {
                leftOutK.add(inOuts.get(i));
            }
            partition.add(new DataSet(inOuts, leftOutK));
        }
        return partition;
    }
    
    public DataSet deepCopy() {
        List<List<Vector>> inOutsCopy = new ArrayList<>(inOuts.size());
        Set<List<Vector>> leftOutCopy = new HashSet<>(leftOut.size());
        for (List<Vector> inOut : inOuts) {
            List<Vector> inOutCopy = new ArrayList<>(inOut.size());
            for (Vector vec : inOut) {
                inOutCopy.add(vec.copy());
            }
            inOutsCopy.add(inOutCopy);
            if (leftOut.contains(inOut)) {
                leftOutCopy.add(inOutCopy);
            }
        }
        return new DataSet(inOutsCopy, leftOutCopy);
    }
    
    public void map(Function<List<Vector>, List<Vector>> map) {
        for (List<Vector> inOut : inOuts) {
            List<Vector> transform = map.apply(inOut);
            for (int i = 0; i < inOut.size(); i++) {
                inOut.set(i, transform.get(i));
            }
        }
    }
    
    public void print() {
        System.out.println("--------------------------");
        for (List<Vector> datum : inOuts) {
            if (!leftOut.contains(datum)) {
                System.out.println(datum.get(0));
                System.out.println(datum.get(1));
                System.out.println();
            }
        }
        System.out.println("--------------------------");
    }
}