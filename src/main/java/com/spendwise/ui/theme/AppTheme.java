package com.spendwise.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public final class AppTheme {

    public static final Color BG_DARK = new Color(0x0B, 0x0F, 0x19);
    public static final Color SIDEBAR_BG = new Color(0x0E, 0x14, 0x24);
    public static final Color CARD_BG = new Color(0x15, 0x1D, 0x2E);
    public static final Color CARD_BG_HOVER = new Color(0x1B, 0x25, 0x3B);
    public static final Color CARD_BORDER = new Color(0x23, 0x30, 0x4A);

    public static final Color PRIMARY_ACCENT = new Color(0x10, 0xB9, 0x81);
    public static final Color PRIMARY_ACCENT_HOVER = new Color(0x05, 0x96, 0x69);
    public static final Color BLUE_ACCENT = new Color(0x3B, 0x82, 0xF6);
    public static final Color TEAL_ACCENT = new Color(0x06, 0xB6, 0xD4);

    public static final Color SUCCESS = new Color(0x10, 0xB9, 0x81);
    public static final Color DANGER = new Color(0xEF, 0x44, 0x44);
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);

    public static final Color TEXT_PRIMARY = new Color(0xF3, 0xF4, 0xF6);
    public static final Color TEXT_SECONDARY = new Color(0x9C, 0xA3, 0xAF);
    public static final Color TEXT_MUTED = new Color(0x6B, 0x72, 0x80);

    public static final Color INPUT_BG = new Color(0x11, 0x18, 0x27);
    public static final Color INPUT_BORDER = new Color(0x37, 0x41, 0x51);
    public static final Color INPUT_FOCUS = new Color(0x3B, 0x82, 0xF6);

    public static final Color TABLE_HEADER_BG = new Color(0x11, 0x18, 0x27);
    public static final Color TABLE_ROW_ALT = new Color(0x13, 0x1A, 0x2A);
    public static final Color TABLE_SELECTION = new Color(0x1E, 0x29, 0x3B);

    public static final Font FONT_TITLE = new Font("Helvetica Neue", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Helvetica Neue", Font.BOLD, 17);
    public static final Font FONT_SUBHEADER = new Font("Helvetica Neue", Font.BOLD, 14);
    public static final Font FONT_BODY = new Font("Helvetica Neue", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Helvetica Neue", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("Helvetica Neue", Font.PLAIN, 11);
    public static final Font FONT_KPI = new Font("Helvetica Neue", Font.BOLD, 26);

    private static boolean reduceAnimations = false;

    private AppTheme() {
    }

    public static void setup() {
        System.setProperty("flatlaf.useWindowDecorations", "true");
        System.setProperty("flatlaf.menuBarEmbedded", "true");
        System.setProperty("sun.java2d.uiScale.enabled", "true");

        FlatDarkLaf.setup();

        UIManager.put("Panel.background", BG_DARK);
        UIManager.put("Label.foreground", TEXT_PRIMARY);
        UIManager.put("Label.font", FONT_BODY);
        UIManager.put("Button.font", FONT_BODY_BOLD);
        UIManager.put("TextField.background", INPUT_BG);
        UIManager.put("TextField.foreground", TEXT_PRIMARY);
        UIManager.put("TextField.caretColor", TEXT_PRIMARY);
        UIManager.put("ComboBox.background", INPUT_BG);
        UIManager.put("ComboBox.foreground", TEXT_PRIMARY);
        UIManager.put("Table.background", CARD_BG);
        UIManager.put("Table.foreground", TEXT_PRIMARY);
        UIManager.put("Table.selectionBackground", TABLE_SELECTION);
        UIManager.put("Table.selectionForeground", TEXT_PRIMARY);
        UIManager.put("TableHeader.background", TABLE_HEADER_BG);
        UIManager.put("TableHeader.foreground", TEXT_SECONDARY);
        UIManager.put("ScrollBar.thumbArc", 8);
        UIManager.put("ScrollBar.thumbInsets", new java.awt.Insets(2, 2, 2, 2));
    }

    public static boolean isReduceAnimations() {
        return reduceAnimations;
    }

    public static void setReduceAnimations(boolean reduce) {
        reduceAnimations = reduce;
    }
}
