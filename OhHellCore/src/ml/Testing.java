package ml;

import java.util.Arrays;

public class Testing {
    public static void main(String[] args) {
        int i = 2123456;
        int j = 568756856;
        
        long ij = pairToLong(i, j);
        int[] ijPair = longToPair(ij);
        
        System.out.println(ij);
        System.out.println(Arrays.toString(ijPair));
    }
    
    public static long pairToLong(int i, int j) {
        return ((long) i << 32) + j;
    }
    
    public static int[] longToPair(long ij) {
        int j = (int) (ij % (1 << 31));
        int i = (int) (ij >> 32);
        return new int[] {i, j};
    }
}
