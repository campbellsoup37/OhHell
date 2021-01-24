package client;
import java.awt.Color;
import java.awt.Graphics;

import graphics.OhcGraphicsTools;

public class CanvasButton extends CanvasInteractable {
    private String text = "";
    
    public CanvasButton(String text) {
        this.text = text;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (isShown()) {
            if (!isEnabled()) {
                graphics.setColor(new Color(96, 96, 96));
            } else if (isMoused()) {
                graphics.setColor(new Color(192, 192, 192));
            } else {
                graphics.setColor(Color.WHITE);
            }
            graphics.fillRoundRect(x(), y(), width(), height(), 10, 10);
            graphics.setColor(Color.BLACK);
            graphics.drawRoundRect(x(), y(), width(), height(), 10, 10);
            graphics.setFont(OhcGraphicsTools.fontBold);
            OhcGraphicsTools.drawStringJustified(graphics, text, x() + width() / 2, y() + height() / 2, 1, 1);
            graphics.setFont(OhcGraphicsTools.font);
        }
    }
}
