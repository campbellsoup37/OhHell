import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiTrainerOI extends AiTrainer {
    @Override
    public boolean backprop() {
        return true;
    }
    
    public void run() {
        int N = 5;
        int reps = 1000000;
        boolean verbose = true;
        boolean forMathematica = true;
        boolean printError = false;
        
        double ovlWEta = 10;
        double ovlBEta = 0.001;
        double ivlWEta = 1;
        double ivlBEta = 0.0001;
        int groupingSize = 1;

        int maxH = Math.min(10, 51 / N);
        int[] ovlLayers = {
                maxH              // Cards left in hand
                + (maxH + 1) * N  // Bids - takens
                + 4               // Number of voids
                + 13              // Trump left
                
                + 2               // Card is trump
                + 13              // That suit left
                + 13,             // Card's adjusted number
                60,               // Hidden layer
                //20,
                1                 // Card's predicted value
        };
        int[] ivlLayers = {
                (maxH + 1) * (N - 1) // Bids - takens
                + 13                 // Trump left
                
                + 2                  // Trump was led
                + 13                 // Led suit left
                
                + 2                  // Card is trump
                + 13,                // Card's adjusted number
                30,                  // Hidden layer
                //20,
                1                    // Card's predicted value
        };
        
        boolean openFromFile = true;
        boolean saveToFile = true;
        int saveEvery = 10;
        
        String folder = "resources/OhHellAIModels/";

        String ovlFileSuffix = "o" + ovlLayers[1] + "";
        for (int i = 2; i < ovlLayers.length - 1; i++) {
            ovlFileSuffix += "_" + ovlLayers[i];
        }
        String ivlFileSuffix = "i" + ivlLayers[1] + "";
        for (int i = 2; i < ivlLayers.length - 1; i++) {
            ivlFileSuffix += "_" + ivlLayers[i];
        }
        String fileSuffix = ovlFileSuffix + ivlFileSuffix;
        
        OverallValueLearner ovl = new OverallValueLearner(ovlLayers, OverallValueLearner.getActFuncs(ovlLayers.length - 1));
        ImmediateValueLearner ivl = new ImmediateValueLearner(ivlLayers, ImmediateValueLearner.getActFuncs(ivlLayers.length - 1));
        
        if (openFromFile) {
            ovl.openFromFile(folder + "ovlN" + N + fileSuffix + ".txt");
            ivl.openFromFile(folder + "ivlN" + N + fileSuffix + ".txt");
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
            core.startGame(N, false, 1, ovl, ivl);
            
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
            
            int[] sortedScores = getNewScores();
            Arrays.sort(sortedScores);
            if (forMathematica) {
                System.out.print("{");
                for (int i = 0; i < N - 1; i++) {
                    System.out.print(sortedScores[i] + ",");
                }
                System.out.println(sortedScores[N - 1] + "},");
            }
            
            for (int score : sortedScores) {
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
                
                System.out.println("OVL ERROR: " + ovl.doEpoch(ovlWEta, ovlBEta, ovl.dataSize(), printError));
                System.out.println("IVL ERROR: " + ivl.doEpoch(ivlWEta, ivlBEta, ivl.dataSize(), printError));
            }
            
            if (g % saveEvery == 0) {
                if (saveToFile) {
                    ovl.saveToFile(new File(folder + "ovlN" + N + fileSuffix + ".txt"));
                    ivl.saveToFile(new File(folder + "ivlN" + N + fileSuffix + ".txt"));
                }
            }
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiTrainerOI();
        ait.start();
    }
}
