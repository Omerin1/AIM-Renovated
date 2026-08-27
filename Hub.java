package aimclassic;

import aimclassic.Models.InstantMessage;
import aimclassic.Models.Presence;
import aimclassic.Models.Profile;
import aimclassic.Models.Status;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/** Local AIM hub on port 5190. First instance listens; everyone (including host) connects as a client. */
public final class Hub implements Runnable {
    public static final int PORT = 5190;

    private final Store store;
    private final ServerSocket server;
    private final Map<String, ClientConn> online = new ConcurrentHashMap<>();
    private final Map<String, List<InstantMessage>> offline = new ConcurrentHashMap<>();

    public Hub(Store store, ServerSocket server) {
        this.store = store;
        this.server = server;
    }

    public static Hub tryStart(Store store) {
        try {
            ServerSocket ss = new ServerSocket(PORT, 50, InetAddress.getByName("127.0.0.1"));
            Hub hub = new Hub(store, ss);
            Thread t = new Thread(hub, "aim-hub");
            t.setDaemon(true);
            t.start();
            return hub;
        } catch (IOException e) {
            return null;
        }
    }

    @Override
    public void run() {
        while (!server.isClosed()) {
            try {
                Socket s = server.accept();
                ClientConn c = new ClientConn(s);
                Thread t = new Thread(c, "aim-client");
                t.setDaemon(true);
                t.start();
            } catch (IOException e) {
                break;
            }
        }
    }

    final class ClientConn implements Runnable {
        private final Socket socket;
        private BufferedWriter out;
        private String screenName;

        ClientConn(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try (socket;
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8))) {
                out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8));
                String line;
                while ((line = in.readLine()) != null) {
                    handle(Json.parse(line));
                }
            } catch (IOException ignored) {
            } finally {
                if (screenName != null) {
                    online.remove(screenName, this);
                    broadcast(Json.of("presence")
                            .put("screenName", screenName)
                            .put("status", Status.OFFLINE.name())
                            .put("away", ""));
                }
            }
        }

        private void handle(Json msg) throws IOException {
            switch (msg.type()) {
                case "auth" -> auth(msg);
                case "presence" -> presence(msg);
                case "im" -> im(msg);
                case "profile-get" -> profileGet(msg);
                case "profile" -> profilePush(msg);
                case "typing" -> typing(msg);
                default -> { }
            }
        }

        private void auth(Json msg) throws IOException {
            String name = msg.get("screenName").trim();
            String pass = msg.get("password");
            if (name.isEmpty() || name.length() > 32 || !name.matches("[A-Za-z0-9._ -]+")) {
                send(Json.of("auth-fail").put("reason", "Enter a screen name (letters, numbers, space, . _ -)."));
                return;
            }
            var existing = store.loadAccount(name);
            if (existing == null) {
                existing = store.createAccount(name, pass, "1".equals(msg.get("remember")));
            } else if (!store.checkPassword(existing, pass)) {
                send(Json.of("auth-fail").put("reason", "The password you entered is incorrect."));
                return;
            } else {
                existing.remember = "1".equals(msg.get("remember"));
                store.saveAccount(existing);
            }
            screenName = existing.screenName;
            online.put(screenName, this);
            send(Json.of("auth-ok").put("screenName", screenName));
            sendProfile(existing.profile, this);
            for (Presence p : snapshot()) {
                send(presenceJson(p));
            }
            List<InstantMessage> queued = offline.remove(screenName.toLowerCase());
            if (queued != null) {
                for (InstantMessage m : queued) {
                    send(imJson(m));
                }
            }
        }

        private void presence(Json msg) {
            if (screenName == null) {
                return;
            }
            Json outMsg = Json.of("presence")
                    .put("screenName", screenName)
                    .put("status", msg.get("status"))
                    .put("away", msg.get("away"));
            broadcast(outMsg);
        }

        private void im(Json msg) throws IOException {
            if (screenName == null) {
                return;
            }
            InstantMessage m = new InstantMessage(
                    System.currentTimeMillis(),
                    screenName,
                    msg.get("to"),
                    msg.get("text"));
            ClientConn dest = find(m.to);
            if (dest != null) {
                dest.send(imJson(m));
            } else {
                offline.computeIfAbsent(m.to.toLowerCase(), k -> new CopyOnWriteArrayList<>()).add(m);
            }
            send(imJson(m).put("echo", "1"));
        }

        private void profileGet(Json msg) throws IOException {
            String who = msg.get("screenName");
            var acc = store.loadAccount(who);
            if (acc != null) {
                sendProfile(acc.profile, this);
            }
        }

        private void profilePush(Json msg) {
            if (screenName == null) {
                return;
            }
            var acc = store.loadAccount(screenName);
            if (acc == null) {
                return;
            }
            acc.profile.displayName = msg.get("displayName");
            acc.profile.location = msg.get("location");
            acc.profile.interests = msg.get("interests");
            acc.profile.about = msg.get("about");
            acc.profile.photoPath = msg.get("photoPath");
            store.saveAccount(acc);
            broadcast(profileJson(acc.profile));
        }

        private void typing(Json msg) {
            if (screenName == null) {
                return;
            }
            ClientConn dest = find(msg.get("to"));
            if (dest != null) {
                dest.send(Json.of("typing").put("from", screenName).put("on", msg.get("on")));
            }
        }

        private void sendProfile(Profile p, ClientConn dest) {
            dest.send(profileJson(p));
        }

        synchronized void send(Json j) {
            try {
                out.write(j.toJson());
                out.write('\n');
                out.flush();
            } catch (IOException ignored) {
            }
        }
    }

    private ClientConn find(String name) {
        for (Map.Entry<String, ClientConn> e : online.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    private void broadcast(Json j) {
        for (ClientConn c : online.values()) {
            c.send(j);
        }
    }

    private List<Presence> snapshot() {
        List<Presence> list = new ArrayList<>();
        for (String name : online.keySet()) {
            Presence p = new Presence(name);
            p.status = Status.ONLINE;
            var acc = store.loadAccount(name);
            if (acc != null) {
                p.profile = acc.profile;
            }
            list.add(p);
        }
        return list;
    }

    private static Json imJson(InstantMessage m) {
        return Json.of("im")
                .put("from", m.from)
                .put("to", m.to)
                .put("text", m.text)
                .put("time", Long.toString(m.time));
    }

    private static Json presenceJson(Presence p) {
        return Json.of("presence")
                .put("screenName", p.screenName)
                .put("status", p.status.name())
                .put("away", p.awayMessage);
    }

    private static Json profileJson(Profile p) {
        return Json.of("profile")
                .put("screenName", p.screenName)
                .put("displayName", p.displayName)
                .put("location", p.location)
                .put("interests", p.interests)
                .put("about", p.about)
                .put("photoPath", p.photoPath);
    }
}
