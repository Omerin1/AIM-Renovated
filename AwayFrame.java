package aimclassic.ui;

import aimclassic.AimClient;
import aimclassic.Models.Status;

import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;

public final class AwayFrame extends AimFrame {
    private final AimClient client;
    private final JTextArea area = new JTextArea();

    public AwayFrame(AimClient client) {
        super("Away Message", 340, 240, true);
        this.client = client;
        finishBuild();
        area.setText(client.awayMessage());
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setLocationRelativeTo(null);
    }

    @Override
    protected void build(JPanel body) {
        javax.swing.JLabel l = new javax.swing.JLabel("I'm away because...");
        l.setFont(AimTheme.UI_BOLD);
        body.add(l, BorderLayout.NORTH);
        area.setFont(new Font("Arial", Font.PLAIN, 13));
        area.setLineWrap(true);
        area.setWrapStyleWord(true);
        area.setBorder(AimTheme.field());
        body.add(new JScrollPane(area), BorderLayout.CENTER);
        JPanel south = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        south.setOpaque(false);
        JButton ok = aimButton("I'm Away");
        ok.addActionListener(e -> {
            client.setAwayMessage(area.getText().trim());
            client.setStatus(Status.AWAY);
            dispose();
        });
        JButton cancel = aimButton("Cancel");
        cancel.addActionListener(e -> dispose());
        south.add(cancel);
        south.add(ok);
        body.add(south, BorderLayout.SOUTH);
    }
}
