package aimclassic.ui;

import aimclassic.Models.Status;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;

public final class AimIcons {
    private AimIcons() {}

    public static BufferedImage runningMan(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        double s = size / 48.0;
        g.scale(s, s);
        g.setColor(AimTheme.NAVY);
        g.fillOval(4, 4, 40, 40);
        g.setColor(AimTheme.AIM_YELLOW);
        g.fillOval(7, 7, 34, 34);
        g.setColor(new Color(0x33, 0x22, 0x00));
        g.fillOval(18, 10, 12, 12);
        g.setStroke(new BasicStroke(3.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g.drawLine(24, 22, 24, 30);
        g.drawLine(24, 24, 14, 20);
        g.drawLine(24, 24, 34, 18);
        g.drawLine(24, 30, 16, 40);
        g.drawLine(24, 30, 34, 38);
        g.dispose();
        return img;
    }

    public static BufferedImage banner(int w, int h) {
        BufferedImage img = new BufferedImage(w, h, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setPaint(new java.awt.GradientPaint(0, 0, AimTheme.TITLE, w, h, AimTheme.TITLE_HI));
        g.fillRect(0, 0, w, h);
        g.dispose();
        return img;
    }

    public static BufferedImage bullet(Status status, int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        Color c = AimTheme.statusColor(status);
        if (!status.appearsOnline()) {
            g.setColor(new Color(0xA0, 0xA0, 0xA0));
            g.setStroke(new BasicStroke(1.4f));
            g.drawOval(2, 2, size - 5, size - 5);
        } else if (status == Status.AWAY || status == Status.OCCUPIED) {
            g.setColor(c);
            Polygon p = new Polygon();
            p.addPoint(size / 2, 1);
            p.addPoint(size - 2, size - 2);
            p.addPoint(1, size - 2);
            g.fillPolygon(p);
        } else {
            g.setColor(c);
            g.fillOval(1, 1, size - 3, size - 3);
            g.setColor(new Color(255, 255, 255, 140));
            g.fillOval(3, 3, size / 3, size / 3);
        }
        g.dispose();
        return img;
    }

    public static BufferedImage closeBtn(int size) {
        BufferedImage img = new BufferedImage(size, size, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(AimTheme.GRAY);
        g.fillRect(0, 0, size, size);
        g.setColor(Color.BLACK);
        g.setStroke(new BasicStroke(1.6f));
        int m = 4;
        g.drawLine(m, m, size - m - 1, size - m - 1);
        g.drawLine(size - m - 1, m, m, size - m - 1);
        g.dispose();
        return img;
    }
}
