package com.evetimer;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("serial")
public class BeltEntryPanel extends JPanel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final int beltIndex;
    private final JLabel timerLabel;
    private final JButton resetBtn;
    private final JButton killBtn;
    private final JLabel killCountLabel;
    private Instant startTime;
    private boolean selected = false;
    private int killCount = 0;

    public BeltEntryPanel(int beltIndex) {
        this.beltIndex = beltIndex;

        setOpaque(false);
        setLayout(new BorderLayout());

        timerLabel = new JLabel("00:00:00");
        timerLabel.setForeground(Color.RED);

        killCountLabel = new JLabel("Kills: 0");
        killCountLabel.setForeground(Color.WHITE);

        resetBtn = new JButton("Reset");
        resetBtn.setMargin(new Insets(2, 4, 2, 4));

        killBtn = new JButton("+1 Kill");
        killBtn.setMargin(new Insets(2, 4, 2, 4));

        JPanel leftPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 2));
        leftPanel.setOpaque(false);
        leftPanel.add(timerLabel);

        JPanel rightPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 2));
        rightPanel.setOpaque(false);
        rightPanel.add(killCountLabel);
        rightPanel.add(killBtn);
        rightPanel.add(resetBtn);

        add(leftPanel, BorderLayout.WEST);
        add(rightPanel, BorderLayout.EAST);

        startTime = Instant.now();
        new Timer(1000, e -> updateTimer()).start();

        resetBtn.addActionListener(e -> {
            startTime = Instant.now();
            updateTimer();
        });

        killBtn.addActionListener(e -> {
            killCount++;
            updateKillCount();
            saveKillCount();
        });

        loadKillCount();
        setSelected(false);
    }

    @Override
    protected void paintComponent(Graphics g) {
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setComposite(AlphaComposite.Clear);
        g2.fillRect(0, 0, getWidth(), getHeight());
        g2.dispose();
        super.paintComponent(g);
    }

    public void setSelected(boolean selected) {
        this.selected = selected;
        if (selected) {
            setBackground(new Color(0, 120, 215, 100));
        } else {
            setBackground(new Color(0, 0, 0, 0));
        }
        repaint();
    }

    private void updateTimer() {
        Duration elapsed = Duration.between(startTime, Instant.now());
        long h = elapsed.toHours();
        long m = elapsed.toMinutes() % 60;
        long s = elapsed.getSeconds() % 60;

        long totalMinutes = elapsed.toMinutes();
        if (totalMinutes >= 20) {
            timerLabel.setForeground(Color.GREEN);
        } else if (totalMinutes >= 15) {
            timerLabel.setForeground(Color.YELLOW);
        } else {
            timerLabel.setForeground(Color.RED);
        }

        timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        repaint();
    }

    private void updateKillCount() {
        killCountLabel.setText("Kills: " + killCount);
    }

    private void saveKillCount() {
        File file = new File("killcount_belt_" + beltIndex + ".dat");
        try (FileWriter fw = new FileWriter(file)) {
            fw.write(Integer.toString(killCount));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void loadKillCount() {
        File file = new File("killcount_belt_" + beltIndex + ".dat");
        if (file.exists()) {
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                killCount = Integer.parseInt(br.readLine());
                updateKillCount();
            } catch (IOException | NumberFormatException e) {
                e.printStackTrace();
            }
        }
    }
}