package com.evetimer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.*;
import java.nio.file.*;
import java.util.Properties;

public class App {
    private JFrame frame;
    private JPanel beltsPanel;
    private JComboBox<Integer> beltCountDropdown;
    private JSlider opacitySlider;
    private Config config;
    private static final Path CONFIG_PATH = Paths.get("config.properties");

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new App().start());
    }

    private void start() {
        loadConfig();
        setupGUI();
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

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        top.setOpaque(false);

        beltCountDropdown = new JComboBox<>();
        for (int i = 1; i <= 20; i++) beltCountDropdown.addItem(i);
        beltCountDropdown.setSelectedItem(config.beltCount);
        beltCountDropdown.addActionListener(e -> {
            config.beltCount = (Integer) beltCountDropdown.getSelectedItem();
            rebuildBelts(); saveConfig();
        });
        top.add(new JLabel("Belts:"));
        top.add(beltCountDropdown);

        opacitySlider = new JSlider(15, 100, (int)(config.opacity * 100));
        opacitySlider.setPreferredSize(new Dimension(100, 16));
        opacitySlider.addChangeListener(e -> {
            config.opacity = opacitySlider.getValue() / 100f;
            frame.setOpacity(config.opacity);
            saveConfig();
        });
        top.add(new JLabel("Opacity:"));
        top.add(opacitySlider);

        JPanel topDrag = createDragHandle();
        top.add(Box.createHorizontalGlue());
        top.add(topDrag);

        root.add(top, BorderLayout.NORTH);

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

        rebuildBelts();

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

    private void rebuildBelts() {
        beltsPanel.removeAll();
        for (int i = 0; i < config.beltCount; i++) {
            BeltEntryPanel p = new BeltEntryPanel(i);
            p.setMaximumSize(new Dimension(Integer.MAX_VALUE, 24));
            beltsPanel.add(p);
        }
        beltsPanel.revalidate();
        beltsPanel.repaint();
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
                config.beltCount = Integer.parseInt(props.getProperty("beltCount", "" + config.beltCount));
                config.opacity = Float.parseFloat(props.getProperty("opacity", "" + config.opacity));
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
            p.setProperty("beltCount", "" + config.beltCount);
            p.setProperty("opacity", "" + config.opacity);
            try (OutputStream out = Files.newOutputStream(CONFIG_PATH)) {
                p.store(out, null);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static class Config {
        int x = 100, y = 100, width = 400, height = 200;
        int beltCount = 5;
        float opacity = 0.85f;
    }
}