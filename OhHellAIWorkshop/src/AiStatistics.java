import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiStatistics extends AiTrainer {
    public void run() {
        int N = 5;
        int reps = 1000000;
        
        String outputFolder = "C:/Users/Campb/Desktop/OhHellAiStats/";
        int toPrint = 100;

        OverallValueLearner ovl = new OverallValueLearner("resources/OhHellAIModels/ovlN5.txt");
        ImmediateValueLearner ivl = new ImmediateValueLearner("resources/OhHellAIModels/ivlN5.txt");

        int maxH = Math.min(10, 51 / N);
        int[][][] bidsTakens = new int[maxH + 1][maxH + 1][maxH + 1];
        
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
            core.startGame(N, false, 0, ovl, ivl);
            
            try {
                while (true) {
                    sleep(1);
                }
            } catch (InterruptedException e) {
                
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] -= scores[(g - 1 + M - toAve[k]) % M] / toAve[k];
                accs[k] -= mades[(g - 1 + M - toAve[k]) % M] / toAve[k];
            }
            scores[(g - 1) % M] = 0;
            mades[(g - 1) % M] = 0;
            
            int[] newRounds = getNewRounds();
            int[][] newBids = getNewBids();
            int[][] newTakens = getNewTakens();
            int[] newScores = getNewScores();
            Arrays.sort(newScores);
            
            for (int i = 0; i < N; i++) {
                for (int j = 0; j < newRounds.length; j++) {
                    bidsTakens[newRounds[j]][newBids[i][j]][newTakens[i][j]]++;
                }
            }
            
            for (int score : newScores) {
                scores[(g - 1) % M] += (double) score / N;
            }
            
            if (newScores[N - 1] > bestScore) {
                bestScore = newScores[N - 1];
                if (newScores[N - 1] > overallBest) {
                    overallBest = newScores[N - 1];
                }
            }
            
            for (int k = 0; k < toAve.length; k++) {
                aves[k] += scores[(g - 1) % M] / toAve[k];
                accs[k] += mades[(g - 1) % M] / toAve[k];
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % toAve[0] == 0) {
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
            
            if (g % toPrint == 0) {
                for (int j = 1; j <= maxH; j++) {
                    try {
                        BufferedWriter bw = new BufferedWriter(new FileWriter(outputFolder + j + " round.txt"));
                        bw.write("{");
                        for (int bid = 0; bid <= j; bid++) {
                            bw.write("{");
                            for (int taken = 0; taken <= j; taken++) {
                                bw.write("" + bidsTakens[j][bid][taken]);
                                if (taken < j) {
                                    bw.write(",");
                                } else {
                                    bw.write("}");
                                }
                            }
                            if (bid < j) {
                                bw.write(",\n");
                            } else {
                                bw.write("}\n");
                            }
                        }
                        bw.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiStatistics();
        ait.start();
    }
}
