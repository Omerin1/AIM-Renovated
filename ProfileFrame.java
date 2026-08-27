package aimclassic.ui;

import aimclassic.AimClient;
import aimclassic.Models.Profile;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.nio.file.Path;

public final class ProfileFrame extends AimFrame {
    private final AimClient client;
    private final boolean mine;
    private String screenName;
    private final JLabel photo = new JLabel();
    private final JTextField display = new JTextField();
    private final JTextField location = new JTextField();
    private final JTextField interests = new JTextField();
    private final JTextArea about = new JTextArea(5, 20);
    private Path photoPath;

    public ProfileFrame(AimClient client, String screenName, boolean mine) {
        super((mine ? "My Profile - " : "Buddy Info - ") + screenName, 360, 460, true);
        this.client = client;
        this.mine = mine;
        this.screenName = screenName;
        finishBuild();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        apply(loadInitial());
        if (!mine) {
            client.requestProfile(screenName);
        }
    }

    public String screenName() {
        return screenName;
    }

    public void apply(Profile p) {
        if (p == null || (p.screenName != null && !p.screenName.isBlank() && !p.screenName.equalsIgnoreCase(screenName))) {
            if (p != null && p.screenName != null && !p.screenName.equalsIgnoreCase(screenName)) {
                return;
            }
        }
        if (p == null) {
            return;
        }
        display.setText(p.displayName);
        location.setText(p.location);
        interests.setText(p.interests);
        about.setText(p.about);
        if (p.photoPath != null && !p.photoPath.isBlank()) {
            photoPath = Path.of(p.photoPath);
            setPhoto(photoPath);
        }
    }

    private Profile loadInitial() {
        if (mine && client.account() != null) {
            return client.account().profile;
        }
        var acc = client.store().loadAccount(screenName);
        return acc == null ? new Profile() : acc.profile;
    }

    @Override
    protected void build(JPanel body) {
        photo.setPreferredSize(new Dimension(96, 96));
        photo.setBorder(AimTheme.sunken());
        photo.setHorizontalAlignment(JLabel.CENTER);
        photo.setText("No Photo");
        photo.setFont(AimTheme.UI);

        JPanel north = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 4));
        north.setOpaque(false);
        north.add(photo);
        JLabel name = new JLabel("<html><b>" + screenName + "</b><br>Member Name</html>");
        name.setFont(AimTheme.UI);
        north.add(name);
        body.add(north, BorderLayout.NORTH);

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints c = new GridBagConstraints();
        c.gridx = 0;
        c.gridy = 0;
        c.anchor = GridBagConstraints.WEST;
        c.insets = new Insets(3, 2, 3, 2);
        addRow(form, c, "Display Name", display);
        addRow(form, c, "Location", location);
        addRow(form, c, "Interests", interests);
        c.gridwidth = 2;
        JLabel aboutL = new JLabel("This is me:");
        aboutL.setFont(AimTheme.UI_BOLD);
        form.add(aboutL, c);
        c.gridy++;
        c.fill = GridBagConstraints.BOTH;
        c.weightx = 1;
        c.weighty = 1;
        about.setFont(AimTheme.CHAT);
        about.setLineWrap(true);
        about.setWrapStyleWord(true);
        about.setEnabled(mine);
        JScrollPane sp = new JScrollPane(about);
        sp.setBorder(AimTheme.field());
        form.add(sp, c);
        body.add(form, BorderLayout.CENTER);

        display.setEnabled(mine);
        location.setEnabled(mine);
        interests.setEnabled(mine);

        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        if (mine) {
            JButton browse = aimButton("Buddy Icon...");
            browse.addActionListener(e -> choosePhoto());
            south.add(browse);
            JButton save = aimButton("Save");
            save.addActionListener(e -> save());
            south.add(save);
        }
        JButton close = aimButton("Close");
        close.addActionListener(e -> dispose());
        south.add(close);
        body.add(south, BorderLayout.SOUTH);
    }

    private void addRow(JPanel form, GridBagConstraints c, String label, JTextField field) {
        JLabel l = new JLabel(label);
        l.setFont(AimTheme.UI_BOLD);
        c.gridwidth = 1;
        c.fill = GridBagConstraints.NONE;
        c.weightx = 0;
        form.add(l, c);
        c.gridy++;
        c.fill = GridBagConstraints.HORIZONTAL;
        c.weightx = 1;
        field.setFont(AimTheme.UI);
        field.setBorder(AimTheme.field());
        form.add(field, c);
        c.gridy++;
    }

    private void choosePhoto() {
        JFileChooser ch = new JFileChooser();
        ch.setFileFilter(new FileNameExtensionFilter("Pictures", "png", "jpg", "jpeg", "gif"));
        if (ch.showOpenDialog(this) == JFileChooser.APPROVE_OPTION) {
            try {
                photoPath = client.store().savePhoto(client.account().screenName, ch.getSelectedFile().toPath());
                setPhoto(photoPath);
            } catch (Exception ex) {
                javax.swing.JOptionPane.showMessageDialog(this, "Could not copy that picture.");
            }
        }
    }

    private void setPhoto(Path path) {
        try {
            ImageIcon icon = new ImageIcon(path.toString());
            Image scaled = icon.getImage().getScaledInstance(96, 96, Image.SCALE_SMOOTH);
            photo.setIcon(new ImageIcon(scaled));
            photo.setText("");
        } catch (Exception ignored) {
        }
    }

    private void save() {
        Profile p = client.account().profile;
        p.screenName = client.account().screenName;
        p.displayName = display.getText().trim();
        p.location = location.getText().trim();
        p.interests = interests.getText().trim();
        p.about = about.getText();
        if (photoPath != null) {
            p.photoPath = photoPath.toString();
        }
        client.publishProfile(p);
        dispose();
    }
}
