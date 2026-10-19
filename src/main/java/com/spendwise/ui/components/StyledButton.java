package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JButton;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.RoundRectangle2D;

public class StyledButton extends JButton {

    public enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        DANGER,
        GHOST
    }

    private final ButtonStyle style;
    private boolean hovered = false;
    private boolean pressed = false;
    private float rippleProgress = 1.0f;
    private int clickX = 0;
    private int clickY = 0;
    private Timer rippleTimer;

    public StyledButton(String text, ButtonStyle style) {
        super(text);
        this.style = style;
        setFont(AppTheme.FONT_BODY_BOLD);
        setFocusPainted(false);
        setBorderPainted(false);
        setContentAreaFilled(false);
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setBorder(new EmptyBorder(8, 16, 8, 16));

        addMouseListener(new MouseAdapter() {
            @Override
            public void mouseEntered(MouseEvent e) {
                hovered = true;
                repaint();
            }

            @Override
            public void mouseExited(MouseEvent e) {
                hovered = false;
                pressed = false;
                repaint();
            }

            @Override
            public void mousePressed(MouseEvent e) {
                pressed = true;
                clickX = e.getX();
                clickY = e.getY();
                rippleProgress = 0.0f;
                if (rippleTimer != null && rippleTimer.isRunning()) {
                    rippleTimer.stop();
                }
                final int totalFrames = 15;
                final int[] frame = {0};
                rippleTimer = new Timer(14, ev -> {
                    frame[0]++;
                    rippleProgress = (float) frame[0] / totalFrames;
                    if (rippleProgress >= 1.0f) {
                        rippleProgress = 1.0f;
                        ((Timer) ev.getSource()).stop();
                    }
                    repaint();
                });
                rippleTimer.start();
                repaint();
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                pressed = false;
                repaint();
            }
        });
    }

    public StyledButton(String text) {
        this(text, ButtonStyle.PRIMARY);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int offsetY = pressed ? 2 : 0;
        g2.translate(0, offsetY);

        Color bg;
        Color fg;

        switch (style) {
            case PRIMARY -> {
                bg = hovered ? AppTheme.PRIMARY_ACCENT_HOVER : AppTheme.PRIMARY_ACCENT;
                fg = Color.WHITE;
            }
            case SECONDARY -> {
                bg = hovered ? AppTheme.CARD_BG_HOVER : AppTheme.CARD_BG;
                fg = AppTheme.TEXT_PRIMARY;
            }
            case DANGER -> {
                bg = hovered ? new Color(0xDC, 0x26, 0x26) : AppTheme.DANGER;
                fg = Color.WHITE;
            }
            case GHOST -> {
                bg = hovered ? new Color(255, 255, 255, 18) : new Color(0, 0, 0, 0);
                fg = hovered ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY;
            }
            default -> {
                bg = AppTheme.CARD_BG;
                fg = AppTheme.TEXT_PRIMARY;
            }
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight() - (pressed ? 2 : 0), 8, 8);

        if (rippleProgress < 1.0f) {
            Graphics2D rg = (Graphics2D) g2.create();
            rg.setClip(new RoundRectangle2D.Float(0, 0, getWidth(), getHeight() - (pressed ? 2 : 0), 8, 8));
            int maxDim = Math.max(getWidth(), getHeight());
            int radius = (int) (maxDim * 1.5f * rippleProgress);
            int alpha = (int) ((1.0f - rippleProgress) * 75);
            rg.setColor(new Color(255, 255, 255, Math.max(0, Math.min(255, alpha))));
            rg.fillOval(clickX - radius, clickY - radius, radius * 2, radius * 2);
            rg.dispose();
        }

        if (style == ButtonStyle.SECONDARY) {
            g2.setColor(AppTheme.CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1 - (pressed ? 2 : 0), 8, 8);
        }

        setForeground(fg);
        g2.dispose();

        Graphics textG = g.create();
        if (pressed) {
            textG.translate(0, 2);
        }
        super.paintComponent(textG);
        textG.dispose();
    }
}
