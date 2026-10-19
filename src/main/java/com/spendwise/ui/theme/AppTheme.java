package com.spendwise.ui.theme;

import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.UIManager;
import java.awt.Color;
import java.awt.Font;

public final class AppTheme {

    public static final Color BG_DARK = new Color(0x05, 0x07, 0x0A);
    public static final Color SIDEBAR_BG = new Color(0x0A, 0x0E, 0x12);
    public static final Color CARD_BG = new Color(0x10, 0x16, 0x1D);
    public static final Color CARD_RAISE = new Color(0x17, 0x1F, 0x28);
    public static final Color CARD_BG_HOVER = new Color(0x1A, 0x21, 0x2A);
    public static final Color CARD_BORDER = new Color(0x27, 0x30, 0x3B);

    public static final Color PRIMARY_ACCENT = new Color(0x3F, 0xB6, 0x8B);
    public static final Color PRIMARY_ACCENT_HOVER = new Color(0x34, 0xD3, 0x99);
    public static final Color SECONDARY_ACCENT = new Color(0xE0, 0x23, 0x1C);
    public static final Color EMBER = new Color(0xFF, 0x5A, 0x3C);
    public static final Color BLUE_ACCENT = new Color(0x3B, 0x82, 0xF6);
    public static final Color TEAL_ACCENT = new Color(0x10, 0xB9, 0x81);

    public static final Color SUCCESS = new Color(0x3F, 0xB6, 0x8B);
    public static final Color DANGER = new Color(0xE0, 0x23, 0x1C);
    public static final Color WARNING = new Color(0xF5, 0x9E, 0x0B);

    public static final Color TEXT_PRIMARY = new Color(0xDF, 0xE7, 0xE0);
    public static final Color TEXT_SECONDARY = new Color(0xAA, 0xB4, 0xAD);
    public static final Color TEXT_MUTED = new Color(0x78, 0x83, 0x7C);

    public static final Color INPUT_BG = new Color(0x0A, 0x0E, 0x12);
    public static final Color INPUT_BORDER = new Color(0x27, 0x30, 0x3B);
    public static final Color INPUT_FOCUS = new Color(0xE0, 0x23, 0x1C);

    public static final Color TABLE_HEADER_BG = new Color(0x0A, 0x0E, 0x12);
    public static final Color TABLE_ROW_ALT = new Color(0x0E, 0x13, 0x1A);
    public static final Color TABLE_SELECTION = new Color(0x1A, 0x21, 0x2A);

    public static final Font FONT_HERO = new Font("Onest", Font.BOLD, 38);
    public static final Font FONT_TITLE = new Font("Onest", Font.BOLD, 22);
    public static final Font FONT_HEADER = new Font("Onest", Font.BOLD, 16);
    public static final Font FONT_SUBHEADER = new Font("Onest", Font.BOLD, 13);
    public static final Font FONT_BODY = new Font("Onest", Font.PLAIN, 13);
    public static final Font FONT_BODY_BOLD = new Font("Onest", Font.BOLD, 13);
    public static final Font FONT_SMALL = new Font("JetBrains Mono", Font.PLAIN, 11);
    public static final Font FONT_MONO_BOLD = new Font("JetBrains Mono", Font.BOLD, 11);
    public static final Font FONT_KPI = new Font("JetBrains Mono", Font.BOLD, 26);

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
