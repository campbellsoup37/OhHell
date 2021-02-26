package core;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.Timer;

public class PendingAction extends Timer {
    private static final long serialVersionUID = 1L;
    
    private boolean on;

    public PendingAction(int delay) {
        super(10, null);
        
        on = true;
        long startTime = System.currentTimeMillis();
        addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent arg0) {
                if (!on) {
                    stop();
                }
                if (System.currentTimeMillis() - startTime >= delay) {
                    if (on) {
                        action();
                    }
                    stop();
                }
            }
        });
        start();
    }

    public void action() {}
    
    public void turnOff() {
        on = false;
    }
}
