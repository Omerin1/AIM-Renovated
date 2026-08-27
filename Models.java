package aimclassic;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Models {
    private Models() {}

    public enum Status {
        ONLINE("Online"),
        AWAY("Away"),
        OCCUPIED("Occupied"),
        IDLE("Idle"),
        INVISIBLE("Invisible"),
        OFFLINE("Offline");

        public final String label;

        Status(String label) {
            this.label = label;
        }

        @Override
        public String toString() {
            return label;
        }

        public static Status fromWire(String s) {
            if (s == null) {
                return OFFLINE;
            }
            try {
                return Status.valueOf(s);
            } catch (IllegalArgumentException e) {
                return OFFLINE;
            }
        }

        public boolean appearsOnline() {
            return this == ONLINE || this == AWAY || this == OCCUPIED || this == IDLE;
        }
    }

    public static final class Profile {
        public String screenName = "";
        public String displayName = "";
        public String location = "";
        public String interests = "";
        public String about = "";
        public String photoPath = "";
    }

    public static final class Buddy {
        public String screenName;
        public String group = "Buddies";

        public Buddy() {}

        public Buddy(String screenName, String group) {
            this.screenName = screenName;
            this.group = group;
        }
    }

    public static final class InstantMessage {
        public long time;
        public String from;
        public String to;
        public String text;

        public InstantMessage() {}

        public InstantMessage(long time, String from, String to, String text) {
            this.time = time;
            this.from = from;
            this.to = to;
            this.text = text;
        }
    }

    public static final class Presence {
        public String screenName;
        public Status status = Status.OFFLINE;
        public String awayMessage = "";
        public Profile profile = new Profile();
        public boolean typing;

        public Presence(String screenName) {
            this.screenName = screenName;
        }
    }

    public static final class Account {
        public String screenName;
        public String salt;
        public String passwordHash;
        public boolean remember;
        public List<Buddy> buddies = new ArrayList<>();
        public Profile profile = new Profile();
        public String awayMessage = "I am away from my computer right now.";
        public Status lastStatus = Status.ONLINE;

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Account a)) {
                return false;
            }
            return Objects.equals(screenName, a.screenName);
        }

        @Override
        public int hashCode() {
            return Objects.hashCode(screenName);
        }
    }
}
