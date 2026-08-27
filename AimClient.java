package aimclassic;

import aimclassic.Models.Account;
import aimclassic.Models.Buddy;
import aimclassic.Models.InstantMessage;
import aimclassic.Models.Presence;
import aimclassic.Models.Profile;
import aimclassic.Models.Status;

import javax.swing.SwingUtilities;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public final class AimClient {
    public interface Listener {
        void onAuthOk(Account account);
        void onAuthFail(String reason);
        void onPresence();
        void onMessage(InstantMessage message, boolean echo);
        void onProfile(Profile profile);
        void onTyping(String from, boolean on);
        void onDisconnected();
    }

    private final Store store;
    private final List<Listener> listeners = new CopyOnWriteArrayList<>();
    private final Map<String, Presence> people = new LinkedHashMap<>();
    private final Map<String, Boolean> awayReplied = new LinkedHashMap<>();

    private Socket socket;
    private BufferedWriter out;
    private Account account;
    private Status myStatus = Status.ONLINE;
    private String awayMessage = "I am away from my computer right now.";
    private volatile boolean running;

    public AimClient(Store store) {
        this.store = store;
    }

    public Store store() {
        return store;
    }

    public Account account() {
        return account;
    }

    public Status myStatus() {
        return myStatus;
    }

    public String awayMessage() {
        return awayMessage;
    }

    public void addListener(Listener l) {
        listeners.add(l);
    }

    public void removeListener(Listener l) {
        listeners.remove(l);
    }

    public List<Presence> people() {
        return new ArrayList<>(people.values());
    }

    public Presence presenceOf(String screenName) {
        for (Presence p : people.values()) {
            if (p.screenName.equalsIgnoreCase(screenName)) {
                return p;
            }
        }
        Presence offline = new Presence(screenName);
        offline.status = Status.OFFLINE;
        return offline;
    }

    public void signOn(String screenName, String password, boolean remember) {
        Thread t = new Thread(() -> connectLoop(screenName, password, remember), "aim-net");
        t.setDaemon(true);
        t.start();
    }

    private void connectLoop(String screenName, String password, boolean remember) {
        try {
            Hub.tryStart(store);
            Thread.sleep(120);
            socket = new Socket(InetAddress.getByName("127.0.0.1"), Hub.PORT);
            out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
            running = true;
            send(Json.of("auth")
                    .put("screenName", screenName.trim())
                    .put("password", password)
                    .put("remember", remember ? "1" : "0"));
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
            String line;
            while (running && (line = in.readLine()) != null) {
                Json msg = Json.parse(line);
                SwingUtilities.invokeLater(() -> dispatch(msg));
            }
        } catch (Exception e) {
            SwingUtilities.invokeLater(() -> {
                if (account == null) {
                    fireFail("Could not reach the AIM hub on this computer (port 5190).");
                } else {
                    for (Listener l : listeners) {
                        l.onDisconnected();
                    }
                }
            });
        }
    }

    private void dispatch(Json msg) {
        switch (msg.type()) {
            case "auth-ok" -> {
                account = store.loadAccount(msg.get("screenName"));
                if (account == null) {
                    fireFail("Signed on, but the local profile is missing.");
                    return;
                }
                awayMessage = account.awayMessage;
                myStatus = Status.ONLINE;
                people.clear();
                for (Listener l : listeners) {
                    l.onAuthOk(account);
                }
                sendPresence();
            }
            case "auth-fail" -> fireFail(msg.get("reason"));
            case "presence" -> {
                String name = msg.get("screenName");
                if (account != null && name.equalsIgnoreCase(account.screenName)) {
                    return;
                }
                Presence p = people.computeIfAbsent(name.toLowerCase(Locale.ROOT), k -> new Presence(name));
                p.screenName = name;
                p.status = Status.fromWire(msg.get("status"));
                p.awayMessage = msg.get("away");
                if (p.status == Status.OFFLINE) {
                    people.remove(name.toLowerCase(Locale.ROOT));
                }
                for (Listener l : listeners) {
                    l.onPresence();
                }
            }
            case "im" -> {
                InstantMessage m = new InstantMessage();
                try {
                    m.time = Long.parseLong(msg.get("time"));
                } catch (NumberFormatException e) {
                    m.time = System.currentTimeMillis();
                }
                m.from = msg.get("from");
                m.to = msg.get("to");
                m.text = msg.get("text");
                boolean echo = "1".equals(msg.get("echo"));
                if (account != null) {
                    store.appendHistory(account.screenName, m);
                }
                if (!echo && isAwayAutoReply() && account != null
                        && !m.from.equalsIgnoreCase(account.screenName)) {
                    String key = m.from.toLowerCase(Locale.ROOT);
                    if (!Boolean.TRUE.equals(awayReplied.get(key))) {
                        awayReplied.put(key, true);
                        sendIm(m.from, "Auto response: " + awayMessage);
                    }
                }
                for (Listener l : listeners) {
                    l.onMessage(m, echo);
                }
            }
            case "profile" -> {
                Profile p = new Profile();
                p.screenName = msg.get("screenName");
                p.displayName = msg.get("displayName");
                p.location = msg.get("location");
                p.interests = msg.get("interests");
                p.about = msg.get("about");
                p.photoPath = msg.get("photoPath");
                Presence pr = people.get(p.screenName.toLowerCase(Locale.ROOT));
                if (pr != null) {
                    pr.profile = p;
                }
                for (Listener l : listeners) {
                    l.onProfile(p);
                }
            }
            case "typing" -> {
                for (Listener l : listeners) {
                    l.onTyping(msg.get("from"), "1".equals(msg.get("on")));
                }
            }
            default -> { }
        }
    }

    private boolean isAwayAutoReply() {
        return myStatus == Status.AWAY || myStatus == Status.OCCUPIED;
    }

    private void fireFail(String reason) {
        for (Listener l : listeners) {
            l.onAuthFail(reason);
        }
    }

    public synchronized void sendIm(String to, String text) {
        send(Json.of("im").put("to", to).put("text", text));
    }

    public void setStatus(Status status) {
        this.myStatus = status;
        if (status != Status.AWAY && status != Status.OCCUPIED) {
            awayReplied.clear();
        }
        if (account != null) {
            account.lastStatus = status;
            store.saveAccount(account);
        }
        sendPresence();
    }

    public void setAwayMessage(String msg) {
        this.awayMessage = msg;
        if (account != null) {
            account.awayMessage = msg;
            store.saveAccount(account);
        }
        sendPresence();
    }

    public void sendTyping(String to, boolean on) {
        send(Json.of("typing").put("to", to).put("on", on ? "1" : "0"));
    }

    public void requestProfile(String screenName) {
        send(Json.of("profile-get").put("screenName", screenName));
    }

    public void publishProfile(Profile p) {
        if (account == null) {
            return;
        }
        account.profile = p;
        store.saveAccount(account);
        send(Json.of("profile")
                .put("displayName", p.displayName)
                .put("location", p.location)
                .put("interests", p.interests)
                .put("about", p.about)
                .put("photoPath", p.photoPath));
    }

    public void addBuddy(String screenName, String group) {
        if (account == null || screenName.isBlank()) {
            return;
        }
        for (Buddy b : account.buddies) {
            if (b.screenName.equalsIgnoreCase(screenName)) {
                return;
            }
        }
        account.buddies.add(new Buddy(screenName.trim(), group == null || group.isBlank() ? "Buddies" : group));
        store.saveAccount(account);
        for (Listener l : listeners) {
            l.onPresence();
        }
    }

    public void removeBuddy(String screenName) {
        if (account == null) {
            return;
        }
        account.buddies.removeIf(b -> b.screenName.equalsIgnoreCase(screenName));
        store.saveAccount(account);
        for (Listener l : listeners) {
            l.onPresence();
        }
    }

    public void sendPresence() {
        Status wire = myStatus == Status.INVISIBLE ? Status.OFFLINE : myStatus;
        send(Json.of("presence")
                .put("status", wire.name())
                .put("away", (myStatus == Status.AWAY || myStatus == Status.OCCUPIED) ? awayMessage : ""));
    }

    public synchronized void signOff() {
        running = false;
        try {
            if (socket != null) {
                socket.close();
            }
        } catch (IOException ignored) {
        }
        account = null;
        people.clear();
    }

    private synchronized void send(Json j) {
        if (out == null) {
            return;
        }
        try {
            out.write(j.toJson());
            out.write('\n');
            out.flush();
        } catch (IOException ignored) {
        }
    }
}
