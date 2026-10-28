package accountingworksheet;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AdjustmentWorksheetFrame extends JFrame {
    private static final Color INK = new Color(8, 25, 30);
    private static final Color TEAL = new Color(0, 107, 113);
    private static final Color AQUA = new Color(80, 208, 213);
    private static final Color CANVAS = new Color(242, 247, 248);
    private static final Color MUTED = new Color(91, 122, 130);
    private static final Color BORDER = new Color(195, 216, 220);
    private static final Color[] GROUP_COLORS = {
            new Color(48, 89, 107), new Color(0, 107, 113), new Color(0, 136, 143),
            new Color(95, 160, 183), new Color(54, 119, 143), new Color(0, 90, 97)
    };
    private static final int ACCOUNT_COLUMN = 1;
    private static final int TYPE_COLUMN = 2;
    private static final int TRIAL_DEBIT = 3;
    private static final int TRIAL_CREDIT = 4;
    private static final int ADJUSTMENT_DEBIT = 5;
    private static final int ADJUSTMENT_CREDIT = 6;
    private static final int ADJUSTED_DEBIT = 7;
    private static final int ADJUSTED_CREDIT = 8;
    private static final int INCOME_DEBIT = 9;
    private static final int INCOME_CREDIT = 10;
    private static final int EQUITY_DEBIT = 11;
    private static final int EQUITY_CREDIT = 12;
    private static final int BALANCE_DEBIT = 13;
    private static final int BALANCE_CREDIT = 14;

    private final UserRepository.User user;
    private final AdjustmentWorksheetRepository repository = new AdjustmentWorksheetRepository();
    private final JTextField titleField = new JTextField("Year-end worksheet", 24);
    private final DefaultTableModel model = new DefaultTableModel(
            new Object[]{"#", "Account title", "Type", "A Debit", "B Credit", "C Debit", "D Credit",
                    "E Debit", "F Credit", "G Debit", "H Credit", "I Debit", "J Credit", "K Debit", "L Credit"},
            0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return row < accountRowCount()
                    && column >= ACCOUNT_COLUMN && column <= ADJUSTMENT_CREDIT;
        }

        @Override
        public Object getValueAt(int row, int column) {
            if (row >= accountRowCount()) {
                return summaryValue(row, column);
            }
            if (column == 0) {
                return row + 1;
            }
            if (column >= ADJUSTED_DEBIT) {
                return computedValue(row, column);
            }
            return super.getValueAt(row, column);
        }
    };
    private final JTable table = new JTable(model);
    private final JScrollPane tableScroll = new JScrollPane(table);
    private final JTextField inputEditor = new JTextField();
    private final JComboBox<AdjustmentWorksheet.AccountType> accountTypeEditor =
            new JComboBox<>(AdjustmentWorksheet.AccountType.values());
    private final JLabel incomeMetric = new JLabel("0.00");
    private final JLabel expenseMetric = new JLabel("0.00");
    private final JLabel netIncomeMetric = new JLabel("0.00");
    private final JLabel statusMetric = new JLabel("Enter balanced trial balance and adjustments");
    private final JLabel statusLabel = new JLabel("Enter the trial balance and adjustment entries; report columns calculate automatically.");
    private final JLabel breadcrumbLabel = new JLabel("WORKSHEETS  ›  12-COLUMN ADJUSTMENTS");
    private final JLabel worksheetTitleLabel = new JLabel("12-column worksheet");
    private final JLabel hintLabel = new JLabel(
            "Enter the trial balance and adjustments; all remaining columns are calculated.");
    private final JLabel worksheetNameLabel = new JLabel("Worksheet");
    private final JLabel totalsLabel = new JLabel(
            "Calculated statement columns balance when the input trial balance and adjustments balance.");
    private final JToggleButton themeToggle = new JToggleButton("Dark mode", AppTheme.isDarkMode());
    private JPanel rootPanel;
    private JPanel workspacePanel;
    private boolean darkMode = AppTheme.isDarkMode();
    private boolean loading;

    public AdjustmentWorksheetFrame(UserRepository.User user) {
        this(user, -1);
    }

    public AdjustmentWorksheetFrame(UserRepository.User user, long worksheetId) {
        this.user = user;
        setTitle("Accounting Worksheet | 12-column Adjustments");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(1040, 640));
        setSize(1440, 820);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        applyTheme();
        model.addTableModelListener(event -> {
            if (!loading && event.getType() == TableModelEvent.UPDATE) {
                refreshSummary();
                table.repaint();
            }
        });
        if (worksheetId >= 0) {
            loadWorksheet(worksheetId);
        } else {
            addSummaryRows();
            addBlankRow();
            refreshSummary();
        }
    }

    private JPanel buildContent() {
        rootPanel = new JPanel(new BorderLayout(0, 12));
        rootPanel.setBackground(CANVAS);
        rootPanel.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        rootPanel.add(buildMetrics(), BorderLayout.NORTH);

        workspacePanel = new JPanel(new BorderLayout(0, 10));
        workspacePanel.setBackground(CANVAS);
        workspacePanel.setBorder(BorderFactory.createEmptyBorder(16, 22, 0, 22));
        workspacePanel.add(buildHeading(), BorderLayout.NORTH);
        workspacePanel.add(buildTableArea(), BorderLayout.CENTER);
        workspacePanel.add(buildFooter(), BorderLayout.SOUTH);
        rootPanel.add(workspacePanel, BorderLayout.CENTER);
        return rootPanel;
    }

    private JPanel buildMetrics() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setBackground(INK);
        metrics.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        metrics.add(metricCard("TOTAL INCOME", incomeMetric, "Revenue after adjustments"));
        metrics.add(metricCard("TOTAL EXPENSES", expenseMetric, "Expenses after adjustments"));
        metrics.add(metricCard("NET INCOME", netIncomeMetric, "Income less expenses"));
        metrics.add(metricCard("WORKSHEET STATUS", statusMetric, "Input and report totals"));
        return metrics;
    }

    private JPanel metricCard(String title, JLabel value, String description) {
        JPanel card = new JPanel(new BorderLayout(4, 6));
        card.setBackground(new Color(10, 48, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(24, 91, 100), 1, true),
                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setForeground(AQUA);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 10f));
        value.setForeground(Color.WHITE);
        value.setFont(value.getFont().deriveFont(Font.BOLD, 22f));
        JLabel detail = new JLabel(description);
        detail.setForeground(new Color(155, 188, 195));
        detail.setFont(detail.getFont().deriveFont(10f));
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        text.add(titleLabel, BorderLayout.NORTH);
        text.add(value, BorderLayout.CENTER);
        text.add(detail, BorderLayout.SOUTH);
        card.add(text);
        return card;
    }

    private JPanel buildHeading() {
        JPanel heading = new JPanel(new BorderLayout(10, 8));
        heading.setOpaque(false);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 6));
        titleBlock.setOpaque(false);
        JLabel breadcrumb = breadcrumbLabel;
        breadcrumb.setForeground(TEAL);
        breadcrumb.setFont(breadcrumb.getFont().deriveFont(Font.BOLD, 10f));
        JLabel title = worksheetTitleLabel;
        title.setForeground(INK);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 25f));
        JLabel hint = hintLabel;
        hint.setForeground(MUTED);
        titleBlock.add(breadcrumb, BorderLayout.NORTH);
        JPanel titleAndHint = new JPanel(new BorderLayout(0, 4));
        titleAndHint.setOpaque(false);
        titleAndHint.add(title, BorderLayout.NORTH);
        titleAndHint.add(hint, BorderLayout.SOUTH);
        titleBlock.add(titleAndHint, BorderLayout.CENTER);

        JPanel topActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 3));
        topActions.setOpaque(false);
        JButton dashboardButton = outlineButton("Dashboard");
        dashboardButton.addActionListener(event -> returnToDashboard());
        JButton save = primaryButton("Save worksheet");
        save.addActionListener(event -> saveWorksheet());
        themeToggle.setIcon(new ThemeSwitchIcon());
        themeToggle.setIconTextGap(8);
        themeToggle.setBorderPainted(false);
        themeToggle.setContentAreaFilled(false);
        themeToggle.setFocusPainted(false);
        themeToggle.addActionListener(event -> {
            darkMode = themeToggle.isSelected();
            AppTheme.setDarkMode(darkMode);
            applyTheme();
        });
        topActions.add(themeToggle);
        topActions.add(dashboardButton);
        topActions.add(save);
        JPanel firstRow = new JPanel(new BorderLayout());
        firstRow.setOpaque(false);
        firstRow.add(titleBlock, BorderLayout.CENTER);
        firstRow.add(topActions, BorderLayout.EAST);
        heading.add(firstRow, BorderLayout.NORTH);

        JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
        controls.setOpaque(false);
        JLabel nameLabel = worksheetNameLabel;
        nameLabel.setForeground(INK);
        nameLabel.setFont(nameLabel.getFont().deriveFont(Font.BOLD, 11f));
        styleInput(titleField);
        JButton add = primaryButton("+ Add account");
        add.addActionListener(event -> addBlankRow());
        JButton delete = outlineButton("Delete selected");
        delete.addActionListener(event -> deleteSelected());
        JButton sample = outlineButton("Load sample");
        sample.addActionListener(event -> loadSample());
        JButton clear = outlineButton("Clear numbers");
        clear.addActionListener(event -> clearNumbers());
        controls.add(nameLabel);
        controls.add(titleField);
        controls.add(add);
        controls.add(delete);
        controls.add(sample);
        controls.add(clear);
        heading.add(controls, BorderLayout.SOUTH);
        return heading;
    }

    private JPanel buildTableArea() {
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        table.setRowHeight(29);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setGridColor(new Color(215, 229, 232));
        table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, new DefaultCellEditor(inputEditor));
        table.getColumnModel().getColumn(TYPE_COLUMN).setCellEditor(new DefaultCellEditor(accountTypeEditor));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable table, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel label = (JLabel) super.getTableCellRendererComponent(
                        table, value, selected, focused, row, column);
                int modelRow = table.convertRowIndexToModel(row);
                if (!selected && modelRow >= accountRowCount()) {
                    int summaryIndex = modelRow - accountRowCount();
                    label.setBackground(summaryIndex == 2 ? TEAL
                            : summaryIndex == 1
                                    ? (darkMode ? new Color(37, 51, 62) : new Color(229, 242, 243))
                                    : (darkMode ? new Color(48, 65, 72) : new Color(207, 231, 234)));
                    label.setForeground(summaryIndex == 2 || darkMode ? Color.WHITE : INK);
                    label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
                } else if (!selected) {
                    label.setBackground(column >= TRIAL_DEBIT && column <= ADJUSTMENT_CREDIT
                            ? (darkMode ? new Color(25, 42, 48) : new Color(239, 247, 248))
                            : (darkMode ? new Color(13, 24, 30) : Color.WHITE));
                    label.setForeground(darkMode ? new Color(236, 242, 245) : INK);
                    label.setFont(label.getFont().deriveFont(Font.PLAIN, 11f));
                }
                label.setHorizontalAlignment(column == ACCOUNT_COLUMN || column == TYPE_COLUMN
                        ? SwingConstants.LEFT : SwingConstants.RIGHT);
                return label;
            }
        });
        configureColumns();
        table.getTableHeader().setReorderingAllowed(false);
        table.getTableHeader().setPreferredSize(new Dimension(0, 54));
        table.getTableHeader().setDefaultRenderer((table1, value, isSelected, hasFocus, row, column) -> {
            JLabel label = new JLabel("<html><center>" + headerText(column) + "</center></html>",
                    SwingConstants.CENTER);
            label.setOpaque(true);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(204, 223, 227)));
            if (column < 3) {
                label.setBackground(INK);
                label.setForeground(Color.WHITE);
            } else {
                int group = (column - 3) / 2;
                label.setBackground(GROUP_COLORS[group]);
                label.setForeground(Color.WHITE);
            }
            return label;
        });
        tableScroll.setBorder(BorderFactory.createLineBorder(BORDER, 1, true));
        tableScroll.getViewport().setBackground(Color.WHITE);
        JPanel container = new JPanel(new BorderLayout());
        container.setOpaque(false);
        container.add(tableScroll, BorderLayout.CENTER);
        return container;
    }

    private String headerText(int column) {
        if (column == 0) {
            return "#";
        }
        if (column == 1) {
            return "Account<br>title";
        }
        if (column == 2) {
            return "Type";
        }
        String[] groups = {"TRIAL BALANCE", "ADJUSTMENTS", "ADJUSTED<br>TRIAL BALANCE",
                "INCOME STATEMENT", "OWNER'S EQUITY", "BALANCE SHEET"};
        return groups[(column - 3) / 2] + "<br>" + model.getColumnName(column);
    }

    private void configureColumns() {
        TableColumnModel columns = table.getColumnModel();
        int[] widths = {42, 220, 130, 115, 115, 115, 115, 120, 120, 120, 120, 120, 120, 120, 120};
        for (int index = 0; index < widths.length; index++) {
            TableColumn column = columns.getColumn(index);
            column.setPreferredWidth(widths[index]);
            column.setMinWidth(widths[index]);
        }
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(0, 5));
        footer.setOpaque(false);
        JLabel totals = totalsLabel;
        totals.setForeground(TEAL);
        totals.setFont(totals.getFont().deriveFont(Font.BOLD, 11f));
        statusLabel.setForeground(MUTED);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        footer.add(totals, BorderLayout.NORTH);
        footer.add(statusLabel, BorderLayout.SOUTH);
        return footer;
    }

    private JButton primaryButton(String text) {
        JButton button = new JButton(text);
        ButtonStyles.useConfiguredColors(button);
        Color background = text.contains("Add account")
                ? new Color(31, 132, 93) : new Color(0, 107, 113);
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1, true),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        return button;
    }

    private JButton outlineButton(String text) {
        JButton button = new JButton(text);
        ButtonStyles.useConfiguredColors(button);
        Color background = text.equals("Delete selected") ? new Color(174, 62, 56)
                : text.equals("Dashboard") ? new Color(65, 86, 104) : new Color(35, 105, 166);
        button.setForeground(Color.WHITE);
        button.setBackground(background);
        button.setFont(button.getFont().deriveFont(Font.BOLD, 11f));
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(background.darker(), 1, true),
                BorderFactory.createEmptyBorder(9, 12, 9, 12)));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
        return button;
    }

    private void styleInput(JTextField field) {
        field.setForeground(darkMode ? new Color(236, 242, 245) : INK);
        field.setBackground(darkMode ? new Color(37, 51, 62) : Color.WHITE);
        field.setCaretColor(darkMode ? AQUA : TEAL);
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(darkMode ? new Color(74, 92, 101) : BORDER, 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        field.setPreferredSize(new Dimension(205, 36));
    }

    private void applyTheme() {
        Color background = darkMode ? new Color(13, 24, 30) : CANVAS;
        Color surface = darkMode ? new Color(25, 42, 48) : Color.WHITE;
        Color text = darkMode ? new Color(236, 242, 245) : INK;
        Color muted = darkMode ? new Color(166, 181, 187) : MUTED;
        Color rule = darkMode ? new Color(74, 92, 101) : BORDER;

        rootPanel.setBackground(background);
        workspacePanel.setBackground(background);
        breadcrumbLabel.setForeground(darkMode ? AQUA : TEAL);
        worksheetTitleLabel.setForeground(text);
        hintLabel.setForeground(muted);
        worksheetNameLabel.setForeground(text);
        totalsLabel.setForeground(darkMode ? AQUA : TEAL);
        statusLabel.setForeground(muted);
        themeToggle.setText(darkMode ? "Light mode" : "Dark mode");
        themeToggle.setForeground(text);
        themeToggle.setIcon(new ThemeSwitchIcon());
        styleInput(titleField);
        styleInput(inputEditor);
        table.setForeground(text);
        table.setBackground(surface);
        table.setSelectionForeground(text);
        table.setSelectionBackground(darkMode ? new Color(48, 65, 72) : new Color(225, 239, 240));
        table.setGridColor(rule);
        table.getTableHeader().setBackground(darkMode ? new Color(25, 42, 48) : INK);
        table.getTableHeader().setForeground(Color.WHITE);
        tableScroll.setBorder(BorderFactory.createLineBorder(rule, 1, true));
        tableScroll.getViewport().setBackground(surface);
        accountTypeEditor.setForeground(text);
        accountTypeEditor.setBackground(surface);
        inputEditor.setForeground(text);
        inputEditor.setBackground(surface);
        table.repaint();
        repaint();
    }

    private final class ThemeSwitchIcon implements javax.swing.Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean selected = themeToggle.isSelected();
            g.setColor(selected ? (darkMode ? AQUA : TEAL) : new Color(132, 145, 136));
            g.fillRoundRect(x, y + 2, 34, 18, 18, 18);
            g.setColor(Color.WHITE);
            g.fillOval(x + (selected ? 18 : 2), y + 4, 14, 14);
            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 34;
        }

        @Override
        public int getIconHeight() {
            return 22;
        }
    }

    private void addBlankRow() {
        int row = accountRowCount();
        model.insertRow(row, new Object[]{null, "", AdjustmentWorksheet.AccountType.ASSET,
                "", "", "", ""});
        table.changeSelection(row, ACCOUNT_COLUMN, false, false);
    }

    private void deleteSelected() {
        int row = table.getSelectedRow();
        if (row >= 0 && row < accountRowCount()) {
            model.removeRow(table.convertRowIndexToModel(row));
            refreshSummary();
        }
    }

    private void clearNumbers() {
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            return;
        }
        loading = true;
        for (int row = 0; row < accountRowCount(); row++) {
            for (int column = TRIAL_DEBIT; column <= ADJUSTMENT_CREDIT; column++) {
                model.setValueAt("", row, column);
            }
        }
        loading = false;
        refreshSummary();
    }

    private void loadSample() {
        loading = true;
        model.setRowCount(0);
        addSummaryRows();
        addSample("Cash", AdjustmentWorksheet.AccountType.ASSET, "12100", "", "", "");
        addSample("Supplies", AdjustmentWorksheet.AccountType.ASSET, "2300", "", "", "2100");
        addSample("Equipment", AdjustmentWorksheet.AccountType.ASSET, "30000", "", "", "");
        addSample("Accounts payable", AdjustmentWorksheet.AccountType.LIABILITY, "", "5000", "", "");
        addSample("Salaries payable", AdjustmentWorksheet.AccountType.LIABILITY, "", "", "", "1600");
        addSample("Owner's capital", AdjustmentWorksheet.AccountType.CAPITAL, "", "30000", "", "");
        addSample("Owner's drawings", AdjustmentWorksheet.AccountType.DRAWING, "3500", "", "", "");
        addSample("Service revenue", AdjustmentWorksheet.AccountType.REVENUE, "", "18800", "", "");
        addSample("Salaries expense", AdjustmentWorksheet.AccountType.EXPENSE, "4100", "", "1600", "");
        addSample("Rent expense", AdjustmentWorksheet.AccountType.EXPENSE, "1000", "", "", "");
        addSample("Supplies expense", AdjustmentWorksheet.AccountType.EXPENSE, "", "", "2100", "");
        addSample("Utilities expense", AdjustmentWorksheet.AccountType.EXPENSE, "800", "", "", "");
        loading = false;
        titleField.setText("Year-end worksheet");
        refreshSummary();
    }

    private void addSample(String name, AdjustmentWorksheet.AccountType type,
                           String trialDebit, String trialCredit, String adjustmentDebit, String adjustmentCredit) {
        model.insertRow(accountRowCount(), new Object[]{
                null, name, type, trialDebit, trialCredit, adjustmentDebit, adjustmentCredit});
    }

    private Object computedValue(int row, int column) {
        try {
            if (column == ADJUSTED_DEBIT || column == ADJUSTED_CREDIT) {
                BigDecimal[] adjusted = adjustedBalance(row);
                return display(adjusted[column == ADJUSTED_DEBIT ? 0 : 1]);
            }
            if (column == INCOME_DEBIT || column == INCOME_CREDIT) {
                return display(statementBalance(row, column == INCOME_DEBIT));
            }
            if (column == EQUITY_DEBIT || column == EQUITY_CREDIT) {
                return display(ownerEquityBalance(row, column == EQUITY_DEBIT));
            }
            if (column == BALANCE_DEBIT || column == BALANCE_CREDIT) {
                return display(balanceSheetBalance(row, column == BALANCE_DEBIT));
            }
        } catch (IllegalArgumentException exception) {
            return "";
        }
        return null;
    }

    private int accountRowCount() {
        return Math.max(0, model.getRowCount() - 3);
    }

    private void addSummaryRows() {
        model.addRow(new Object[]{null, "Column totals", null, "", "", "", ""});
        model.addRow(new Object[]{null, "Net income", null, "", "", "", ""});
        model.addRow(new Object[]{null, "Final totals", null, "", "", "", ""});
    }

    private Object summaryValue(int row, int column) {
        int firstSummaryRow = accountRowCount();
        int summaryIndex = row - firstSummaryRow;
        if (column == 0) {
            return "";
        }
        if (column == ACCOUNT_COLUMN) {
            return summaryIndex == 0 ? "Column totals" : summaryIndex == 1 ? "Net income" : "Final totals";
        }
        if (column == TYPE_COLUMN) {
            return "";
        }
        try {
            Summary totals = calculateSummary();
            BigDecimal value = summaryAmount(totals, column, summaryIndex);
            return display(value);
        } catch (IllegalArgumentException exception) {
            return "";
        }
    }

    private BigDecimal summaryAmount(Summary totals, int column, int row) {
        BigDecimal zero = BigDecimal.ZERO.setScale(2);
        if (row == 1) {
            BigDecimal profit = totals.netIncome();
            if (profit.signum() >= 0) {
                if (column == INCOME_DEBIT || column == EQUITY_CREDIT || column == BALANCE_CREDIT) {
                    return profit;
                }
            } else if (column == INCOME_CREDIT || column == EQUITY_DEBIT || column == BALANCE_DEBIT) {
                return profit.abs();
            }
            return zero;
        }
        BigDecimal total = switch (column) {
            case TRIAL_DEBIT -> totals.trialDebits();
            case TRIAL_CREDIT -> totals.trialCredits();
            case ADJUSTMENT_DEBIT -> totals.adjustmentDebits();
            case ADJUSTMENT_CREDIT -> totals.adjustmentCredits();
            case ADJUSTED_DEBIT -> totals.adjustedDebits();
            case ADJUSTED_CREDIT -> totals.adjustedCredits();
            case INCOME_DEBIT -> totals.expenses();
            case INCOME_CREDIT -> totals.income();
            case EQUITY_DEBIT -> totals.drawings();
            case EQUITY_CREDIT -> totals.capital();
            case BALANCE_DEBIT -> totals.balanceDebits();
            case BALANCE_CREDIT -> totals.balanceCredits();
            default -> zero;
        };
        if (row == 2) {
            if (column == INCOME_DEBIT && totals.netIncome().signum() > 0
                    || column == INCOME_CREDIT && totals.netIncome().signum() < 0
                    || column == EQUITY_DEBIT && totals.netIncome().signum() < 0
                    || column == EQUITY_CREDIT && totals.netIncome().signum() > 0
                    || column == BALANCE_DEBIT && totals.netIncome().signum() < 0
                    || column == BALANCE_CREDIT && totals.netIncome().signum() > 0) {
                total = total.add(totals.netIncome().abs());
            }
        }
        return total;
    }

    private BigDecimal[] adjustedBalance(int row) {
        BigDecimal net = valueAt(row, TRIAL_DEBIT).add(valueAt(row, ADJUSTMENT_DEBIT))
                .subtract(valueAt(row, TRIAL_CREDIT)).subtract(valueAt(row, ADJUSTMENT_CREDIT));
        return net.signum() >= 0
                ? new BigDecimal[]{net, BigDecimal.ZERO.setScale(2)}
                : new BigDecimal[]{BigDecimal.ZERO.setScale(2), net.abs()};
    }

    private BigDecimal statementBalance(int row, boolean debitColumn) {
        AdjustmentWorksheet.AccountType type = accountTypeAt(row);
        BigDecimal[] balance = adjustedBalance(row);
        if (type == AdjustmentWorksheet.AccountType.EXPENSE) {
            return debitColumn ? balance[0] : balance[1];
        }
        if (type == AdjustmentWorksheet.AccountType.REVENUE) {
            return debitColumn ? balance[1] : balance[0];
        }
        return BigDecimal.ZERO.setScale(2);
    }

    private BigDecimal ownerEquityBalance(int row, boolean debitColumn) {
        AdjustmentWorksheet.AccountType type = accountTypeAt(row);
        BigDecimal[] balance = adjustedBalance(row);
        if (type == AdjustmentWorksheet.AccountType.CAPITAL) {
            return debitColumn ? balance[0] : balance[1];
        }
        if (type == AdjustmentWorksheet.AccountType.DRAWING) {
            return debitColumn ? balance[0] : balance[1];
        }
        return BigDecimal.ZERO.setScale(2);
    }

    private BigDecimal balanceSheetBalance(int row, boolean debitColumn) {
        AdjustmentWorksheet.AccountType type = accountTypeAt(row);
        BigDecimal[] balance = adjustedBalance(row);
        if (type == AdjustmentWorksheet.AccountType.ASSET
                || type == AdjustmentWorksheet.AccountType.LIABILITY
                || type == AdjustmentWorksheet.AccountType.CAPITAL
                || type == AdjustmentWorksheet.AccountType.DRAWING) {
            return debitColumn ? balance[0] : balance[1];
        }
        return BigDecimal.ZERO.setScale(2);
    }

    private BigDecimal valueAt(int row, int column) {
        Object value = model.getValueAt(row, column);
        if (value == null || value.toString().isBlank()) {
            return BigDecimal.ZERO.setScale(2);
        }
        try {
            BigDecimal amount = new BigDecimal(value.toString().replace(",", ""))
                    .setScale(2, RoundingMode.UNNECESSARY);
            if (amount.signum() < 0) {
                throw new IllegalArgumentException("Amounts cannot be negative.");
            }
            return amount;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Enter a valid amount with up to two decimal places.");
        }
    }

    private AdjustmentWorksheet.AccountType accountTypeAt(int row) {
        Object value = model.getValueAt(row, TYPE_COLUMN);
        if (value instanceof AdjustmentWorksheet.AccountType type) {
            return type;
        }
        return AdjustmentWorksheet.AccountType.fromLabel(String.valueOf(value));
    }

    private static String display(BigDecimal value) {
        return value.signum() == 0 ? "" : value.setScale(2, RoundingMode.UNNECESSARY).toPlainString();
    }

    private List<AdjustmentWorksheet.Entry> readEntries() {
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            throw new IllegalArgumentException("Finish editing the selected cell before continuing.");
        }
        List<AdjustmentWorksheet.Entry> entries = new ArrayList<>();
        for (int row = 0; row < accountRowCount(); row++) {
            String name = String.valueOf(model.getValueAt(row, ACCOUNT_COLUMN)).trim();
            String trialDebit = valueAt(row, TRIAL_DEBIT).toPlainString();
            String trialCredit = valueAt(row, TRIAL_CREDIT).toPlainString();
            String adjustmentDebit = valueAt(row, ADJUSTMENT_DEBIT).toPlainString();
            String adjustmentCredit = valueAt(row, ADJUSTMENT_CREDIT).toPlainString();
            boolean emptyRow = name.isEmpty() && valueAt(row, TRIAL_DEBIT).signum() == 0
                    && valueAt(row, TRIAL_CREDIT).signum() == 0
                    && valueAt(row, ADJUSTMENT_DEBIT).signum() == 0
                    && valueAt(row, ADJUSTMENT_CREDIT).signum() == 0;
            if (emptyRow) {
                continue;
            }
            if (name.isEmpty()) {
                throw new IllegalArgumentException("Enter an account title on row " + (row + 1) + ".");
            }
            if (valueAt(row, TRIAL_DEBIT).signum() > 0 && valueAt(row, TRIAL_CREDIT).signum() > 0) {
                throw new IllegalArgumentException("Use either a trial balance debit or credit on row " + (row + 1) + ".");
            }
            if (valueAt(row, ADJUSTMENT_DEBIT).signum() > 0
                    && valueAt(row, ADJUSTMENT_CREDIT).signum() > 0) {
                throw new IllegalArgumentException("Use either an adjustment debit or credit on row " + (row + 1) + ".");
            }
            entries.add(new AdjustmentWorksheet.Entry(name, accountTypeAt(row),
                    new BigDecimal(trialDebit), new BigDecimal(trialCredit),
                    new BigDecimal(adjustmentDebit), new BigDecimal(adjustmentCredit)));
        }
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("Add at least one account.");
        }
        return entries;
    }

    private void saveWorksheet() {
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Enter a worksheet name.");
            }
            List<AdjustmentWorksheet.Entry> entries = readEntries();
            Summary summary = calculateSummary();
            if (summary.trialDebits().compareTo(summary.trialCredits()) != 0) {
                throw new IllegalArgumentException("Trial balance debits and credits must be equal.");
            }
            if (summary.trialDebits().signum() == 0) {
                throw new IllegalArgumentException("Enter a nonzero trial balance.");
            }
            if (summary.adjustmentDebits().compareTo(summary.adjustmentCredits()) != 0) {
                throw new IllegalArgumentException("Adjustment debits and credits must be equal.");
            }
            if (!reportsBalanced(summary)) {
                throw new IllegalArgumentException("The calculated adjusted trial balance and statements do not balance.");
            }
            repository.save(user.id(), title, entries);
            statusLabel.setText("Worksheet saved. Trial balance and adjustments are balanced.");
            refreshSummary();
        } catch (SQLException | IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Could not save worksheet", JOptionPane.ERROR_MESSAGE);
        }
    }

    private Summary calculateSummary() {
        BigDecimal trialDebits = BigDecimal.ZERO;
        BigDecimal trialCredits = BigDecimal.ZERO;
        BigDecimal adjustmentDebits = BigDecimal.ZERO;
        BigDecimal adjustmentCredits = BigDecimal.ZERO;
        BigDecimal income = BigDecimal.ZERO;
        BigDecimal expenses = BigDecimal.ZERO;
        BigDecimal adjustedDebits = BigDecimal.ZERO;
        BigDecimal adjustedCredits = BigDecimal.ZERO;
        BigDecimal capital = BigDecimal.ZERO;
        BigDecimal drawings = BigDecimal.ZERO;
        BigDecimal balanceDebits = BigDecimal.ZERO;
        BigDecimal balanceCredits = BigDecimal.ZERO;
        for (int row = 0; row < accountRowCount(); row++) {
            trialDebits = trialDebits.add(valueAt(row, TRIAL_DEBIT));
            trialCredits = trialCredits.add(valueAt(row, TRIAL_CREDIT));
            adjustmentDebits = adjustmentDebits.add(valueAt(row, ADJUSTMENT_DEBIT));
            adjustmentCredits = adjustmentCredits.add(valueAt(row, ADJUSTMENT_CREDIT));
            AdjustmentWorksheet.AccountType type = accountTypeAt(row);
            BigDecimal[] adjusted = adjustedBalance(row);
            adjustedDebits = adjustedDebits.add(adjusted[0]);
            adjustedCredits = adjustedCredits.add(adjusted[1]);
            if (type == AdjustmentWorksheet.AccountType.REVENUE) {
                income = income.add(adjusted[1].subtract(adjusted[0]));
            } else if (type == AdjustmentWorksheet.AccountType.EXPENSE) {
                expenses = expenses.add(adjusted[0].subtract(adjusted[1]));
            } else if (type == AdjustmentWorksheet.AccountType.CAPITAL) {
                capital = capital.add(adjusted[1].subtract(adjusted[0]));
            } else if (type == AdjustmentWorksheet.AccountType.DRAWING) {
                drawings = drawings.add(adjusted[0].subtract(adjusted[1]));
            }
            if (type == AdjustmentWorksheet.AccountType.ASSET
                    || type == AdjustmentWorksheet.AccountType.LIABILITY
                    || type == AdjustmentWorksheet.AccountType.CAPITAL
                    || type == AdjustmentWorksheet.AccountType.DRAWING) {
                balanceDebits = balanceDebits.add(adjusted[0]);
                balanceCredits = balanceCredits.add(adjusted[1]);
            }
        }
        return new Summary(trialDebits, trialCredits, adjustmentDebits, adjustmentCredits,
                adjustedDebits, adjustedCredits, income, expenses, income.subtract(expenses),
                capital, drawings, balanceDebits, balanceCredits);
    }

    private void refreshSummary() {
        try {
            Summary totals = calculateSummary();
            incomeMetric.setText(money(totals.income()));
            expenseMetric.setText(money(totals.expenses()));
            netIncomeMetric.setText(money(totals.netIncome()));
            boolean trialBalanced = totals.trialDebits().compareTo(totals.trialCredits()) == 0;
            boolean adjustmentsBalanced = totals.adjustmentDebits().compareTo(totals.adjustmentCredits()) == 0;
            boolean hasEntries = false;
            for (int row = 0; row < accountRowCount(); row++) {
                Object account = model.getValueAt(row, ACCOUNT_COLUMN);
                if (account != null && !account.toString().isBlank()) {
                    hasEntries = true;
                    break;
                }
            }
            boolean balanced = hasEntries && trialBalanced && adjustmentsBalanced && reportsBalanced(totals);
            statusMetric.setText(balanced ? "Balanced" : "Draft");
            statusMetric.setForeground(balanced ? AQUA : new Color(255, 205, 131));
            table.repaint();
        } catch (IllegalArgumentException exception) {
            statusMetric.setText("Check amounts");
            statusMetric.setForeground(new Color(255, 205, 131));
        }
    }

    private String money(BigDecimal value) {
        return String.format(java.util.Locale.ROOT, "%,.2f", value);
    }

    private boolean reportsBalanced(Summary totals) {
        BigDecimal profit = totals.netIncome().max(BigDecimal.ZERO);
        BigDecimal loss = totals.netIncome().negate().max(BigDecimal.ZERO);
        boolean adjustedTrialBalance = totals.adjustedDebits().compareTo(totals.adjustedCredits()) == 0;
        boolean incomeStatement = totals.expenses().add(profit)
                .compareTo(totals.income().add(loss)) == 0;
        boolean balanceSheet = totals.balanceDebits().add(loss)
                .compareTo(totals.balanceCredits().add(profit)) == 0;
        return adjustedTrialBalance && incomeStatement && balanceSheet;
    }

    private void loadWorksheet(long id) {
        try {
            AdjustmentWorksheet worksheet = repository.load(user.id(), id);
            loading = true;
            titleField.setText(worksheet.title());
            model.setRowCount(0);
            addSummaryRows();
            for (AdjustmentWorksheet.Entry entry : worksheet.entries()) {
                model.insertRow(accountRowCount(), new Object[]{null, entry.accountName(), entry.accountType(),
                        display(entry.trialBalanceDebit()), display(entry.trialBalanceCredit()),
                        display(entry.adjustmentDebit()), display(entry.adjustmentCredit())});
            }
            if (accountRowCount() == 0) {
                addBlankRow();
            }
            loading = false;
            statusLabel.setText("Opened saved 12-column worksheet.");
            refreshSummary();
        } catch (SQLException exception) {
            loading = false;
            JOptionPane.showMessageDialog(this, "Could not open worksheet.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void returnToDashboard() {
        dispose();
        new DashboardFrame(user).setVisible(true);
    }

    private record Summary(
            BigDecimal trialDebits,
            BigDecimal trialCredits,
            BigDecimal adjustmentDebits,
            BigDecimal adjustmentCredits,
            BigDecimal adjustedDebits,
            BigDecimal adjustedCredits,
            BigDecimal income,
            BigDecimal expenses,
            BigDecimal netIncome,
            BigDecimal capital,
            BigDecimal drawings,
            BigDecimal balanceDebits,
            BigDecimal balanceCredits) {
    }
}
