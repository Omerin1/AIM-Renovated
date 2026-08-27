package aimclassic.ui;

import aimclassic.AimClient;
import aimclassic.Models.InstantMessage;
import aimclassic.Models.Presence;
import aimclassic.Models.Status;

import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.HyperlinkEvent;
import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.net.URI;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class ChatFrame extends AimFrame {
    private static final Pattern URL = Pattern.compile("(?i)(https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)|(www\\.[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)");
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("h:mm:ss a").withZone(ZoneId.systemDefault());

    public final String buddy;
    private final AimClient client;
    private final JEditorPane history = new JEditorPane();
    private final JTextArea input = new JTextArea(3, 20);
    private final JLabel statusLine = new JLabel(" ");
    private final StringBuilder html = new StringBuilder();
    private boolean typingSent;

    public ChatFrame(AimClient client, String buddy) {
        super("Instant Message with " + buddy, 420, 400, true);
        this.client = client;
        this.buddy = buddy;
        finishBuild();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        loadHistory();
        refreshPresence();
    }

    public void append(InstantMessage m) {
        boolean mine = client.account() != null && m.from.equalsIgnoreCase(client.account().screenName);
        String color = mine ? "#000080" : "#800000";
        String who = esc(m.from);
        String when = TIME.format(Instant.ofEpochMilli(m.time));
        html.append("<div style='margin:0 0 6px 0'>")
                .append("<b><font face='Arial' size='2' color='").append(color).append("'>")
                .append(who).append("</font></b>")
                .append("<font face='Arial' size='2' color='#666666'> (").append(when).append("):</font><br>")
                .append("<font face='Arial' size='3'>").append(linkify(esc(m.text))).append("</font></div>");
        render();
    }

    public void refreshPresence() {
        Presence p = client.presenceOf(buddy);
        String extra = "";
        if (p.status == Status.AWAY || p.status == Status.OCCUPIED) {
            extra = " — " + p.status.label;
            if (p.awayMessage != null && !p.awayMessage.isBlank()) {
                extra += ": " + p.awayMessage;
            }
        } else if (!p.status.appearsOnline()) {
            extra = " — Offline (they'll get it when they sign on)";
        } else if (p.status == Status.IDLE) {
            extra = " — Idle";
        }
        statusLine.setText(buddy + extra);
        titleBar.setCaption("Instant Message with " + buddy);
    }

    public void setTyping(boolean on) {
        if (on) {
            statusLine.setText(buddy + " is typing...");
        } else {
            refreshPresence();
        }
    }

    @Override
    protected void build(JPanel body) {
        statusLine.setFont(AimTheme.UI);
        body.add(statusLine, BorderLayout.NORTH);

        history.setContentType("text/html");
        history.setEditable(false);
        history.setBackground(java.awt.Color.WHITE);
        history.setBorder(AimTheme.field());
        history.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                try {
                    Desktop.getDesktop().browse(e.getURL() != null ? e.getURL().toURI() : URI.create(e.getDescription()));
                } catch (Exception ignored) {
                }
            }
        });
        JScrollPane histScroll = new JScrollPane(history);
        histScroll.setPreferredSize(new Dimension(10, 220));
        body.add(histScroll, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.setOpaque(false);
        input.setFont(AimTheme.CHAT);
        input.setLineWrap(true);
        input.setWrapStyleWord(true);
        input.setBorder(AimTheme.field());
        input.addKeyListener(new java.awt.event.KeyAdapter() {
            @Override
            public void keyPressed(java.awt.event.KeyEvent e) {
                if (e.getKeyCode() == java.awt.event.KeyEvent.VK_ENTER && !e.isShiftDown()) {
                    e.consume();
                    send();
                }
            }
        });
        input.getDocument().addDocumentListener(new DocumentListener() {
            public void insertUpdate(DocumentEvent e) { typing(true); }
            public void removeUpdate(DocumentEvent e) { typing(!input.getText().isBlank()); }
            public void changedUpdate(DocumentEvent e) { }
        });
        JScrollPane inScroll = new JScrollPane(input);
        south.add(inScroll, BorderLayout.CENTER);

        JPanel row = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        row.setOpaque(false);
        JLabel hint = new JLabel("Enter sends  ·  Shift+Enter new line  ·  links are clickable");
        hint.setFont(new Font("Tahoma", Font.PLAIN, 10));
        hint.setForeground(AimTheme.GRAY_DARK);
        JButton send = aimButton("Send");
        send.setPreferredSize(new Dimension(72, 28));
        send.addActionListener(e -> send());
        row.add(hint);
        row.add(send);
        south.add(row, BorderLayout.SOUTH);
        body.add(south, BorderLayout.SOUTH);
    }

    private void typing(boolean on) {
        if (on && !typingSent) {
            typingSent = true;
            client.sendTyping(buddy, true);
        } else if (!on && typingSent) {
            typingSent = false;
            client.sendTyping(buddy, false);
        }
    }

    private void send() {
        String text = input.getText().trim();
        if (text.isEmpty() || client.account() == null) {
            return;
        }
        client.sendIm(buddy, text);
        input.setText("");
        typing(false);
    }

    private void loadHistory() {
        if (client.account() == null) {
            return;
        }
        List<InstantMessage> list = client.store().loadHistory(client.account().screenName, buddy);
        html.setLength(0);
        for (InstantMessage m : list) {
            append(m);
        }
        if (list.isEmpty()) {
            render();
        }
    }

    private void render() {
        history.setText("<html><body style='margin:6px; background:#ffffff'>" + html + "</body></html>");
        SwingUtilities.invokeLater(() -> history.setCaretPosition(history.getDocument().getLength()));
    }

    private static String esc(String s) {
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
    }

    private static String linkify(String escText) {
        Matcher m = URL.matcher(escText);
        StringBuilder out = new StringBuilder();
        while (m.find()) {
            String raw = m.group();
            String href = raw.startsWith("http") ? raw : "http://" + raw;
            m.appendReplacement(out, Matcher.quoteReplacement("<a href=\"" + href + "\">" + raw + "</a>"));
        }
        m.appendTail(out);
        return out.toString().replace("\n", "<br>");
    }
}
