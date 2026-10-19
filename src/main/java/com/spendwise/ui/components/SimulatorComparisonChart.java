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
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public class SimulatorComparisonChart extends JPanel {

    private double baselineSpending = 0.0;
    private double currentSimulatedSpending = 0.0;
    private double targetSimulatedSpending = 0.0;
    private double reductionPercent = 0.0;
    private String categoryName = "All Expenses";
    private Timer morphTimer;

    public SimulatorComparisonChart() {
        setBackground(AppTheme.CARD_BG);
        setOpaque(false);
        setPreferredSize(new Dimension(320, 150));
    }

    public void updateSimulation(String categoryName, double baseline, double reductionPercent) {
        this.categoryName = categoryName != null ? categoryName : "Category";
        this.baselineSpending = Math.max(0.0, baseline);
        this.reductionPercent = Math.max(0.0, Math.min(100.0, reductionPercent));
        this.targetSimulatedSpending = this.baselineSpending * (1.0 - this.reductionPercent / 100.0);

        if (morphTimer != null && morphTimer.isRunning()) {
            morphTimer.stop();
        }

        morphTimer = new Timer(15, e -> {
            double diff = targetSimulatedSpending - currentSimulatedSpending;
            if (Math.abs(diff) < 1.0) {
                currentSimulatedSpending = targetSimulatedSpending;
                ((Timer) e.getSource()).stop();
            } else {
                currentSimulatedSpending += diff * 0.35;
            }
            repaint();
        });
        morphTimer.start();
        repaint();
    }

    public void reset(double baseline) {
        this.baselineSpending = baseline;
        this.targetSimulatedSpending = baseline;
        this.currentSimulatedSpending = baseline;
        this.reductionPercent = 0.0;
        if (morphTimer != null && morphTimer.isRunning()) {
            morphTimer.stop();
        }
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        int padLeft = 16;
        int padRight = 16;
        int padTop = 14;
        int padBottom = 28;
        int chartH = h - padTop - padBottom;

        double maxVal = Math.max(100.0, baselineSpending * 1.15);

        int barW = Math.min(64, (w - padLeft - padRight - 60) / 2);
        int bar1X = padLeft + 20;
        int bar2X = w - padRight - barW - 20;

        float bar1H = (float) ((baselineSpending / maxVal) * chartH);
        float bar2H = (float) ((currentSimulatedSpending / maxVal) * chartH);

        int bar1Y = padTop + (int) (chartH - bar1H);
        int bar2Y = padTop + (int) (chartH - bar2H);

        g2.setColor(new Color(39, 48, 59, 100));
        g2.drawLine(padLeft, padTop + chartH, w - padRight, padTop + chartH);

        Color b1Top = new Color(0xFF, 0x4B, 0x2B, 220);
        Color b1Bot = new Color(0xFF, 0x4B, 0x2B, 80);
        g2.setPaint(new LinearGradientPaint(0, bar1Y, 0, padTop + chartH, new float[]{0f, 1f}, new Color[]{b1Top, b1Bot}));
        g2.fill(new RoundRectangle2D.Float(bar1X, bar1Y, barW, bar1H, 8, 8));

        Color b2Top = new Color(0x10, 0xB9, 0x81, 230);
        Color b2Bot = new Color(0x10, 0xB9, 0x81, 90);
        g2.setPaint(new LinearGradientPaint(0, bar2Y, 0, padTop + chartH, new float[]{0f, 1f}, new Color[]{b2Top, b2Bot}));
        g2.fill(new RoundRectangle2D.Float(bar2X, bar2Y, barW, bar2H, 8, 8));

        float p1x = bar1X + barW / 2.0f;
        float p1y = bar1Y;
        float p2x = bar2X + barW / 2.0f;
        float p2y = bar2Y;
        float dx = p2x - p1x;

        Path2D.Float spline = new Path2D.Float();
        spline.moveTo(p1x, p1y);
        spline.curveTo(p1x + dx * 0.45f, p1y, p2x - dx * 0.45f, p2y, p2x, p2y);

        g2.setColor(new Color(245, 158, 11, 80));
        g2.setStroke(new BasicStroke(4.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.draw(spline);

        g2.setColor(new Color(245, 158, 11, 230));
        g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0, new float[]{4, 4}, 0));
        g2.draw(spline);

        g2.setColor(Color.WHITE);
        g2.fillOval((int) p1x - 3, (int) p1y - 3, 6, 6);
        g2.fillOval((int) p2x - 3, (int) p2y - 3, 6, 6);

        double savings = Math.max(0.0, baselineSpending - currentSimulatedSpending);
        if (savings > 0) {
            String deltaStr = "-" + String.format("%.0f%%", reductionPercent) + " (Rs " + String.format("%,.0f", savings) + " saved)";
            g2.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
            FontMetrics mfm = g2.getFontMetrics();
            int badgeW = mfm.stringWidth(deltaStr) + 12;
            int badgeH = 18;
            int badgeX = (int) ((p1x + p2x) / 2.0f - badgeW / 2.0f);
            int badgeY = (int) ((p1y + p2y) / 2.0f - badgeH / 2.0f - 10);

            g2.setColor(new Color(16, 185, 129, 210));
            g2.fillRoundRect(badgeX, badgeY, badgeW, badgeH, 6, 6);
            g2.setColor(Color.WHITE);
            g2.drawString(deltaStr, badgeX + 6, badgeY + 13);
        }

        g2.setFont(new Font("JetBrains Mono", Font.BOLD, 9));
        FontMetrics fm = g2.getFontMetrics();

        String b1Val = "Rs " + String.format("%,.0f", baselineSpending);
        int b1ValW = fm.stringWidth(b1Val);
        g2.setColor(AppTheme.TEXT_PRIMARY);
        g2.drawString(b1Val, bar1X + (barW - b1ValW) / 2, Math.max(12, bar1Y - 4));

        String b2Val = "Rs " + String.format("%,.0f", currentSimulatedSpending);
        int b2ValW = fm.stringWidth(b2Val);
        g2.setColor(AppTheme.PRIMARY_ACCENT);
        g2.drawString(b2Val, bar2X + (barW - b2ValW) / 2, Math.max(12, bar2Y - 4));

        g2.setFont(AppTheme.FONT_SMALL);
        FontMetrics lfm = g2.getFontMetrics();

        String l1 = "Baseline";
        int l1w = lfm.stringWidth(l1);
        g2.setColor(AppTheme.TEXT_MUTED);
        g2.drawString(l1, bar1X + (barW - l1w) / 2, padTop + chartH + 16);

        String l2 = "Simulated";
        int l2w = lfm.stringWidth(l2);
        g2.setColor(AppTheme.PRIMARY_ACCENT);
        g2.drawString(l2, bar2X + (barW - l2w) / 2, padTop + chartH + 16);

        g2.dispose();
    }
}
