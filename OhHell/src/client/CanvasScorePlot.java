package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import graphics.OhcGraphicsTools;

public class CanvasScorePlot extends CanvasInteractable {
    public static final double pointSize = 4;
    public static final Color[] colors = {
            Color.BLUE,
            Color.RED,
            Color.GREEN,
            Color.MAGENTA,
            Color.CYAN,
            Color.ORANGE,
            Color.PINK,
            Color.YELLOW,
            Color.DARK_GRAY
    };
    
    private final double tooltipWidth = 150;
    private final double tooltipMargin = 4;
    
    private List<List<List<Double>>> dataTabs = new ArrayList<>();
    private List<List<String>> dataNames = new ArrayList<>();
    private List<List<String>> dataTicks = new ArrayList<>();
    private int selectedTab = 0;
    private List<Double> minX = new ArrayList<>();
    private List<Double> maxX = new ArrayList<>();
    private List<Double> minY = new ArrayList<>();
    private List<Double> maxY = new ArrayList<>();
    private double paddingX = 0.05;
    private double paddingY = 0.1;
    
    private double mouseX;
    private double mouseY;
    
    public int x() {
        return 0;
    }
    
    public int y() {
        return 0;
    }
    
    public int width() {
        return 0;
    }
    
    public int height() {
        return 0;
    }
    
    public void addIntData(int index, String name, List<Integer> intData) {
        List<Double> data = new ArrayList<>(intData.size());
        for (Integer y : intData) {
            data.add((double) y);
        }
        addData(index, name, data);
    }
    
    public void addTicks(int index, List<String> ticks) {
        dataTicks.add(ticks);
    }
    
    public void addData(int index, String name, List<Double> data) {
        while (index >= dataTabs.size()) {
            addTab();
        }
        maxX.set(index, Math.max(maxX.get(index), data.size() - 1));
        for (Double y : data) {
            minY.set(index, Math.min(minY.get(index), y));
            maxY.set(index, Math.max(maxY.get(index), y));
        }
        dataTabs.get(index).add(data);
        dataNames.get(index).add(name);
    }
    
    public void addTab() {
        dataTabs.add(new ArrayList<>());
        dataNames.add(new ArrayList<>());
        minX.add((double) 0);
        maxX.add((double) 0);
        minY.add((double) Integer.MAX_VALUE);
        maxY.add((double) Integer.MIN_VALUE);
    }
    
    public void selectTab(int index) {
        this.selectedTab = index;
    }
    
    public int getSelectedTab() {
        return selectedTab;
    }
    
    public void paint(Graphics graphics) {
        graphics.setColor(Color.WHITE);
        graphics.fillRoundRect(x(), y(), width(), height(), 10, 10);
        graphics.setColor(Color.BLACK);
        graphics.drawRoundRect(x(), y(), width(), height(), 10, 10);
        
        if (selectedTab < dataTabs.size()) {
            double nearestX = 0;
            double boxX = 0;
            double boxWidth = 0;
            if (isMoused()) {
                nearestX = Math.min(maxX.get(selectedTab), Math.max(minX.get(selectedTab), Math.round(mouseX)));
                boxX = minX.get(selectedTab) < nearestX ? nearestX - 0.5 : nearestX;
                boxWidth = minX.get(selectedTab) < nearestX && nearestX < maxX.get(selectedTab) ? 1 : 0.5;
                graphics.setColor(new Color(192, 192, 192));
                drawRect(graphics, boxX, minY.get(selectedTab), boxWidth, maxY.get(selectedTab) - minY.get(selectedTab));
            }

            graphics.setColor(Color.BLACK);
            drawLine(graphics, 0, minY.get(selectedTab), 0, maxY.get(selectedTab));
            if (minY.get(selectedTab) <= 0 && 0 <= maxY.get(selectedTab)) {
                drawLine(graphics, minX.get(selectedTab), 0, maxX.get(selectedTab), 0);
            }
            
            graphics.setFont(OhcGraphicsTools.fontSmall);
            for (int x = 0; x <= maxX.get(selectedTab); x++) {
                OhcGraphicsTools.drawStringJustified(graphics, 
                        dataTicks.get(selectedTab).get(x), 
                        (int) canvasX(x), 
                        y() + height() - 10, 
                        1, 1);
            }
            graphics.setFont(OhcGraphicsTools.font);
            
            int p = 0;
            for (List<Double> data : dataTabs.get(selectedTab)) {
                graphics.setColor(colors[p]);
                double x = 0;
                double y = 0;
                for (Double newY : data) {
                    drawPoint(graphics, x, newY);
                    if (x > 0) {
                        drawLine(graphics, x - 1, y, x, newY);
                    }
                    x++;
                    y = newY;
                }
                p++;
            }
            
            if (isMoused()) {
                List<double[]> boxData = new ArrayList<>(dataTabs.get(selectedTab).size());
                for (int k = 0; k < dataTabs.get(selectedTab).size(); k++) {
                    boxData.add(new double[] {
                            k,
                            dataTabs.get(selectedTab).get(k).get((int) nearestX)
                    });
                }
                boxData.sort((d1, d2) -> (int) Math.signum(d2[1] - d1[1]));
                
                int ttX = (int) canvasX(boxX + boxWidth);
                int ttHeight = (int) (tooltipMargin * 2 + 15 + 15 * boxData.size());
                int ttY = (int) canvasY((minY.get(selectedTab) + maxY.get(selectedTab)) / 2) - ttHeight / 2;
                graphics.setColor(Color.WHITE);
                graphics.fillRoundRect(ttX, ttY, (int) tooltipWidth, ttHeight, 10, 10);
                graphics.setColor(Color.BLACK);
                graphics.drawRoundRect(ttX, ttY, (int) tooltipWidth, ttHeight, 10, 10);
                
                OhcGraphicsTools.drawStringJustified(graphics, 
                        dataTicks.get(selectedTab).get((int) nearestX),
                        (int) (ttX + tooltipMargin), 
                        (int) (ttY + tooltipMargin), 
                        0, 2);
                
                for (int k = 0; k < boxData.size(); k++) {
                    graphics.setColor(colors[(int) boxData.get(k)[0]]);
                    graphics.fillOval(
                            (int) (ttX + tooltipMargin - pointSize / 2), 
                            (int) (ttY + tooltipMargin + 15 * (k + 1) - pointSize / 2 + 6), 
                            (int) pointSize, 
                            (int) pointSize);
                    graphics.setColor(Color.BLACK);
                    OhcGraphicsTools.drawStringJustified(graphics, 
                            OhcGraphicsTools.fitString(
                                    graphics,
                                    dataNames.get(selectedTab).get((int) boxData.get(k)[0]), 
                                    tooltipWidth * 0.8 - 2 * tooltipMargin),
                            (int) (ttX + 2 * tooltipMargin), 
                            (int) (ttY + tooltipMargin + 15 * (k + 1)), 
                            0, 2);
                    OhcGraphicsTools.drawStringJustified(graphics, 
                            String.format("%.1f", boxData.get(k)[1]), 
                            (int) (ttX + tooltipMargin + tooltipWidth * 0.8), 
                            (int) (ttY + tooltipMargin + 15 * (k + 1)), 
                            1, 2);
                }
            }
        }
    }
    
    private double canvasX(double x) {
        return x() 
                + (double) width() * paddingX 
                + (x - minX.get(selectedTab)) * (1 - 2 * paddingX) * (double) width() / (maxX.get(selectedTab) - minX.get(selectedTab));
    }
    
    private double canvasY(double y) {
        return y() 
                + (double) height() * (1 - paddingY) 
                - (y - minY.get(selectedTab)) * (1 - 2 * paddingY) * (double) height() / (maxY.get(selectedTab) - minY.get(selectedTab));
    }
    
    private double plotX(double x) {
        return minX.get(selectedTab)
                + (x - x() - (double) width() * paddingX) * (maxX.get(selectedTab) - minX.get(selectedTab)) / ((1 - 2 * paddingX) * (double) width());
    }
    
    private double plotY(double y) {
        return minY.get(selectedTab)
                - (y - y() - (double) height() * (1 - paddingY)) * (maxY.get(selectedTab) - minY.get(selectedTab)) / ((1 - 2 * paddingY) * (double) height());
    }
    
    private void drawPoint(Graphics graphics, double x, double y) {
        graphics.fillOval(
                (int) (canvasX(x) - pointSize / 2), 
                (int) (canvasY(y) - pointSize / 2), 
                (int) pointSize, 
                (int) pointSize);
    }
    
    private void drawLine(Graphics graphics, double x1, double y1, double x2, double y2) {
        graphics.drawLine(
                (int) canvasX(x1), 
                (int) canvasY(y1), 
                (int) canvasX(x2), 
                (int) canvasY(y2));
    }
    
    private void drawRect(Graphics graphics, double x, double y, double width, double height) {
        graphics.fillRoundRect(
                (int) canvasX(x), 
                (int) canvasY(y + height), 
                (int) (canvasX(x + width) - canvasX(x)), 
                (int) (canvasY(y) - canvasY(y + height)),
                10, 10);
    }
    
    @Override
    public boolean updateMoused(int x, int y) {
        boolean ans = super.updateMoused(x, y);
        if (isMoused()) {
            mouseX = plotX(x);
            mouseY = plotY(y);
        }
        return ans;
    }
}
