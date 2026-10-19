package com.spendwise.ui.screens;

import com.spendwise.ui.components.StyledButton;
import com.spendwise.ui.theme.AppTheme;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GradientPaint;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.HierarchyEvent;
import java.awt.event.HierarchyListener;
import java.awt.geom.Path2D;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WelcomeScreen extends JPanel implements HierarchyListener {

    private final Runnable onOpenDashboard;
    private final Timer animationTimer;
    private final List<Particle> particles = new ArrayList<>();

    private float wavePhase = 0.0f;
    private float demoChartProgress = 0.0f;
    private boolean demoApproved = false;

    public WelcomeScreen(Runnable onOpenDashboard) {
        this.onOpenDashboard = onOpenDashboard;
        setLayout(new BorderLayout());
        setBackground(AppTheme.BG_DARK);

        initParticles(30);

        animationTimer = new Timer(33, e -> {
            if (!AppTheme.isReduceAnimations()) {
                wavePhase += 0.03f;
                if (demoChartProgress < 1.0f) {
                    demoChartProgress = Math.min(1.0f, demoChartProgress + 0.02f);
                }
                for (Particle p : particles) {
                    p.update(getWidth(), getHeight());
                }
            }
            repaint();
        });

        addHierarchyListener(this);

        JPanel mainLayout = new JPanel(new BorderLayout());
        mainLayout.setOpaque(false);

        JPanel topNav = buildTopNav();
        mainLayout.add(topNav, BorderLayout.NORTH);

        JPanel centerPanel = new JPanel();
        centerPanel.setLayout(new BoxLayout(centerPanel, BoxLayout.Y_AXIS));
        centerPanel.setOpaque(false);
        centerPanel.setBorder(new EmptyBorder(24, 40, 24, 40));

        JPanel heroGrid = new JPanel(new GridLayout(1, 2, 36, 0));
        heroGrid.setOpaque(false);
        heroGrid.setAlignmentX(Component.CENTER_ALIGNMENT);
        heroGrid.setMaximumSize(new Dimension(1160, 420));
        heroGrid.setPreferredSize(new Dimension(1100, 420));

        JPanel leftHero = buildLeftHero();
        JPanel rightDemo = buildRightDemoCard();

        heroGrid.add(leftHero);
        heroGrid.add(rightDemo);
        centerPanel.add(heroGrid);

        centerPanel.add(Box.createVerticalStrut(28));

        JPanel chaptersPanel = buildChaptersPanel();
        chaptersPanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        chaptersPanel.setMaximumSize(new Dimension(1160, 110));
        chaptersPanel.setPreferredSize(new Dimension(1100, 110));
        centerPanel.add(chaptersPanel);

        JScrollPane scrollPane = new JScrollPane(centerPanel);
        scrollPane.setOpaque(false);
        scrollPane.getViewport().setOpaque(false);
        scrollPane.setBorder(null);
        scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        scrollPane.getVerticalScrollBar().setUnitIncrement(16);

        mainLayout.add(scrollPane, BorderLayout.CENTER);
        add(mainLayout, BorderLayout.CENTER);
    }

    private JPanel buildTopNav() {
        JPanel nav = new JPanel(new BorderLayout());
        nav.setBackground(new Color(0x05, 0x07, 0x0A, 220));
        nav.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, AppTheme.CARD_BORDER),
                new EmptyBorder(14, 36, 14, 36)
        ));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);

        JPanel logoMark = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(AppTheme.SECONDARY_ACCENT);
                g2.fillOval(2, 2, 22, 22);
                g2.setColor(AppTheme.TEXT_PRIMARY);
                g2.setStroke(new BasicStroke(1.6f));
                g2.drawLine(2, 13, 24, 13);
                g2.drawLine(13, 2, 13, 24);
                g2.dispose();
            }
        };
        logoMark.setPreferredSize(new Dimension(26, 26));
        logoMark.setOpaque(false);

        JLabel brandName = new JLabel("SpendWise");
        brandName.setFont(new Font("Onest", Font.BOLD, 16));
        brandName.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel brandSub = new JLabel("· Financial Decision Engine");
        brandSub.setFont(new Font("JetBrains Mono", Font.PLAIN, 11));
        brandSub.setForeground(AppTheme.TEXT_MUTED);

        brand.add(logoMark);
        brand.add(brandName);
        brand.add(brandSub);

        JPanel rightLinks = new JPanel(new FlowLayout(FlowLayout.RIGHT, 18, 0));
        rightLinks.setOpaque(false);

        StyledButton dashBtn = new StyledButton("Open Dashboard →", StyledButton.ButtonStyle.PRIMARY);
        dashBtn.setPreferredSize(new Dimension(160, 36));
        dashBtn.addActionListener(e -> launchDashboard());

        rightLinks.add(createNavLink("01 One View"));
        rightLinks.add(createNavLink("02 Spot Problems"));
        rightLinks.add(createNavLink("03 Explain Why"));
        rightLinks.add(createNavLink("04 Act & Plan"));
        rightLinks.add(dashBtn);

        nav.add(brand, BorderLayout.WEST);
        nav.add(rightLinks, BorderLayout.EAST);
        return nav;
    }

    private JLabel createNavLink(String text) {
        JLabel l = new JLabel(text);
        l.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        l.setForeground(AppTheme.TEXT_MUTED);
        l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        return l;
    }

    private JPanel buildLeftHero() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);
        left.setBorder(new EmptyBorder(12, 0, 12, 0));

        JPanel kicker = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        kicker.setOpaque(false);
        JLabel dot = new JLabel("●");
        dot.setFont(new Font("SansSerif", Font.BOLD, 9));
        dot.setForeground(AppTheme.SECONDARY_ACCENT);
        JLabel kickerText = new JLabel("COLLEGE PROJECT · AIML SEM 4");
        kickerText.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        kickerText.setForeground(AppTheme.TEXT_MUTED);
        kicker.add(dot);
        kicker.add(kickerText);

        JLabel h1 = new JLabel("<html><body style='width: 480px;'>" +
                "<span style='color:#dfe7e0; font-size:32px; font-weight:700;'>Find the leaks.<br>Fuel the savings.</span>" +
                "</body></html>");
        h1.setFont(AppTheme.FONT_HERO);

        JLabel sub = new JLabel("<html><body style='width: 450px; line-height: 1.6;'>" +
                "SpendWise connects your income, expense categories and monthly budgets, " +
                "spots what's wasting money, explains why with rule-based baselines, and tells you where to save next." +
                "</body></html>");
        sub.setFont(AppTheme.FONT_BODY);
        sub.setForeground(AppTheme.TEXT_SECONDARY);

        JPanel ctas = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        ctas.setOpaque(false);

        StyledButton openBtn = new StyledButton("Open Dashboard →", StyledButton.ButtonStyle.PRIMARY);
        openBtn.setPreferredSize(new Dimension(170, 42));
        openBtn.addActionListener(e -> launchDashboard());

        StyledButton howBtn = new StyledButton("See How It Works", StyledButton.ButtonStyle.SECONDARY);
        howBtn.setPreferredSize(new Dimension(150, 42));
        howBtn.addActionListener(e -> launchDashboard());

        ctas.add(openBtn);
        ctas.add(howBtn);

        left.add(kicker);
        left.add(Box.createVerticalStrut(14));
        left.add(h1);
        left.add(Box.createVerticalStrut(14));
        left.add(sub);
        left.add(Box.createVerticalStrut(24));
        left.add(ctas);
        return left;
    }

    private JPanel buildRightDemoCard() {
        JPanel card = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                GradientPaint gp = new GradientPaint(
                        0, 0, new Color(0x13, 0x19, 0x20),
                        getWidth(), getHeight(), new Color(0x0A, 0x0E, 0x12)
                );
                g2.setPaint(gp);
                g2.fillRect(0, 0, getWidth(), getHeight());
                g2.setColor(AppTheme.CARD_BORDER);
                g2.drawRect(0, 0, getWidth() - 1, getHeight() - 1);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(new EmptyBorder(16, 18, 16, 18));

        JPanel head = new JPanel(new BorderLayout());
        head.setOpaque(false);
        JLabel title = new JLabel("Travel & Commute · College Fest Run Rate");
        title.setFont(new Font("Onest", Font.BOLD, 13));
        title.setForeground(AppTheme.TEXT_PRIMARY);

        JPanel pill = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pill.setOpaque(false);
        JLabel pDot = new JLabel("●");
        pDot.setFont(new Font("SansSerif", Font.BOLD, 8));
        pDot.setForeground(AppTheme.SECONDARY_ACCENT);
        JLabel pTxt = new JLabel("Live example");
        pTxt.setFont(new Font("JetBrains Mono", Font.PLAIN, 10));
        pTxt.setForeground(AppTheme.TEXT_MUTED);
        pill.add(pDot);
        pill.add(pTxt);

        head.add(title, BorderLayout.WEST);
        head.add(pill, BorderLayout.EAST);
        card.add(head, BorderLayout.NORTH);

        JPanel chart = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                int w = getWidth();
                int h = getHeight();

                g2.setColor(new Color(0x27, 0x30, 0x3B));
                g2.drawLine(20, h - 20, w - 20, h - 20);
                g2.drawLine(20, 20, w - 20, 20);

                Path2D.Float baseline = new Path2D.Float();
                baseline.moveTo(20, h - 45);
                baseline.curveTo(w * 0.3f, h - 50, w * 0.6f, h - 40, w - 20, h - 48);
                g2.setColor(new Color(0xDF, 0xE7, 0xE0, 180));
                g2.setStroke(new BasicStroke(1.8f));
                g2.draw(baseline);

                Path2D.Float spike = new Path2D.Float();
                spike.moveTo(20, h - 45);
                spike.lineTo(w * 0.45f, h - 45);
                spike.curveTo(w * 0.6f, h - 85, w * 0.75f, 25, w - 20, 28);
                g2.setColor(AppTheme.SECONDARY_ACCENT);
                g2.setStroke(new BasicStroke(2.2f));
                g2.draw(spike);

                if (demoApproved) {
                    g2.setColor(AppTheme.PRIMARY_ACCENT);
                    g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{4, 4}, 0));
                    g2.drawLine((int) (w * 0.6f), h - 45, w - 20, h - 45);
                }

                g2.setFont(new Font("JetBrains Mono", Font.PLAIN, 9));
                g2.setColor(AppTheme.TEXT_MUTED);
                g2.drawString("Oct 1", 20, h - 6);
                g2.drawString("Oct 19", (int) (w * 0.55f), h - 6);
                g2.drawString("Oct 31", w - 50, h - 6);
                g2.dispose();
            }
        };
        chart.setOpaque(false);
        chart.setPreferredSize(new Dimension(420, 110));
        card.add(chart, BorderLayout.CENTER);

        JPanel alertStack = new JPanel();
        alertStack.setLayout(new BoxLayout(alertStack, BoxLayout.Y_AXIS));
        alertStack.setOpaque(false);

        JPanel alert1 = createDemoAlert("Deviation detected", "Category run-rate at 126% over 3-month baseline; leaking ₹2,500/day.", AppTheme.SECONDARY_ACCENT);
        JPanel recCard = new JPanel(new BorderLayout(8, 0));
        recCard.setBackground(AppTheme.CARD_RAISE);
        recCard.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                new EmptyBorder(8, 12, 8, 12)
        ));

        JPanel recLeft = new JPanel();
        recLeft.setLayout(new BoxLayout(recLeft, BoxLayout.Y_AXIS));
        recLeft.setOpaque(false);
        JLabel rT = new JLabel("Cap category expenses until month end");
        rT.setFont(AppTheme.FONT_BODY_BOLD);
        rT.setForeground(AppTheme.TEXT_PRIMARY);
        JLabel rS = new JLabel("Protects ~₹2,500 buffer balance");
        rS.setFont(AppTheme.FONT_SMALL);
        rS.setForeground(AppTheme.PRIMARY_ACCENT);
        recLeft.add(rT);
        recLeft.add(rS);

        JButton approveBtn = new JButton(demoApproved ? "✓ Approved" : "Approve");
        approveBtn.setFont(new Font("JetBrains Mono", Font.BOLD, 10));
        approveBtn.setBackground(demoApproved ? AppTheme.PRIMARY_ACCENT : AppTheme.TEXT_PRIMARY);
        approveBtn.setForeground(AppTheme.BG_DARK);
        approveBtn.setPreferredSize(new Dimension(100, 32));
        approveBtn.setFocusPainted(false);
        approveBtn.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        approveBtn.addActionListener(e -> {
            demoApproved = true;
            approveBtn.setText("✓ Approved");
            approveBtn.setBackground(AppTheme.PRIMARY_ACCENT);
            card.repaint();
        });

        recCard.add(recLeft, BorderLayout.CENTER);
        recCard.add(approveBtn, BorderLayout.EAST);

        alertStack.add(alert1);
        alertStack.add(Box.createVerticalStrut(6));
        alertStack.add(recCard);

        card.add(alertStack, BorderLayout.SOUTH);
        return card;
    }

    private JPanel createDemoAlert(String title, String desc, Color dotColor) {
        JPanel p = new JPanel(new BorderLayout(6, 2));
        p.setBackground(AppTheme.CARD_RAISE);
        p.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), 80), 1),
                new EmptyBorder(6, 10, 6, 10)
        ));

        JLabel t = new JLabel(title);
        t.setFont(AppTheme.FONT_BODY_BOLD);
        t.setForeground(dotColor);

        JLabel d = new JLabel(desc);
        d.setFont(AppTheme.FONT_SMALL);
        d.setForeground(AppTheme.TEXT_SECONDARY);

        p.add(t, BorderLayout.NORTH);
        p.add(d, BorderLayout.SOUTH);
        return p;
    }

    private JPanel buildChaptersPanel() {
        JPanel panel = new JPanel(new GridLayout(1, 4, 16, 0));
        panel.setOpaque(false);
        panel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, AppTheme.CARD_BORDER),
                new EmptyBorder(16, 0, 0, 0)
        ));

        panel.add(createChapterChip("01", "One view", "Cashflow, categories & budgets unified in MySQL."));
        panel.add(createChapterChip("02", "Spot problems", "Statistical spikes & unusual category deviation."));
        panel.add(createChapterChip("03", "Explain why", "3-month rolling baseline over elapsed days."));
        panel.add(createChapterChip("04", "Act & learn", "Interactive What-If simulation slider."));
        return panel;
    }

    private JPanel createChapterChip(String num, String title, String desc) {
        JPanel chip = new JPanel();
        chip.setLayout(new BoxLayout(chip, BoxLayout.Y_AXIS));
        chip.setOpaque(false);
        chip.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        JPanel top = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        top.setOpaque(false);

        JLabel n = new JLabel(num);
        n.setFont(new Font("JetBrains Mono", Font.BOLD, 18));
        n.setForeground(AppTheme.TEXT_PRIMARY);

        JLabel t = new JLabel(title);
        t.setFont(new Font("Onest", Font.BOLD, 13));
        t.setForeground(AppTheme.TEXT_PRIMARY);

        top.add(n);
        top.add(t);

        JLabel d = new JLabel("<html><body style='width: 200px;'>" + desc + "</body></html>");
        d.setFont(AppTheme.FONT_SMALL);
        d.setForeground(AppTheme.TEXT_MUTED);

        chip.add(top);
        chip.add(Box.createVerticalStrut(2));
        chip.add(d);
        return chip;
    }

    private void launchDashboard() {
        stopAnimation();
        if (onOpenDashboard != null) {
            onOpenDashboard.run();
        }
    }

    private void initParticles(int count) {
        particles.clear();
        Random r = new Random();
        for (int i = 0; i < count; i++) {
            particles.add(new Particle(
                    r.nextFloat() * 1200,
                    r.nextFloat() * 800,
                    r.nextFloat() * 1.6f + 0.4f,
                    (r.nextFloat() - 0.5f) * 0.4f,
                    (r.nextFloat() - 0.5f) * 0.4f,
                    r.nextFloat() * 0.3f + 0.08f
            ));
        }
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        Graphics2D g2 = (Graphics2D) g.create();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        GradientPaint radial = new GradientPaint(
                getWidth() * 0.8f, 0, new Color(0x17, 0x1F, 0x28, 120),
                getWidth() * 0.2f, getHeight(), new Color(0x05, 0x07, 0x0A)
        );
        g2.setPaint(radial);
        g2.fillRect(0, 0, getWidth(), getHeight());

        g2.setColor(new Color(0xDF, 0xE7, 0xE0, 6));
        for (int x = 0; x < getWidth(); x += 64) {
            g2.drawLine(x, 0, x, getHeight());
        }
        for (int y = 0; y < getHeight(); y += 64) {
            g2.drawLine(0, y, getWidth(), y);
        }

        for (Particle p : particles) {
            p.draw(g2);
        }

        g2.dispose();
    }

    @Override
    public void hierarchyChanged(HierarchyEvent e) {
        if ((e.getChangeFlags() & HierarchyEvent.SHOWING_CHANGED) != 0) {
            if (isShowing()) {
                animationTimer.start();
            } else {
                animationTimer.stop();
            }
        }
    }

    public void stopAnimation() {
        if (animationTimer != null && animationTimer.isRunning()) {
            animationTimer.stop();
        }
    }

    private static class Particle {
        float x, y, radius, vx, vy, alpha;

        Particle(float x, float y, float radius, float vx, float vy, float alpha) {
            this.x = x;
            this.y = y;
            this.radius = radius;
            this.vx = vx;
            this.vy = vy;
            this.alpha = alpha;
        }

        void update(int boundsX, int boundsY) {
            x += vx;
            y += vy;
            if (boundsX > 0 && (x < 0 || x > boundsX)) vx = -vx;
            if (boundsY > 0 && (y < 0 || y > boundsY)) vy = -vy;
        }

        void draw(Graphics2D g2) {
            g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, alpha));
            g2.setColor(new Color(0xDF, 0xE7, 0xE0));
            g2.fillOval((int) x, (int) y, (int) radius * 2, (int) radius * 2);
        }
    }
}
