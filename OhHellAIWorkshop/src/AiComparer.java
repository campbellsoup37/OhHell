import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class AiComparer extends AiTrainer {
    public void run() {
        int N = 5;
        int reps = 1000000;
        
        String outputFolder = "C:/Users/Campbell/Desktop/OhHellAiStats/";
        int toPrint = 1;
        
        OverallValueLearner[] ovls = {
                new OverallValueLearner("resources/OhHellAIModels/ovlN5.txt"),
                new OverallValueLearner("resources/OhHellAIModels/ovlN5o60i30.txt")
        };
        ImmediateValueLearner[] ivls = {
                new ImmediateValueLearner("resources/OhHellAIModels/ivlN5.txt"),
                new ImmediateValueLearner("resources/OhHellAIModels/ivlN5o60i30.txt")
        };
        
        int K = ovls.length;
        OhHellCore core = new OhHellCore();
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        core.execute(false);
        
        double[] means = new double[K];
        double[] vars = new double[K];
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            for (int a = 0; a < K; a++) {
                core.startGame(N, false, 0, ovls[a], ivls[a]);
                
                try {
                    while (true) {
                        sleep(10);
                    }
                } catch (InterruptedException e) {
                    
                }
                
                int[] newScores = getNewScores();
                Arrays.sort(newScores);
                
                double x = 0;
                for (int score : newScores) {
                    x += (double) score / N;
                }
                
                double prevMean = means[a];
                means[a] = (means[a] * (g - 1) + x) / g;
                if (g >= 2) {
                    vars[a] = (vars[a] * (g - 2) + Math.pow(x - means[a], 2)) / (g - 1) + Math.pow(prevMean - means[a], 2);
                }
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % toPrint == 0) {
                System.out.println(g + "/" + reps + ": ");
                for (int a = 0; a < K; a++) {
                    System.out.println("     AI #" + (a + 1)
                            + ": μ = " + String.format("%.5f", means[a])
                            + ", σ² = " + String.format("%.5f", vars[a]));
                }
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    System.out.println("     Time left: " + hours + ":" + minutes + ":" + seconds);
                }
            }
            
            /*if (g % toPrint == 0) {
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
            }*/
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiComparer();
        ait.start();
    }
}
