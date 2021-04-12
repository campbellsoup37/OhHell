package common;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;

public class OhcTextField extends JTextField {
    private static final long serialVersionUID = 1L;
    
    private boolean showingDefaultText;
    private String defaultText;
    
    public OhcTextField(String defaultText) {
        this.defaultText = defaultText;
        
        showDefaultText();
        setOpaque(false);
        setBorder(new EmptyBorder(2, 6, 2, 6));
        addFocusListener(new FocusListener() {
            @Override
            public void focusGained(FocusEvent e) {
                if (showingDefaultText) {
                    setText("");
                }
            }

            @Override
            public void focusLost(FocusEvent e) {
                if (getText().isEmpty()) {
                    showDefaultText();
                }
            }
        });
        setFont(GraphicsTools.font);
    }
    
    public void showDefaultText() {
        showingDefaultText = true;
        super.setText(defaultText);
    }
    
    @Override
    public void setText(String text) {
        if (text.isEmpty() && !isFocusOwner()) {
            showDefaultText();
        } else {
            showingDefaultText = false;
            super.setText(text);
        }
    }
    
    @Override
    public String getText() {
        if (showingDefaultText) {
            return "";
        } else {
            return super.getText();
        }
    }
    
    public boolean isEmpty() {
        return showingDefaultText;
    }
    
    public boolean isBoxed() {
        return true;
    }

    @Override
    public void paintComponent(Graphics graphics) {
        if (isVisible()) {
            Graphics2D graphics2 = GraphicsTools.makeGraphics2D(graphics, true, false);
            if (isBoxed()) {
                graphics2.setColor(Color.WHITE);
                GraphicsTools.drawBox(graphics2, 0, 0, getWidth() - 1, getHeight() - 1, 15);
            }
            
            if (showingDefaultText) {
                setForeground(Color.GRAY);
            } else {
                setForeground(Color.BLACK);
            }
            
            super.paintComponent(graphics2);
        }
    }
}
