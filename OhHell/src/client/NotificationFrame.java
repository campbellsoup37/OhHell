package client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

import common.OhcButton;
import common.FileTools;

public class NotificationFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private JLabel message = new JLabel();
    private JButton okButton = new OhcButton("OK");
    
    public NotificationFrame(String text) {
        message.setText(text);
        message.setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    public void execute() {
        setTitle("Message");
        setIconImage(FileTools.loadImage("resources/icon/cw.png", this));
        
        message.setBounds(0, 12, 340, 40);
        add(message);
        
        okButton.setBounds(130, 65, 80, 30);
        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        add(okButton);
        
        setSize(340, 140);
        setResizable(false);
        setLayout(null);
        setVisible(true);
        
        int windowWidth = Math.max(340, 
                message.getGraphics().getFontMetrics().stringWidth(message.getText()) + 50);
        message.setBounds(0, 12, windowWidth, 40);
        setSize(windowWidth, 140);
        okButton.setBounds(windowWidth / 2 - 40, 65, 80, 30);
    }
    
    public void close() {
        setVisible(false);
        dispose();
    }
}