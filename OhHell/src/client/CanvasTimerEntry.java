package client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import javax.swing.Timer;

public class CanvasTimerEntry implements ActionListener {
    private long startTime;
    private long elapsedTime = 0;
    private long endTime;
    private boolean firstAction = true;
    private GameCanvas canvas;
    private LinkedList<Timer> queue;
    
    public CanvasTimerEntry(long endTime, GameCanvas canvas, LinkedList<Timer> queue) {
        this.endTime = endTime;
        this.canvas = canvas;
        this.queue = queue;
        
        Timer timer = new Timer(0, this);
        queue.add(timer);
        if (queue.size() == 1) {
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
            startNextAction();
            //canvas.repaint();
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
    
    public void startNextAction() {
        try {
            queue.remove().stop();
            queue.getFirst().start();
        } catch (NoSuchElementException exc) {
            
        }
    }
    
    public void onFirstAction() {}
    
    public void onAction() {}
    
    public void onLastAction() {}
}