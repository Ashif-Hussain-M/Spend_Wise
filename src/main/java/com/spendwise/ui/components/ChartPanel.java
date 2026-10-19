package com.spendwise.ui.components;

import com.spendwise.model.CategorySpending;
import com.spendwise.model.MonthComparison;
import com.spendwise.ui.theme.AppTheme;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.block.BlockBorder;
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

    public ChartPanel() {
        setLayout(new BorderLayout());
        setBackground(AppTheme.CARD_BG);
        setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(AppTheme.CARD_BORDER, 1),
                BorderFactory.createEmptyBorder(8, 8, 8, 8)
        ));
        setPreferredSize(new Dimension(400, 260));
    }

    public void displayCategoryPieChart(String title, List<CategorySpending> data) {
        removeAll();
        if (data == null || data.isEmpty()) {
            add(new EmptyStatePanel("No Expense Data", "Record expenses to see category breakdown.", null, null), BorderLayout.CENTER);
            revalidate();
            repaint();
            return;
        }

        DefaultPieDataset<String> dataset = new DefaultPieDataset<>();
        for (CategorySpending cs : data) {
            dataset.setValue(cs.categoryName(), cs.totalAmount().doubleValue());
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
        add(jfreePanel, BorderLayout.CENTER);
        revalidate();
        repaint();
    }

    public void displayIncomeVsExpenseBarChart(String title, List<MonthComparison> history) {
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
            dataset.addValue(mc.income().doubleValue(), "Income", monthName);
            dataset.addValue(mc.expense().doubleValue(), "Expense", monthName);
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
        add(jfreePanel, BorderLayout.CENTER);
        revalidate();
        repaint();
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
