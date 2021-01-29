package ml;
import java.util.List;

public class MLTools {
    public static String vectorToString(double[] vec) {
        String output = "{";
        for (double x : vec) output += x + ", ";
        output = output.substring(0, output.length() - 2);
        output += "}";
        return output;
    }
    
    public static Vector stringToVector(String s) {
        String[] row = s.trim().substring(1, s.length() - 1).split(",");
        double[] output = new double[row.length];
        for (int i = 0; i < row.length; i++) {
            output[i] = Double.parseDouble(row[i].trim());
        }
        return new BasicVector(output);
    }
    
    public static String matrixToString(double[][] mat) {
        String output = "{";
        for (double[] vec : mat) output += vectorToString(vec) + ", \n";
        output = output.substring(0, output.length() - 3);
        output += "}";
        return output;
    }
    
    public static Matrix stringToMatrix(String s) {
        String[] rows = s.split("\\{+");
        String[] row = rows[1].split("[,\\}\n ]+");
        double[][] output = new double[rows.length - 1][row.length];
        for (int i = 1; i < rows.length; i++) {
            row = rows[i].split("[,\\}\n ]+");
            for (int j = 0; j < row.length; j++) {
                output[i - 1][j] = Double.parseDouble(row[j]);
            }
        }
        return new BasicMatrix(output);
    }
    
    public static int classify(double[] v) {
        int i = 0;
        double max = v[0];
        for (int j = 0; j < v.length; j++) if(v[j] > max) {
            max = v[j];
            i = j;
        }
        return i + 1;
    }
    
    public static double[] oneHot(int j, int d) {
        double[] out = new double[d];
        for (int i = 0; i < d; i++) {
            if (i == j - 1) out[i] = 1;
            else out[i] = 0;
        }
        return out;
    }
    
    public static double[] concatenate(List<double[]> vs) {
        int sum = vs.stream()
                .map(v -> v.length)
                .reduce(0, (a, b) -> a + b);
        double[] out = new double[sum];
        int i = 0;
        for (double[] v : vs) {
            for (double x : v) {
                out[i] = x;
                i++;
            }
        }
        return out;
    }
    
    public static double vectorSizeSquared(double[] v) {
        double ans = 0;
        for (double x : v) {
            ans += x * x;
        }
        return ans;
    }
    
    public static double vectorSize(double[] v) {
         return Math.sqrt(vectorSizeSquared(v));
    }
    
    public static double matrixSize(double[][] M) {
        double ans = 0;
        for (double[] row : M) {
            for (double x : row) {
                ans += x * x;
            }
        }
        return Math.sqrt(ans);
    }
}
