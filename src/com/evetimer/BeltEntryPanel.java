package com.evetimer;

import javax.swing.*;
import java.awt.*;
import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;

@SuppressWarnings("serial")
public class BeltEntryPanel extends JPanel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final JLabel timerLabel;
    private final JButton resetBtn;
    private Instant startTime;
    private boolean selected = false;

    public BeltEntryPanel() {
        setOpaque(false);
        setLayout(new FlowLayout(FlowLayout.LEFT, 4, 2));

        timerLabel = new JLabel("00:00:00");
        timerLabel.setForeground(Color.WHITE);
        add(timerLabel);

        resetBtn = new JButton("Reset");
        resetBtn.setMargin(new Insets(2, 4, 2, 4));
        add(resetBtn);

        startTime = Instant.now();
        new Timer(1000, e -> updateTimer()).start();

        resetBtn.addActionListener(e -> {
            startTime = Instant.now();
            updateTimer();
        });

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
        timerLabel.setText(String.format("%02d:%02d:%02d", h, m, s));
        repaint();
    }
}
