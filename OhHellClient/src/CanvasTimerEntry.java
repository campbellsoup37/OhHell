import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.LinkedList;
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
        
        boolean wasEmpty = queue.isEmpty();
        Timer timer = new Timer(0, this);
        queue.add(timer);
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
            queue.remove().stop();
            if (!queue.isEmpty()) {
                queue.getFirst().start();
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