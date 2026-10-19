package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JButton;
import javax.swing.border.EmptyBorder;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

public class StyledButton extends JButton {

    public enum ButtonStyle {
        PRIMARY,
        SECONDARY,
        DANGER,
        GHOST
    }

    private final ButtonStyle style;
    private boolean hovered = false;

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
                repaint();
            }
        });
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

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
                bg = hovered ? new Color(255, 255, 255, 15) : new Color(0, 0, 0, 0);
                fg = hovered ? AppTheme.TEXT_PRIMARY : AppTheme.TEXT_SECONDARY;
            }
            default -> {
                bg = AppTheme.CARD_BG;
                fg = AppTheme.TEXT_PRIMARY;
            }
        }

        g2.setColor(bg);
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        if (style == ButtonStyle.SECONDARY) {
            g2.setColor(AppTheme.CARD_BORDER);
            g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
        }

        setForeground(fg);
        g2.dispose();
        super.paintComponent(g);
    }
}
