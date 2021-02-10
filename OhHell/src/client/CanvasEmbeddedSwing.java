package client;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;

import javax.swing.SwingUtilities;

public class CanvasEmbeddedSwing extends CanvasInteractable {
    private Component component;
    private OhcCanvas canvas;
    
    public CanvasEmbeddedSwing(Component component, OhcCanvas canvas) {
        this.component = component;
        this.canvas = canvas;
        canvas.add(component);
    }
    
    public Component getComponent() {
        return component;
    }
    
    public void dispose() {
        canvas.remove(component);
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (isShown()) {
            final Rectangle newPosition = new Rectangle(x(), y(), width(), height());
            if (!component.getBounds().equals(newPosition)) {
                SwingUtilities.invokeLater(new Runnable() {
                    public void run() {
                        component.setBounds(newPosition);
                    }
                });
            }
        }
        if (isShown() ^ component.isVisible()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    component.setVisible(isShown());
                    component.repaint();
                }
            });
        }
        if (isEnabled() ^ component.isEnabled()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    component.setEnabled(isEnabled());
                    component.repaint();
                }
            });
        }
    }
}
