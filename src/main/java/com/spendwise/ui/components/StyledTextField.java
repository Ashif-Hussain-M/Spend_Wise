package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JTextField;
import javax.swing.border.EmptyBorder;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;

public class StyledTextField extends JTextField implements FocusListener {

    private String placeholder;
    private boolean isFocused = false;

    public StyledTextField(int columns) {
        super(columns);
        init();
    }

    public StyledTextField(String placeholder, int columns) {
        super(columns);
        this.placeholder = placeholder;
        init();
    }

    private void init() {
        setFont(AppTheme.FONT_BODY);
        setBackground(AppTheme.INPUT_BG);
        setForeground(AppTheme.TEXT_PRIMARY);
        setCaretColor(AppTheme.TEXT_PRIMARY);
        setBorder(new EmptyBorder(8, 12, 8, 12));
        addFocusListener(this);
    }

    public void setPlaceholder(String placeholder) {
        this.placeholder = placeholder;
        repaint();
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        g2.setColor(getBackground());
        g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);

        g2.setColor(isFocused ? AppTheme.INPUT_FOCUS : AppTheme.INPUT_BORDER);
        g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);

        g2.dispose();
        super.paintComponent(g);

        if (placeholder != null && !placeholder.isEmpty() && getText().isEmpty() && !isFocused) {
            Graphics2D gPlaceholder = (Graphics2D) g.create();
            gPlaceholder.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            gPlaceholder.setColor(AppTheme.TEXT_MUTED);
            gPlaceholder.setFont(getFont());
            int y = (getHeight() - gPlaceholder.getFontMetrics().getHeight()) / 2 + gPlaceholder.getFontMetrics().getAscent();
            gPlaceholder.drawString(placeholder, 12, y);
            gPlaceholder.dispose();
        }
    }

    @Override
    public void focusGained(FocusEvent e) {
        isFocused = true;
        repaint();
    }

    @Override
    public void focusLost(FocusEvent e) {
        isFocused = false;
        repaint();
    }
}
