package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLayeredPane;
import javax.swing.JPanel;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;

public class Toast extends JPanel {

    public enum ToastType {
        SUCCESS,
        ERROR,
        INFO
    }

    private final Timer dismissTimer;

    public Toast(String message, ToastType type) {
        setLayout(new BorderLayout());
        setOpaque(true);

        Color bg;
        Color border;
        switch (type) {
            case SUCCESS -> {
                bg = new Color(0x06, 0x4E, 0x3B);
                border = AppTheme.SUCCESS;
            }
            case ERROR -> {
                bg = new Color(0x7F, 0x1D, 0x1D);
                border = AppTheme.DANGER;
            }
            case INFO -> {
                bg = new Color(0x1E, 0x3A, 0x8A);
                border = AppTheme.BLUE_ACCENT;
            }
            default -> {
                bg = AppTheme.CARD_BG;
                border = AppTheme.CARD_BORDER;
            }
        }

        setBackground(bg);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(border, 1),
                new EmptyBorder(10, 16, 10, 16)
        ));

        JLabel label = new JLabel(message);
        label.setFont(AppTheme.FONT_BODY_BOLD);
        label.setForeground(AppTheme.TEXT_PRIMARY);
        add(label, BorderLayout.CENTER);

        dismissTimer = new Timer(3200, e -> {
            if (getParent() != null) {
                getParent().remove(this);
                getParent().repaint();
            }
        });
        dismissTimer.setRepeats(false);
    }

    public static void show(JFrame frame, String message, ToastType type) {
        if (frame == null) {
            return;
        }
        JLayeredPane layeredPane = frame.getLayeredPane();
        Toast toast = new Toast(message, type);
        Dimension toastSize = toast.getPreferredSize();

        int x = frame.getWidth() - toastSize.width - 32;
        int y = frame.getHeight() - toastSize.height - 48;
        toast.setBounds(Math.max(20, x), Math.max(20, y), toastSize.width, toastSize.height);

        layeredPane.add(toast, JLayeredPane.POPUP_LAYER);
        layeredPane.repaint();
        toast.dismissTimer.start();
    }
}
