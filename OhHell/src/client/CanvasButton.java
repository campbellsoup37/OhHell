package client;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Font;
import java.awt.Graphics;

import common.GraphicsTools;

public class CanvasButton extends CanvasInteractable {
    private String text = "";
    
    public CanvasButton(String text) {
        this.text = text;
    }
    
    public boolean isSelected() {
        return false;
    }
    
    public String text() {
        return text;
    }
    
    public Font font() {
        return GraphicsTools.fontBold;
    }
    
    public boolean alert() {
        return false;
    }
    
    @Override
    public Cursor mousedCursor() {
        return new Cursor(Cursor.HAND_CURSOR);
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (isShown()) {
            if (!isEnabled()) {
                graphics.setColor(new Color(96, 96, 96));
            } else {
                if (alert()) {
                    if (isMoused()) {
                        graphics.setColor(new Color(112, 225, 112));
                    } else {
                        graphics.setColor(new Color(175, 255, 175));
                    }
                } else if (isSelected()) {
                    if (isMoused()) {
                        graphics.setColor(new Color(137, 137, 137));
                    } else {
                        graphics.setColor(new Color(200, 200, 200));
                    }
                } else {
                    if (isMoused()) {
                        graphics.setColor(new Color(192, 192, 192));
                    } else {
                        graphics.setColor(Color.WHITE);
                    }
                }
            }
            GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 15);
            graphics.setFont(font());
            graphics.setColor(Color.BLACK);
            GraphicsTools.drawStringJustified(graphics, text(), x() + width() / 2, y() + height() / 2, 1, 1);
            graphics.setFont(GraphicsTools.font);
        }
    }
}
