package client;
import java.awt.Graphics;

public abstract class CanvasInteractable {
    private boolean moused = false;
    private boolean pressed = false;
    
    public int x() {
        return 0;
    }

    public int y() {
        return 0;
    }

    public int width() {
        return 0;
    }

    public int height() {
        return 0;
    }

    public boolean isEnabled() {
        return true;
    }
    
    public boolean isShown() {
        return true;
    }
    
    public void paint(Graphics graphics) {}
    
    public void setPressed(boolean pressed) {
        this.pressed = pressed;
    }
    
    public boolean isPressed() {
        return pressed;
    }
    
    public boolean updateMoused(int x, int y) {
        boolean oldMoused = isMoused();
        setMoused(isShown() && isEnabled() && x >= x() && x <= x() + width() && y >= y() && y <= y() + height());
        return isMoused() == oldMoused;
    }
    
    public void setMoused(boolean moused) {
        this.moused = moused;
    }
    
    public boolean isMoused() {
        return moused;
    }
    
    public void click() {}
}
