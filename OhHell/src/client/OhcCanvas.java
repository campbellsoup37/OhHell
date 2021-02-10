package client;

import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;
import javax.swing.SwingUtilities;

import graphics.OhcGraphicsTools;

public abstract class OhcCanvas extends JPanel {
    private static final long serialVersionUID = 1L;

    private GameClient client;
    private BufferedImage background;
    
    private List<List<? extends CanvasInteractable>> interactables = new LinkedList<>();
    private CanvasInteractable interactableMoused;
    private CanvasInteractable interactablePressed;
    
    public OhcCanvas(GameClient client) {
        this.client = client;
        initialize();
    }
    
    public void initialize() {}
    
    public void setBackground(BufferedImage background) {
        this.background = background;
    }
    
    public void setInteractables(List<List<? extends CanvasInteractable>> interactables) {
        this.interactables = interactables;
    }
    
    public boolean isShown() {
        return true;
    }
    
    @Override
    public void paintComponent(Graphics graphics) {
        if (!isShown()) {
            return;
        }
        
        super.paintComponent(graphics);
        
        Graphics2D graphics2 = OhcGraphicsTools.makeGraphics2D(graphics, 
                client.antialiasingSelected(),
                false);
        graphics2.setFont(OhcGraphicsTools.font);
        
        graphics.drawImage(background, 
                0, 0, 
                Math.max(getWidth(), background.getWidth()), 
                Math.max(getHeight(), background.getHeight()), 
                0, 0, 
                background.getWidth(), background.getHeight(), null);
        
        customPaint(graphics2);
        
        paintInteractables(graphics2);
        
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                if (isShown()) {
                    repaint();
                }
            }
        });
    }
    
    public void paintInteractables(Graphics graphics) {
        for (List<? extends CanvasInteractable> inters : interactables) {
            for (CanvasInteractable inter : inters) {
                inter.paint(graphics);
            }
        }
    }
    
    public void customPaint(Graphics graphics) {}
    
    public void mousePressed(int x, int y) {
        mouseMoved(x, y);
        if (interactableMoused != null) {
            interactableMoused.setPressed(true);
            interactablePressed = interactableMoused;
        }
    }
    
    public boolean canClick() {
        return true;
    }
    
    public void mouseReleased(int x, int y) {
        if (canClick()) {
            if (interactableMoused != null && interactableMoused == interactablePressed) {
                CanvasInteractable relay = interactableMoused;
                interactableMoused = null;
                interactablePressed = null;
                relay.click();
            }
        }
    }
    
    public void mouseMoved(int x, int y) {
        boolean anyMoused = false;
        
        for (List<? extends CanvasInteractable> inters : interactables) {
            for (CanvasInteractable inter : inters) {
                if (inter != null) {
                    CanvasInteractable moused = inter.updateMoused(x, y);
                    if (moused != null) {
                        if (interactableMoused != null && interactableMoused != moused) {
                            interactableMoused.setMoused(false);
                            interactableMoused.setPressed(false);
                        }
                        interactableMoused = moused;
                        anyMoused = true;        
                    }
                }
            }
        }
        
        if (interactableMoused != null && !anyMoused) {
            interactableMoused.setMoused(false);
            interactableMoused.setPressed(false);
            interactableMoused = null;
        }
    }
}
