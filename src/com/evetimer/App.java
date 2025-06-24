package com.evetimer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.stream.Collectors;

public class App {
    private JFrame frame;
    private JPanel beltsPanel;
    private JComboBox<String> systemDropdown;
    private JComboBox<Integer> beltCountDropdown;
    private JButton addSystemBtn;
    private JButton removeSystemBtn;
    private JSlider opacitySlider;
    private Config config;
    private static final Path CONFIG_PATH = Paths.get("config.properties");

    // Map system name -> SystemData (belt count + kill counts)
    private Map<String, SystemData> systems = new LinkedHashMap<>();
    private String currentSystemName = null;

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().start());
    }

    private void start() {
        loadConfig();
        setupGUI();
        if (systems.isEmpty()) {
            addSystem("Default");
        }
        setCurrentSystem(systems.keySet().iterator().next());
    }

    private void setupGUI() {
        frame = new JFrame("Eve Rat Timer Tracker");
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setBackground(new Color(0,0,0,0));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0,0));
        root.setOpaque(false);
        frame.setContentPane(root);

        // Top panel with system selector, add/remove, belt count, opacity, drag handle
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        top.setOpaque(false);

        systemDropdown = new JComboBox<>();
        systemDropdown.addActionListener(e -> {
            String selected = (String) systemDropdown.getSelectedItem();
            if (selected != null && !selected.equals(currentSystemName)) {
                setCurrentSystem(selected);
            }
        });
        top.add(new JLabel("System:"));
        top.add(systemDropdown);

        addSystemBtn = new JButton("+");
        addSystemBtn.setToolTipText("Add new system");
        addSystemBtn.addActionListener(e -> {
            String name = JOptionPane.showInputDialog(frame, "Enter new system name:");
            if (name != null) {
                name = name.trim();
                if (name.isEmpty()) return;
                if (systems.containsKey(name)) {
                    JOptionPane.showMessageDialog(frame, "System already exists!");
                    return;
                }
                addSystem(name);
                setCurrentSystem(name);
            }
        });
        top.add(addSystemBtn);

        removeSystemBtn = new JButton("-");
        removeSystemBtn.setToolTipText("Remove selected system");
        removeSystemBtn.addActionListener(e -> {
            if (currentSystemName == null) return;
            int confirm = JOptionPane.showConfirmDialog(frame,
                "Remove system '" + currentSystemName + "'?",
                "Confirm Remove",
                JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                removeSystem(currentSystemName);
            }
        });
        top.add(removeSystemBtn);

        beltCountDropdown = new JComboBox<>();
        for (int i = 1; i <= 20; i++) beltCountDropdown.addItem(i);
        beltCountDropdown.setSelectedItem(config.beltCount);
        beltCountDropdown.addActionListener(e -> {
            if (currentSystemName == null) return;
            int count = (Integer) beltCountDropdown.getSelectedItem();
            SystemData sd = systems.get(currentSystemName);
            if (sd != null && sd.beltCount != count) {
                sd.beltCount = count;
                rebuildBelts();
                saveConfig();
            }
        });
        top.add(new JLabel("Belts:"));
        top.add(beltCountDropdown);

        opacitySlider = new JSlider(15, 100, (int) (config.opacity * 100));
        opacitySlider.setPreferredSize(new Dimension(100,16));
        opacitySlider.addChangeListener(e -> {
            config.opacity = opacitySlider.getValue() / 100f;
            frame.setOpacity(config.opacity);
            saveConfig();
        });
        top.add(new JLabel("Opacity:"));
        top.add(opacitySlider);

        // Add drag handle
        JPanel topDrag = createDragHandle();
        top.add(Box.createHorizontalGlue());
        top.add(topDrag);

        root.add(top, BorderLayout.NORTH);

        // Center belts panel
        beltsPanel = new JPanel();
        beltsPanel.setOpaque(false);
        beltsPanel.setLayout(new BoxLayout(beltsPanel, BoxLayout.Y_AXIS));
        JScrollPane scroll = new JScrollPane(beltsPanel,
                JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        scroll.setBorder(null);
        root.add(scroll, BorderLayout.CENTER);

        // Bottom panel with Close, drag handle, resize handle
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        bottom.setOpaque(false);

        JButton close = new JButton("Close");
        close.addActionListener(e -> System.exit(0));
        bottom.add(close);

        JPanel bottomDrag = createDragHandle();
        bottom.add(bottomDrag);

        JPanel resize = createResizeHandle();
        bottom.add(Box.createHorizontalGlue());
        bottom.add(resize);

        root.add(bottom, BorderLayout.SOUTH);

        frame.setSize(config.width, config.height);
        frame.setLocation(config.x, config.y);
        frame.setOpacity(config.opacity);
        frame.setVisible(true);
    }

    private void setCurrentSystem(String systemName) {
        if (systemName == null || !systems.containsKey(systemName)) return;

        currentSystemName = systemName;
        systemDropdown.setSelectedItem(systemName);

        SystemData sd = systems.get(systemName);

        beltCountDropdown.setSelectedItem(sd.beltCount);
        rebuildBelts();
    }

    private void addSystem(String name) {
        SystemData sd = new SystemData();
        sd.beltCount = config.beltCount;
        sd.killCounts = new int[sd.beltCount];
        systems.put(name, sd);
        systemDropdown.addItem(name);
        saveConfig();
    }

    private void removeSystem(String name) {
        if (!systems.containsKey(name)) return;
        systems.remove(name);
        systemDropdown.removeItem(name);
        if (!systems.isEmpty()) {
            setCurrentSystem(systems.keySet().iterator().next());
        } else {
            beltsPanel.removeAll();
            beltsPanel.revalidate();
            beltsPanel.repaint();
            currentSystemName = null;
        }
        saveConfig();
    }

    private void rebuildBelts() {
        beltsPanel.removeAll();
        if (currentSystemName == null) {
            beltsPanel.revalidate();
            beltsPanel.repaint();
            return;
        }
        SystemData sd = systems.get(currentSystemName);
        if (sd == null) return;

        int beltCount = sd.beltCount;
        if (sd.killCounts == null || sd.killCounts.length != beltCount) {
            sd.killCounts = new int[beltCount];
        }

        for (int i = 0; i < beltCount; i++) {
            final int index = i;
            BeltEntryPanel beltPanel = new BeltEntryPanel("Belt " + (i+1), sd.killCounts[i]);

            // Reset timer button
            beltPanel.setTimerResetAction(e -> {
                beltPanel.resetTimer();
            });

            // Kill count button increments count & saves
            beltPanel.setTimerResetAction(e -> {
                sd.killCounts[index]++;
                beltPanel.setKillCount(sd.killCounts[index]);
                saveConfig();
            });

            beltsPanel.add(beltPanel);
        }
        beltsPanel.revalidate();
        beltsPanel.repaint();
    }

    private JPanel createDragHandle() {
        JPanel dragHandle = new JPanel();
        dragHandle.setPreferredSize(new Dimension(60, 20));
        dragHandle.setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
        final Point offset = new Point();
        dragHandle.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                Point p = e.getLocationOnScreen();
                offset.x = p.x - frame.getX();
                offset.y = p.y - frame.getY();
            }
        });
        dragHandle.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                Point p = e.getLocationOnScreen();
                frame.setLocation(p.x - offset.x, p.y - offset.y);
                config.x = frame.getX();
                config.y = frame.getY();
                saveConfig();
            }
        });
        return dragHandle;
    }

    private JPanel createResizeHandle() {
        JPanel resize = new JPanel();
        resize.setPreferredSize(new Dimension(16,16));
        resize.setBackground(new Color(150,150,150));
        resize.setCursor(Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));
        final Point start = new Point();
        final Rectangle bounds = new Rectangle();
        resize.addMouseListener(new MouseAdapter() {
            public void mousePressed(MouseEvent e) {
                start.setLocation(e.getXOnScreen(), e.getYOnScreen());
                bounds.setBounds(frame.getBounds());
            }
        });
        resize.addMouseMotionListener(new MouseMotionAdapter() {
            public void mouseDragged(MouseEvent e) {
                int dx = e.getXOnScreen() - start.x;
                int dy = e.getYOnScreen() - start.y;
                frame.setBounds(bounds.x, bounds.y,
                        Math.max(200, bounds.width + dx),
                        Math.max(100, bounds.height + dy));
                config.width = frame.getWidth();
                config.height = frame.getHeight();
                saveConfig();
            }
        });
        return resize;
    }

    private void loadConfig() {
        config = new Config();
        Properties props = new Properties();
        if (Files.exists(CONFIG_PATH)) {
            try (InputStream in = Files.newInputStream(CONFIG_PATH)) {
                props.load(in);
                config.x = Integer.parseInt(props.getProperty("x", "" + config.x));
                config.y = Integer.parseInt(props.getProperty("y", "" + config.y));
                config.width = Integer.parseInt(props.getProperty("width", "" + config.width));
                config.height = Integer.parseInt(props.getProperty("height", "" + config.height));
                config.opacity = Float.parseFloat(props.getProperty("opacity", "" + config.opacity));
                config.beltCount = Integer.parseInt(props.getProperty("beltCount", "" + config.beltCount));

                // Load systems data: system names (comma separated)
                String systemNames = props.getProperty("systems");
                if (systemNames != null && !systemNames.isEmpty()) {
                    String[] names = systemNames.split(",");
                    for (String name : names) {
                        name = name.trim();
                        if (name.isEmpty()) continue;

                        SystemData sd = new SystemData();
                        sd.beltCount = Integer.parseInt(props.getProperty(name + ".beltCount", "" + config.beltCount));

                        String killCountsStr = props.getProperty(name + ".killCounts", "");
                        if (!killCountsStr.isEmpty()) {
                            String[] counts = killCountsStr.split(",");
                            sd.killCounts = new int[sd.beltCount];
                            for (int i = 0; i < Math.min(counts.length, sd.beltCount); i++) {
                                try {
                                    sd.killCounts[i] = Integer.parseInt(counts[i].trim());
                                } catch (NumberFormatException ignored) {}
                            }
                        } else {
                            sd.killCounts = new int[sd.beltCount];
                        }
                        systems.put(name, sd);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveConfig() {
        try {
            Properties p = new Properties();
            p.setProperty("x", "" + config.x);
            p.setProperty("y", "" + config.y);
            p.setProperty("width", "" + config.width);
            p.setProperty("height", "" + config.height);
            p.setProperty("opacity", "" + config.opacity);
            p.setProperty("beltCount", "" + config.beltCount);

            // Save system names
            String systemNames = systems.keySet().stream().collect(Collectors.joining(","));
            p.setProperty("systems", systemNames);

            // Save per system beltCount and kill counts
            for (Map.Entry<String, SystemData> e : systems.entrySet()) {
                String name = e.getKey();
                SystemData sd = e.getValue();
                p.setProperty(name + ".beltCount", String.valueOf(sd.beltCount));
                String killsStr = Arrays.stream(sd.killCounts).mapToObj(String::valueOf).collect(Collectors.joining(","));
                p.setProperty(name + ".killCounts", killsStr);
            }

            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                p.store(out, "EveRatTimerTracker Settings");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class SystemData {
        int beltCount = 5;
        int[] killCounts = new int[5];
    }

    private static class Config {
        int x = 100, y = 100, width = 400, height = 300;
        int beltCount = 5;
        float opacity = 0.85f;
    }
}
