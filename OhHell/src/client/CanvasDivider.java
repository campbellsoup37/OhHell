package client;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;

public class CanvasDivider extends CanvasInteractable {
    private boolean horizontal;
    private double value;
    
    public CanvasDivider(boolean horizontal, double initialValue) {
        this.horizontal = horizontal;
        this.value = initialValue;
    }
    
    public double min() {
        return 0;
    }
    public double max() {
        return 1;
    }
    
    @Override
    public boolean isDraggable() {
        return true;
    }
    
    public void setValue(double v) {
        value = Math.max(Math.min(v, max()), min());
    }
    
    public double getValue() {
        return value;
    }
    
    @Override
    public Cursor mousedCursor() {
        if (horizontal) {
            return new Cursor(Cursor.S_RESIZE_CURSOR);
        } else {
            return new Cursor(Cursor.E_RESIZE_CURSOR);
        }
    }
    
    @Override
    public void paint(Graphics graphics) {
        setValue(value);
        
        if (isShown()) {
            graphics.setColor(Color.BLACK);
            if (horizontal) {
                graphics.drawLine(x(), y() + height() / 2, x() + width(), y() + height() / 2);
            } else {
                graphics.drawLine(x() + width() / 2, y(), x() + width() / 2, y() + height());
            }
        }
    }
}