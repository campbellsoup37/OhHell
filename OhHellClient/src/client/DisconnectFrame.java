package client;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JScrollPane;

import graphics.OhcGraphicsTools;

public class DisconnectFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private GameClient client;
    
    private JLabel message = new JLabel("The following players are disconnected:");
    
    private DefaultListModel<String> playersListModel = new DefaultListModel<String>();
    private JList<String> playersJList = new JList<String>(playersListModel);
    private JScrollPane playersScrollPane = new JScrollPane(playersJList);
    
    private JButton kickButton = new JButton("Kick");
    //private JButton kickAllButton = new JButton("Kick All");
    
    public DisconnectFrame(GameClient client) {
        this.client = client;
    }
    
    public void execute() {
        setTitle("Message");
        setIconImage(OhcGraphicsTools.loadImage("resources/cw.png", this));
        message.setBounds(20, 10, 500, 20);
        add(message);
        
        playersScrollPane.setBounds(20, 40, 300, 200);
        add(playersScrollPane);
        
        kickButton.setBounds(20, 250, 300, 40);
        kickButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int index = playersJList.getSelectedIndex();
                if (index != -1) {
                    client.voteKick(index);
                }
            }
        });
        add(kickButton);
        
        /*kickAllButton.setBounds(180, 250, 140, 40);
        kickAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                for (int i = 0; i < playersListModel.size(); i++) {
                    client.voteKick(i);
                }
            }
        });
        add(kickAllButton);*/
        
        setSize(346, 340);
        setResizable(false);
        setLayout(null);
        setVisible(true);
    }
    
    public void setDcList(List<String> names) {
        playersListModel.clear();
        for (String name : names) {
            playersListModel.addElement(name);
        }
        playersJList.setSelectedIndex(0);
    }
    
    public void close() {
        setVisible(false);
        dispose();
    }
}