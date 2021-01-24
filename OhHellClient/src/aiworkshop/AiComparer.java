package aiworkshop;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.OhHellCore;
import core.Player;
import strategyOI.AiStrategyModuleDynamicOI;
import strategyOI.AiStrategyModuleOI;
import strategyOI.ImmediateValueLearner;
import strategyOI.OverallValueLearner;

public class AiComparer extends AiTrainer {
    public void run() {
        int N = 5;
        int reps = 1000000;
        
        String outputFolder = "C:/Users/Campbell/Desktop/OhHellAiStats/";
        int toPrint = 1;
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = Arrays.asList(
                new AiStrategyModuleDynamicOI(core, N, 
                        new OverallValueLearner[] {
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid0.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid1.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid2.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid3.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid4.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid5.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid6.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid7.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid8.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid9.txt"),
                                new OverallValueLearner("resources/OhHellAIModels/dovlN5bid10.txt"),
                                }, 
                        new ImmediateValueLearner("resources/OhHellAIModels/divlN5.txt")),
                new AiStrategyModuleOI(core, N, new OverallValueLearner("resources/OhHellAIModels/ovlN5.txt"), new ImmediateValueLearner("resources/OhHellAIModels/ivlN5.txt")),
                new AiStrategyModuleOI(core, N, new OverallValueLearner("resources/OhHellAIModels/ovlN5.txt"), new ImmediateValueLearner("resources/OhHellAIModels/ivlN5.txt")),
                new AiStrategyModuleOI(core, N, new OverallValueLearner("resources/OhHellAIModels/ovlN5.txt"), new ImmediateValueLearner("resources/OhHellAIModels/ivlN5.txt")),
                new AiStrategyModuleOI(core, N, new OverallValueLearner("resources/OhHellAIModels/ovlN5.txt"), new ImmediateValueLearner("resources/OhHellAIModels/ivlN5.txt"))
        );
        
        double[] means = new double[N];
        double[] vars = new double[N];
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(N, false, aiStrategyModules, 0);
            
            try {
                while (true) {
                    sleep(10);
                }
            } catch (InterruptedException e) {
                
            }
            
            players = getPlayers();
            Hashtable<AiStrategyModule, Integer> scoreMap = new Hashtable<>();
            for (Player player : players) {
                scoreMap.put(player.getAiStrategyModule(), player.getScore());
            }
            
            int i = 0;
            for (AiStrategyModule aiStrategyModule : aiStrategyModules) {
                double x = scoreMap.get(aiStrategyModule);
                double prevMean = means[i];
                means[i] = (means[i] * (g - 1) + x) / g;
                if (g >= 2) {
                    vars[i] = (vars[i] * (g - 2) + Math.pow(x - means[i], 2)) / (g - 1) + Math.pow(prevMean - means[i], 2);
                }
                i++;
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % toPrint == 0) {
                System.out.println(g + "/" + reps + ": ");
                for (i = 0; i < N; i++) {
                    System.out.println("     AI #" + (i + 1)
                            + ": μ = " + String.format("%.5f", means[i])
                            + ", σ² = " + String.format("%.5f", vars[i]));
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
