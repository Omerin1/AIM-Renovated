package aimclassic;

import aimclassic.Models.Account;
import aimclassic.Models.Buddy;
import aimclassic.Models.InstantMessage;
import aimclassic.Models.Profile;
import aimclassic.Models.Status;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

public final class Store {
    private static final SecureRandom RANDOM = new SecureRandom();
    public final Path root;

    public Store() {
        this.root = Path.of(System.getProperty("user.home"), ".aim-classic");
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<String> rememberedNames() {
        List<String> names = new ArrayList<>();
        Path index = root.resolve("accounts.txt");
        if (!Files.isRegularFile(index)) {
            return names;
        }
        try {
            for (String line : Files.readAllLines(index, StandardCharsets.UTF_8)) {
                if (line.startsWith("R\t")) {
                    names.add(line.substring(2).trim());
                }
            }
        } catch (IOException ignored) {
        }
        return names;
    }

    public Account loadAccount(String screenName) {
        Path dir = userDir(screenName);
        Path file = dir.resolve("account.txt");
        Account a = new Account();
        a.screenName = screenName;
        a.profile.screenName = screenName;
        a.profile.displayName = screenName;
        if (!Files.isRegularFile(file)) {
            return null;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                int eq = line.indexOf('=');
                if (eq < 0) {
                    continue;
                }
                String k = line.substring(0, eq);
                String v = unescape(line.substring(eq + 1));
                switch (k) {
                    case "salt" -> a.salt = v;
                    case "hash" -> a.passwordHash = v;
                    case "remember" -> a.remember = "1".equals(v);
                    case "away" -> a.awayMessage = v;
                    case "status" -> a.lastStatus = Status.fromWire(v);
                    case "display" -> a.profile.displayName = v;
                    case "location" -> a.profile.location = v;
                    case "interests" -> a.profile.interests = v;
                    case "about" -> a.profile.about = v;
                    case "photo" -> a.profile.photoPath = v;
                }
            }
        } catch (IOException e) {
            return a;
        }
        a.buddies = loadBuddies(screenName);
        return a;
    }

    public Account createAccount(String screenName, String password, boolean remember) {
        Account a = new Account();
        a.screenName = screenName;
        a.remember = remember;
        a.salt = HexFormat.of().formatHex(randomBytes(8));
        a.passwordHash = hash(password, a.salt);
        a.profile.screenName = screenName;
        a.profile.displayName = screenName;
        saveAccount(a);
        return a;
    }

    public boolean checkPassword(Account a, String password) {
        if (a.passwordHash == null || a.passwordHash.isEmpty()) {
            return true;
        }
        return a.passwordHash.equals(hash(password, a.salt));
    }

    public void saveAccount(Account a) {
        try {
            Path dir = userDir(a.screenName);
            Files.createDirectories(dir);
            StringBuilder sb = new StringBuilder();
            line(sb, "salt", a.salt);
            line(sb, "hash", a.passwordHash);
            line(sb, "remember", a.remember ? "1" : "0");
            line(sb, "away", a.awayMessage);
            line(sb, "status", a.lastStatus.name());
            line(sb, "display", a.profile.displayName);
            line(sb, "location", a.profile.location);
            line(sb, "interests", a.profile.interests);
            line(sb, "about", a.profile.about);
            line(sb, "photo", a.profile.photoPath);
            Files.writeString(dir.resolve("account.txt"), sb.toString(), StandardCharsets.UTF_8);
            saveBuddies(a.screenName, a.buddies);
            rewriteIndex();
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<Buddy> loadBuddies(String screenName) {
        List<Buddy> list = new ArrayList<>();
        Path file = userDir(screenName).resolve("buddies.txt");
        if (!Files.isRegularFile(file)) {
            return list;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                if (line.isBlank()) {
                    continue;
                }
                String[] p = line.split("\t", 2);
                Buddy b = new Buddy();
                b.screenName = p[0];
                b.group = p.length > 1 && !p[1].isBlank() ? p[1] : "Buddies";
                list.add(b);
            }
        } catch (IOException ignored) {
        }
        return list;
    }

    public void saveBuddies(String screenName, List<Buddy> buddies) {
        try {
            Path dir = userDir(screenName);
            Files.createDirectories(dir);
            StringBuilder sb = new StringBuilder();
            for (Buddy b : buddies) {
                sb.append(b.screenName).append('\t').append(b.group == null ? "Buddies" : b.group).append('\n');
            }
            Files.writeString(dir.resolve("buddies.txt"), sb.toString(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

    public List<InstantMessage> loadHistory(String me, String buddy) {
        List<InstantMessage> list = new ArrayList<>();
        Path file = historyFile(me, buddy);
        if (!Files.isRegularFile(file)) {
            return list;
        }
        try {
            for (String line : Files.readAllLines(file, StandardCharsets.UTF_8)) {
                String[] p = line.split("\t", 4);
                if (p.length < 4) {
                    continue;
                }
                InstantMessage m = new InstantMessage();
                m.time = Long.parseLong(p[0]);
                m.from = p[1];
                m.to = p[2];
                m.text = unescape(p[3]);
                list.add(m);
            }
        } catch (Exception ignored) {
        }
        return list;
    }

    public void appendHistory(String me, InstantMessage m) {
        String other = me.equalsIgnoreCase(m.from) ? m.to : m.from;
        try {
            Path file = historyFile(me, other);
            Files.createDirectories(file.getParent());
            String line = m.time + "\t" + m.from + "\t" + m.to + "\t" + escape(m.text) + "\n";
            Files.writeString(file, line, StandardCharsets.UTF_8,
                    Files.exists(file)
                            ? java.nio.file.StandardOpenOption.APPEND
                            : java.nio.file.StandardOpenOption.CREATE);
        } catch (IOException ignored) {
        }
    }

    public Path savePhoto(String screenName, Path source) throws IOException {
        Path dir = userDir(screenName);
        Files.createDirectories(dir);
        String ext = ".png";
        String name = source.getFileName().toString().toLowerCase(Locale.ROOT);
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            ext = ".jpg";
        } else if (name.endsWith(".gif")) {
            ext = ".gif";
        }
        Path dest = dir.resolve("photo" + ext);
        Files.copy(source, dest, java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        return dest;
    }

    public Path userDir(String screenName) {
        return root.resolve(safe(screenName));
    }

    private Path historyFile(String me, String buddy) {
        return userDir(me).resolve("history").resolve(safe(buddy) + ".txt");
    }

    private void rewriteIndex() throws IOException {
        StringBuilder sb = new StringBuilder();
        try (var stream = Files.list(root)) {
            stream.filter(Files::isDirectory).forEach(dir -> {
                Account a = loadAccount(dir.getFileName().toString());
                if (a != null && a.remember) {
                    sb.append("R\t").append(a.screenName).append('\n');
                }
            });
        }
        Files.writeString(root.resolve("accounts.txt"), sb.toString(), StandardCharsets.UTF_8);
    }

    public static String hash(String password, String salt) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            md.update((salt + ":" + password).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(md.digest());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] randomBytes(int n) {
        byte[] b = new byte[n];
        RANDOM.nextBytes(b);
        return b;
    }

    private static String safe(String name) {
        return name.replaceAll("[^A-Za-z0-9._-]", "_");
    }

    private static void line(StringBuilder sb, String k, String v) {
        sb.append(k).append('=').append(escape(v == null ? "" : v)).append('\n');
    }

    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }

    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '\\' && i + 1 < s.length()) {
                char n = s.charAt(++i);
                switch (n) {
                    case 'n' -> out.append('\n');
                    case 't' -> out.append('\t');
                    default -> out.append(n);
                }
            } else {
                out.append(c);
            }
        }
        return out.toString();
    }

    public Profile copyProfile(Account a) {
        Profile p = new Profile();
        p.screenName = a.screenName;
        p.displayName = a.profile.displayName;
        p.location = a.profile.location;
        p.interests = a.profile.interests;
        p.about = a.profile.about;
        p.photoPath = a.profile.photoPath;
        return p;
    }
}
