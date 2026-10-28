package accountingworksheet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.sql.SQLException;
import java.util.List;

public final class AdminFrame extends JFrame {
    private final UserRepository.User admin;
    private final UserRepository users = new UserRepository();
    private final WorksheetRepository worksheets = new WorksheetRepository();
    private final AdjustmentWorksheetRepository adjustments = new AdjustmentWorksheetRepository();
    private final DefaultTableModel userModel = readOnlyModel("Username", "Role", "Created");
    private final DefaultTableModel worksheetModel =
            readOnlyModel("Worksheet", "Owner", "Created", "ID");
    private final DefaultTableModel adjustmentModel =
            readOnlyModel("Worksheet", "Owner", "Created", "ID");
    private List<WorksheetRepository.AdminWorksheetSummary> worksheetRows = List.of();
    private List<AdjustmentWorksheetRepository.AdminWorksheetSummary> adjustmentRows = List.of();

    public AdminFrame(UserRepository.User admin) {
        if (admin == null || !admin.admin()) {
            throw new IllegalArgumentException("Administrator access is required.");
        }
        this.admin = admin;
        setTitle("Accounting Worksheet | Admin");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(760, 500));
        setSize(980, 640);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        refreshData();
    }

    private JPanel buildContent() {
        JPanel root = new JPanel(new BorderLayout(0, 10));
        root.setBorder(BorderFactory.createEmptyBorder(14, 18, 18, 18));
        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("Users", tablePanel(new JTable(userModel), null));
        JTable trialBalanceTable = new JTable(worksheetModel);
        tabs.addTab("Trial balances", tablePanel(trialBalanceTable, () -> viewTrialBalance(trialBalanceTable)));
        JTable adjustmentTable = new JTable(adjustmentModel);
        tabs.addTab("12-column worksheets",
                tablePanel(adjustmentTable, () -> viewAdjustment(adjustmentTable)));
        root.add(tabs, BorderLayout.CENTER);
        JButton refresh = new JButton("Refresh");
        refresh.addActionListener(event -> refreshData());
        JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        actions.add(refresh);
        root.add(actions, BorderLayout.SOUTH);
        return root;
    }

    private JPanel tablePanel(JTable table, Runnable viewAction) {
        JPanel panel = new JPanel(new BorderLayout(0, 8));
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        if (viewAction != null) {
            JButton view = new JButton("View selected worksheet");
            view.addActionListener(event -> viewAction.run());
            JPanel actions = new JPanel(new FlowLayout(FlowLayout.RIGHT));
            actions.add(view);
            panel.add(actions, BorderLayout.SOUTH);
            table.addMouseListener(new java.awt.event.MouseAdapter() {
                @Override
                public void mouseClicked(java.awt.event.MouseEvent event) {
                    if (event.getClickCount() == 2) {
                        viewAction.run();
                    }
                }
            });
        }
        return panel;
    }

    private void refreshData() {
        try {
            userModel.setRowCount(0);
            for (UserRepository.UserSummary user : users.listUsers(admin)) {
                userModel.addRow(new Object[]{user.username(), user.role(), user.createdAt()});
            }

            worksheetRows = worksheets.listAllForAdmin(admin);
            worksheetModel.setRowCount(0);
            for (WorksheetRepository.AdminWorksheetSummary worksheet : worksheetRows) {
                worksheetModel.addRow(new Object[]{worksheet.title(), worksheet.owner(),
                        worksheet.createdAt(), worksheet.id()});
            }

            adjustmentRows = adjustments.listAllForAdmin(admin);
            adjustmentModel.setRowCount(0);
            for (AdjustmentWorksheetRepository.AdminWorksheetSummary worksheet : adjustmentRows) {
                adjustmentModel.addRow(new Object[]{worksheet.title(), worksheet.owner(),
                        worksheet.createdAt(), worksheet.id()});
            }
        } catch (SQLException exception) {
            JOptionPane.showMessageDialog(this, "Could not load admin data.\n" + exception.getMessage(),
                    "Database error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void viewTrialBalance(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a worksheet first.",
                    "View worksheet", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            Worksheet worksheet = worksheets.loadForAdmin(admin, worksheetRows.get(table.convertRowIndexToModel(row)).id());
            String[] columns = {"Date", "Account", "Debit", "Credit"};
            DefaultTableModel model = readOnlyModel(columns);
            for (Worksheet.Entry entry : worksheet.entries()) {
                model.addRow(new Object[]{entry.date(), entry.accountName(), entry.debit(), entry.credit()});
            }
            showEntries(worksheet.title(), model);
        } catch (SQLException exception) {
            showLoadError(exception);
        }
    }

    private void viewAdjustment(JTable table) {
        int row = table.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Select a worksheet first.",
                    "View worksheet", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        try {
            AdjustmentWorksheet worksheet =
                    adjustments.loadForAdmin(admin, adjustmentRows.get(table.convertRowIndexToModel(row)).id());
            DefaultTableModel model = readOnlyModel(
                    "Account", "Type", "Trial debit", "Trial credit", "Adjustment debit", "Adjustment credit");
            for (AdjustmentWorksheet.Entry entry : worksheet.entries()) {
                model.addRow(new Object[]{entry.accountName(), entry.accountType(),
                        entry.trialBalanceDebit(), entry.trialBalanceCredit(),
                        entry.adjustmentDebit(), entry.adjustmentCredit()});
            }
            showEntries(worksheet.title(), model);
        } catch (SQLException exception) {
            showLoadError(exception);
        }
    }

    private void showEntries(String title, DefaultTableModel model) {
        JTable table = new JTable(model);
        table.setFillsViewportHeight(true);
        JScrollPane scrollPane = new JScrollPane(table);
        scrollPane.setPreferredSize(new Dimension(760, 360));
        JOptionPane.showMessageDialog(this, scrollPane, title, JOptionPane.PLAIN_MESSAGE);
    }

    private void showLoadError(SQLException exception) {
        JOptionPane.showMessageDialog(this, "Could not open worksheet.\n" + exception.getMessage(),
                "Database error", JOptionPane.ERROR_MESSAGE);
    }

    private static DefaultTableModel readOnlyModel(String... columns) {
        return new DefaultTableModel(columns, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
    }
}
