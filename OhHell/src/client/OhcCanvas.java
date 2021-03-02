package client;

import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.util.LinkedList;
import java.util.List;

import javax.swing.JPanel;

import common.GraphicsTools;

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
    
    public int backgroundCenterX() {
        return getWidth() / 2;
    }
    
    public int backgroundCenterY() {
        return getHeight() / 2;
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
        
        Graphics2D graphics2 = GraphicsTools.makeGraphics2D(graphics, 
                client.antialiasingSelected(),
                false);
        graphics2.setFont(GraphicsTools.font);
        
        if (background != null) {
            double[] ratios = {
                    (double) backgroundCenterX() * 2 / background.getWidth(),
                    (double) (getWidth() - backgroundCenterX()) * 2 / background.getWidth(),
                    (double) backgroundCenterY() * 2 / background.getHeight(),
                    (double) (getHeight() - backgroundCenterY()) * 2 / background.getHeight()
            };
            double scale = 1;
            for (double ratio : ratios) {
                scale = Math.max(scale, ratio);
            }
            
            graphics.drawImage(background, 
                    (int) (backgroundCenterX() - scale * background.getWidth() / 2), 
                    (int) (backgroundCenterY() - scale * background.getHeight() / 2), 
                    (int) (backgroundCenterX() + scale * background.getWidth() / 2), 
                    (int) (backgroundCenterY() + scale * background.getHeight() / 2),
                    0, 
                    0, 
                    background.getWidth(), 
                    background.getHeight(), 
                    null);
        }

        customPaintFirst(graphics2);
        
        paintInteractables(graphics2);

        customPaintLast(graphics2);
    }
    
    public void paintInteractables(Graphics graphics) {
        for (List<? extends CanvasInteractable> inters : interactables) {
            for (CanvasInteractable inter : inters) {
                inter.paint(graphics);
            }
        }
    }

    public void customPaintFirst(Graphics graphics) {}
    
    public void customPaintLast(Graphics graphics) {}
    
    public void mousePressed(int x, int y) {
        mouseMoved(x, y, false);
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
        mouseMoved(x, y, false);
    }
    
    public void mouseMoved(int x, int y, boolean drag) {
        if (drag && interactablePressed != null 
                && interactablePressed.isDraggable()
                && interactablePressed.isEnabled()
                && interactablePressed.isShown()) {
            interactablePressed.drag(x, y);
            return;
        }
        
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
        
        updateCursor();
    }
    
    public void updateCursor() {
        if (interactableMoused != null) {
            Cursor cursor = interactableMoused.mousedCursor();
            if (cursor != null) {
                setCursor(cursor);
            } else {
                setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
            }
        } else {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }
}
