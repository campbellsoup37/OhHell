package ml;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class DiagonalMatrix implements Matrix {
    double[] diagonal;
    
    public DiagonalMatrix(double[] diagonal) {
        this.diagonal = diagonal;
    }
    
    @Override
    public int numRows() {
        return diagonal.length;
    }

    @Override
    public int numCols() {
        return diagonal.length;
    }
    
    @Override
    public double[][] toArray() {
        double[][] matrix = new double[diagonal.length][diagonal.length];
        for (int i = 0; i < diagonal.length; i++) {
            matrix[i][i] = diagonal[i];
        }
        return matrix;
    }
    
    @Override
    public double get(int i, int j) {
        return i == j ? diagonal[i] : 0;
    }
    
    @Override
    public String toString() {
        double[][] matrix = toArray();
        String[] rows = new String[numRows()];
        for (int i = 0; i < numRows(); i++) {
            rows[i] = (i > 0 ? "\n" : "") + Arrays.toString(matrix[i]);
        }
        return Arrays.toString(rows);
    }

    @Override
    public Vector applyVector(Vector vector) {
        if (vector.size() != numCols()) {
            throw new MLException("Attempted to multiply a " + numRows() + "-by-" + numCols() + " matrix by a " + vector.size() + "-by-1 vector.");
        }

        if (vector.isSparse()) {
            List<SparseVectorEntry> ans = new LinkedList<>();
            for (SparseVectorEntry sve : vector.getEntries()) {
                double newValue = sve.value() * diagonal[sve.key()];
                if (newValue != 0) {
                    ans.add(new SparseVectorEntry(sve.key(), newValue));
                }
            }
            return new SparseVector(ans, vector.size());
        } else {
            double[] vec = vector.toArray();
            double[] ans = new double[vector.size()];
            boolean allZeros = true;
            for (int i = 0; i < vec.length; i++) {
                ans[i] = diagonal[i] * vec[i];
                allZeros = allZeros && ans[i] == 0;
            }
            if (allZeros) {
                return new SparseVector(new LinkedList<>(), vector.size());
            } else {
                return new BasicVector(ans);
            }
        }
    }
    
    @Override
    public void add(Matrix matrix) {
        add(matrix, 1);
    }
    
    @Override
    public void add(Matrix matrix, double scale) {
        if (matrix.numCols() != numCols() || matrix.numRows() != numRows()) {
            throw new MLException("Attempted to add " + numRows() + "-by-" + numCols() + " and " + matrix.numRows() + "-by-" + matrix.numCols() + " matrices. Also, adding a matrix to a diagonal matrix is not supported.");
        } else {
            throw new MLException("Adding a matrix to a diagonal matrix is not supported.");
        }
    }

    @Override
    public Vector scaledCol(int j, double scale) {
        List<SparseVectorEntry> entries = new LinkedList<>();
        double newValue = diagonal[j] * scale;
        if (newValue != 0) {
            entries.add(new SparseVectorEntry(j, newValue));
        }
        
        return new SparseVector(entries, numRows());
    }

    @Override
    public double norm() {
        double ans = 0;
        for (double x : diagonal) {
            ans += x * x;
        }
        return Math.sqrt(ans);
    }
}
