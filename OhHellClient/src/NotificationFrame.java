import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingConstants;

public class NotificationFrame extends JFrame {
    private static final long serialVersionUID = 1L;

    private JLabel message = new JLabel();
    private JButton okButton = new JButton("OK");
    
    public NotificationFrame(String text) {
        message.setText(text);
        message.setHorizontalAlignment(SwingConstants.CENTER);
    }
    
    public void execute() {
        setTitle("Message");
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
    }
    
    public void close() {
        setVisible(false);
        dispose();
    }
}