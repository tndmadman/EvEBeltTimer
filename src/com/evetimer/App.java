package com.evetimer;

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;
import java.util.stream.Collectors;

public class App {
    private JFrame frame;
    private JPanel beltsPanel;
    private JComboBox<String> systemDropdown;
    private JButton addSystemBtn, removeSystemBtn;
    private JSlider opacitySlider;

    private Config config;
    private Map<String, SystemData> systems = new LinkedHashMap<>();
    private String currentSystemName = null;

    private ScheduledExecutorService timerExecutor;
    private ScheduledFuture<?> timerFuture;

    private static final Path CONFIG_PATH = Paths.get("config.properties");
    private static final Path DATA_PATH = Paths.get("systems_data.ser");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().start());
    }

    private void start() {
        loadConfig();
        loadSystems();
        setupGUI();
        startTimerUpdater();
    }

    private void setupGUI() {
        frame = new JFrame("Eve Rat Timer Tracker");
        frame.setUndecorated(true);
        frame.setAlwaysOnTop(true);
        frame.setBackground(new Color(0, 0, 0, 0));
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setOpaque(false);
        frame.setContentPane(root);

        // Top panel with system selector and controls
        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        top.setOpaque(false);

        systemDropdown = new JComboBox<>();
        systemDropdown.setPreferredSize(new Dimension(150, 22));
        systemDropdown.addActionListener(e -> {
            String selected = (String) systemDropdown.getSelectedItem();
            if (selected != null && !selected.equals(currentSystemName)) {
                saveCurrentSystemState();
                currentSystemName = selected;
                rebuildBelts();
                saveSystems();
                saveConfig();
            }
        });
        top.add(new JLabel("System:"));
        top.add(systemDropdown);

        addSystemBtn = new JButton("+");
        addSystemBtn.setToolTipText("Add System");
        addSystemBtn.addActionListener(e -> addSystem());
        top.add(addSystemBtn);

        removeSystemBtn = new JButton("–");
        removeSystemBtn.setToolTipText("Remove Current System");
        removeSystemBtn.addActionListener(e -> removeCurrentSystem());
        top.add(removeSystemBtn);

        // Opacity slider
        opacitySlider = new JSlider(15, 100, (int) (config.opacity * 100));
        opacitySlider.setPreferredSize(new Dimension(120, 16));
        opacitySlider.addChangeListener(e -> {
            config.opacity = opacitySlider.getValue() / 100f;
            frame.setOpacity(config.opacity);
            saveConfig();
        });
        top.add(Box.createHorizontalStrut(20));
        top.add(new JLabel("Opacity:"));
        top.add(opacitySlider);

        // Drag handle on right side
        JPanel topDrag = createDragHandle();
        top.add(Box.createHorizontalGlue());
        top.add(topDrag);

        root.add(top, BorderLayout.NORTH);

        // Center belts panel with scroll
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

        // Bottom panel with close button, drag handle, resize handle
        JPanel bottom = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 2));
        bottom.setOpaque(false);

        JButton close = new JButton("Close");
        close.addActionListener(e -> {
            saveCurrentSystemState();
            saveSystems();
            saveConfig();
            System.exit(0);
        });
        bottom.add(close);

        JPanel bottomDrag = createDragHandle();
        bottom.add(bottomDrag);

        bottom.add(Box.createHorizontalGlue());

        JPanel resize = createResizeHandle();
        bottom.add(resize);

        root.add(bottom, BorderLayout.SOUTH);

        // Populate system dropdown
        refreshSystemDropdown();

        // If no systems exist, add default one
        if (systems.isEmpty()) {
            addSystem("Default");
        } else if (!systems.containsKey(currentSystemName)) {
            currentSystemName = systems.keySet().iterator().next();
        }
        systemDropdown.setSelectedItem(currentSystemName);

        rebuildBelts();

        // Set window properties
        frame.setSize(config.width, config.height);
        frame.setLocation(config.x, config.y);
        frame.setOpacity(config.opacity);
        frame.setVisible(true);
    }

    private void rebuildBelts() {
        beltsPanel.removeAll();
        if (currentSystemName == null) {
            beltsPanel.revalidate();
            beltsPanel.repaint();
            return;
        }
        SystemData sys = systems.get(currentSystemName);
        if (sys == null) return;

        for (int i = 0; i < sys.beltCount; i++) {
            BeltData beltData = sys.belts.getOrDefault(i, new BeltData());
            BeltEntryPanel beltPanel = new BeltEntryPanel(i + 1, beltData);

            beltPanel.setTimerResetAction(() -> {
                beltData.timerStartMillis = System.currentTimeMillis();
                saveSystems();
            });
            beltPanel.setKillCountIncrementAction(() -> {
                beltData.killCount++;
                saveSystems();
                beltPanel.updateKillCount(beltData.killCount);
            });

            beltPanel.setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));
            beltsPanel.add(beltPanel);
        }

        beltsPanel.revalidate();
        beltsPanel.repaint();
    }

    private void refreshSystemDropdown() {
        systemDropdown.removeAllItems();
        for (String name : systems.keySet()) {
            systemDropdown.addItem(name);
        }
    }

    private void addSystem() {
        String newName = JOptionPane.showInputDialog(frame, "Enter new system name:", "Add System", JOptionPane.PLAIN_MESSAGE);
        if (newName != null) {
            newName = newName.trim();
            if (newName.isEmpty()) {
                JOptionPane.showMessageDialog(frame, "Name cannot be empty.");
                return;
            }
            if (systems.containsKey(newName)) {
                JOptionPane.showMessageDialog(frame, "System already exists.");
                return;
            }
            addSystem(newName);
        }
    }

    private void addSystem(String name) {
        SystemData newSys = new SystemData();
        newSys.beltCount = 5; // default belts
        // Initialize belts
        for (int i = 0; i < newSys.beltCount; i++) {
            newSys.belts.put(i, new BeltData());
        }
        systems.put(name, newSys);
        currentSystemName = name;
        refreshSystemDropdown();
        systemDropdown.setSelectedItem(name);
        rebuildBelts();
        saveSystems();
        saveConfig();
    }

    private void removeCurrentSystem() {
        if (currentSystemName == null) return;
        int confirm = JOptionPane.showConfirmDialog(frame,
                "Remove system '" + currentSystemName + "'?",
                "Confirm Remove", JOptionPane.YES_NO_OPTION);
        if (confirm == JOptionPane.YES_OPTION) {
            systems.remove(currentSystemName);
            if (systems.isEmpty()) {
                addSystem("Default");
            } else {
                currentSystemName = systems.keySet().iterator().next();
            }
            refreshSystemDropdown();
            systemDropdown.setSelectedItem(currentSystemName);
            rebuildBelts();
            saveSystems();
            saveConfig();
        }
    }

    private void saveCurrentSystemState() {
        // Currently we keep state live in 'systems', so no need to sync UI back
        // But if you add editable belt count, update it here
    }

    private void startTimerUpdater() {
        timerExecutor = Executors.newSingleThreadScheduledExecutor();
        timerFuture = timerExecutor.scheduleAtFixedRate(() -> {
            SwingUtilities.invokeLater(() -> updateTimers());
        }, 0, 1, TimeUnit.SECONDS);
    }

    private void updateTimers() {
        if (currentSystemName == null) return;
        SystemData sys = systems.get(currentSystemName);
        if (sys == null) return;

        Component[] comps = beltsPanel.getComponents();
        for (int i = 0; i < comps.length; i++) {
            if (!(comps[i] instanceof BeltEntryPanel)) continue;
            BeltEntryPanel p = (BeltEntryPanel) comps[i];
            BeltData bd = sys.belts.get(i);
            if (bd == null || bd.timerStartMillis == 0) {
                p.updateTimerLabel(0);
            } else {
                long elapsed = (System.currentTimeMillis() - bd.timerStartMillis) / 1000L;
                p.updateTimerLabel(elapsed);
                // Update color coding:
                if (elapsed < 15 * 60) {
                    p.setTimerLabelColor(Color.GREEN);
                } else if (elapsed < 20 * 60) {
                    p.setTimerLabelColor(Color.YELLOW.darker());
                } else {
                    p.setTimerLabelColor(Color.RED);
                    // Could beep here for 25+ min if you want
                }
            }
        }
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
                config.lastSystem = props.getProperty("lastSystem", null);
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
            if (currentSystemName != null) p.setProperty("lastSystem", currentSystemName);
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                p.store(out, "EveRatTimerTracker Settings");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadSystems() {
        if (Files.exists(DATA_PATH)) {
            try (ObjectInputStream ois = new ObjectInputStream(Files.newInputStream(DATA_PATH))) {
                Object obj = ois.readObject();
                if (obj instanceof Map) {
                    Map<?, ?> map = (Map<?, ?>) obj;
                    boolean valid = true;
                    for (Object key : map.keySet()) {
                        if (!(key instanceof String)) {
                            valid = false;
                            break;
                        }
                    }
                    if (valid) {
                        //noinspection unchecked
                        systems = (Map<String, SystemData>) map;
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        if (config.lastSystem != null && systems.containsKey(config.lastSystem)) {
            currentSystemName = config.lastSystem;
        }
    }

    private void saveSystems() {
        try (ObjectOutputStream oos = new ObjectOutputStream(Files.newOutputStream(DATA_PATH))) {
            oos.writeObject(systems);
        } catch (IOException e) {
            e.printStackTrace();
        }
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
        resize.setPreferredSize(new Dimension(16, 16));
        resize.setBackground(new Color(150, 150, 150));
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

    // --- Internal classes ---

    private static class Config {
        int x = 100, y = 100, width = 400, height = 300;
        float opacity = 0.85f;
        String lastSystem = null;
    }

    private static class SystemData implements Serializable {
        private static final long serialVersionUID = 1L;
        Map<Integer, BeltData> belts = new HashMap<>();
        int beltCount = 5;
    }

    private static class BeltData implements Serializable {
        private static final long serialVersionUID = 1L;
        long timerStartMillis = 0L; // 0 means not started
        int killCount = 0;
    }

    // --- BeltEntryPanel ---

    private static class BeltEntryPanel extends JPanel {
        private final JLabel beltLabel = new JLabel();
        private final JLabel timerLabel = new JLabel("00:00");
        private final JButton resetTimerBtn = new JButton("Reset Timer");
        private final JButton killCountBtn = new JButton("Kills: 0");

        private Runnable timerResetAction;
        private Runnable killCountIncrementAction;

        public BeltEntryPanel(int beltNumber, BeltData data) {
            setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
            setOpaque(false);

            beltLabel.setText("Belt " + beltNumber);
            beltLabel.setPreferredSize(new Dimension(60, 20));
            timerLabel.setPreferredSize(new Dimension(50, 20));

            killCountBtn.setPreferredSize(new Dimension(90, 24));
            resetTimerBtn.setPreferredSize(new Dimension(100, 24));

            add(beltLabel);
            add(timerLabel);
            add(resetTimerBtn);
            add(killCountBtn);

            updateKillCount(data.killCount);
            updateTimerLabel(data.timerStartMillis == 0 ? 0 : (int) ((System.currentTimeMillis() - data.timerStartMillis) / 1000L));

            resetTimerBtn.addActionListener(e -> {
                if (timerResetAction != null) timerResetAction.run();
            });

            killCountBtn.addActionListener(e -> {
                if (killCountIncrementAction != null) killCountIncrementAction.run();
            });
        }

        public void setTimerResetAction(Runnable action) {
            this.timerResetAction = action;
        }

        public void setKillCountIncrementAction(Runnable action) {
            this.killCountIncrementAction = action;
        }

        public void updateTimerLabel(long secondsElapsed) {
            long minutes = secondsElapsed / 60;
            long seconds = secondsElapsed % 60;
            timerLabel.setText(String.format("%02d:%02d", minutes, seconds));
        }

        public void updateKillCount(int kills) {
            killCountBtn.setText("Kills: " + kills);
        }

        public void setTimerLabelColor(Color c) {
            timerLabel.setForeground(c);
        }
    }
}
