import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;

public class Dashboard extends JFrame {
    private static final long serialVersionUID = 1L;
    
    private int epochCount = 0;
    private long lastEpochTime = 0;
    private long epochTime = 0;
    
    private int datumCount = 0;
    private int datumTotal = 1;
    private Color datumColor = Color.CYAN;
    
    private String logText = "";
    
    private JPanel epochPanel = new JPanel() {
        private static final long serialVersionUID = 1L;

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.setColor(Color.BLACK);
            g.drawString("Epoch: " + epochCount, 20, 26);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    };
    private JPanel epochTimePanel = new JPanel() {
        private static final long serialVersionUID = 1L;
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.setColor(Color.BLACK);
            if (lastEpochTime > 0) {
                g.drawString("Epoch time: " + (epochTime - lastEpochTime) + " millisecs", 20, 26);
            }
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    };
    private JPanel datumPanel = new JPanel() {
        private static final long serialVersionUID = 1L;
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.setColor(datumColor);
            g.fillRect(0, 0, (getWidth() - 1) * datumCount / datumTotal, getHeight() - 1);
            g.setColor(Color.BLACK);
            g.drawString("Datum: " + datumCount + " / " + datumTotal, 20, 26);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    };
    private JPanel datumTimePanel = new JPanel() {
        private static final long serialVersionUID = 1L;
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            g.setColor(Color.BLACK);
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
    };
    
    private class Graph extends JPanel {
        private static final long serialVersionUID = 1L;
        
        private final int numGraphPoints = 100;
        
        private List<Double[]> datas = new ArrayList<>();
        private List<Integer> pointers = new ArrayList<>();
        private List<Color> colors = new ArrayList<>();
        private double min = Integer.MAX_VALUE;
        private double max = Integer.MIN_VALUE;
        
        private JLabel label = new JLabel("");
        
        public Graph(int index) {}
        
        public void addData(int index, double y) {
            while (datas.size() <= index) {
                datas.add(new Double[numGraphPoints]);
                pointers.add(0);
            }
            while (colors.size() <= index) {
                colors.add(Color.BLACK);
            }
            datas.get(index)[pointers.get(index)] = y;
            pointers.set(index, (pointers.get(index) + 1) % numGraphPoints);
            min = Math.min(min, y);
            max = Math.max(max, y);
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    updateUI();
                }
            });
        }
        
        public void setColor(int index, Color color) {
            while (colors.size() <= index) {
                colors.add(Color.BLACK);
            }
            colors.set(index, color);
        }
        
        public JLabel getLabel() {
            return label;
        }
        
        public void setLabel(String text) {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    label.setText(text);
                }
            });
        }
        
        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);
            g.setColor(Color.WHITE);
            g.fillRect(0, 0, getWidth() - 1, getHeight() - 1);
            for (int index2 = 0; index2 < datas.size(); index2++) {
                g.setColor(colors.get(index2));
                Double prev = null;
                for (int i = 0; i < numGraphPoints; i++) {
                    Double curr = datas.get(index2)[(pointers.get(index2) + i) % numGraphPoints];
                    if (curr != null && prev != null) {
                        g.drawLine(getX(i - 1), getY(prev), getX(i), getY(curr));
                    }
                    prev = curr;
                    if (curr != null && i == numGraphPoints - 1 && index2 == datas.size() - 1) {
                        g.setColor(Color.BLACK);
                        int y = Math.max(13, Math.min(getHeight() - 2, getY(curr) + 6));
                        g.drawString(String.format("%.2f", curr), getWidth() - 39, y);
                    }
                }
            }
            g.setColor(Color.BLACK);
            if (max > min) {
                g.drawString(String.format("%.2f", max), 1, 13);
                g.drawString(String.format("%.2f", min), 1, getHeight() - 2);
            }
            g.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
        }
        
        private int getX(int i) {
            return 40 + i * (getWidth() - 80) / numGraphPoints;
        }
        
        private int getY(double y) {
            return (int) ((max - y) / (max - min) * (getHeight() - 1));
        }
    }
    private List<Graph> graphs;
    
    private JTextArea logArea = new JTextArea();
    private JScrollPane logScrollPane = new JScrollPane(logArea,
            JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
            JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    
    private JButton goButton = new JButton("Go");
    private JButton closeButton = new JButton("Close");
    
    public Dashboard() {
        
    }
    
    public void updateEpoch(int epochNumber) {
        epochCount = epochNumber;
        lastEpochTime = epochTime;
        epochTime = System.currentTimeMillis();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                epochPanel.updateUI();
                epochTimePanel.updateUI();
            }
        });
    }
    
    public void updateDatum(int datumNumber, int datumTotal, Color color) {
        datumCount = datumNumber;
        this.datumTotal = datumTotal;
        datumColor = color;
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                datumPanel.updateUI();
            }
        });
    }
    
    public void addLog(String text) {
        logText += text;
    }
    
    public void updateLog() {
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                logArea.setText(logText);
                logText = "";
            }
        });
    }
    
    public void setGraphCount(int count) {
        graphs = new ArrayList<>(count);
        int h = 555 / count - 30;
        for (int i = 0; i < count; i++) {
            Graph graph = new Graph(i);
            graphs.add(graph);
            graph.getLabel().setBounds(40, 180 + (h + 30) * i, 440, 15);
            add(graph.getLabel());
            graph.setBounds(40, 180 + (h + 30) * i + 15, 440, h);
            add(graph);
        }
    }
    
    public void setGraphColor(int index1, int index2, Color color) {
        graphs.get(index1).setColor(index2, color);
    }
    
    public void setGraphLabel(int index, String text) {
        graphs.get(index).setLabel(text);
    }
    
    public void addGraphData(int index1, int index2, double y) {
        graphs.get(index1).addData(index2, y);
    }
    
    public void initialize() {}
    
    public void execute() {
        setTitle("Dashboard");
        
        epochPanel.setBounds(40, 40, 200, 40);
        add(epochPanel);
        
        epochTimePanel.setBounds(280, 40, 200, 40);
        add(epochTimePanel);
        
        datumPanel.setBounds(40, 120, 200, 40);
        add(datumPanel);
        
        datumTimePanel.setBounds(280, 120, 200, 40);
        add(datumTimePanel);
        
        logScrollPane.setBounds(520, 40, 440, 800);
        add(logScrollPane);
        
        goButton.setBounds(40, 760, 200, 40);
        goButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                
            }
        });
        add(goButton);
        
        closeButton.setBounds(280, 760, 200, 40);
        closeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
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
                close();
            }
        });
        
        setSize(1000, 900);
        setResizable(false);
        setLayout(null);
        setVisible(true);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        
        initialize();
    }
    
    public void close() {
        setVisible(false);
        dispose();
    }
    
    public static void main(String[] args) {
        Dashboard dash = new Dashboard();
        dash.execute();
    }
}