package client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.LinkedList;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import graphics.OhcButton;
import graphics.OhcGraphicsTools;
import graphics.OhcScrollPane;

public class ReconnectFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private LinkedList<String> names;
    private GameClient client;
    
    private JLabel message = new JLabel("Reconnect as:");
    
    private DefaultListModel<String> playersListModel = new DefaultListModel<>();
    private JList<String> playersJList = new JList<>(playersListModel);
    private JScrollPane playersScrollPane = new OhcScrollPane(playersJList, 
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
    
    private JButton selectButton = new OhcButton("Select");
    private JButton closeButton = new OhcButton("Close");
    
    public ReconnectFrame(LinkedList<String> names, GameClient client) {
        this.names = names;
        this.client = client;
    }
    
    public void execute() {
        setTitle("Reconnect");
        setIconImage(OhcGraphicsTools.loadImage("resources/icon/cw.png", this));
        message.setBounds(20,10,500,20);
        add(message);
        
        for (String name : names) {
            playersListModel.addElement(name);
        }
        playersJList.setSelectedIndex(0);
        playersScrollPane.setBounds(20, 40, 300, 200);
        add(playersScrollPane);
        
        selectButton.setBounds(20, 250, 140, 40);
        selectButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = playersJList.getSelectedIndex();
                if (index != -1) {
                    client.reconnectAs(index);
                    close();
                }
            }
        });
        add(selectButton);
        
        closeButton.setBounds(180, 250, 140, 40);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                client.close();
                close();
            }
        });
        add(closeButton);
        
        addWindowListener(new WindowListener() {
            @Override
            public void windowActivated(WindowEvent arg0) {}
            
            @Override
            public void windowClosed(WindowEvent arg0) {}
            
            @Override
            public void windowDeactivated(WindowEvent arg0) {}
            
            @Override
            public void windowDeiconified(WindowEvent arg0) {}
            
            @Override
            public void windowIconified(WindowEvent arg0) {}
            
            @Override
            public void windowOpened(WindowEvent arg0) {}

            @Override
            public void windowClosing(WindowEvent e) {
                client.close();
                close();
            }
        });
        
        setSize(346, 340);
        setResizable(false);
        setLayout(null);
        setVisible(true);
        //setDefaultCloseOperation(EXIT_ON_CLOSE);
    }
    
    public void close() {
        setVisible(false);
        dispose();
    }
}