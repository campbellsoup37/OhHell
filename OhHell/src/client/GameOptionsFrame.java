package client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import graphics.OhcButton;
import graphics.OhcGraphicsTools;

public class GameOptionsFrame extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private JLabel robotsLabel = new JLabel("Robots:");
    private JSpinner robotsSpinner = new JSpinner(new SpinnerNumberModel(0, 0, 6, 1));
    
    private JLabel doubleDeckLabel = new JLabel("Double deck:");
    private JCheckBox doubleDeckCheckBox = new JCheckBox();
    
    private JButton applyButton = new OhcButton("Apply");
    
    GameClient client;
    
    public GameOptionsFrame(GameClient client, int numRobots, boolean doubleDeck) {
        this.client = client;
        robotsSpinner.setValue(numRobots);
        doubleDeckCheckBox.setSelected(doubleDeck);
    }
    
    public void execute() {
        setTitle("Game Options");
        setIconImage(OhcGraphicsTools.loadImage("resources/icon/cw.png", this));

        robotsLabel.setBounds(57, 30, 200, 40);
        add(robotsLabel);
        
        robotsSpinner.setBounds(150, 30, 40, 40);
        add(robotsSpinner);
        
        doubleDeckLabel.setBounds(28, 70, 200, 40);
        add(doubleDeckLabel);
        
        doubleDeckCheckBox.setBounds(150, 70, 100, 40);
        add(doubleDeckCheckBox);
        
        applyButton.setBounds(50, 210, 125, 40);
        applyButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                close();
            }
        });
        add(applyButton);
        
        setSize(225, 300);
        setResizable(false);
        setLayout(null);
        setVisible(true);
    }
    
    public void close() {
        /*client.setMpNumRobots(Integer.parseInt(robotsSpinner.getValue().toString()));
        client.setMpDoubleDeck(doubleDeckCheckBox.isSelected());*/
        
        setVisible(false);
        dispose();
    }
}
