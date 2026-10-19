package com.spendwise.ui.components;

import com.spendwise.ui.theme.AppTheme;

import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.Component;
import java.awt.Dimension;

public class StyledTable extends JTable {

    public StyledTable(TableModel model) {
        super(model);
        init();
    }

    private void init() {
        setRowHeight(36);
        setShowGrid(false);
        setIntercellSpacing(new Dimension(0, 0));
        setFont(AppTheme.FONT_BODY);
        setBackground(AppTheme.CARD_BG);
        setForeground(AppTheme.TEXT_PRIMARY);
        setSelectionBackground(AppTheme.TABLE_SELECTION);
        setSelectionForeground(AppTheme.TEXT_PRIMARY);

        JTableHeader header = getTableHeader();
        header.setFont(AppTheme.FONT_BODY_BOLD);
        header.setBackground(AppTheme.TABLE_HEADER_BG);
        header.setForeground(AppTheme.TEXT_SECONDARY);
        header.setPreferredSize(new Dimension(header.getPreferredSize().width, 38));
        header.setReorderingAllowed(false);

        DefaultTableCellRenderer headerRenderer = (DefaultTableCellRenderer) header.getDefaultRenderer();
        headerRenderer.setBorder(new EmptyBorder(8, 12, 8, 12));
    }

    @Override
    public Component prepareRenderer(TableCellRenderer renderer, int row, int column) {
        Component c = super.prepareRenderer(renderer, row, column);
        if (!isRowSelected(row)) {
            c.setBackground(row % 2 == 0 ? AppTheme.CARD_BG : AppTheme.TABLE_ROW_ALT);
        }
        return c;
    }
}
