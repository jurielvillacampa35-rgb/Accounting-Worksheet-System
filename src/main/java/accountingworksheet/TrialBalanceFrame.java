package accountingworksheet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.DefaultCellEditor;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.DefaultTableCellRenderer;
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

public final class TrialBalanceFrame extends JFrame {
    private static final Palette DARK = new Palette(
            new Color(13, 24, 30), new Color(25, 42, 48), new Color(37, 51, 62),
            new Color(236, 242, 245), new Color(166, 181, 187), new Color(80, 208, 213),
            new Color(0, 107, 113), new Color(34, 58, 65), new Color(48, 65, 72),
            new Color(74, 92, 101));
    private static final Palette LIGHT = new Palette(
            new Color(245, 247, 243), new Color(255, 255, 252), new Color(237, 242, 239),
            new Color(8, 25, 30), new Color(91, 122, 130), new Color(0, 107, 113),
            new Color(0, 107, 113), new Color(225, 239, 240), new Color(224, 231, 232),
            new Color(197, 210, 201));
    private static final Color SAVE_COLOR = new Color(0, 107, 113);
    private static final Color ADD_COLOR = new Color(31, 132, 93);
    private static final Color OPEN_COLOR = new Color(35, 105, 166);
    private static final Color DASHBOARD_COLOR = new Color(65, 86, 104);
    private static final Color REMOVE_COLOR = new Color(174, 62, 56);

    private final UserRepository.User user;
    private final WorksheetRepository worksheets = new WorksheetRepository();
    private final JTextField titleField = new JTextField(28);
    private final DefaultTableModel tableModel = new DefaultTableModel(
            new Object[]{"Date", "Account name", "Debit", "Credit"}, 0) {
        @Override
        public boolean isCellEditable(int row, int column) {
            return true;
        }
    };
    private final JTable table = new JTable(tableModel);
    private final JScrollPane tableScroll = new JScrollPane(table);
    private final JTextField cellEditor = new JTextField();
    private final JLabel totalsLabel = new JLabel("", SwingConstants.RIGHT);
    private final JLabel statusLabel = new JLabel("Enter accounts and amounts, then save a balanced worksheet.");
    private final JComboBox<WorksheetRepository.WorksheetSummary> savedWorksheets = new JComboBox<>();
    private final JButton saveButton = new JButton("Save worksheet");
    private final JButton addRowButton = new JButton("Add row");
    private final JButton removeRowButton = new JButton("Remove selected");
    private final JButton loadButton = new JButton("Open saved");
    private final JButton dashboardButton = new JButton("Dashboard");
    private final JToggleButton themeToggle = new JToggleButton("Dark mode", AppTheme.isDarkMode());
    private final JPanel root = new JPanel(new BorderLayout(0, 12));
    private final JPanel workspace = new JPanel(new BorderLayout(0, 10));
    private final JPanel controls = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 2));
    private final JLabel heading = new JLabel("Trial Balance Worksheet");
    private final JLabel breadcrumb = new JLabel("WORKSHEETS  ›  TRIAL BALANCE");
    private final JLabel hint = new JLabel("Enter account balances and save when total debits match total credits.");
    private final JLabel signedInLabel = new JLabel();
    private final JLabel worksheetNameLabel = new JLabel("Worksheet name");
    private final JLabel debitMetric = new JLabel("0.00");
    private final JLabel creditMetric = new JLabel("0.00");
    private final JLabel differenceMetric = new JLabel("0.00");
    private final JLabel balanceMetric = new JLabel("Draft");
    private Palette palette = AppTheme.isDarkMode() ? DARK : LIGHT;
    private boolean loading;

    public TrialBalanceFrame(UserRepository.User user) {
        this(user, -1);
    }

    public TrialBalanceFrame(UserRepository.User user, long worksheetId) {
        this.user = user;
        signedInLabel.setText("Signed in as " + user.username());
        setTitle("Accounting Worksheet | Trial Balance");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new java.awt.Dimension(1040, 680));
        setSize(1280, 800);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        tableModel.addTableModelListener(event -> {
            if (!loading && event.getType() == TableModelEvent.UPDATE) {
                updateTotals();
            }
        });
        if (worksheetId < 0) {
            addRow();
        } else {
            loadWorksheet(worksheetId);
        }
        refreshSavedWorksheets();
    }

    private JPanel buildContent() {
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 18, 0));
        root.add(buildMetrics(), BorderLayout.NORTH);
        workspace.setBackground(palette.background());
        workspace.setBorder(BorderFactory.createEmptyBorder(16, 22, 0, 22));
        workspace.add(buildHeading(), BorderLayout.NORTH);
        workspace.add(buildTableArea(), BorderLayout.CENTER);
        workspace.add(buildFooter(), BorderLayout.SOUTH);
        root.add(workspace, BorderLayout.CENTER);
        applyTheme();
        return root;
    }

    private JPanel buildMetrics() {
        JPanel metrics = new JPanel(new GridLayout(1, 4, 12, 0));
        metrics.setBackground(new Color(8, 25, 30));
        metrics.setBorder(BorderFactory.createEmptyBorder(14, 22, 14, 22));
        metrics.add(metricCard("TRIAL BALANCE DEBITS", debitMetric, "Total debit entries"));
        metrics.add(metricCard("TRIAL BALANCE CREDITS", creditMetric, "Total credit entries"));
        metrics.add(metricCard("DIFFERENCE", differenceMetric, "Debits less credits"));
        metrics.add(metricCard("WORKSHEET STATUS", balanceMetric, "Input and report totals"));
        return metrics;
    }

    private JPanel buildHeading() {
        JPanel headingPanel = new JPanel(new BorderLayout(0, 10));
        headingPanel.setOpaque(false);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 5));
        titleBlock.setOpaque(false);
        breadcrumb.setForeground(palette.accent());
        breadcrumb.setFont(breadcrumb.getFont().deriveFont(Font.BOLD, 10f));
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 25f));
        hint.setForeground(palette.muted());
        signedInLabel.setFont(signedInLabel.getFont().deriveFont(11f));
        signedInLabel.setForeground(palette.muted());
        JPanel titleAndHint = new JPanel(new BorderLayout(0, 4));
        titleAndHint.setOpaque(false);
        titleAndHint.add(heading, BorderLayout.NORTH);
        titleAndHint.add(hint, BorderLayout.CENTER);
        titleAndHint.add(signedInLabel, BorderLayout.SOUTH);
        titleBlock.add(breadcrumb, BorderLayout.NORTH);
        titleBlock.add(titleAndHint, BorderLayout.CENTER);

        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 7, 2));
        actions.setOpaque(false);
        themeToggle.setFocusPainted(false);
        themeToggle.setBorderPainted(false);
        themeToggle.setContentAreaFilled(false);
        themeToggle.addActionListener(event -> {
            AppTheme.setDarkMode(themeToggle.isSelected());
            palette = AppTheme.isDarkMode() ? DARK : LIGHT;
            applyTheme();
        });
        actions.add(themeToggle);
        actions.add(dashboardButton);
        actions.add(saveButton);
        JPanel firstRow = new JPanel(new BorderLayout(12, 0));
        firstRow.setOpaque(false);
        firstRow.add(titleBlock, BorderLayout.CENTER);
        firstRow.add(actions, BorderLayout.EAST);

        controls.setOpaque(false);
        worksheetNameLabel.setFont(worksheetNameLabel.getFont().deriveFont(Font.BOLD, 11f));
        titleField.setColumns(20);
        controls.add(worksheetNameLabel);
        controls.add(titleField);
        controls.add(addRowButton);
        controls.add(removeRowButton);
        savedWorksheets.setPreferredSize(new Dimension(190, 36));
        controls.add(savedWorksheets);
        controls.add(loadButton);
        JPanel controlsRow = new JPanel(new BorderLayout(8, 0));
        controlsRow.setOpaque(false);
        controlsRow.add(controls, BorderLayout.WEST);
        headingPanel.add(firstRow, BorderLayout.NORTH);
        headingPanel.add(controlsRow, BorderLayout.SOUTH);

        addRowButton.addActionListener(event -> addRow());
        removeRowButton.addActionListener(event -> removeSelectedRow());
        saveButton.addActionListener(event -> saveWorksheet());
        loadButton.addActionListener(event -> loadSelectedWorksheet());
        dashboardButton.addActionListener(event -> {
            dispose();
            new DashboardFrame(user).setVisible(true);
        });
        return headingPanel;
    }

    private Component buildTableArea() {
        table.setRowHeight(32);
        table.setFillsViewportHeight(true);
        table.setShowVerticalLines(true);
        table.setShowHorizontalLines(true);
        table.setIntercellSpacing(new Dimension(0, 1));
        table.setSelectionMode(javax.swing.ListSelectionModel.SINGLE_SELECTION);
        table.setDefaultEditor(Object.class, new DefaultCellEditor(cellEditor));
        table.setDefaultRenderer(Object.class, new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(
                    JTable source, Object value, boolean selected, boolean focused, int row, int column) {
                JLabel cell = (JLabel) super.getTableCellRendererComponent(
                        source, value, selected, focused, row, column);
                cell.setBackground(selected ? palette.banner() : palette.surface());
                cell.setForeground(palette.text());
                cell.setHorizontalAlignment(column < 2 ? SwingConstants.LEFT : SwingConstants.RIGHT);
                return cell;
            }
        });
        table.getColumnModel().getColumn(0).setPreferredWidth(140);
        table.getColumnModel().getColumn(1).setPreferredWidth(380);
        table.getColumnModel().getColumn(2).setPreferredWidth(180);
        table.getColumnModel().getColumn(3).setPreferredWidth(180);
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setPreferredSize(new Dimension(0, 46));
        tableHeader.setReorderingAllowed(false);
        tableHeader.setDefaultRenderer((source, value, selected, focused, row, column) -> {
            JLabel label = new JLabel(column == 0 ? "DATE"
                    : column == 1 ? "<html><center>ACCOUNT<br>TITLE</center></html>"
                    : "<html><center>TRIAL BALANCE<br>" + (column == 2 ? "DEBIT" : "CREDIT")
                    + "</center></html>", SwingConstants.CENTER);
            label.setOpaque(true);
            label.setFont(label.getFont().deriveFont(Font.BOLD, 10f));
            label.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 1, new Color(204, 223, 227)));
            label.setBackground(column <= 1 ? new Color(8, 25, 30) : new Color(0, 107, 113));
            label.setForeground(Color.WHITE);
            return label;
        });
        tableScroll.setBorder(BorderFactory.createLineBorder(palette.rule(), 1, true));
        tableScroll.getViewport().setBackground(palette.surface());
        return tableScroll;
    }

    private JPanel buildFooter() {
        JPanel footer = new JPanel(new BorderLayout(12, 0));
        footer.setOpaque(false);
        statusLabel.setFont(statusLabel.getFont().deriveFont(11f));
        totalsLabel.setFont(totalsLabel.getFont().deriveFont(Font.BOLD, 14f));
        footer.add(statusLabel, BorderLayout.WEST);
        footer.add(totalsLabel, BorderLayout.EAST);
        return footer;
    }

    private JPanel metricCard(String title, JLabel value, String description) {
        JPanel card = new JPanel(new BorderLayout(4, 6));
        card.setBackground(new Color(10, 48, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(24, 91, 100), 1, true),
                BorderFactory.createEmptyBorder(10, 13, 10, 13)));
        JLabel titleLabel = new JLabel(title);
        titleLabel.setFont(titleLabel.getFont().deriveFont(Font.BOLD, 10f));
        titleLabel.setForeground(new Color(80, 208, 213));
        value.setFont(value.getFont().deriveFont(Font.BOLD, 22f));
        value.setForeground(Color.WHITE);
        JLabel detail = new JLabel(description);
        detail.setFont(detail.getFont().deriveFont(10f));
        detail.setForeground(new Color(155, 188, 195));
        JPanel text = new JPanel(new BorderLayout(0, 5));
        text.setOpaque(false);
        text.add(titleLabel, BorderLayout.NORTH);
        text.add(value, BorderLayout.CENTER);
        text.add(detail, BorderLayout.SOUTH);
        card.add(text);
        return card;
    }

    private void applyTheme() {
        Color text = palette.text();
        root.setBackground(palette.background());
        workspace.setBackground(palette.background());
        heading.setForeground(text);
        breadcrumb.setForeground(palette.accent());
        hint.setForeground(palette.muted());
        signedInLabel.setForeground(palette.muted());
        worksheetNameLabel.setForeground(text);
        statusLabel.setForeground(palette.muted());
        totalsLabel.setForeground(palette.accent());
        themeToggle.setForeground(text);
        themeToggle.setText(AppTheme.isDarkMode() ? "Light mode" : "Dark mode");
        titleField.setForeground(text);
        titleField.setBackground(palette.field());
        titleField.setCaretColor(text);
        titleField.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.fieldBorder(), 1, true),
                BorderFactory.createEmptyBorder(8, 10, 8, 10)));
        table.setForeground(text);
        table.setBackground(palette.surface());
        table.setSelectionForeground(text);
        table.setSelectionBackground(palette.banner());
        table.setGridColor(palette.rule());
        cellEditor.setForeground(text);
        cellEditor.setBackground(palette.field());
        cellEditor.setCaretColor(text);
        cellEditor.setBorder(BorderFactory.createEmptyBorder(3, 5, 3, 5));
        JTableHeader tableHeader = table.getTableHeader();
        tableHeader.setForeground(text);
        tableHeader.setForeground(Color.WHITE);
        tableHeader.setBackground(palette.button());
        tableHeader.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, palette.rule()));
        table.getParent().setBackground(palette.surface());
        tableScroll.setBorder(BorderFactory.createLineBorder(palette.rule(), 1, true));
        styleButton(saveButton, SAVE_COLOR);
        styleButton(addRowButton, ADD_COLOR);
        styleButton(removeRowButton, REMOVE_COLOR);
        styleButton(loadButton, OPEN_COLOR);
        styleButton(dashboardButton, DASHBOARD_COLOR);
        savedWorksheets.setForeground(text);
        savedWorksheets.setBackground(palette.field());
        savedWorksheets.setBorder(BorderFactory.createLineBorder(palette.fieldBorder(), 1, true));
        themeToggle.setIcon(new ThemeSwitchIcon());
        table.repaint();
        repaint();
    }

    private void styleButton(JButton button, Color color) {
        ButtonStyles.useConfiguredColors(button);
        button.setForeground(Color.WHITE);
        button.setBackground(color);
        button.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(color.darker(), 1, true),
                BorderFactory.createEmptyBorder(8, 12, 8, 12)));
        button.setOpaque(true);
        button.setContentAreaFilled(true);
        button.setBorderPainted(true);
        button.setFocusPainted(false);
    }

    private record Palette(Color background, Color surface, Color field, Color text, Color muted,
                           Color accent, Color button, Color banner, Color rule, Color fieldBorder) {
    }

    private final class ThemeSwitchIcon implements javax.swing.Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setColor(themeToggle.isSelected() ? palette.accent() : new Color(132, 145, 136));
            g.fillRoundRect(x, y + 2, 34, 18, 18, 18);
            g.setColor(Color.WHITE);
            g.fillOval(x + (themeToggle.isSelected() ? 18 : 2), y + 4, 14, 14);
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

    private void addRow() {
        tableModel.addRow(new Object[]{"", "", "", ""});
        updateTotals();
    }

    private void removeSelectedRow() {
        int selectedRow = table.getSelectedRow();
        if (selectedRow >= 0) {
            tableModel.removeRow(table.convertRowIndexToModel(selectedRow));
            updateTotals();
        }
    }

    private void updateTotals() {
        try {
            Totals totals = readEntries(false).totals();
            debitMetric.setText(totals.debits().toPlainString());
            creditMetric.setText(totals.credits().toPlainString());
            BigDecimal difference = totals.debits().subtract(totals.credits()).abs();
            differenceMetric.setText(difference.toPlainString());
            balanceMetric.setText(difference.signum() == 0 && totals.debits().signum() > 0
                    ? "Balanced" : "Draft");
            totalsLabel.setText("Debits  " + totals.debits().toPlainString()
                    + "     Credits  " + totals.credits().toPlainString()
                    + "     Difference  " + difference.toPlainString());
        } catch (IllegalArgumentException exception) {
            balanceMetric.setText("Check values");
            totalsLabel.setText("Check amount values");
        }
    }

    private void saveWorksheet() {
        if (table.isEditing() && !table.getCellEditor().stopCellEditing()) {
            return;
        }
        try {
            String title = titleField.getText().trim();
            if (title.isEmpty()) {
                throw new IllegalArgumentException("Enter a worksheet name.");
            }
            EntryCollection worksheet = readEntries(true);
            List<Worksheet.Entry> entries = worksheet.entries();
            Totals totals = worksheet.totals();
            if (totals.debits().signum() == 0 || totals.debits().compareTo(totals.credits()) != 0) {
                throw new IllegalArgumentException("Debits and credits must be equal and greater than zero.");
            }
            worksheets.save(user.id(), title, entries);
            statusLabel.setText("Worksheet saved successfully.");
            refreshSavedWorksheets();
        } catch (SQLException | IllegalArgumentException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    "Could not save worksheet", JOptionPane.ERROR_MESSAGE);
        }
    }

    private EntryCollection readEntries(boolean requireAccounts) {
        List<Worksheet.Entry> entries = new ArrayList<>();
        BigDecimal debits = BigDecimal.ZERO.setScale(2);
        BigDecimal credits = BigDecimal.ZERO.setScale(2);
        for (int row = 0; row < tableModel.getRowCount(); row++) {
            String date = valueAt(row, 0).trim();
            String account = valueAt(row, 1).trim();
            String debitText = valueAt(row, 2).trim();
            String creditText = valueAt(row, 3).trim();
            if (date.isEmpty() && account.isEmpty() && debitText.isEmpty() && creditText.isEmpty()) {
                continue;
            }
            if (requireAccounts && account.isEmpty()) {
                throw new IllegalArgumentException("Enter an account name on row " + (row + 1) + ".");
            }
            BigDecimal debit = parseAmount(debitText, row);
            BigDecimal credit = parseAmount(creditText, row);
            if (debit.signum() > 0 && credit.signum() > 0) {
                throw new IllegalArgumentException(
                        "Use either a debit or a credit, not both, on row " + (row + 1) + ".");
            }
            if (!account.isEmpty()) {
                entries.add(new Worksheet.Entry(date, account, debit, credit));
            } else if (debit.signum() > 0 || credit.signum() > 0) {
                throw new IllegalArgumentException("Enter an account name on row " + (row + 1) + ".");
            }
            debits = debits.add(debit);
            credits = credits.add(credit);
        }
        if (requireAccounts && entries.isEmpty()) {
            throw new IllegalArgumentException("Add at least one account row.");
        }
        return new EntryCollection(entries, new Totals(debits, credits));
    }

    private String valueAt(int row, int column) {
        Object value = tableModel.getValueAt(row, column);
        return value == null ? "" : value.toString();
    }

    private BigDecimal parseAmount(String amount, int row) {
        if (amount.isEmpty()) {
            return BigDecimal.ZERO.setScale(2);
        }
        try {
            BigDecimal parsed = new BigDecimal(amount).setScale(2, RoundingMode.UNNECESSARY);
            if (parsed.signum() < 0) {
                throw new IllegalArgumentException("Amounts cannot be negative (row " + (row + 1) + ").");
            }
            return parsed;
        } catch (NumberFormatException | ArithmeticException exception) {
            throw new IllegalArgumentException("Enter a valid amount with up to two decimal places on row "
                    + (row + 1) + ".");
        }
    }

    private void refreshSavedWorksheets() {
        try {
            savedWorksheets.removeAllItems();
            for (WorksheetRepository.WorksheetSummary summary : worksheets.listForUser(user.id())) {
                savedWorksheets.addItem(summary);
            }
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, "Could not load saved worksheets.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSelectedWorksheet() {
        WorksheetRepository.WorksheetSummary selected =
                (WorksheetRepository.WorksheetSummary) savedWorksheets.getSelectedItem();
        if (selected == null) {
            JOptionPane.showMessageDialog(this, "There are no saved worksheets to open.",
                    "Open worksheet", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (JOptionPane.showConfirmDialog(this, "Open \"" + selected.title() + "\"?",
                "Open worksheet", JOptionPane.OK_CANCEL_OPTION) != JOptionPane.OK_OPTION) {
            return;
        }
        loadWorksheet(selected.id());
    }

    private void loadWorksheet(long worksheetId) {
        try {
            Worksheet worksheet = worksheets.load(user.id(), worksheetId);
            loading = true;
            titleField.setText(worksheet.title());
            tableModel.setRowCount(0);
            for (Worksheet.Entry entry : worksheet.entries()) {
                tableModel.addRow(new Object[]{entry.date(), entry.accountName(),
                        entry.debit().toPlainString(), entry.credit().toPlainString()});
            }
            loading = false;
            updateTotals();
            statusLabel.setText("Opened saved worksheet.");
        } catch (SQLException exception) {
            loading = false;
            JOptionPane.showMessageDialog(this, "Could not open worksheet.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private record Totals(BigDecimal debits, BigDecimal credits) {
    }

    private record EntryCollection(List<Worksheet.Entry> entries, Totals totals) {
    }
}
