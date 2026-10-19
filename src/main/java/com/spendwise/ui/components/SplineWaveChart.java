package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.LinearGradientPaint;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;

public class SplineWaveChart extends JPanel {

    public record Series(String name, Color color, List<Double> values) {
    }

    private String title = "";
    private final List<String> xLabels = new ArrayList<>();
    private final List<Series> seriesList = new ArrayList<>();
    private float animProgress = 0.0f;
    private Timer animTimer;
    private int hoveredIndex = -1;
    private int mouseX = -1;
    private int mouseY = -1;

    public SplineWaveChart() {
        setBackground(AppTheme.CARD_BG);
        setOpaque(true);
        setPreferredSize(new Dimension(500, 260));

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mouseX = e.getX();
                mouseY = e.getY();
                updateHoverIndex();
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hoveredIndex = -1;
                mouseX = -1;
                mouseY = -1;
                repaint();
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
    }

    public void setData(String title, List<String> labels, List<Series> series) {
        this.title = title;
        this.xLabels.clear();
        if (labels != null) {
            this.xLabels.addAll(labels);
        }
        this.seriesList.clear();
        if (series != null) {
            this.seriesList.addAll(series);
        }
        playEntranceAnimation();
    }

    public void playEntranceAnimation() {
        if (animTimer != null && animTimer.isRunning()) {
            animTimer.stop();
        }
        animProgress = 0.0f;
        final int totalFrames = 24;
        final int[] frame = {0};
        animTimer = new Timer(16, e -> {
            frame[0]++;
            float p = (float) frame[0] / totalFrames;
            if (p >= 1.0f) {
                p = 1.0f;
                ((Timer) e.getSource()).stop();
            }
            animProgress = p;
            repaint();
        });
        animTimer.start();
        repaint();
    }

    public void updateSimulatedSeries(int seriesIndex, List<Double> newValues) {
        if (seriesIndex >= 0 && seriesIndex < seriesList.size()) {
            Series old = seriesList.get(seriesIndex);
            seriesList.set(seriesIndex, new Series(old.name(), old.color(), newValues));
            repaint();
        }
    }

    private void updateHoverIndex() {
        if (xLabels.isEmpty() || mouseX < 0) {
            hoveredIndex = -1;
            return;
        }
        int leftPad = 68;
        int rightPad = 24;
        int chartW = getWidth() - leftPad - rightPad;
        if (chartW <= 0) {
            hoveredIndex = -1;
            return;
        }
        int n = xLabels.size();
        float step = (float) chartW / Math.max(1, n - 1);
        int nearest = Math.round((mouseX - leftPad) / step);
        if (nearest >= 0 && nearest < n) {
            hoveredIndex = nearest;
        } else {
            hoveredIndex = -1;
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        g2.setColor(AppTheme.CARD_BG);
        g2.fillRect(0, 0, w, h);

        if (title != null && !title.isEmpty()) {
            g2.setFont(AppTheme.FONT_SUBHEADER);
            g2.setColor(AppTheme.TEXT_PRIMARY);
            g2.drawString(title, 20, 24);
        }

        if (xLabels.isEmpty() || seriesList.isEmpty()) {
            g2.setFont(AppTheme.FONT_BODY);
            g2.setColor(AppTheme.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            String noData = "No activity data recorded yet";
            g2.drawString(noData, (w - fm.stringWidth(noData)) / 2, h / 2);
            g2.dispose();
            return;
        }

        int leftPad = 68;
        int rightPad = 24;
        int topPad = 48;
        int bottomPad = 52;
        int chartW = w - leftPad - rightPad;
        int chartH = h - topPad - bottomPad;

        if (chartW <= 0 || chartH <= 0) {
            g2.dispose();
            return;
        }

        double maxVal = 1000.0;
        for (Series s : seriesList) {
            for (Double d : s.values()) {
                if (d != null && d > maxVal) {
                    maxVal = d;
                }
            }
        }
        maxVal = Math.ceil(maxVal * 1.15 / 1000.0) * 1000.0;

        g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        g2.setColor(AppTheme.CARD_BORDER);
        int gridLines = 4;
        for (int i = 0; i <= gridLines; i++) {
            int y = topPad + (int) ((float) (gridLines - i) / gridLines * chartH);
            g2.setColor(new Color(39, 48, 59, 120));
            g2.drawLine(leftPad, y, w - rightPad, y);

            double labelVal = maxVal * ((double) i / gridLines);
            String valStr = formatCurrencyShort(labelVal);
            g2.setColor(AppTheme.TEXT_MUTED);
            FontMetrics fm = g2.getFontMetrics();
            g2.drawString(valStr, leftPad - fm.stringWidth(valStr) - 8, y + 4);
        }

        int n = xLabels.size();
        float stepX = (float) chartW / Math.max(1, n - 1);
        g2.setFont(AppTheme.FONT_SMALL);
        for (int i = 0; i < n; i++) {
            int x = (int) (leftPad + i * stepX);
            String label = xLabels.get(i);
            FontMetrics fm = g2.getFontMetrics();
            int labelW = fm.stringWidth(label);
            g2.setColor(i == hoveredIndex ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_MUTED);
            g2.drawString(label, x - labelW / 2, h - bottomPad + 18);
        }

        float eased = 1.0f - (float) Math.pow(1.0f - animProgress, 3);

        for (Series s : seriesList) {
            if (s.values().isEmpty()) {
                continue;
            }
            List<Double> vals = s.values();
            int pointCount = Math.min(n, vals.size());
            if (pointCount < 1) {
                continue;
            }

            float[] xs = new float[pointCount];
            float[] ys = new float[pointCount];

            for (int i = 0; i < pointCount; i++) {
                xs[i] = leftPad + i * stepX;
                double val = (vals.get(i) != null ? vals.get(i) : 0.0) * eased;
                ys[i] = topPad + chartH - (float) (val / maxVal * chartH);
            }

            Path2D.Float curvePath = new Path2D.Float();
            curvePath.moveTo(xs[0], ys[0]);

            for (int i = 0; i < pointCount - 1; i++) {
                float x1 = xs[i];
                float y1 = ys[i];
                float x2 = xs[i + 1];
                float y2 = ys[i + 1];
                float dx = x2 - x1;
                float cp1x = x1 + dx * 0.45f;
                float cp1y = y1;
                float cp2x = x2 - dx * 0.45f;
                float cp2y = y2;
                curvePath.curveTo(cp1x, cp1y, cp2x, cp2y, x2, y2);
            }

            Path2D.Float areaPath = new Path2D.Float(curvePath);
            areaPath.lineTo(xs[pointCount - 1], topPad + chartH);
            areaPath.lineTo(xs[0], topPad + chartH);
            areaPath.closePath();

            Color sc = s.color();
            Color topGrad = new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), 60);
            Color botGrad = new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), 2);
            g2.setPaint(new LinearGradientPaint(0, topPad, 0, topPad + chartH, new float[]{0.0f, 1.0f}, new Color[]{topGrad, botGrad}));
            g2.fill(areaPath);

            g2.setColor(new Color(sc.getRed(), sc.getGreen(), sc.getBlue(), 75));
            g2.setStroke(new BasicStroke(5.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(curvePath);

            g2.setColor(sc);
            g2.setStroke(new BasicStroke(2.6f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(curvePath);

            for (int i = 0; i < pointCount; i++) {
                g2.setColor(AppTheme.CARD_BG);
                g2.fillOval((int) xs[i] - 4, (int) ys[i] - 4, 8, 8);
                g2.setColor(sc);
                g2.fillOval((int) xs[i] - 2, (int) ys[i] - 2, 4, 4);
            }
        }

        int legendX = leftPad;
        int legendY = h - 14;
        g2.setFont(AppTheme.FONT_SMALL);
        for (Series s : seriesList) {
            g2.setColor(s.color());
            g2.fillRoundRect(legendX, legendY - 8, 10, 10, 3, 3);
            g2.setColor(AppTheme.TEXT_SECONDARY);
            g2.drawString(s.name(), legendX + 14, legendY);
            FontMetrics fm = g2.getFontMetrics();
            legendX += fm.stringWidth(s.name()) + 26;
        }

        if (hoveredIndex >= 0 && hoveredIndex < n) {
            float hx = leftPad + hoveredIndex * stepX;
            g2.setColor(new Color(255, 255, 255, 45));
            g2.setStroke(new BasicStroke(1.2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
            g2.drawLine((int) hx, topPad, (int) hx, topPad + chartH);

            List<String> tipLines = new ArrayList<>();
            tipLines.add(xLabels.get(hoveredIndex));
            for (Series s : seriesList) {
                if (hoveredIndex < s.values().size() && s.values().get(hoveredIndex) != null) {
                    double v = s.values().get(hoveredIndex);
                    tipLines.add(s.name() + ": Rs " + String.format("%,.0f", v));
                }
            }

            g2.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
            FontMetrics tfm = g2.getFontMetrics();
            int maxTipW = 0;
            for (String l : tipLines) {
                maxTipW = Math.max(maxTipW, tfm.stringWidth(l));
            }
            int tipPad = 8;
            int boxW = maxTipW + tipPad * 2;
            int boxH = tipLines.size() * 16 + tipPad * 2;
            int boxX = (int) hx + 12;
            if (boxX + boxW > w - 8) {
                boxX = (int) hx - boxW - 12;
            }
            int boxY = Math.max(topPad + 8, mouseY - boxH / 2);
            if (boxY + boxH > topPad + chartH) {
                boxY = topPad + chartH - boxH;
            }

            g2.setColor(new Color(19, 25, 32, 235));
            g2.fillRoundRect(boxX, boxY, boxW, boxH, 6, 6);
            g2.setColor(AppTheme.CARD_BORDER);
            g2.setStroke(new BasicStroke(1.0f));
            g2.drawRoundRect(boxX, boxY, boxW, boxH, 6, 6);

            for (int li = 0; li < tipLines.size(); li++) {
                if (li == 0) {
                    g2.setColor(AppTheme.TEXT_PRIMARY);
                } else {
                    Series s = seriesList.get(li - 1);
                    g2.setColor(s.color());
                }
                g2.drawString(tipLines.get(li), boxX + tipPad, boxY + tipPad + (li + 1) * 14 - 2);
            }
        }

        g2.dispose();
    }

    private String formatCurrencyShort(double val) {
        if (val >= 100000.0) {
            return String.format("Rs %.0fL", val / 100000.0);
        } else if (val >= 1000.0) {
            return String.format("Rs %.0fk", val / 1000.0);
        }
        return String.format("Rs %.0f", val);
    }
}
