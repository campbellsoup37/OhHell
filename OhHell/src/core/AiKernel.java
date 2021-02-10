package core;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import strategyOI.AiStrategyModuleOI;
import strategyOI.ImmediateValueLearner;
import strategyOI.OverallValueLearner;

/**
 * This class contains the AI. Functions makeBid and makePlay are used to bid and play cards on 
 * behalf of a given AiPlayer and are called by the appropriate AiPlayer objects. For the game to
 * work properly, these functions need to be called in a thread not running the corresponding
 * OhHellCore. An object of this class can act as this thread if it is currently running when the 
 * functions are called. If not, a Timer object is created to take care of it (see the code for
 * makeBid and makePlay). When running many games with only AI players, it is significantly more 
 * efficient to run this class as a Thread.
 */
public class AiKernel {
    AiThread aiThread;

    private OhHellCore core;
    private List<String> firstNames;
    private Random random = new Random();
    private List<AiPlayer> aiPlayers = new ArrayList<>();
    
    public AiKernel(OhHellCore core) {
        this.core = core;
        
        try {
            String file = "resources/core/firstnames.txt";
            InputStream in = getClass().getResourceAsStream("/" + file);
            BufferedReader reader;
            if (in != null) {
                reader = new BufferedReader(new InputStreamReader(in));
            } else {
                reader = new BufferedReader(new FileReader(file));
            }
            firstNames = new ArrayList<>(18239);
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                firstNames.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public boolean hasAiPlayers() {
        return !aiPlayers.isEmpty();
    }
    
    public List<AiPlayer> createAiPlayers(int N, int robotCount, List<AiStrategyModule> aiStrategyModules, int delay) {
        aiPlayers.clear();
        
        if (aiStrategyModules == null) {
            aiStrategyModules = createDefaultAiStrategyModules(N, robotCount);
        }
        for (int i = 0; i < robotCount; i++) {
            aiPlayers.add(new AiPlayer(
                    firstNames.get(random.nextInt(firstNames.size())) + " Bot", 
                    this,
                    aiStrategyModules.get(i),
                    delay
                    ));
        }
        
        return aiPlayers;
    }
    
    public List<AiStrategyModule> createDefaultAiStrategyModules(int N, int robotCount) {
        OverallValueLearner ovl = new OverallValueLearner("resources/models/" + "ovlN" + N + ".txt");
        ImmediateValueLearner ivl = new ImmediateValueLearner("resources/models/" + "ivlN" + N + ".txt");
        List<AiStrategyModule> aiStrategyModules = new ArrayList<>(robotCount);
        for (int i = 0; i < robotCount; i++) {
            aiStrategyModules.add(new AiStrategyModuleOI(core, N, ovl, ivl));
        }
        return aiStrategyModules;
    }
    
    public void reloadAiStrategyModules(int N) {
        List<AiStrategyModule> aiStrategyModules = createDefaultAiStrategyModules(N, aiPlayers.size());
        for (int i = 0; i < aiPlayers.size(); i++) {
            aiPlayers.get(i).setAiStrategyModule(aiStrategyModules.get(i));
        }
    }
    
    public void start() {
        aiThread = new AiThread(this);
        aiThread.start();
    }
    
    public void stop() {
        if (aiThread != null && aiThread.isRunning()) {
            aiThread.end();
        }
        aiPlayers.clear();
    }
    
    public void makeBid(AiPlayer player, int delay) {
        if (aiThread.isRunning()) {
            aiThread.makeBid(player, delay);
        } else {
            System.out.println(player.getName() + " bid");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    makeBid(player);
                    cancel();
                }
            }, delay);
        }
    }
    
    public void makePlay(AiPlayer player, int delay) {
        if (aiThread.isRunning()) {
            aiThread.makePlay(player, delay);
        } else {
            System.out.println(player.getName() + " play");
            Timer timer = new Timer();
            timer.schedule(new TimerTask() {
                @Override
                public void run() {
                    makePlay(player);
                    cancel();
                }
            }, delay);
        }
    }
    
    public void makeBid(AiPlayer player) {
        player.getAiStrategyModule().makeBid();
    }
    
    public void makePlay(AiPlayer player) {
        player.getAiStrategyModule().makePlay();
    }
    
    public void processClaimRequest(AiPlayer player, int index) {
        core.processClaimResponse(player, true);
    }
}
