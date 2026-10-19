package com.spendwise.ui.theme;

import javax.swing.Icon;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Line2D;
import java.awt.geom.Path2D;
import java.awt.geom.RoundRectangle2D;

public final class AppIcons {

    private AppIcons() {
    }

    public static Icon dashboard(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            float p = s * 0.12f;
            float b = s * 0.32f;
            float gap = s * 0.12f;
            g2.fill(new RoundRectangle2D.Float(p, p, b, b, 3, 3));
            g2.fill(new RoundRectangle2D.Float(p + b + gap, p, b, b, 3, 3));
            g2.fill(new RoundRectangle2D.Float(p, p + b + gap, b, b, 3, 3));
            g2.fill(new RoundRectangle2D.Float(p + b + gap, p + b + gap, b, b, 3, 3));
        });
    }

    public static Icon transactions(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            float y1 = s * 0.3f;
            float y2 = s * 0.5f;
            float y3 = s * 0.7f;
            g2.draw(new Line2D.Float(s * 0.2f, y1, s * 0.8f, y1));
            g2.draw(new Line2D.Float(s * 0.2f, y2, s * 0.8f, y2));
            g2.draw(new Line2D.Float(s * 0.2f, y3, s * 0.8f, y3));
            g2.fill(new Ellipse2D.Float(s * 0.75f - 1.5f, y1 - 2, 4, 4));
            g2.fill(new Ellipse2D.Float(s * 0.75f - 1.5f, y2 - 2, 4, 4));
            g2.fill(new Ellipse2D.Float(s * 0.75f - 1.5f, y3 - 2, 4, 4));
        });
    }

    public static Icon categories(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float tag = new Path2D.Float();
            tag.moveTo(s * 0.2f, s * 0.5f);
            tag.lineTo(s * 0.5f, s * 0.2f);
            tag.lineTo(s * 0.8f, s * 0.2f);
            tag.lineTo(s * 0.8f, s * 0.5f);
            tag.lineTo(s * 0.5f, s * 0.8f);
            tag.closePath();
            g2.draw(tag);
            g2.fill(new Ellipse2D.Float(s * 0.65f, s * 0.3f, 3, 3));
        });
    }

    public static Icon budgets(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new RoundRectangle2D.Float(s * 0.15f, s * 0.25f, s * 0.7f, s * 0.55f, 6, 6));
            g2.draw(new Line2D.Float(s * 0.15f, s * 0.42f, s * 0.85f, s * 0.42f));
            g2.fill(new Ellipse2D.Float(s * 0.65f, s * 0.55f, 3.5f, 3.5f));
        });
    }

    public static Icon reports(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(s * 0.15f, s * 0.85f, s * 0.85f, s * 0.85f));
            float w = s * 0.14f;
            g2.fill(new RoundRectangle2D.Float(s * 0.22f, s * 0.55f, w, s * 0.3f, 2, 2));
            g2.fill(new RoundRectangle2D.Float(s * 0.43f, s * 0.30f, w, s * 0.55f, 2, 2));
            g2.fill(new RoundRectangle2D.Float(s * 0.64f, s * 0.42f, w, s * 0.43f, 2, 2));
        });
    }

    public static Icon settings(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(s * 0.3f, s * 0.3f, s * 0.4f, s * 0.4f));
            g2.draw(new Line2D.Float(s * 0.5f, s * 0.12f, s * 0.5f, s * 0.25f));
            g2.draw(new Line2D.Float(s * 0.5f, s * 0.75f, s * 0.5f, s * 0.88f));
            g2.draw(new Line2D.Float(s * 0.12f, s * 0.5f, s * 0.25f, s * 0.5f));
            g2.draw(new Line2D.Float(s * 0.75f, s * 0.5f, s * 0.88f, s * 0.5f));
        });
    }

    public static Icon add(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(s * 0.5f, s * 0.2f, s * 0.5f, s * 0.8f));
            g2.draw(new Line2D.Float(s * 0.2f, s * 0.5f, s * 0.8f, s * 0.5f));
        });
    }

    public static Icon edit(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            Path2D.Float pen = new Path2D.Float();
            pen.moveTo(s * 0.25f, s * 0.75f);
            pen.lineTo(s * 0.7f, s * 0.3f);
            pen.lineTo(s * 0.8f, s * 0.4f);
            pen.lineTo(s * 0.35f, s * 0.85f);
            pen.closePath();
            g2.draw(pen);
        });
    }

    public static Icon delete(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(s * 0.25f, s * 0.3f, s * 0.75f, s * 0.3f));
            g2.draw(new Line2D.Float(s * 0.4f, s * 0.2f, s * 0.6f, s * 0.2f));
            g2.draw(new RoundRectangle2D.Float(s * 0.3f, s * 0.3f, s * 0.4f, s * 0.55f, 3, 3));
            g2.draw(new Line2D.Float(s * 0.43f, s * 0.42f, s * 0.43f, s * 0.72f));
            g2.draw(new Line2D.Float(s * 0.57f, s * 0.42f, s * 0.57f, s * 0.72f));
        });
    }

    public static Icon search(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Ellipse2D.Float(s * 0.2f, s * 0.2f, s * 0.45f, s * 0.45f));
            g2.draw(new Line2D.Float(s * 0.55f, s * 0.55f, s * 0.82f, s * 0.82f));
        });
    }

    public static Icon refresh(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new java.awt.geom.Arc2D.Float(s * 0.2f, s * 0.2f, s * 0.6f, s * 0.6f, 45, 270, java.awt.geom.Arc2D.OPEN));
            Path2D.Float arrow = new Path2D.Float();
            arrow.moveTo(s * 0.65f, s * 0.2f);
            arrow.lineTo(s * 0.8f, s * 0.2f);
            arrow.lineTo(s * 0.8f, s * 0.35f);
            g2.draw(arrow);
        });
    }

    public static Icon export(int size, Color color) {
        return new VectorIcon(size, color, (g2, s, c) -> {
            g2.setStroke(new BasicStroke(1.8f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g2.draw(new Line2D.Float(s * 0.2f, s * 0.75f, s * 0.8f, s * 0.75f));
            g2.draw(new Line2D.Float(s * 0.5f, s * 0.6f, s * 0.5f, s * 0.2f));
            g2.draw(new Line2D.Float(s * 0.35f, s * 0.35f, s * 0.5f, s * 0.2f));
            g2.draw(new Line2D.Float(s * 0.65f, s * 0.35f, s * 0.5f, s * 0.2f));
        });
    }

    @FunctionalInterface
    public interface IconPainter {
        void paint(Graphics2D g2, int size, Color color);
    }

    public static class VectorIcon implements Icon {
        private final int size;
        private final Color color;
        private final IconPainter painter;

        public VectorIcon(int size, Color color, IconPainter painter) {
            this.size = size;
            this.color = color;
            this.painter = painter;
        }

        @Override
        public void paintIcon(Component c, Graphics g, int x, int y) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.translate(x, y);
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(color != null ? color : c.getForeground());
            painter.paint(g2, size, color);
            g2.dispose();
        }

        @Override
        public int getIconWidth() {
            return size;
        }

        @Override
        public int getIconHeight() {
            return size;
        }
    }
}
