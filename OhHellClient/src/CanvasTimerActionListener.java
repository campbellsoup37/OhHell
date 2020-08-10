import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import javax.swing.Timer;

public class CanvasTimerActionListener implements ActionListener {
    private long startTime;
    private long elapsedTime = 0;
    private long endTime;
    private boolean firstAction = true;
    private GameCanvas canvas;
    private LinkedList<Timer> timerQueue;
    
    public CanvasTimerActionListener(long endTime, GameCanvas canvas) {
        this.endTime = endTime;
        this.canvas = canvas;
        timerQueue = canvas.getTimerQueue();
        
        boolean wasEmpty = timerQueue.isEmpty();
        Timer timer = new Timer(0, this);
        timerQueue.add(timer);
        if (wasEmpty) {
            timer.start();
        }
    }
    
    @Override
    public void actionPerformed(ActionEvent e) {
        if (firstAction) {
            onFirstAction();
            startTime = System.currentTimeMillis();
            firstAction = false;
        }
        
        elapsedTime = System.currentTimeMillis() - startTime;
        
        onAction();
        
        if (elapsedTime >= endTime) {
            onLastAction();
            timerQueue.remove().stop();
            if (!timerQueue.isEmpty()) {
                timerQueue.getFirst().start();
            }
            canvas.repaint();
        }
    }
    
    public long getElapsedTime() {
        return elapsedTime;
    }
    
    public void onFirstAction() {}
    
    public void onAction() {}
    
    public void onLastAction() {}
}