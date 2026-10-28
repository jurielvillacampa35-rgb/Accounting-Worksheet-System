package accountingworksheet;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.sql.SQLException;

public final class DashboardFrame extends JFrame {
    private static final Color INK = new Color(8, 25, 30);
    private static final Color TEAL = new Color(0, 107, 113);
    private static final Color AQUA = new Color(80, 208, 213);
    private static final Color CANVAS = new Color(242, 247, 248);
    private static final Color MUTED = new Color(91, 122, 130);
    private static final Color BORDER = new Color(195, 216, 220);

    private final UserRepository.User user;
    private final WorksheetRepository trialBalanceRepository = new WorksheetRepository();
    private final AdjustmentWorksheetRepository adjustmentRepository = new AdjustmentWorksheetRepository();
    private final JComboBox<WorksheetRepository.WorksheetSummary> trialBalances = new JComboBox<>();
    private final JComboBox<AdjustmentWorksheetRepository.WorksheetSummary> adjustments = new JComboBox<>();
    private final JPanel root = new JPanel(new BorderLayout(0, 22));
    private final JToggleButton themeToggle = new JToggleButton("Dark mode", AppTheme.isDarkMode());
    private final JButton adminButton = new JButton("Admin console");
    private boolean darkMode = AppTheme.isDarkMode();

    public DashboardFrame(UserRepository.User user) {
        this.user = user;
        setTitle("Accounting Worksheet | Dashboard");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(860, 600));
        setSize(1120, 740);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        applyTheme();
        refreshWorksheets();
    }

    private JPanel buildContent() {
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 28, 0));
        root.setBackground(CANVAS);
        root.add(buildSummaryStrip(), BorderLayout.NORTH);
        root.add(buildWorkspace(), BorderLayout.CENTER);
        return root;
    }

    private JPanel buildSummaryStrip() {
        JPanel strip = new JPanel(new GridLayout(1, 4, 12, 0));
        strip.setName("summaryStrip");
        strip.setBackground(INK);
        strip.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        strip.add(metric("WORKSHEET TYPES", "02", "Trial balance and adjustments"));
        strip.add(metric("TRIAL BALANCE", "A / B", "Enter debit and credit balances"));
        strip.add(metric("ADJUSTMENTS", "12 COL", "Prepare year-end statements"));
        strip.add(metric("SIGNED IN", user.username(), "Your worksheets are private"));
        return strip;
    }

    private JPanel metric(String label, String value, String detail) {
        JPanel card = new JPanel(new BorderLayout(4, 7));
        card.setName("metricCard");
        card.setBackground(new Color(10, 48, 55));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(24, 91, 100), 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        JLabel title = new JLabel(label);
        title.setFont(title.getFont().deriveFont(Font.BOLD, 10f));
        title.setForeground(AQUA);
        JLabel main = new JLabel(value);
        main.setFont(main.getFont().deriveFont(Font.BOLD, value.length() > 14 ? 15f : 22f));
        main.setForeground(Color.WHITE);
        JLabel caption = new JLabel(detail);
        caption.setFont(caption.getFont().deriveFont(10f));
        caption.setForeground(new Color(155, 188, 195));
        JPanel text = new JPanel(new BorderLayout(0, 6));
        text.setOpaque(false);
        text.add(title, BorderLayout.NORTH);
        text.add(main, BorderLayout.CENTER);
        text.add(caption, BorderLayout.SOUTH);
        card.add(text);
        return card;
    }

    private JPanel buildWorkspace() {
        JPanel content = new JPanel(new BorderLayout(0, 22));
        content.setBackground(CANVAS);
        content.setBorder(BorderFactory.createEmptyBorder(30, 34, 0, 34));

        JPanel introduction = new JPanel(new BorderLayout(0, 7));
        introduction.setOpaque(false);
        JLabel breadcrumb = new JLabel("WORKSHEETS  ›  DASHBOARD");
        breadcrumb.setFont(breadcrumb.getFont().deriveFont(Font.BOLD, 10f));
        breadcrumb.setForeground(TEAL);
        JLabel title = new JLabel("Choose a worksheet");
        title.setFont(title.getFont().deriveFont(Font.BOLD, 29f));
        title.setForeground(INK);
        JLabel subtitle = new JLabel("Start a new worksheet or continue working on a saved one.");
        subtitle.setForeground(MUTED);
        JPanel titleBlock = new JPanel(new BorderLayout(0, 7));
        titleBlock.setOpaque(false);
        titleBlock.add(title, BorderLayout.NORTH);
        titleBlock.add(subtitle, BorderLayout.SOUTH);
        JPanel topLine = new JPanel(new BorderLayout());
        topLine.setOpaque(false);
        topLine.add(breadcrumb, BorderLayout.WEST);
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
        JPanel rightControls = new JPanel(new java.awt.FlowLayout(java.awt.FlowLayout.RIGHT, 10, 0));
        rightControls.setOpaque(false);
        if (user.admin()) {
            adminButton.addActionListener(event -> new AdminFrame(user).setVisible(true));
            rightControls.add(adminButton);
        }
        rightControls.add(themeToggle);
        topLine.add(rightControls, BorderLayout.EAST);
        introduction.add(topLine, BorderLayout.NORTH);
        introduction.add(titleBlock, BorderLayout.CENTER);
        content.add(introduction, BorderLayout.NORTH);

        JPanel cards = new JPanel(new GridLayout(1, 2, 18, 0));
        cards.setOpaque(false);
        cards.add(worksheetCard(
                "01  /  DAILY ACCOUNTING",
                "Trial balance worksheet",
                "<html>Enter account balances and check that total debits<br>match total credits.</html>",
                "New trial balance", trialBalances, this::refreshWorksheets,
                () -> {
                    dispose();
                    new TrialBalanceFrame(user).setVisible(true);
                },
                () -> openSelectedTrialBalance()));
        cards.add(worksheetCard(
                "02  /  PERIOD-END",
                "12-column adjustments",
                "<html>Record adjustments and calculate the adjusted trial balance,<br>income statement, owner's equity, and balance sheet.</html>",
                "New 12-column worksheet", adjustments, this::refreshWorksheets,
                () -> {
                    dispose();
                    new AdjustmentWorksheetFrame(user).setVisible(true);
                },
                () -> openSelectedAdjustment()));
        content.add(cards, BorderLayout.CENTER);

        JPanel footer = new JPanel(new BorderLayout());
        footer.setOpaque(false);
        JLabel note = new JLabel("Accounting, made clear — one entry at a time.");
        note.setForeground(MUTED);
        footer.add(note, BorderLayout.WEST);
        JButton guideButton = new JButton("Need help using the app?");
        guideButton.setName("appGuideButton");
        guideButton.setToolTipText("Open the app guide");
        guideButton.setBorderPainted(false);
        guideButton.setContentAreaFilled(false);
        guideButton.setFocusPainted(false);
        guideButton.addActionListener(event -> showAppGuide());
        footer.add(guideButton, BorderLayout.EAST);
        content.add(footer, BorderLayout.SOUTH);
        return content;
    }

    private void showAppGuide() {
        JTextArea guide = new JTextArea(
                "Getting started\n"
                + "Choose a worksheet type from the dashboard. Use the New button to start, "
                + "or select a saved worksheet and choose Open to continue.\n\n"
                + "Trial balance\n"
                + "Enter a date (optional), an account name, and its amount in either Debit or Credit. "
                + "Add rows as needed. Total debits must equal total credits before you can save.\n\n"
                + "12-column adjustments\n"
                + "Enter each account, choose its account type, then fill in the trial balance and "
                + "adjustment debit or credit columns. The remaining statement columns and totals "
                + "are calculated automatically.\n\n"
                + "Save and reopen\n"
                + "Give the worksheet a name and choose Save worksheet. To continue later, select it "
                + "from the saved worksheets list on the dashboard and choose Open.\n\n"
                + "Change appearance\n"
                + "Use the Dark mode / Light mode control in the top-right corner. Your choice "
                + "continues across the app's screens.");
        guide.setEditable(false);
        guide.setLineWrap(true);
        guide.setWrapStyleWord(true);
        guide.setFont(guide.getFont().deriveFont(14f));
        guide.setBackground(darkMode ? new Color(25, 42, 48) : Color.WHITE);
        guide.setForeground(darkMode ? new Color(236, 242, 245) : INK);
        guide.setBorder(BorderFactory.createEmptyBorder(14, 14, 14, 14));
        JScrollPane guideScroll = new JScrollPane(guide);
        guideScroll.setPreferredSize(new Dimension(560, 390));
        JOptionPane.showMessageDialog(this, guideScroll, "App guide", JOptionPane.INFORMATION_MESSAGE);
    }

    private JPanel worksheetCard(
            String eyebrow,
            String title,
            String description,
            String createLabel,
            JComboBox<?> savedWorksheets,
            Runnable refresh,
            Runnable create,
            Runnable open) {
        JPanel card = new JPanel(new BorderLayout(0, 14));
        card.setName("worksheetCard");
        card.setBackground(Color.WHITE);
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(22, 22, 20, 22)));

        JPanel copy = new JPanel(new BorderLayout(0, 9));
        copy.setOpaque(false);
        JLabel category = new JLabel(eyebrow);
        category.setForeground(TEAL);
        category.setFont(category.getFont().deriveFont(Font.BOLD, 10f));
        JLabel name = new JLabel(title);
        name.setForeground(INK);
        name.setFont(name.getFont().deriveFont(Font.BOLD, 20f));
        JLabel detail = new JLabel(description);
        detail.setForeground(MUTED);
        detail.setFont(detail.getFont().deriveFont(12f));
        JPanel heading = new JPanel(new BorderLayout(0, 9));
        heading.setOpaque(false);
        heading.add(category, BorderLayout.NORTH);
        heading.add(name, BorderLayout.CENTER);
        heading.add(detail, BorderLayout.SOUTH);
        copy.add(heading, BorderLayout.NORTH);

        JPanel actions = new JPanel(new BorderLayout(0, 12));
        actions.setOpaque(false);
        JButton createButton = new JButton(createLabel);
        ButtonStyles.useConfiguredColors(createButton);
        createButton.setForeground(Color.WHITE);
        createButton.setBackground(new Color(0, 107, 113));
        createButton.setFont(createButton.getFont().deriveFont(Font.BOLD, 12f));
        createButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(TEAL.darker(), 1, true),
                BorderFactory.createEmptyBorder(11, 14, 11, 14)));
        createButton.setOpaque(true);
        createButton.setContentAreaFilled(true);
        createButton.setBorderPainted(true);
        createButton.setFocusPainted(false);
        createButton.addActionListener(event -> create.run());
        actions.add(createButton, BorderLayout.NORTH);

        JPanel saved = new JPanel(new BorderLayout(8, 0));
        saved.setOpaque(false);
        savedWorksheets.setToolTipText("Choose a saved worksheet to continue.");
        JButton openButton = new JButton("Open");
        ButtonStyles.useConfiguredColors(openButton);
        openButton.setForeground(Color.WHITE);
        openButton.setBackground(new Color(35, 105, 166));
        openButton.setFont(openButton.getFont().deriveFont(Font.BOLD, 12f));
        openButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(24, 77, 126), 1, true),
                BorderFactory.createEmptyBorder(8, 16, 8, 16)));
        openButton.setOpaque(true);
        openButton.setContentAreaFilled(true);
        openButton.setBorderPainted(true);
        openButton.setFocusPainted(false);
        openButton.addActionListener(event -> open.run());
        saved.add(savedWorksheets, BorderLayout.CENTER);
        saved.add(openButton, BorderLayout.EAST);
        actions.add(saved, BorderLayout.CENTER);
        JButton refreshButton = new JButton("Refresh saved worksheets");
        ButtonStyles.useConfiguredColors(refreshButton);
        refreshButton.setHorizontalAlignment(SwingConstants.CENTER);
        refreshButton.setForeground(INK);
        refreshButton.setBackground(new Color(246, 250, 250));
        refreshButton.setFont(refreshButton.getFont().deriveFont(Font.BOLD, 11f));
        refreshButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(BORDER, 1, true),
                BorderFactory.createEmptyBorder(7, 10, 7, 10)));
        refreshButton.setOpaque(true);
        refreshButton.setContentAreaFilled(true);
        refreshButton.setBorderPainted(true);
        refreshButton.setFocusPainted(false);
        refreshButton.addActionListener(event -> refresh.run());
        actions.add(refreshButton, BorderLayout.SOUTH);

        card.add(copy, BorderLayout.NORTH);
        card.add(actions, BorderLayout.SOUTH);
        return card;
    }

    private void applyTheme() {
        styleComponentTree(root);
        root.setBackground(darkMode ? new Color(13, 24, 30) : CANVAS);
        themeToggle.setText(darkMode ? "Light mode" : "Dark mode");
        themeToggle.setForeground(darkMode ? Color.WHITE : INK);
        themeToggle.setIcon(new ThemeSwitchIcon());
        if (user.admin()) {
            adminButton.setForeground(Color.WHITE);
            adminButton.setBackground(darkMode ? new Color(0, 107, 113) : TEAL);
        }
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

    private void styleComponentTree(Component component) {
        if (component instanceof JPanel panel) {
            if (panel.isOpaque()) {
                if ("summaryStrip".equals(panel.getName())) {
                    panel.setBackground(INK);
                } else if ("metricCard".equals(panel.getName())) {
                    panel.setBackground(darkMode ? new Color(25, 42, 48) : new Color(10, 48, 55));
                } else if ("worksheetCard".equals(panel.getName())) {
                    panel.setBackground(darkMode ? new Color(25, 42, 48) : Color.WHITE);
                } else {
                    panel.setBackground(darkMode ? new Color(25, 42, 48) : CANVAS);
                }
            }
        }
        if (component instanceof JLabel label) {
            Container parent = label.getParent();
            boolean inMetric = false;
            while (parent != null) {
                if ("summaryStrip".equals(parent.getName()) || "metricCard".equals(parent.getName())) {
                    inMetric = true;
                    break;
                }
                parent = parent.getParent();
            }
            label.setForeground(inMetric
                    ? (darkMode ? new Color(220, 235, 237) : Color.WHITE)
                    : (darkMode ? new Color(236, 242, 245) : INK));
        } else if (component instanceof JButton && component != themeToggle) {
            JButton button = (JButton) component;
            if ("appGuideButton".equals(button.getName())) {
                button.setForeground(darkMode ? AQUA : TEAL);
            } else if (button.getText().equals("Refresh saved worksheets")) {
                button.setForeground(darkMode ? new Color(236, 242, 245) : INK);
                button.setBackground(darkMode ? new Color(37, 51, 62) : new Color(246, 250, 250));
            } else {
                button.setForeground(Color.WHITE);
            }
        } else if (component instanceof JComboBox<?> comboBox) {
            comboBox.setForeground(darkMode ? new Color(236, 242, 245) : INK);
            comboBox.setBackground(darkMode ? new Color(37, 51, 62) : Color.WHITE);
        }
        if (component instanceof Container container) {
            for (Component child : container.getComponents()) {
                styleComponentTree(child);
            }
        }
    }

    private void refreshWorksheets() {
        try {
            trialBalances.removeAllItems();
            for (WorksheetRepository.WorksheetSummary summary : trialBalanceRepository.listForUser(user.id())) {
                trialBalances.addItem(summary);
            }
            adjustments.removeAllItems();
            for (AdjustmentWorksheetRepository.WorksheetSummary summary : adjustmentRepository.listForUser(user.id())) {
                adjustments.addItem(summary);
            }
        } catch (SQLException exception) {
            javax.swing.JOptionPane.showMessageDialog(this,
                    "Could not load saved worksheets.\n" + exception.getMessage(),
                    "Database error", javax.swing.JOptionPane.ERROR_MESSAGE);
        }
    }

    private void openSelectedTrialBalance() {
        WorksheetRepository.WorksheetSummary selected =
                (WorksheetRepository.WorksheetSummary) trialBalances.getSelectedItem();
        if (selected == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "There are no saved trial balance worksheets to open.",
                    "Open worksheet", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new TrialBalanceFrame(user, selected.id()).setVisible(true);
        dispose();
    }

    private void openSelectedAdjustment() {
        AdjustmentWorksheetRepository.WorksheetSummary selected =
                (AdjustmentWorksheetRepository.WorksheetSummary) adjustments.getSelectedItem();
        if (selected == null) {
            javax.swing.JOptionPane.showMessageDialog(this, "There are no saved adjustment worksheets to open.",
                    "Open worksheet", javax.swing.JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        new AdjustmentWorksheetFrame(user, selected.id()).setVisible(true);
        dispose();
    }
}
