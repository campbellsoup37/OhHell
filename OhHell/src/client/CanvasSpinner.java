package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;
import java.util.List;

import common.GraphicsTools;

public class CanvasSpinner extends CanvasInteractable {
    private int value;
    
    private List<CanvasButton> buttons;
    
    public CanvasSpinner(int defaultValue) {
        value = defaultValue;
        
        CanvasSpinner spinner = this;
        buttons = Arrays.asList(
                new CanvasButton("-") {
                    @Override
                    public int x() {
                        return spinner.x() + 5;
                    }
                    
                    @Override
                    public int y() {
                        return spinner.y() + 5;
                    }
                    
                    @Override
                    public int width() {
                        return 20;
                    }
                    
                    @Override
                    public int height() {
                        return 20;
                    }
                    
                    @Override
                    public boolean isEnabled() {
                        return spinner.isEnabled();
                    }
                    
                    @Override
                    public void click() {
                        value = Math.max(min(), value - step());
                        onUpdate();
                    }
                },
                new CanvasButton("+") {
                    @Override
                    public int x() {
                        return spinner.x() + spinner.width() - 25;
                    }
                    
                    @Override
                    public int y() {
                        return spinner.y() + 5;
                    }
                    
                    @Override
                    public int width() {
                        return 20;
                    }
                    
                    @Override
                    public int height() {
                        return 20;
                    }
                    
                    @Override
                    public boolean isEnabled() {
                        return spinner.isEnabled();
                    }
                    
                    @Override
                    public void click() {
                        value = Math.min(max(), value + step());
                        onUpdate();
                    }
                });
    }
    
    public int min() {
        return 0;
    }
    
    public int value() {
        if (value < min()) {
            value = min();
            onUpdate();
        } else if (value > max()) {
            value = max();
            onUpdate();
        }
        return value;
    }
    
    public int max() {
        return 0;
    }
    
    public int step() {
        return 1;
    }
    
    @Override
    public int width() {
        return 80;
    }
    
    @Override
    public int height() {
        return 30;
    }
    
    public int getValue() {
        return value;
    }
    
    public void setValue(int value) {
        this.value = value;
    }
    
    @Override
    public void paint(Graphics graphics) {
        if (!isShown()) {
            return;
        }

        graphics.setColor(new Color(255, 255, 255, 210));
        GraphicsTools.drawBox(graphics, x(), y(), width(), height(), 20);
        for (CanvasButton button : buttons) {
            button.paint(graphics);
        }
        graphics.setColor(Color.BLACK);
        graphics.setFont(GraphicsTools.fontBold);
        GraphicsTools.drawStringJustified(graphics, 
                value() + "", 
                x() + width() / 2,
                y() + height() / 2, 
                1, 1);
        graphics.setFont(GraphicsTools.font);
    }
    
    @Override
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            for (CanvasButton button : buttons) {
                CanvasInteractable inter = button.updateMoused(x, y);
                if (inter != null) {
                    ans = inter;
                }
            }
        }
        return ans;
    }
    
    public void onUpdate() {}
}
