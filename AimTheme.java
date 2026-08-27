package aimclassic.ui;

import aimclassic.Models.Status;

import javax.swing.BorderFactory;
import javax.swing.border.Border;
import java.awt.Color;
import java.awt.Font;
import java.awt.Insets;

public final class AimTheme {
    public static final Color GRAY = new Color(0xD4, 0xD0, 0xC8);
    public static final Color GRAY_DARK = new Color(0x80, 0x80, 0x80);
    public static final Color GRAY_LIGHT = Color.WHITE;
    public static final Color NAVY = new Color(0x00, 0x33, 0x99);
    public static final Color TITLE = new Color(0x0A, 0x24, 0x6A);
    public static final Color TITLE_HI = new Color(0x16, 0x58, 0xC8);
    public static final Color AIM_YELLOW = new Color(0xF5, 0xC4, 0x00);
    public static final Color AIM_ORANGE = new Color(0xFF, 0x99, 0x00);
    public static final Color LINK = new Color(0x00, 0x00, 0xCC);
    public static final Color SENT = new Color(0x00, 0x00, 0x80);
    public static final Color RECV = new Color(0x80, 0x00, 0x00);
    public static final Color ONLINE = new Color(0x00, 0xC0, 0x00);
    public static final Color AWAY = new Color(0xC0, 0x20, 0x20);
    public static final Color OFFLINE = new Color(0x80, 0x80, 0x80);
    public static final Color BUSY = new Color(0xC0, 0x80, 0x00);
    public static final Color IDLE = new Color(0x20, 0x80, 0xC0);

    public static final Font UI = new Font("Tahoma", Font.PLAIN, 11);
    public static final Font UI_BOLD = new Font("Tahoma", Font.BOLD, 11);
    public static final Font TITLE_FONT = new Font("Tahoma", Font.BOLD, 12);
    public static final Font CHAT = new Font("Arial", Font.PLAIN, 13);
    public static final Font LOGO = new Font("Arial Black", Font.BOLD, 22);

    private AimTheme() {}

    public static Border raised() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0x40, 0x40, 0x40)),
                BorderFactory.createRaisedBevelBorder());
    }

    public static Border sunken() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createEmptyBorder(2, 2, 2, 2),
                BorderFactory.createLoweredBevelBorder());
    }

    public static Border field() {
        return BorderFactory.createCompoundBorder(
                BorderFactory.createLoweredBevelBorder(),
                BorderFactory.createEmptyBorder(2, 4, 2, 4));
    }

    public static Color statusColor(Status s) {
        return switch (s) {
            case ONLINE -> ONLINE;
            case AWAY, OCCUPIED -> AWAY;
            case IDLE -> IDLE;
            case INVISIBLE, OFFLINE -> OFFLINE;
        };
    }

    public static Insets pad(int n) {
        return new Insets(n, n, n, n);
    }
}
