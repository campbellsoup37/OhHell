import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiTrainer extends Thread {
    int[] newScores;
    
    public void run() {
        int N = 5;
        int reps = 1000000;
        boolean verbose = true;
        boolean forMathematica = true;
        
        double ovlWEta = 1;
        double ovlBEta = 0.0001;
        double ivlWEta = 0.1;
        double ivlBEta = 0.00001;
        int groupingSize = 1;
        boolean backprop = true;
        
        boolean openFromFile = true;
        boolean saveToFile = true;
        int saveEvery = 10;
        
        String folder = "C:/Users/Campbell/Desktop/OhHellAIModels/";

        int maxH = Math.min(10, 51 / N);

        OverallValueLearner ovl = new OverallValueLearner(new int[] {
                maxH              // Cards left in hand
                + (maxH + 1) * N  // Bids - takens
                + 4               // Number of voids
                + 13              // Trump left
                
                + 2               // Card is trump
                + 13              // That suit left
                + 13,             // Card's adjusted number
                40,               // Hidden layer
                1                 // Card's predicted value
        });
        ImmediateValueLearner ivl = new ImmediateValueLearner(new int[] {
                (maxH + 1) * (N - 1) // Bids - takens
                + 13                 // Trump left
                
                + 2                  // Trump was led
                + 13                 // Led suit left
                
                + 2                  // Card is trump
                + 13,                // Card's adjusted number
                30,                  // Hidden layer
                1                    // Card's predicted value
        });
        
        if (openFromFile) {
            ovl.openFromFile(folder + "ovlN" + N + ".txt");
            ivl.openFromFile(folder + "ivlN" + N + ".txt");
        }
        
        OhHellCore core = new OhHellCore();
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        core.execute(false);
        
        int M = 10000;
        int[] toAve = {1, 10, 100, 1000, 10000};
        double[] scores = new double[M];
        double[] mades = new double[M];
        double[] aves = new double[toAve.length];
        double[] accs = new double[toAve.length];
        int bestScore = Integer.MIN_VALUE;
        int overallBest = Integer.MIN_VALUE;
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(N, true, ovl, ivl);
            
            try {
                while (true) {
                    sleep(10);
                }
            } catch (InterruptedException e) {
                
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] -= scores[(g - 1 + M - toAve[k]) % M] / toAve[k];
                accs[k] -= mades[(g - 1 + M - toAve[k]) % M] / toAve[k];
            }
            scores[(g - 1) % M] = 0;
            mades[(g - 1) % M] = 0;
            
            int[] sortedScores = newScores;
            Arrays.sort(sortedScores);
            if (forMathematica) {
                System.out.print("{");
                for (int i = 0; i < N - 1; i++) {
                    System.out.print(sortedScores[i] + ",");
                }
                System.out.println(sortedScores[N - 1] + "},");
            }
            
            for (int score : newScores) {
                scores[(g - 1) % M] += (double) score / N;
            }
            
            if (sortedScores[N - 1] > bestScore) {
                bestScore = sortedScores[N - 1];
                if (sortedScores[N - 1] > overallBest) {
                    overallBest = sortedScores[N - 1];
                }
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] += scores[(g - 1) % M] / toAve[k];
                accs[k] += mades[(g - 1) % M] / toAve[k];
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % toAve[0] == 0 && verbose) {
                System.out.println(g + "/" + reps + ": ");
                System.out.println("     Best score: " + bestScore + " (" + overallBest + ")");
                bestScore = Integer.MIN_VALUE;
                for (int k = 0; k < toAve.length; k++) {
                    if (g >= toAve[k]) {
                        System.out.println("     Average of " + toAve[k] + ": " + String.format("%.5f", accs[k]) + "   (" + String.format("%.5f", aves[k]) + ")");
                    }
                }
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    System.out.println("     Time left: " + hours + ":" + minutes + ":" + seconds);
                }
            }
            if (g % groupingSize == 0) {
                ovl.makeDataList();
                ivl.makeDataList();
                
                if (backprop) {
                    System.out.println("OVL ERROR: " + ovl.doEpoch(ovlWEta, ovlBEta, ovl.dataSize(), true));
                    System.out.println("IVL ERROR: " + ivl.doEpoch(ivlWEta, ivlBEta, ivl.dataSize(), true));
                }
            }
            
            if (g % saveEvery == 0) {
                if (saveToFile) {
                    ovl.saveToFile(new File(folder + "ovlN" + N + ".txt"));
                    ivl.saveToFile(new File(folder + "ivlN" + N + ".txt"));
                }
            }
        }
    }
    
    public void notifyGameDone(int[] scores) {
        newScores = scores;
        interrupt();
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiTrainer();
        ait.start();
    }
}
