package client;

import java.awt.Color;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.List;

import graphics.OhcGraphicsTools;

public class CanvasScorePlot extends CanvasInteractable {
    private final double tooltipWidth = 150;
    private final double tooltipMargin = 4;
    
    private boolean boxed = true;
    private boolean axes = true;
    
    private List<List<Double>> datas = new ArrayList<>();
    private List<String> dataNames = new ArrayList<>();
    private List<String> dataTicks = new ArrayList<>();
    private double minX = 0;
    private double maxX = 0;
    private double minY = Integer.MAX_VALUE;
    private double maxY = Integer.MIN_VALUE;
    private double paddingX = 0.05;
    private double paddingY = 0.1;
    
    private double mouseX;
    @SuppressWarnings("unused")
    private double mouseY;
    
    public void addIntData(String name, List<Integer> intData) {
        List<Double> data = new ArrayList<>(intData.size());
        for (Integer y : intData) {
            data.add((double) y);
        }
        addData(name, data);
    }
    
    public void setBoxed(boolean boxed) {
        this.boxed = boxed;
    }
    
    public void setAxes(boolean axes) {
        this.axes = axes;
    }
    
    public void setTicks(List<String> ticks) {
        dataTicks = ticks;
    }
    
    public void addData(String name, List<Double> data) {
        maxX = Math.max(data.size() - 1, maxX);
        for (Double y : data) {
            minY = Math.min(y, minY);
            maxY = Math.max(y, maxY);
        }
        datas.add(data);
        dataNames.add(name);
    }
    
    public void setMaxY(double maxY) {
        this.maxY = maxY;
    }
    
    public void setMinY(double minY) {
        this.minY = minY;
    }
    
    public void paint(Graphics graphics) {
        if (boxed) {
            graphics.setColor(Color.WHITE);
            OhcGraphicsTools.drawBox(graphics, x(), y(), width(), height(), 10);
        }
        
        double nearestX = 0;
        double boxX = 0;
        double boxWidth = 0;
        if (isMoused()) {
            nearestX = Math.min(maxX, Math.max(minX, Math.round(mouseX)));
            boxX = minX < nearestX ? nearestX - 0.5 : nearestX;
            boxWidth = minX < nearestX && nearestX < maxX ? 1 : 0.5;
            graphics.setColor(new Color(192, 192, 192));
            drawRect(graphics, boxX, minY, boxWidth, maxY - minY);
        }

        graphics.setColor(Color.BLACK);
        if (axes) {
            drawLine(graphics, 0, minY, 0, maxY);
            if (minY <= 0 && 0 <= maxY) {
                drawLine(graphics, minX, 0, maxX, 0);
            }
        }
        
        if (!dataTicks.isEmpty()) {
            graphics.setFont(OhcGraphicsTools.fontSmall);
            for (int x = 0; x <= maxX; x++) {
                OhcGraphicsTools.drawStringJustified(graphics, 
                        dataTicks.get(x), 
                        (int) canvasX(x), 
                        y() + height() - 10, 
                        1, 1);
            }
            graphics.setFont(OhcGraphicsTools.font);
        }
        
        int p = 0;
        for (List<Double> data : datas) {
            graphics.setColor(GameCanvas.colors[p]);
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
            List<double[]> boxData = new ArrayList<>(datas.size());
            for (int k = 0; k < datas.size(); k++) {
                boxData.add(new double[] {
                        k,
                        datas.get(k).get((int) nearestX)
                });
            }
            boxData.sort((d1, d2) -> (int) Math.signum(d2[1] - d1[1]));
            
            int ttX = (int) canvasX(boxX + boxWidth);
            int ttHeight = (int) (tooltipMargin * 2 + 15 + 15 * boxData.size());
            int ttY = (int) canvasY((minY + maxY) / 2) - ttHeight / 2;
            graphics.setColor(Color.WHITE);
            OhcGraphicsTools.drawBox(graphics, ttX, ttY, tooltipWidth, ttHeight, 10);
            
            OhcGraphicsTools.drawStringJustified(graphics, 
                    dataTicks.get((int) nearestX),
                    (int) (ttX + tooltipMargin), 
                    (int) (ttY + tooltipMargin), 
                    0, 2);
            
            for (int k = 0; k < boxData.size(); k++) {
                graphics.setColor(GameCanvas.colors[(int) boxData.get(k)[0]]);
                graphics.fillOval(
                        (int) (ttX + tooltipMargin - GameCanvas.pointSize / 2), 
                        (int) (ttY + tooltipMargin + 15 * (k + 1) - GameCanvas.pointSize / 2 + 6), 
                        (int) GameCanvas.pointSize, 
                        (int) GameCanvas.pointSize);
                graphics.setColor(Color.BLACK);
                OhcGraphicsTools.drawStringJustified(graphics, 
                        OhcGraphicsTools.fitString(
                                graphics,
                                dataNames.get((int) boxData.get(k)[0]), 
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
    
    private double canvasX(double x) {
        return x() 
                + (double) width() * paddingX 
                + (x - minX) * (1 - 2 * paddingX) * (double) width() / (maxX - minX);
    }
    
    private double canvasY(double y) {
        return y() 
                + (double) height() * (1 - paddingY) 
                - (y - minY) * (1 - 2 * paddingY) * (double) height() / (maxY - minY);
    }
    
    private double plotX(double x) {
        return minX
                + (x - x() - (double) width() * paddingX) * (maxX - minX) / ((1 - 2 * paddingX) * (double) width());
    }
    
    private double plotY(double y) {
        return minY
                - (y - y() - (double) height() * (1 - paddingY)) * (maxY - minY) / ((1 - 2 * paddingY) * (double) height());
    }
    
    private void drawPoint(Graphics graphics, double x, double y) {
        graphics.fillOval(
                (int) (canvasX(x) - GameCanvas.pointSize / 2), 
                (int) (canvasY(y) - GameCanvas.pointSize / 2), 
                (int) GameCanvas.pointSize, 
                (int) GameCanvas.pointSize);
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
    public CanvasInteractable updateMoused(int x, int y) {
        CanvasInteractable ans = super.updateMoused(x, y);
        if (isMoused()) {
            mouseX = plotX(x);
            mouseY = plotY(y);
        }
        return ans;
    }
}
