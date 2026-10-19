package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;

public class ProgressIndicator extends JComponent {

    private double percentage;
    private Color trackColor = AppTheme.INPUT_BG;

    public ProgressIndicator(double percentage) {
        this.percentage = percentage;
        setPreferredSize(new Dimension(150, 10));
    }

    public void setPercentage(double percentage) {
        this.percentage = percentage;
        repaint();
    }

    public double getPercentage() {
        return percentage;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int width = getWidth();
        int height = getHeight();
        int arc = height;

        g2.setColor(trackColor);
        g2.fillRoundRect(0, 0, width, height, arc, arc);

        Color fillColor;
        if (percentage > 100.0) {
            fillColor = AppTheme.DANGER;
        } else if (percentage >= 80.0) {
            fillColor = AppTheme.WARNING;
        } else {
            fillColor = AppTheme.SUCCESS;
        }

        int fillWidth = (int) Math.min(width, Math.max(0, width * (percentage / 100.0)));
        if (fillWidth > 0) {
            g2.setColor(fillColor);
            g2.fillRoundRect(0, 0, fillWidth, height, arc, arc);
        }

        g2.dispose();
    }
}
