package aimclassic.ui;

import aimclassic.AimClient;
import aimclassic.Models.Buddy;
import aimclassic.Models.Presence;
import aimclassic.Models.Status;

import javax.swing.ImageIcon;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class BuddyListFrame extends AimFrame {
    private final AimClient client;
    private final JTree tree = new JTree();
    private final JComboBox<Status> statusBox = new JComboBox<>(new Status[]{
            Status.ONLINE, Status.AWAY, Status.OCCUPIED, Status.IDLE, Status.INVISIBLE
    });
    private Consumer<String> openChat;
    private Consumer<String> openProfile;
    private Runnable openAway;
    private Runnable signOff;

    public BuddyListFrame(AimClient client) {
        super("Buddy List", 230, 520, true);
        this.client = client;
        finishBuild();
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
    }

    public void setHandlers(Consumer<String> openChat, Consumer<String> openProfile, Runnable openAway, Runnable signOff) {
        this.openChat = openChat;
        this.openProfile = openProfile;
        this.openAway = openAway;
        this.signOff = signOff;
    }

    public void refresh() {
        if (client.account() != null) {
            titleBar.setCaption("AIM - " + client.account().screenName);
        }
        rebuildTree();
        if (statusBox.getSelectedItem() != client.myStatus()) {
            statusBox.setSelectedItem(client.myStatus());
        }
    }

    @Override
    protected void build(JPanel body) {
        JPanel top = new JPanel(new BorderLayout(4, 0));
        top.setOpaque(false);
        JLabel logo = new JLabel("  AIM", new ImageIcon(AimIcons.runningMan(36)), JLabel.LEFT);
        logo.setFont(new Font("Arial Black", Font.BOLD, 18));
        logo.setForeground(AimTheme.NAVY);
        top.add(logo, BorderLayout.CENTER);
        body.add(top, BorderLayout.NORTH);

        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setFont(AimTheme.UI);
        tree.setBackground(java.awt.Color.WHITE);
        tree.setCellRenderer(new BuddyRenderer());
        tree.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String n = selectedBuddy();
                    if (n != null && openChat != null) {
                        openChat.accept(n);
                    }
                }
            }

            @Override
            public void mousePressed(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup(e);
                }
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                if (e.isPopupTrigger()) {
                    popup(e);
                }
            }
        });
        JScrollPane sp = new JScrollPane(tree);
        sp.setBorder(AimTheme.sunken());
        body.add(sp, BorderLayout.CENTER);

        JPanel south = new JPanel(new BorderLayout(4, 4));
        south.setOpaque(false);
        statusBox.setFont(AimTheme.UI);
        statusBox.addActionListener(e -> {
            Status s = (Status) statusBox.getSelectedItem();
            if (s != null && s != client.myStatus()) {
                if (s == Status.AWAY && openAway != null) {
                    openAway.run();
                }
                client.setStatus(s);
            }
        });
        south.add(statusBox, BorderLayout.CENTER);

        JPanel tools = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        tools.setOpaque(false);
        var add = aimButton("Add Buddy");
        add.addActionListener(e -> addBuddy());
        var me = aimButton("My Profile");
        me.addActionListener(e -> {
            if (openProfile != null && client.account() != null) {
                openProfile.accept(client.account().screenName);
            }
        });
        var im = aimButton("IM");
        im.addActionListener(e -> {
            String n = selectedBuddy();
            if (n != null && openChat != null) {
                openChat.accept(n);
            } else {
                String typed = JOptionPane.showInputDialog(this, "Send Instant Message to:", "New IM", JOptionPane.PLAIN_MESSAGE);
                if (typed != null && !typed.isBlank() && openChat != null) {
                    openChat.accept(typed.trim());
                }
            }
        });
        var off = aimButton("Sign Off");
        off.addActionListener(e -> {
            if (signOff != null) {
                signOff.run();
            }
        });
        tools.add(add);
        tools.add(me);
        tools.add(im);
        tools.add(off);
        south.add(tools, BorderLayout.SOUTH);
        body.add(south, BorderLayout.SOUTH);
    }

    private void popup(MouseEvent e) {
        TreePath path = tree.getPathForLocation(e.getX(), e.getY());
        if (path == null) {
            return;
        }
        tree.setSelectionPath(path);
        String n = selectedBuddy();
        if (n == null) {
            return;
        }
        JPopupMenu menu = new JPopupMenu();
        JMenuItem send = new JMenuItem("Send Instant Message");
        send.addActionListener(a -> {
            if (openChat != null) {
                openChat.accept(n);
            }
        });
        JMenuItem info = new JMenuItem("Get Info");
        info.addActionListener(a -> {
            if (openProfile != null) {
                openProfile.accept(n);
            }
        });
        JMenuItem del = new JMenuItem("Delete Buddy");
        del.addActionListener(a -> client.removeBuddy(n));
        menu.add(send);
        menu.add(info);
        menu.addSeparator();
        menu.add(del);
        menu.show(tree, e.getX(), e.getY());
    }

    private String selectedBuddy() {
        TreePath path = tree.getSelectionPath();
        if (path == null) {
            return null;
        }
        Object last = path.getLastPathComponent();
        if (last instanceof DefaultMutableTreeNode node && node.getUserObject() instanceof BuddyNode bn) {
            return bn.screenName;
        }
        return null;
    }

    private void addBuddy() {
        String name = JOptionPane.showInputDialog(this, "Enter a Screen Name to add:", "Add Buddy", JOptionPane.PLAIN_MESSAGE);
        if (name == null || name.isBlank()) {
            return;
        }
        String group = (String) JOptionPane.showInputDialog(this, "Buddy Group:", "Add Buddy",
                JOptionPane.PLAIN_MESSAGE, null, new String[]{"Buddies", "Family", "Co-Workers", "Recent Buddies"}, "Buddies");
        client.addBuddy(name.trim(), group == null ? "Buddies" : group);
    }

    private void rebuildTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("root");
        Map<String, DefaultMutableTreeNode> groups = new LinkedHashMap<>();
        if (client.account() != null) {
            List<Buddy> buddies = client.account().buddies.stream()
                    .sorted(Comparator.comparing((Buddy b) -> b.group, String.CASE_INSENSITIVE_ORDER)
                            .thenComparing(b -> b.screenName, String.CASE_INSENSITIVE_ORDER))
                    .toList();
            Map<String, int[]> counts = new LinkedHashMap<>();
            for (Buddy b : buddies) {
                String g = b.group == null || b.group.isBlank() ? "Buddies" : b.group;
                Presence p = client.presenceOf(b.screenName);
                counts.computeIfAbsent(g, k -> new int[2]);
                counts.get(g)[1]++;
                if (p.status.appearsOnline()) {
                    counts.get(g)[0]++;
                }
                DefaultMutableTreeNode gn = groups.computeIfAbsent(g, k -> {
                    DefaultMutableTreeNode n = new DefaultMutableTreeNode(k);
                    root.add(n);
                    return n;
                });
                gn.add(new DefaultMutableTreeNode(new BuddyNode(b.screenName, p)));
            }
            for (Map.Entry<String, DefaultMutableTreeNode> e : groups.entrySet()) {
                int[] c = counts.getOrDefault(e.getKey(), new int[]{0, 0});
                e.getValue().setUserObject(e.getKey() + " (" + c[0] + "/" + c[1] + ")");
            }
        }
        if (groups.isEmpty()) {
            DefaultMutableTreeNode empty = new DefaultMutableTreeNode("Buddies (0/0)");
            empty.add(new DefaultMutableTreeNode("(Add a Buddy to start)"));
            root.add(empty);
        }
        DefaultTreeModel model = new DefaultTreeModel(root);
        tree.setModel(model);
        for (int i = 0; i < tree.getRowCount(); i++) {
            tree.expandRow(i);
        }
    }

    record BuddyNode(String screenName, Presence presence) {
        @Override
        public String toString() {
            Status st = presence.status;
            if (st == Status.AWAY || st == Status.OCCUPIED) {
                return screenName + " (" + st.label + ")";
            }
            if (st == Status.IDLE) {
                return screenName + " (Idle)";
            }
            return screenName;
        }
    }

    static final class BuddyRenderer extends DefaultTreeCellRenderer {
        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean exp, boolean leaf, int row, boolean focus) {
            super.getTreeCellRendererComponent(tree, value, sel, exp, leaf, row, focus);
            setFont(AimTheme.UI);
            if (value instanceof DefaultMutableTreeNode n && n.getUserObject() instanceof BuddyNode bn) {
                setIcon(new ImageIcon(AimIcons.bullet(bn.presence.status, 12)));
                if (!bn.presence.status.appearsOnline()) {
                    setForeground(sel ? java.awt.Color.WHITE : AimTheme.OFFLINE);
                }
            } else {
                setIcon(null);
                setFont(AimTheme.UI_BOLD);
            }
            return this;
        }
    }
}
