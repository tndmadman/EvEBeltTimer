package com.evetimer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class BeltEntryPanel extends JPanel {
    private final JLabel beltLabel;
    private final JLabel timerLabel;
    private final JButton resetTimerBtn;
    private final JButton killCountBtn;

    private int killCount;
    private int elapsedSeconds;
    private Timer timer;

    // Time thresholds for color coding (seconds)
    private static final int RED_THRESHOLD = 15 * 60;
    private static final int YELLOW_THRESHOLD = 20 * 60;
    private static final int GREEN_THRESHOLD = 25 * 60;

    public BeltEntryPanel(String beltName, int initialKillCount) {
        this.killCount = initialKillCount;
        this.elapsedSeconds = 0;

        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 8, 2));
        setMaximumSize(new Dimension(Integer.MAX_VALUE, 28));

        beltLabel = new JLabel(beltName);
        beltLabel.setPreferredSize(new Dimension(60, 20));
        add(beltLabel);

        timerLabel = new JLabel(formatTime(elapsedSeconds));
        timerLabel.setPreferredSize(new Dimension(70, 20));
        add(timerLabel);

        resetTimerBtn = new JButton("Reset Timer");
        resetTimerBtn.setMargin(new Insets(2, 5, 2, 5));
        add(resetTimerBtn);

        killCountBtn = new JButton("Kills: " + killCount);
        killCountBtn.setMargin(new Insets(2, 5, 2, 5));
        add(killCountBtn);

        // Timer updates every second
        timer = new Timer(1000, e -> {
            elapsedSeconds++;
            updateTimerLabel();
        });
        timer.start();

        // Reset timer button action
        resetTimerBtn.addActionListener(e -> {
            resetTimer();
            if (timerResetAction != null) timerResetAction.actionPerformed(e);
        });

        // Kill count button action
        killCountBtn.addActionListener(e -> {
            killCount++;
            updateKillCountButton();
            if (killCountAction != null) killCountAction.actionPerformed(e);
        });

        updateTimerLabel();
        updateKillCountButton();
    }

    private void updateTimerLabel() {
        timerLabel.setText(formatTime(elapsedSeconds));
        // Color coding based on elapsed time
        if (elapsedSeconds >= GREEN_THRESHOLD) {
            timerLabel.setForeground(Color.GREEN.darker());
        } else if (elapsedSeconds >= YELLOW_THRESHOLD) {
            timerLabel.setForeground(Color.ORANGE.darker());
        } else if (elapsedSeconds >= RED_THRESHOLD) {
            timerLabel.setForeground(Color.RED.darker());
        } else {
            timerLabel.setForeground(Color.WHITE);
        }
    }

    private void updateKillCountButton() {
        killCountBtn.setText("Kills: " + killCount);
    }

    public void resetTimer() {
        elapsedSeconds = 0;
        updateTimerLabel();
    }

    public void setKillCount(int count) {
        this.killCount = count;
        updateKillCountButton();
    }

    // Allows App to handle external kill count updates (e.g. loading)
    public int getKillCount() {
        return killCount;
    }

    // Allows App to reset timer programmatically if needed
    public void setElapsedSeconds(int seconds) {
        this.elapsedSeconds = seconds;
        updateTimerLabel();
    }

    // Callbacks for button actions
    private ActionListener timerResetAction;
    private ActionListener killCountAction;

    public void setTimerResetAction(ActionListener action) {
        this.timerResetAction = action;
    }

    public void setKillCountAction(ActionListener action) {
        this.killCountAction = action;
    }

    private String formatTime(int seconds) {
        int m = seconds / 60;
        int s = seconds % 60;
        return String.format("%02d:%02d", m, s);
    }
}
