package client;

import java.awt.Component;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;

import javax.swing.SwingUtilities;

public class CanvasEmbeddedSwing extends CanvasInteractable {
    private Component component;
    private OhcCanvas canvas;
    
    private boolean grabbingFocus = false;
    
    public CanvasEmbeddedSwing(Component component, OhcCanvas canvas) {
        this.component = component;
        this.canvas = canvas;
        component.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                canvas.dispatchEvent(displacedMouseEvent(e));
                component.requestFocus();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                canvas.dispatchEvent(displacedMouseEvent(e));
                component.requestFocus();
            }
        });
        component.addMouseMotionListener(new MouseMotionListener() {
            @Override
            public void mouseDragged(MouseEvent e) {
                canvas.dispatchEvent(displacedMouseEvent(e));
            }

            @Override
            public void mouseMoved(MouseEvent e) {
                canvas.dispatchEvent(displacedMouseEvent(e));
            }
        });
        canvas.add(component);
    }
    
    private MouseEvent displacedMouseEvent(MouseEvent e) {
        return new MouseEvent(
                canvas,
                e.getID(),
                e.getWhen(),
                e.getModifiers(),
                e.getX() + x(),
                e.getY() + y(),
                e.getClickCount(),
                e.isPopupTrigger(),
                e.getButton());
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
                    if (grabbingFocus && component.isEnabled()) {
                        focusGrabber();
                        grabbingFocus = false;
                    }
                }
            });
        }
        if (isEnabled() ^ component.isEnabled()) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    component.setEnabled(isEnabled());
                    component.repaint();
                    if (grabbingFocus && component.isVisible()) {
                        focusGrabber();
                        grabbingFocus = false;
                    }
                }
            });
        }
    }
    
    public void focusGrabber() {}
    
    public void grabFocus() {
        if (component.isVisible() && component.isEnabled()) {
            focusGrabber();
        } else {
            grabbingFocus = true;
        }
    }
}
