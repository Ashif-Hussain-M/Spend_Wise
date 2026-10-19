package com.spendwise.ui.components;

import com.spendwise.model.CategorySpending;
import com.spendwise.model.MonthComparison;
import com.spendwise.ui.theme.AppTheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartMouseEvent;
import org.jfree.chart.ChartMouseListener;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
import org.jfree.chart.entity.CategoryItemEntity;
import org.jfree.chart.entity.ChartEntity;
import org.jfree.chart.entity.PieSectionEntity;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PiePlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.BarRenderer;
import org.jfree.chart.renderer.category.StandardBarPainter;
import org.jfree.chart.title.TextTitle;
import org.jfree.data.category.DefaultCategoryDataset;
import org.jfree.data.general.DefaultPieDataset;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.Timer;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

public class ChartPanel extends JPanel {

    private static final Color[] PALETTE = {
            new Color(0x10, 0xB9, 0x81),
            new Color(0x3B, 0x82, 0xF6),
            new Color(0xF5, 0x9E, 0x0B),
            new Color(0x8B, 0x5C, 0xF6),
            new Color(0xEC, 0x48, 0x99),
            new Color(0x06, 0xB6, 0xD4),
            new Color(0x14, 0xB8, 0xA6),
            new Color(0xF9, 0x73, 0x16),
            new Color(0x63, 0x66, 0xF1)
    };

    private Timer currentTimer;
    private Timer explodeTimer;

    public ChartPanel() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        setPreferredSize(new Dimension(400, 260));
    }

    private void stopActiveTimer() {
        if (currentTimer != null && currentTimer.isRunning()) {
            currentTimer.stop();
        }
    }

    public void displayCategoryPieChart(String title, List<CategorySpending> data) {
        stopActiveTimer();
        removeAll();
        if (data == null || data.isEmpty()) {
            add(new EmptyStatePanel("No Expense Data", "Record expenses to see category breakdown.", null, null), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (CategorySpending cs : data) {
            dataset.setValue(cs.categoryName(), 0.001);
        }

        JFreeChart chart = ChartFactory.createPieChart(title, dataset, true, true, false);
        styleChart(chart);

        @SuppressWarnings("unchecked")
        PiePlot<String> plot = (PiePlot<String>) chart.getPlot();
        plot.setBackgroundPaint(AppTheme.CARD_BG);
        plot.setOutlinePaint(null);
        plot.setLabelBackgroundPaint(AppTheme.CARD_BG);
        plot.setLabelOutlinePaint(AppTheme.CARD_BORDER);
        plot.setLabelPaint(AppTheme.TEXT_SECONDARY);
        plot.setLabelFont(AppTheme.FONT_SMALL);
        plot.setShadowPaint(null);

        int colorIdx = 0;
        for (CategorySpending cs : data) {
            plot.setSectionPaint(cs.categoryName(), PALETTE[colorIdx % PALETTE.length]);
            colorIdx++;
        }

        org.jfree.chart.ChartPanel jfreePanel = new org.jfree.chart.ChartPanel(chart);
        jfreePanel.setBackground(AppTheme.CARD_BG);
        jfreePanel.setOpaque(true);

        jfreePanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                if (entity instanceof PieSectionEntity pieEntity) {
                    String key = pieEntity.getSectionKey().toString();
                    animatePieExplode(plot, key, data);
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }
        });

        add(jfreePanel, BorderLayout.CENTER);
        revalidate();
        repaint();

        final int totalFrames = 22;
        final int[] currentFrame = {0};
        currentTimer = new Timer(16, e -> {
            currentFrame[0]++;
            float progress = (float) currentFrame[0] / totalFrames;
            if (progress >= 1.0f) {
                progress = 1.0f;
                ((Timer) e.getSource()).stop();
            }
            float eased = 1.0f - (float) Math.pow(1.0f - progress, 3);
            for (CategorySpending cs : data) {
                double target = cs.totalAmount().doubleValue();
                dataset.setValue(cs.categoryName(), Math.max(0.001, target * eased));
            }
        });
        currentTimer.start();
    }

    private void animatePieExplode(PiePlot<String> plot, String clickedKey, List<CategorySpending> data) {
        if (explodeTimer != null && explodeTimer.isRunning()) {
            explodeTimer.stop();
        }

        double currentExplode = plot.getExplodePercent(clickedKey);
        boolean shouldRetract = currentExplode > 0.04;
        double targetExplode = shouldRetract ? 0.0 : 0.16;

        for (CategorySpending cs : data) {
            if (!cs.categoryName().equals(clickedKey.toString())) {
                plot.setExplodePercent(cs.categoryName(), 0.0);
            }
        }

        final int frames = 10;
        final int[] frame = {0};
        final double start = currentExplode;
        explodeTimer = new Timer(16, ev -> {
            frame[0]++;
            float t = (float) frame[0] / frames;
            if (t >= 1.0f) {
                t = 1.0f;
                ((Timer) ev.getSource()).stop();
            }
            float eased = 1.0f - (float) Math.pow(1.0f - t, 2);
            double val = start + (targetExplode - start) * eased;
            plot.setExplodePercent(clickedKey, val);
        });
        explodeTimer.start();
    }

    public void displayIncomeVsExpenseBarChart(String title, List<MonthComparison> history) {
        stopActiveTimer();
        removeAll();
        if (history == null || history.isEmpty()) {
            add(new EmptyStatePanel("No Monthly History", "Add transactions across months to compare.", null, null), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        DefaultCategoryDataset dataset = new DefaultCategoryDataset();
        for (MonthComparison mc : history) {
            String monthName = Month.of(mc.month()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            dataset.addValue(0.0, "Income", monthName);
            dataset.addValue(0.0, "Expense", monthName);
        }

        JFreeChart chart = ChartFactory.createBarChart(
                title, "", "Amount (Rs)", dataset, PlotOrientation.VERTICAL, true, true, false
        );
        styleChart(chart);

        CategoryPlot plot = chart.getCategoryPlot();
        plot.setBackgroundPaint(AppTheme.CARD_BG);
        plot.setOutlinePaint(null);
        plot.setRangeGridlinePaint(AppTheme.CARD_BORDER);

        BarRenderer renderer = (BarRenderer) plot.getRenderer();
        renderer.setBarPainter(new StandardBarPainter());
        renderer.setSeriesPaint(0, AppTheme.SUCCESS);
        renderer.setSeriesPaint(1, AppTheme.DANGER);
        renderer.setShadowVisible(false);

        CategoryAxis domainAxis = plot.getDomainAxis();
        domainAxis.setTickLabelPaint(AppTheme.TEXT_SECONDARY);
        domainAxis.setTickLabelFont(AppTheme.FONT_SMALL);
        domainAxis.setAxisLinePaint(AppTheme.CARD_BORDER);

        NumberAxis rangeAxis = (NumberAxis) plot.getRangeAxis();
        rangeAxis.setTickLabelPaint(AppTheme.TEXT_SECONDARY);
        rangeAxis.setTickLabelFont(AppTheme.FONT_SMALL);
        rangeAxis.setLabelPaint(AppTheme.TEXT_SECONDARY);
        rangeAxis.setLabelFont(AppTheme.FONT_SMALL);
        rangeAxis.setAxisLinePaint(AppTheme.CARD_BORDER);

        org.jfree.chart.ChartPanel jfreePanel = new org.jfree.chart.ChartPanel(chart);
        jfreePanel.setBackground(AppTheme.CARD_BG);
        jfreePanel.setOpaque(true);

        jfreePanel.addChartMouseListener(new ChartMouseListener() {
            @Override
            public void chartMouseClicked(ChartMouseEvent event) {
                ChartEntity entity = event.getEntity();
                if (entity instanceof CategoryItemEntity catEntity) {
                    animateBarPulse(dataset, history, catEntity.getColumnKey());
                }
            }

            @Override
            public void chartMouseMoved(ChartMouseEvent event) {
            }
        });

        add(jfreePanel, BorderLayout.CENTER);
        revalidate();
        repaint();

        final int totalFrames = 22;
        final int[] currentFrame = {0};
        currentTimer = new Timer(16, e -> {
            currentFrame[0]++;
            float progress = (float) currentFrame[0] / totalFrames;
            if (progress >= 1.0f) {
                progress = 1.0f;
                ((Timer) e.getSource()).stop();
            }
            float eased = 1.0f - (float) Math.pow(1.0f - progress, 3);
            for (MonthComparison mc : history) {
                String monthName = Month.of(mc.month()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
                dataset.setValue(mc.income().doubleValue() * eased, "Income", monthName);
                dataset.setValue(mc.expense().doubleValue() * eased, "Expense", monthName);
            }
        });
        currentTimer.start();
    }

    private void animateBarPulse(DefaultCategoryDataset dataset, List<MonthComparison> history, Object clickedColumn) {
        String colStr = clickedColumn != null ? clickedColumn.toString() : "";
        MonthComparison match = null;
        for (MonthComparison mc : history) {
            String monthName = Month.of(mc.month()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
            if (monthName.equalsIgnoreCase(colStr)) {
                match = mc;
                break;
            }
        }
        if (match == null) {
            return;
        }

        final MonthComparison target = match;
        final String colName = Month.of(target.month()).getDisplayName(TextStyle.SHORT, Locale.ENGLISH);
        final double baseIncome = target.income().doubleValue();
        final double baseExpense = target.expense().doubleValue();

        final int frames = 12;
        final int[] frame = {0};
        Timer pulseTimer = new Timer(16, null);
        pulseTimer.addActionListener(e -> {
            frame[0]++;
            float t = (float) frame[0] / frames;
            if (t >= 1.0f) {
                dataset.setValue(baseIncome, "Income", colName);
                dataset.setValue(baseExpense, "Expense", colName);
                ((Timer) e.getSource()).stop();
                return;
            }
            double bump = Math.sin(t * Math.PI) * 0.12;
            dataset.setValue(baseIncome * (1.0 + bump), "Income", colName);
            dataset.setValue(baseExpense * (1.0 + bump), "Expense", colName);
        });
        pulseTimer.start();
    }

    private void styleChart(JFreeChart chart) {
        chart.setBackgroundPaint(AppTheme.CARD_BG);
        TextTitle textTitle = chart.getTitle();
        if (textTitle != null) {
            textTitle.setFont(AppTheme.FONT_SUBHEADER);
            textTitle.setPaint(AppTheme.TEXT_PRIMARY);
        }
        if (chart.getLegend() != null) {
            chart.getLegend().setBackgroundPaint(AppTheme.CARD_BG);
            chart.getLegend().setItemFont(AppTheme.FONT_SMALL);
            chart.getLegend().setItemPaint(AppTheme.TEXT_SECONDARY);
            chart.getLegend().setFrame(BlockBorder.NONE);
        }
    }
}
