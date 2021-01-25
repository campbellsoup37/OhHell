package aiworkshop;
import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;

import ml.BasicVector;
import ml.Feature;
import ml.Learner;

public class LearnerExplorer extends JPanel {
    private static final long serialVersionUID = 1L;

    private Learner L = new strategyRBP.OverallValueLearner("resources/ai workshop/OhHellAIModels/RBP/b30_30o10i40/5/ovl.txt");
    
    List<JLabel> labels = new ArrayList<>();
    List<List<JLabel>> subLabels = new ArrayList<>();
    List<List<JTextField>> fields = new ArrayList<>();
    int totalSize = 0;
    
    JLabel outputLabel = new JLabel("Output: ");
    JTextField outputField = new JTextField();
    
    public void execute() {
        JFrame window = new JFrame();
        window.setTitle("Learner Explorer");
        
        setLayout(new BorderLayout());
        
        JScrollPane scrollPane = new JScrollPane(this,
                JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
                JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        scrollPane.setBounds(0, 0, 900, 800);
        window.add(scrollPane);
        
        List<Feature> features = L.getFeatures();
        int j = 0;
        for (Feature feature : features) {
            JLabel newLabel = new JLabel(feature.getName());
            newLabel.setBounds(20, 20 + 120 * j, 200, 20);
            add(newLabel);
            List<JLabel> newSubLabels = new ArrayList<>(feature.getDimension());
            List<JTextField> newFields = new ArrayList<>(feature.getDimension());
            for (int i = 0; i < feature.getDimension(); i++) {
                JLabel newSubLabel = new JLabel(feature.getSubNames()[i]);
                newSubLabel.setBounds(20 + 60 * i, 40 + 120 * j, 40, 40);
                add(newSubLabel);
                JTextField newField = new JTextField();
                newField.setBounds(20 + 60 * i, 80 + 120 * j, 40, 40);
                newField.setText(0 + "");
                add(newField);
                newSubLabels.add(newSubLabel);
                newFields.add(newField);
                totalSize++;
            }
            subLabels.add(newSubLabels);
            fields.add(newFields);
            j++;
        }
        
        add(outputLabel);
        add(outputField);
        
        for (List<JTextField> featureFields : fields) {
            for (JTextField field : featureFields) {
                field.addActionListener(new ActionListener() {
                    @Override
                    public void actionPerformed(ActionEvent arg0) {
                        double[] arr = new double[totalSize];
                        int i = 0;
                        for (List<JTextField> featureFields : fields) {
                            for (JTextField field : featureFields) {
                                arr[i] = Double.parseDouble(field.getText());
                                i++;
                            }
                        }
                        outputField.setText(L.testValue(new BasicVector(arr)).get(1).get(0) + "");
                    }
                });
            }
        }
        
        window.addWindowListener(new WindowListener() {
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
                window.setVisible(false);
                window.dispose();
            }
        });
        
        window.setSize(1000, 900);
        window.setResizable(false);
        window.setLayout(null);
        window.setVisible(true);
        window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    }

    public static void main(String[] args) {
        LearnerExplorer le = new LearnerExplorer();
        le.execute();
    }
}
