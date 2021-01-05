package ml;

import java.util.Arrays;

public class BasicMatrix implements Matrix {
    private double[][] matrix;
    
    public BasicMatrix(double[][] matrix) {
        this.matrix = matrix;
    }

    @Override
    public int numRows() {
        return matrix.length;
    }

    @Override
    public int numCols() {
        return matrix[0].length;
    }
    
    @Override
    public double[][] toArray() {
        return matrix;
    }
    
    @Override
    public double get(int i, int j) {
        return matrix[i][j];
    }
    
    @Override
    public String toString() {
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
        
        double[] ans = new double[numRows()];
        
        if (vector.isSparse()) {
            for (int i = 0; i < numRows(); i++) {
                for (SparseVectorEntry sve : vector.getEntries()) {
                    ans[i] += matrix[i][sve.key()] * sve.value();
                }
            }
        } else {
            double[] vec = vector.toArray();
            for (int i = 0; i < numRows(); i++) {
                for (int j = 0; j < numCols(); j++) {
                    ans[i] += matrix[i][j] * vec[j];
                }
            }
        }
        
        return new BasicVector(ans);
    }
    
    @Override
    public void add(Matrix matrix) {
        add(matrix, 1);
    }
    
    @Override
    public void add(Matrix matrix, double scale) {
        if (matrix.numCols() != numCols() || matrix.numRows() != numRows()) {
            throw new MLException("Attempted to add " + numRows() + "-by-" + numCols() + " and " + matrix.numRows() + "-by-" + matrix.numCols() + " matrices.");
        }
        
        for (int i = 0; i < numRows(); i++) {
            for (int j = 0; j < numCols(); j++) {
                this.matrix[i][j] += matrix.get(i, j) * scale;
            }
        }
    }

    @Override
    public Vector scaledCol(int j, double scale) {
        double[] ans = new double[numRows()];
        for (int i = 0; i < numRows(); i++) {
            ans[i] = matrix[i][j] * scale;
        }
        return new BasicVector(ans);
    }

    @Override
    public double norm() {
        double ans = 0;
        for (double[] row : matrix) {
            for (double x : row) {
                ans += x * x;
            }
        }
        return Math.sqrt(ans);
    }
}
