package aiworkshop;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import core.AiStrategyModule;
import core.AiTrainer;
import core.OhHellCore;
import core.Player;

public class AiDataGenerator extends AiTrainer {
    @Override
    public boolean backprop() {
        return true;
    }
    
    public void run() {
        int N = 5;
        int reps = 1;
        
        String outputFolder = "ai resources/AiData/";
        
        OhHellCore core = new OhHellCore(false);
        List<Player> players = new ArrayList<>();
        core.setPlayers(players);
        core.setAiTrainer(this);
        
        strategyOI.OverallValueLearner ovl = new strategyOI.OverallValueLearner("ai resources/OhHellAIModels/OI/ovlN5o40i30.txt");
        strategyOI.ImmediateValueLearner ivl = new strategyOI.ImmediateValueLearner("ai resources/OhHellAIModels/OI/ivlN5o40i30.txt");
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(N);
        for (int i = 0; i < N; i++) {
            aiStrategyModules.add(new strategyOI.AiStrategyModuleOI(core, N, ovl, ivl));
        }
        String subfolder = "OI/";
        
        File file = new File(outputFolder + subfolder + N + ".txt");
        file.getParentFile().mkdirs();
        BufferedWriter writer = null;
        try {
            writer = new BufferedWriter(new FileWriter(file));
        } catch (IOException e1) {
            e1.printStackTrace();
        }
        
        int toPrint = 1;
        
        int R = 20;
        long[] times = new long[R];
        for (int g = 1; g <= reps; g++) {
            core.startGame(N, false, aiStrategyModules, 0);
            
            try {
                while (true) {
                    sleep(1);
                }
            } catch (InterruptedException e) {
                
            }
            
            long newTime = System.currentTimeMillis();
            long timeDiff = newTime - times[(g - 1) % R];
            times[(g - 1) % R] = newTime;
            
            ovl.dumpData(writer);
            
            if (g % toPrint == 0) {
                System.out.println(g + "/" + reps + ": ");
                if (g > R) {
                    double timeLeft = (double) timeDiff / R * (reps - g);
                    long hours = (long) (timeLeft / 1000 / 60 / 60);
                    long minutes = (long) (timeLeft / 1000 / 60 - hours * 60);
                    long seconds = (long) (timeLeft / 1000 - hours * 60 * 60 - minutes * 60);
                    System.out.println("     Time left: " + hours + ":" + minutes + ":" + seconds);
                }
            }
        }
        
        try {
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) {
        AiTrainer ait = new AiDataGenerator();
        ait.start();
    }
}
