package aimclassic.ui;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Dimension;

public abstract class AimFrame extends JFrame {
    protected final AimTitleBar titleBar;
    protected final JPanel body;

    protected AimFrame(String title, int w, int h, boolean resizable) {
        super(title);
        setUndecorated(true);
        setResizable(resizable);
        setSize(w, h);
        setIconImage(AimIcons.runningMan(32));
        JPanel chrome = new JPanel(new BorderLayout());
        chrome.setBorder(BorderFactory.createLineBorder(AimTheme.TITLE.darker(), 2));
        chrome.setBackground(AimTheme.GRAY);
        titleBar = new AimTitleBar(this, title, true);
        chrome.add(titleBar, BorderLayout.NORTH);
        body = new JPanel(new BorderLayout());
        body.setBackground(AimTheme.GRAY);
        body.setBorder(BorderFactory.createEmptyBorder(6, 8, 8, 8));
        chrome.add(body, BorderLayout.CENTER);
        setContentPane(chrome);
        setMinimumSize(new Dimension(Math.min(200, w), Math.min(180, h)));
    }

    protected abstract void build(JPanel body);

    protected final void finishBuild() {
        build(body);
    }

    public static JButton aimButton(String text) {
        JButton b = new JButton(text);
        b.setFont(AimTheme.UI);
        b.setBackground(AimTheme.GRAY);
        b.setFocusPainted(false);
        b.setBorder(AimTheme.raised());
        return b;
    }
}
