package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JComboBox;
import javax.swing.border.EmptyBorder;
import java.awt.Cursor;
import java.awt.Dimension;

public class StyledComboBox<E> extends JComboBox<E> {

    public StyledComboBox() {
        super();
        init();
    }

    public StyledComboBox(E[] items) {
        super(items);
        init();
    }

    private void init() {
        setFont(AppTheme.FONT_BODY);
        setBackground(AppTheme.INPUT_BG);
        setForeground(AppTheme.TEXT_PRIMARY);
        setBorder(new EmptyBorder(4, 8, 4, 8));
        setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        setPreferredSize(new Dimension(getPreferredSize().width, 36));
    }
}
