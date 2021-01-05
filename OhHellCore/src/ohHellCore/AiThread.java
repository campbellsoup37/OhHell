package ohHellCore;

public class AiThread extends Thread {
    AiKernel aiKernel;
    
    private boolean running = false;
    private enum Task {
        BID,
        PLAY,
        END
    }
    private Task task;
    private AiPlayer player;
    private int delay;
    
    public AiThread(AiKernel aiKernel) {
        this.aiKernel = aiKernel;
    }
    
    public boolean isRunning() {
        return running;
    }
    
    public void run() {
        running = true;
        
        while (running) {
            try {
                while (true) {
                    sleep(1000);
                }
            } catch (InterruptedException e) {
                try {
                    sleep(delay);
                } catch (InterruptedException e1) {
                    e1.printStackTrace();
                }
                
                switch (task) {
                case BID:
                    aiKernel.makeBid(player);
                    break;
                case PLAY:
                    aiKernel.makePlay(player);
                    break;
                case END:
                    running = false;
                    break;
                }
            }
        }
    }
    
    public void makeBid(AiPlayer player, int delay) {
        task = Task.BID;
        this.player = player;
        this.delay = delay;
        interrupt();
    }
    
    public void makePlay(AiPlayer player, int delay) {
        task = Task.PLAY;
        this.player = player;
        this.delay = delay;
        interrupt();
    }
    
    public void end() {
        task = Task.END;
        interrupt();
    }
}
