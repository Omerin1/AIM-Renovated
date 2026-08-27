package aimclassic.ui;

import aimclassic.Store;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.function.BiConsumer;

public final class LoginFrame extends AimFrame {
    private final JComboBox<String> nameBox = new JComboBox<>();
    private final JPasswordField pass = new JPasswordField();
    private final JCheckBox save = new JCheckBox("Save password / remember me");
    private final JButton signOn = aimButton("Sign On");
    private BiConsumer<LoginData, Runnable> onSignOn;

    public record LoginData(String screenName, String password, boolean remember) {}

    public LoginFrame(Store store) {
        super("Sign On - AIM Classic", 320, 390, false);
        nameBox.setEditable(true);
        nameBox.setFont(AimTheme.UI);
        for (String n : store.rememberedNames()) {
            nameBox.addItem(n);
        }
        finishBuild();
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void setOnSignOn(BiConsumer<LoginData, Runnable> onSignOn) {
        this.onSignOn = onSignOn;
    }

    public void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "AIM", JOptionPane.WARNING_MESSAGE);
        setSigning(false);
    }

    private void setSigning(boolean busy) {
        signOn.setEnabled(!busy);
        signOn.setText(busy ? "Signing On..." : "Sign On");
    }

    @Override
    protected void build(JPanel body) {
        JPanel banner = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g;
                g2.setPaint(new java.awt.GradientPaint(0, 0, AimTheme.TITLE, getWidth(), getHeight(), new Color(0x1E, 0x90, 0xFF)));
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.drawImage(AimIcons.runningMan(56), 12, 10, null);
                g2.setColor(AimTheme.AIM_YELLOW);
                g2.setFont(new Font("Arial Black", Font.BOLD, 28));
                g2.drawString("AIM", 78, 48);
                g2.setFont(new Font("Tahoma", Font.PLAIN, 11));
                g2.setColor(Color.WHITE);
                g2.drawString("Instant Messenger", 80, 66);
            }
        };
        banner.setPreferredSize(new Dimension(10, 84));
        body.add(banner, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.insets = new Insets(4, 4, 2, 4);
        c.weightx = 1;

        JLabel hint = new JLabel("Screen Name");
        hint.setFont(AimTheme.UI_BOLD);
        form.add(hint, c);
        nameBox.setBorder(AimTheme.field());
        form.add(nameBox, c);

        JLabel p = new JLabel("Password");
        p.setFont(AimTheme.UI_BOLD);
        form.add(p, c);
        pass.setFont(AimTheme.UI);
        pass.setBorder(AimTheme.field());
        pass.setEchoChar('*');
        form.add(pass, c);

        save.setFont(AimTheme.UI);
        save.setOpaque(false);
        save.setSelected(true);
        form.add(save, c);

        JLabel note = new JLabel("<html><i>New screen name? It will be created on first Sign On.</i></html>");
        note.setFont(new Font("Tahoma", Font.PLAIN, 10));
        form.add(note, c);

        body.add(form, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout());
        south.setOpaque(false);
        JPanel btns = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        btns.setOpaque(false);
        JButton setup = aimButton("Setup");
        setup.addActionListener(e -> JOptionPane.showMessageDialog(this,
                "AIM Classic talks on this computer through port 5190.\nOpen a second window to IM another screen name.",
                "Setup", JOptionPane.INFORMATION_MESSAGE));
        signOn.addActionListener(e -> doSignOn());
        pass.addActionListener(e -> doSignOn());
        getRootPane().setDefaultButton(signOn);
        btns.add(setup);
        btns.add(signOn);
        south.add(btns, BorderLayout.EAST);
        JLabel ver = new JLabel("AIM Classic 5.0");
        ver.setFont(new Font("Tahoma", Font.PLAIN, 10));
        ver.setForeground(AimTheme.GRAY_DARK);
        south.add(ver, BorderLayout.WEST);
        body.add(south, BorderLayout.SOUTH);
    }

    private void doSignOn() {
        Object item = nameBox.getEditor().getItem();
        String name = item == null ? "" : item.toString().trim();
        if (name.isEmpty()) {
            showError("Please enter a Screen Name.");
            return;
        }
        setSigning(true);
        if (onSignOn != null) {
            onSignOn.accept(new LoginData(name, new String(pass.getPassword()), save.isSelected()),
                    () -> setSigning(false));
        }
    }
}
