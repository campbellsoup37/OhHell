package client;
import java.awt.Color;
import java.awt.Graphics;

import graphics.OhcGraphicsTools;

public class CanvasButton extends CanvasInteractable {
    private String text = "";
    
    public CanvasButton(String text) {
        this.text = text;
    }
    
    public boolean isSelected() {
        return false;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (isShown()) {
            if (!isEnabled()) {
                graphics.setColor(new Color(96, 96, 96));
            } else if (isMoused()) {
                graphics.setColor(new Color(192, 192, 192));
            } else if (isSelected()) {
                graphics.setColor(new Color(210, 210, 210));
            } else {
                graphics.setColor(Color.WHITE);
            }
            OhcGraphicsTools.drawBox(graphics, x(), y(), width(), height(), 15);
            graphics.setFont(OhcGraphicsTools.fontBold);
            OhcGraphicsTools.drawStringJustified(graphics, text, x() + width() / 2, y() + height() / 2, 1, 1);
            graphics.setFont(OhcGraphicsTools.font);
        }
    }
}
