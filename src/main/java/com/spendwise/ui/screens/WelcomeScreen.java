package com.spendwise.ui.screens;

import com.spendwise.ui.components.StyledButton;
import com.spendwise.ui.theme.AppIcons;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Timer;
import javax.swing.border.EmptyBorder;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.GeneralPath;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WelcomeScreen extends JPanel implements HierarchyListener {

    private final Runnable onOpenDashboard;
    private final Timer animationTimer;
    private final List<Particle> particles = new ArrayList<>();
    private final Point mousePos = new Point(400, 300);

    private float entranceAlpha = 0.0f;
    private float wavePhase = 0.0f;
    private boolean featuresVisible = false;
    private final JPanel featuresContainer;

    public WelcomeScreen(Runnable onOpenDashboard) {
        this.onOpenDashboard = onOpenDashboard;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        initParticles(35);

        animationTimer = new Timer(33, e -> {
            if (AppTheme.isReduceAnimations()) {
                entranceAlpha = 1.0f;
                repaint();
                return;
            }

            if (entranceAlpha < 1.0f) {
                entranceAlpha = Math.min(1.0f, entranceAlpha + 0.04f);
            }
            wavePhase += 0.03f;

            for (Particle p : particles) {
                p.update(getWidth(), getHeight());
            }
            repaint();
        });

        addHierarchyListener(this);

        addMouseMotionListener(new MouseAdapter() {
            @Override
            public void mouseMoved(MouseEvent e) {
                mousePos.setLocation(e.getPoint());
            }
        });

        JPanel content = new JPanel();
        content.setLayout(new BoxLayout(content, BoxLayout.Y_AXIS));
        content.setOpaque(false);
        content.setBorder(new EmptyBorder(40, 60, 40, 60));

        JPanel brandPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int size = 64;
                int x = (getWidth() - size) / 2;
                int y = 0;

                g2.setColor(new Color(0x10, 0xB9, 0x81, 40));
                g2.fillOval(x - 8, y - 8, size + 16, size + 16);

                g2.setColor(AppTheme.PRIMARY_ACCENT);
                Path2D.Float diamond = new Path2D.Float();
                diamond.moveTo(x + size / 2f, y);
                diamond.lineTo(x + size, y + size / 2f);
                diamond.lineTo(x + size / 2f, y + size);
                diamond.lineTo(x, y + size / 2f);
                diamond.closePath();
                g2.fill(diamond);

                g2.setColor(Color.WHITE);
                g2.setStroke(new BasicStroke(2.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                g2.drawLine(x + size / 2, y + 16, x + size / 2, y + size - 16);
                g2.drawLine(x + 16, y + size / 2, x + size - 16, y + size / 2);
                g2.dispose();
            }
        };
        brandPanel.setOpaque(false);
        brandPanel.setPreferredSize(new Dimension(80, 72));
        brandPanel.setMaximumSize(new Dimension(80, 72));
        brandPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(brandPanel);

        content.add(Box.createVerticalStrut(16));

        JLabel titleLabel = new JLabel("SpendWise");
        titleLabel.setFont(new Font("Helvetica Neue", Font.BOLD, 36));
        titleLabel.setForeground(AppTheme.TEXT_PRIMARY);
        titleLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(titleLabel);

        content.add(Box.createVerticalStrut(8));

        JLabel taglineLabel = new JLabel("Your money. Your clarity.");
        taglineLabel.setFont(new Font("Helvetica Neue", Font.BOLD, 20));
        taglineLabel.setForeground(AppTheme.PRIMARY_ACCENT);
        taglineLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(taglineLabel);

        content.add(Box.createVerticalStrut(14));

        JLabel descLabel = new JLabel("<html><center style='width: 520px;'>" +
                "A complete, database-backed personal finance workstation built for real-world clarity. " +
                "Track expenses, monitor budget utilization, detect unusual spending, and run what-if simulations." +
                "</center></html>");
        descLabel.setFont(AppTheme.FONT_BODY);
        descLabel.setForeground(AppTheme.TEXT_SECONDARY);
        descLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(descLabel);

        content.add(Box.createVerticalStrut(28));

        JPanel ctaPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 16, 0));
        ctaPanel.setOpaque(false);
        ctaPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        StyledButton openBtn = new StyledButton("Open My Dashboard", StyledButton.ButtonStyle.PRIMARY);
        openBtn.setPreferredSize(new Dimension(200, 44));
        openBtn.setFont(new Font("Helvetica Neue", Font.BOLD, 14));
        openBtn.addActionListener(e -> {
            stopAnimation();
            if (this.onOpenDashboard != null) {
                this.onOpenDashboard.run();
            }
        });

        StyledButton exploreBtn = new StyledButton("Explore Features", StyledButton.ButtonStyle.SECONDARY);
        exploreBtn.setPreferredSize(new Dimension(170, 44));
        exploreBtn.addActionListener(e -> toggleFeatures());

        ctaPanel.add(openBtn);
        ctaPanel.add(exploreBtn);
        content.add(ctaPanel);

        content.add(Box.createVerticalStrut(32));

        featuresContainer = buildFeaturesPanel();
        featuresContainer.setAlignmentX(Component.CENTER_ALIGNMENT);
        featuresContainer.setVisible(false);
        content.add(featuresContainer);

        content.add(Box.createVerticalGlue());

        JLabel footerLabel = new JLabel("SpendWise v1.0.0 • Java 21 & MySQL 8 • AIML Engineering College Project");
        footerLabel.setFont(AppTheme.FONT_SMALL);
        footerLabel.setForeground(AppTheme.TEXT_MUTED);
        footerLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        content.add(footerLabel);

        JScrollPane scrollPane = new JScrollPane(content);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        add(scrollPane, BorderLayout.CENTER);
    }

    private JPanel buildFeaturesPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 3, 16, 0));
        panel.setOpaque(false);
        panel.setMaximumSize(new Dimension(880, 150));
        panel.setPreferredSize(new Dimension(880, 150));

        panel.add(createFeatureCard(
                "Precise Tracking",
                "Transaction integrity guaranteed by composite foreign keys and strict database constraints.",
                AppTheme.PRIMARY_ACCENT
        ));

        panel.add(createFeatureCard(
                "Dynamic Budgets",
                "Real-time utilization tracking with warning thresholds and safe over-budget calculation.",
                AppTheme.BLUE_ACCENT
        ));

        panel.add(createFeatureCard(
                "Actionable Insights",
                "Rule-based detection of unusual spending spikes, forecasting, and what-if savings simulation.",
                AppTheme.TEAL_ACCENT
        ));

        return panel;
    }

    private JPanel createFeatureCard(String title, String desc, Color accent) {
        JPanel card = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.CARD_BG);
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 12, 12);
                g2.setColor(AppTheme.CARD_BORDER);
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(accent);
                g2.fillRoundRect(16, 16, 24, 4, 2, 2);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setLayout(new BoxLayout(card, BoxLayout.Y_AXIS));
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(26, 16, 16, 16));

        JLabel titleLbl = new JLabel(title);
        titleLbl.setFont(AppTheme.FONT_SUBHEADER);
        titleLbl.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel descLbl = new JLabel("<html><body style='width: 220px;'>" + desc + "</body></html>");
        descLbl.setFont(AppTheme.FONT_SMALL);
        descLbl.setForeground(AppTheme.TEXT_SECONDARY);

        card.add(titleLbl);
        card.add(Box.createVerticalStrut(6));
        card.add(descLbl);

        return card;
    }

    private void toggleFeatures() {
        featuresVisible = !featuresVisible;
        featuresContainer.setVisible(featuresVisible);
        revalidate();
        repaint();
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            if (isShowing()) {
                startAnimation();
            } else {
                stopAnimation();
            }
        }
    }

    private void startAnimation() {
        if (!animationTimer.isRunning()) {
            animationTimer.start();
        }
    }

    private void stopAnimation() {
        if (animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        int w = getWidth();
        int h = getHeight();

        GradientPaint bgGradient = new GradientPaint(
                0, 0, new Color(0x08, 0x0C, 0x16),
                w, h, new Color(0x10, 0x17, 0x28)
        );
        g2.setPaint(bgGradient);
        g2.fillRect(0, 0, w, h);

        if (!AppTheme.isReduceAnimations()) {
            paintCursorGlow(g2);
            paintFinancialWave(g2, w, h);
            paintParticles(g2);
        }

        g2.dispose();
    }

    private void paintCursorGlow(Graphics2D g2) {
        if (mousePos != null) {
            Graphics2D gGlow = (Graphics2D) g2.create();
            int radius = 180;
            gGlow.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.12f));
            gGlow.setColor(AppTheme.PRIMARY_ACCENT);
            gGlow.fillOval(mousePos.x - radius, mousePos.y - radius, radius * 2, radius * 2);
            gGlow.dispose();
        }
    }

    private void paintFinancialWave(Graphics2D g2, int w, int h) {
        Graphics2D gWave = (Graphics2D) g2.create();
        gWave.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        gWave.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.18f));
        gWave.setStroke(new BasicStroke(2.0f));

        GeneralPath wave1 = new GeneralPath();
        float baseY = h * 0.72f;
        wave1.moveTo(0, baseY);

        for (int x = 0; x <= w; x += 30) {
            float y = (float) (baseY + Math.sin((x * 0.005f) + wavePhase) * 35.0
                    + Math.cos((x * 0.002f) + wavePhase * 0.5f) * 20.0);
            wave1.lineTo(x, y);
        }

        gWave.setColor(AppTheme.TEAL_ACCENT);
        gWave.draw(wave1);
        gWave.dispose();
    }

    private void paintParticles(Graphics2D g2) {
        Graphics2D gp = (Graphics2D) g2.create();
        gp.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        for (Particle p : particles) {
            gp.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, p.alpha));
            gp.setColor(p.color);
            gp.fillOval((int) p.x, (int) p.y, (int) p.size, (int) p.size);
        }
        gp.dispose();
    }

    private void initParticles(int count) {
        Random rnd = new Random(42);
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    rnd.nextFloat() * 1200,
                    rnd.nextFloat() * 800,
                    rnd.nextFloat() * 3 + 1.5f,
                    rnd.nextFloat() * 0.4f + 0.1f,
                    rnd.nextFloat() * 0.3f + 0.1f,
                    rnd.nextBoolean() ? AppTheme.PRIMARY_ACCENT : AppTheme.BLUE_ACCENT
            ));
        }
    }

    private static class Particle {
        float x;
        float y;
        float size;
        float speedY;
        float alpha;
        Color color;

        Particle(float x, float y, float size, float speedY, float alpha, Color color) {
            this.x = x;
            this.y = y;
            this.size = size;
            this.speedY = speedY;
            this.alpha = alpha;
            this.color = color;
        }

        void update(int width, int height) {
            y -= speedY;
            if (y < -10) {
                y = height + 10;
                x = (float) (Math.random() * width);
            }
        }
    }
}
