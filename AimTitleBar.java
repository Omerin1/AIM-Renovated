package aimclassic.ui;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowEvent;

public final class AimTitleBar extends JPanel {
    private Point drag;

    public AimTitleBar(JFrame frame, String title, boolean showMin) {
        setPreferredSize(new Dimension(10, 22));
        setLayout(new BorderLayout());
        JLabel label = new JLabel("  " + title);
        label.setForeground(Color.WHITE);
        label.setFont(new Font("Tahoma", Font.BOLD, 11));
        add(label, BorderLayout.CENTER);

        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 1, 2));
        btns.setOpaque(false);
        if (showMin) {
            JButton min = chrome("_");
            min.addActionListener(e -> frame.setState(JFrame.ICONIFIED));
            btns.add(min);
        }
        JButton close = chrome("X");
        close.addActionListener(e -> frame.dispatchEvent(new WindowEvent(frame, WindowEvent.WINDOW_CLOSING)));
        btns.add(close);
        add(btns, BorderLayout.EAST);

        MouseAdapter ma = new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                drag = e.getPoint();
            }

            @Override
            public void mouseDragged(MouseEvent e) {
                if (drag == null) {
                    return;
                }
                Point loc = frame.getLocation();
                frame.setLocation(loc.x + e.getX() - drag.x, loc.y + e.getY() - drag.y);
            }
        };
        addMouseListener(ma);
        addMouseMotionListener(ma);
        label.addMouseListener(ma);
        label.addMouseMotionListener(ma);
    }

    public void setCaption(String title) {
        ((JLabel) getComponent(0)).setText("  " + title);
    }

    private static JButton chrome(String t) {
        JButton b = new JButton(t);
        b.setFont(new Font("Tahoma", Font.BOLD, 10));
        b.setMargin(new java.awt.Insets(0, 4, 0, 4));
        b.setPreferredSize(new Dimension(16, 14));
        b.setFocusPainted(false);
        b.setBackground(AimTheme.GRAY);
        return b;
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setPaint(new GradientPaint(0, 0, AimTheme.TITLE, getWidth(), 0, AimTheme.TITLE_HI));
        g2.fillRect(0, 0, getWidth(), getHeight());
    }
}
