package aimclassic;

import aimclassic.ui.AimIcons;
import aimclassic.ui.AwayFrame;
import aimclassic.ui.BuddyListFrame;
import aimclassic.ui.ChatFrame;
import aimclassic.ui.LoginFrame;
import aimclassic.ui.ProfileFrame;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.AWTException;
import java.awt.SystemTray;
import java.awt.TrayIcon;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;
import java.util.Map;

public final class AimClassic {
    private final Store store = new Store();
    private final AimClient client = new AimClient(store);
    private LoginFrame login;
    private BuddyListFrame buddies;
    private final Map<String, ChatFrame> chats = new HashMap<>();
    private TrayIcon tray;

    public static void main(String[] args) {
        System.setProperty("apple.awt.application.name", "AIM Classic");
        SwingUtilities.invokeLater(() -> new AimClassic().start());
    }

    private void start() {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
        }
        UIManager.put("FileChooser.useSystemExtensionHiding", Boolean.TRUE);
        showLogin();
        client.addListener(new AimClient.Listener() {
            @Override
            public void onAuthOk(Models.Account account) {
                if (login != null) {
                    login.dispose();
                    login = null;
                }
                AimSound.signOn();
                showBuddyList();
                installTray(account.screenName);
            }

            @Override
            public void onAuthFail(String reason) {
                if (login != null) {
                    login.showError(reason);
                }
            }

            @Override
            public void onPresence() {
                if (buddies != null) {
                    buddies.refresh();
                }
                for (ChatFrame c : chats.values()) {
                    c.refreshPresence();
                }
            }

            @Override
            public void onMessage(Models.InstantMessage message, boolean echo) {
                String other;
                if (client.account() == null) {
                    return;
                }
                if (message.from.equalsIgnoreCase(client.account().screenName)) {
                    other = message.to;
                } else {
                    other = message.from;
                }
                ChatFrame frame = openChat(other, false);
                frame.append(message);
                if (!echo && !message.from.equalsIgnoreCase(client.account().screenName)) {
                    AimSound.imIn();
                    showNote("AIM Instant Message", message.from + ": " + clip(message.text));
                    if (!frame.isFocused()) {
                        frame.toFront();
                    }
                }
            }

            @Override
            public void onProfile(Models.Profile profile) {
                // open profile windows pick this up if visible
                for (java.awt.Window w : java.awt.Window.getWindows()) {
                    if (w instanceof ProfileFrame pf) {
                        pf.apply(profile);
                    }
                }
            }

            @Override
            public void onTyping(String from, boolean on) {
                ChatFrame f = chats.get(from.toLowerCase());
                if (f != null) {
                    f.setTyping(on);
                }
            }

            @Override
            public void onDisconnected() {
                JOptionPane.showMessageDialog(buddies, "You have been signed off.", "AIM", JOptionPane.INFORMATION_MESSAGE);
                doSignOff(false);
            }
        });
    }

    private void showLogin() {
        login = new LoginFrame(store);
        login.setOnSignOn((data, done) -> client.signOn(data.screenName(), data.password(), data.remember()));
        login.setVisible(true);
    }

    private void showBuddyList() {
        buddies = new BuddyListFrame(client);
        buddies.setHandlers(
                name -> openChat(name, true),
                this::openProfile,
                this::openAway,
                () -> doSignOff(true));
        buddies.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                doSignOff(true);
            }
        });
        buddies.setLocation(80, 80);
        buddies.refresh();
        buddies.setVisible(true);
    }

    private ChatFrame openChat(String buddy, boolean focus) {
        String key = buddy.toLowerCase();
        ChatFrame existing = chats.get(key);
        if (existing != null && existing.isDisplayable()) {
            if (focus) {
                existing.toFront();
                existing.requestFocus();
            }
            return existing;
        }
        ChatFrame frame = new ChatFrame(client, buddy);
        chats.put(key, frame);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosed(WindowEvent e) {
                chats.remove(key);
            }
        });
        if (buddies != null) {
            frame.setLocation(buddies.getX() + buddies.getWidth() + 12, buddies.getY() + chats.size() * 18);
        } else {
            frame.setLocationRelativeTo(null);
        }
        frame.setVisible(true);
        if (focus) {
            frame.toFront();
        }
        return frame;
    }

    private void openProfile(String name) {
        boolean mine = client.account() != null && name.equalsIgnoreCase(client.account().screenName);
        ProfileFrame pf = new ProfileFrame(client, name, mine);
        pf.setLocationRelativeTo(buddies);
        pf.setVisible(true);
    }

    private void openAway() {
        AwayFrame af = new AwayFrame(client);
        af.setVisible(true);
    }

    private void doSignOff(boolean send) {
        for (ChatFrame c : chats.values().toArray(ChatFrame[]::new)) {
            c.dispose();
        }
        chats.clear();
        if (buddies != null) {
            buddies.dispose();
            buddies = null;
        }
        removeTray();
        if (send) {
            client.signOff();
        }
        if (login == null || !login.isDisplayable()) {
            showLogin();
        }
    }

    private void installTray(String name) {
        if (!SystemTray.isSupported()) {
            return;
        }
        removeTray();
        try {
            tray = new TrayIcon(AimIcons.runningMan(16), "AIM Classic - " + name);
            tray.setImageAutoSize(true);
            tray.addActionListener(e -> {
                if (buddies != null) {
                    buddies.setState(java.awt.Frame.NORMAL);
                    buddies.toFront();
                }
            });
            SystemTray.getSystemTray().add(tray);
        } catch (AWTException ignored) {
            tray = null;
        }
    }

    private void removeTray() {
        if (tray != null) {
            SystemTray.getSystemTray().remove(tray);
            tray = null;
        }
    }

    private void showNote(String title, String body) {
        if (tray != null) {
            tray.displayMessage(title, body, TrayIcon.MessageType.INFO);
        }
    }

    private static String clip(String s) {
        s = s.replace('\n', ' ');
        return s.length() > 80 ? s.substring(0, 77) + "..." : s;
    }
}
