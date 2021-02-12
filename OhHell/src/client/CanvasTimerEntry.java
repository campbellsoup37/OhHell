package client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import javax.swing.Timer;

public class CanvasTimerEntry implements ActionListener {
    private GameCanvas canvas;
    private Timer timer;
    private long startTime;
    private long elapsedTime = 0;
    private long endTime;
    private boolean firstAction = true;
    
    public CanvasTimerEntry(long endTime, GameCanvas canvas, LinkedList<Timer> queue, boolean immediately) {
        this.endTime = endTime;
        this.canvas = canvas;
        
        timer = new Timer(0, this);
        if (immediately) {
            queue.addFirst(timer);
        } else {
            queue.add(timer);
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
            timer.stop();
            canvas.setPerformingAction(false);
        }
    }
    
    public long getStartTime() {
        return startTime;
    }
    
    public long getElapsedTime() {
        return elapsedTime;
    }
    
    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }
    
    public GameCanvas getCanvas() {
        return canvas;
    }
    
    public void onFirstAction() {}
    
    public void onAction() {}
    
    public void onLastAction() {}
}