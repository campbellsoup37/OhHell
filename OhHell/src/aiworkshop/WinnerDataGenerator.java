package aiworkshop;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.GameOptions;
import core.OhHellCore;
import core.Player;
import ml.MLTools;

public class WinnerDataGenerator extends AiTrainer {
    public void run() {
        int N = 10;
        int reps = 100000;
        boolean verbose = true;
        boolean flush = true;
        
        String folder = "C:/Users/campb/Desktop/AiData/Win/";
        new File(folder).mkdirs();
        
        strategyOI.OverallValueLearner ovl = new strategyOI.OverallValueLearner("resources/ai workshop/OhHellAIModels/OI/ovlN10o80i30.txt");
        strategyOI.ImmediateValueLearner ivl = new strategyOI.ImmediateValueLearner("resources/ai workshop/OhHellAIModels/OI/ivlN10o80i30.txt");
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            aiStrategyModules.add(new strategyOI.AiStrategyModuleOI(core, N, ovl, ivl));
        }
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(new GameOptions(N), aiStrategyModules);
            
            try {
                while (true) {
                    sleep(10);
                }
            } catch (InterruptedException e) {
                int[][] scores = getAllScores();
                int winner = getWinner();
                
                try {
                    BufferedWriter bw = new BufferedWriter(new FileWriter(folder + N + ".txt", g > 1 || !flush));
                    for (int i = 0; i < scores[0].length; i++) {
                        double[] scoresVector = new double[N + 1];
                        for (int j = 0; j < N; j++) {
                            scoresVector[j] = scores[j][i];
                        }
                        scoresVector[N] = scores[0].length - 1 - i;
                        bw.write(MLTools.vectorToString(scoresVector) + "\n");
                        bw.write(MLTools.vectorToString(MLTools.oneHot(winner + 1, N)) + "\n");
                        bw.write("\n");
                    }
                    bw.close();
                } catch (IOException e1) {
                    e1.printStackTrace();
                }
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            if (g % 100 == 0 && verbose) {
                StringBuilder log = new StringBuilder();
                log.append(g + "/" + reps + ": \n");
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    log.append("     Time left: " + hours + ":" + minutes + ":" + seconds + "\n");
                }
                System.out.println(log);
            }
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new WinnerDataGenerator();
        ait.start();
    }
}
