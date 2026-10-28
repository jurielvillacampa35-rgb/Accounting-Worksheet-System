package accountingworksheet;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JToggleButton;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.geom.RoundRectangle2D;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;

public final class LoginFrame extends JFrame {
    private static final Color DARK_BACKGROUND = new Color(13, 24, 20);
    private static final Color DARK_SURFACE = new Color(25, 42, 35);
    private static final Color DARK_FIELD = new Color(37, 51, 62);
    private static final Color DARK_TEXT = new Color(236, 242, 237);
    private static final Color DARK_MUTED = new Color(166, 181, 171);
    private static final Color DARK_ACCENT = new Color(137, 202, 161);
    private static final Color DARK_BUTTON = new Color(31, 97, 75);
    private static final Color LIGHT_BACKGROUND = new Color(245, 247, 243);
    private static final Color LIGHT_SURFACE = new Color(255, 255, 252);
    private static final Color LIGHT_FIELD = new Color(237, 242, 239);
    private static final Color LIGHT_TEXT = new Color(31, 48, 39);
    private static final Color LIGHT_MUTED = new Color(99, 116, 104);
    private static final Color LIGHT_ACCENT = new Color(35, 112, 82);
    private static final Color LIGHT_BUTTON = new Color(31, 112, 83);

    private final UserRepository users = new UserRepository();
    private final JTextField usernameField = new JTextField();
    private final JPasswordField passwordField = new JPasswordField();
    private final JPasswordField confirmPasswordField = new JPasswordField();
    private final JLabel heading = new JLabel();
    private final JLabel eyebrow = new JLabel("YOUR BOOKS, IN BALANCE");
    private final JLabel subtitle = new JLabel();
    private final JLabel statusMessage = new JLabel();
    private final JLabel usernameLabel = new JLabel("Email address");
    private final JLabel passwordLabel = new JLabel("Password");
    private final JLabel confirmPasswordLabel = new JLabel("Confirm password");
    private final JLabel accountPrompt = new JLabel();
    private final JLabel leftCaption = new JLabel(
            "<html><b>ACCOUNTING, MADE CLEAR</b><br><br>One entry at a time.</html>");
    private final JButton submitButton = new JButton();
    private final JButton modeButton = new JButton();
    private final JToggleButton themeToggle = new JToggleButton("Dark mode", AppTheme.isDarkMode());
    private final JPanel root = new JPanel(new BorderLayout());
    private final JPanel header = new JPanel(new BorderLayout());
    private final JPanel main = new JPanel(new GridLayout(1, 2, 1, 0));
    private final JPanel leftPanel = new JPanel(new BorderLayout());
    private final JPanel rightPanel = new JPanel(new BorderLayout());
    private final JPanel statusBanner = new JPanel(new BorderLayout());
    private final JPanel confirmGroup = new JPanel(new BorderLayout(0, 7));
    private final JLabel brandName = new JLabel("<html><b>Accounting</b><br>System Worksheet</html>");
    private Palette palette = AppTheme.isDarkMode() ? Palette.dark() : Palette.light();
    private boolean firstAccountSetup;
    private boolean creatingAccount;

    public LoginFrame() {
        setTitle("Accounting Worksheet | Sign in");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setMinimumSize(new Dimension(900, 640));
        setSize(1180, 780);
        setLocationRelativeTo(null);
        setContentPane(buildContent());
        refreshMode();
    }

    private JPanel buildContent() {
        root.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        header.setBorder(BorderFactory.createEmptyBorder(20, 34, 20, 34));

        JPanel brand = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        brand.setOpaque(false);
        JLabel badge = new JLabel(new WorksheetPaperIcon());
        badge.setPreferredSize(new Dimension(34, 34));
        badge.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        brandName.setFont(brandName.getFont().deriveFont(Font.BOLD, 12f));
        brand.add(badge);
        brand.add(brandName);
        header.add(brand, BorderLayout.WEST);

        themeToggle.setIcon(new SwitchIcon());
        themeToggle.setIconTextGap(8);
        themeToggle.setBorderPainted(false);
        themeToggle.setContentAreaFilled(false);
        themeToggle.setFocusPainted(false);
        themeToggle.addActionListener(event -> {
            AppTheme.setDarkMode(themeToggle.isSelected());
            palette = AppTheme.isDarkMode() ? Palette.dark() : Palette.light();
            applyTheme();
        });
        JPanel themeControl = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 4));
        themeControl.setOpaque(false);
        themeControl.add(themeToggle);
        header.add(themeControl, BorderLayout.EAST);

        leftPanel.setBorder(BorderFactory.createEmptyBorder(30, 34, 28, 34));
        LedgerIllustration illustration = new LedgerIllustration();
        leftPanel.add(illustration, BorderLayout.CENTER);
        leftCaption.setFont(leftCaption.getFont().deriveFont(12f));
        JPanel captionWrap = new JPanel(new BorderLayout());
        captionWrap.setOpaque(false);
        captionWrap.add(leftCaption, BorderLayout.WEST);
        leftPanel.add(captionWrap, BorderLayout.SOUTH);

        rightPanel.setBorder(BorderFactory.createEmptyBorder(26, 54, 26, 54));
        JPanel form = buildForm();
        JPanel centered = new JPanel(new GridBagLayout());
        centered.setOpaque(false);
        centered.add(form);
        rightPanel.add(centered, BorderLayout.CENTER);
        main.add(leftPanel);
        main.add(rightPanel);

        root.add(header, BorderLayout.NORTH);
        root.add(main, BorderLayout.CENTER);
        applyTheme();
        return root;
    }

    private JPanel buildForm() {
        JPanel form = new JPanel();
        form.setLayout(new BoxLayout(form, BoxLayout.Y_AXIS));
        form.setOpaque(false);
        form.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        form.setMaximumSize(new Dimension(430, 520));

        eyebrow.setFont(eyebrow.getFont().deriveFont(Font.BOLD, 10f));
        eyebrow.setAlignmentX(Component.LEFT_ALIGNMENT);
        heading.setFont(heading.getFont().deriveFont(Font.BOLD, 31f));
        heading.setText("<html>Make every<br>number count.</html>");
        heading.setAlignmentX(Component.LEFT_ALIGNMENT);
        subtitle.setFont(subtitle.getFont().deriveFont(13f));
        subtitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        form.add(eyebrow);
        form.add(Box.createVerticalStrut(12));
        form.add(heading);
        form.add(Box.createVerticalStrut(10));
        form.add(subtitle);
        form.add(Box.createVerticalStrut(20));

        statusMessage.setBorder(BorderFactory.createEmptyBorder(10, 11, 10, 11));
        statusBanner.setAlignmentX(Component.LEFT_ALIGNMENT);
        statusBanner.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        statusBanner.add(statusMessage, BorderLayout.CENTER);
        form.add(statusBanner);
        form.add(Box.createVerticalStrut(14));

        styleField(usernameField);
        styleField(passwordField);
        styleField(confirmPasswordField);
        JPanel usernameGroup = fieldGroup(usernameLabel, usernameField);
        JPanel passwordGroup = fieldGroup(passwordLabel, passwordField);
        confirmGroup.add(confirmPasswordLabel, BorderLayout.NORTH);
        confirmGroup.add(confirmPasswordField, BorderLayout.CENTER);
        styleGroup(confirmGroup);
        form.add(usernameGroup);
        form.add(Box.createVerticalStrut(12));
        form.add(passwordGroup);
        form.add(Box.createVerticalStrut(12));
        form.add(confirmGroup);
        form.add(Box.createVerticalStrut(16));

        submitButton.setFont(submitButton.getFont().deriveFont(Font.BOLD, 13f));
        ButtonStyles.useConfiguredColors(submitButton);
        submitButton.setFocusPainted(false);
        submitButton.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(0, 72, 57), 1, true),
                BorderFactory.createEmptyBorder(12, 14, 12, 14)));
        submitButton.setOpaque(true);
        submitButton.setContentAreaFilled(true);
        submitButton.setBorderPainted(false);
        submitButton.setAlignmentX(Component.LEFT_ALIGNMENT);
        submitButton.setMaximumSize(new Dimension(Integer.MAX_VALUE, 46));
        submitButton.addActionListener(this::submit);
        form.add(submitButton);

        JPanel accountPromptPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        accountPromptPanel.setOpaque(false);
        accountPromptPanel.setAlignmentX(Component.LEFT_ALIGNMENT);
        accountPrompt.setFont(accountPrompt.getFont().deriveFont(11f));
        modeButton.setFont(modeButton.getFont().deriveFont(Font.BOLD, 11f));
        modeButton.setBorder(BorderFactory.createEmptyBorder(2, 0, 2, 0));
        modeButton.setContentAreaFilled(false);
        modeButton.setOpaque(false);
        modeButton.setBorderPainted(false);
        modeButton.setFocusPainted(false);
        modeButton.addActionListener(event -> {
            creatingAccount = !creatingAccount;
            passwordField.setText("");
            confirmPasswordField.setText("");
            refreshModeText();
        });
        accountPromptPanel.add(accountPrompt);
        accountPromptPanel.add(modeButton);
        form.add(Box.createVerticalStrut(14));
        form.add(accountPromptPanel);

        form.add(Box.createVerticalGlue());
        getRootPane().setDefaultButton(submitButton);
        return form;
    }

    private JPanel fieldGroup(JLabel label, JTextField field) {
        JPanel group = new JPanel(new BorderLayout(0, 7));
        group.add(label, BorderLayout.NORTH);
        group.add(field, BorderLayout.CENTER);
        styleGroup(group);
        return group;
    }

    private void styleGroup(JPanel group) {
        group.setOpaque(false);
        group.setAlignmentX(Component.LEFT_ALIGNMENT);
        group.setMaximumSize(new Dimension(Integer.MAX_VALUE, 72));
        group.setPreferredSize(new Dimension(400, 72));
        for (Component component : group.getComponents()) {
            if (component instanceof JLabel label) {
                label.setFont(label.getFont().deriveFont(Font.BOLD, 11f));
            }
        }
    }

    private void styleField(JTextField field) {
        field.setFont(field.getFont().deriveFont(13f));
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(74, 92, 101), 1, true),
                BorderFactory.createEmptyBorder(10, 11, 10, 11)));
        field.setPreferredSize(new Dimension(400, 43));
        field.setMaximumSize(new Dimension(Integer.MAX_VALUE, 43));
        field.setToolTipText("Enter your email address or username.");
    }

    private void refreshMode() {
        try {
            firstAccountSetup = !users.hasUsers();
            creatingAccount = firstAccountSetup;
            refreshModeText();
            if (!firstAccountSetup) {
                usernameField.setText("");
            }
        } catch (SQLException exception) {
            showError("Could not check the account database.", exception);
        }
    }

    private void refreshModeText() {
        boolean showConfirmation = creatingAccount;
        String accountText = firstAccountSetup
                ? "Create the first account to get started."
                : creatingAccount ? "Create your account to get started." : "You have been signed out.";
        statusMessage.setText(accountText);
        subtitle.setText("<html>A calm place to record transactions,<br>"
                + "organize accounts, and see the full picture.</html>");
        confirmGroup.setVisible(showConfirmation);
        submitButton.setText(creatingAccount ? "Create account" : "Sign in");
        if (firstAccountSetup) {
            modeButton.setVisible(false);
        } else {
            modeButton.setVisible(true);
            modeButton.setText(creatingAccount ? "Sign in"
                    : "<html><u>Create an account</u></html>");
        }
        accountPrompt.setText(firstAccountSetup ? "Use at least 8 characters for your password."
                : creatingAccount ? "Already have an account?" : "New to the worksheet?");
        revalidate();
        repaint();
    }

    private void submit(ActionEvent event) {
        char[] password = passwordField.getPassword();
        char[] confirmation = confirmPasswordField.getPassword();
        try {
            if (creatingAccount) {
                if (!Arrays.equals(password, confirmation)) {
                    throw new IllegalArgumentException("The passwords do not match.");
                }
                users.createUser(usernameField.getText(), password);
                UserRepository.User user = users.authenticate(usernameField.getText(), password);
                if (user == null) {
                    throw new IllegalStateException("The new account could not be signed in.");
                }
                openWorksheet(user);
                return;
            }

            UserRepository.User user = users.authenticate(usernameField.getText(), password);
            if (user == null) {
                throw new IllegalArgumentException("Email address or password is incorrect.");
            }
            openWorksheet(user);
        } catch (SQLException | GeneralSecurityException | IllegalArgumentException
                 | IllegalStateException exception) {
            JOptionPane.showMessageDialog(this, exception.getMessage(),
                    creatingAccount ? "Account creation failed" : "Sign-in failed",
                    JOptionPane.ERROR_MESSAGE);
        } finally {
            Arrays.fill(password, '\0');
            Arrays.fill(confirmation, '\0');
            passwordField.setText("");
            confirmPasswordField.setText("");
        }
    }

    private void openWorksheet(UserRepository.User user) {
        dispose();
        new DashboardFrame(user).setVisible(true);
    }

    private void showError(String message, Exception exception) {
        JOptionPane.showMessageDialog(this, message + "\n" + exception.getMessage(),
                "Database error", JOptionPane.ERROR_MESSAGE);
        submitButton.setEnabled(false);
    }

    private void applyTheme() {
        Color background = palette.background();
        Color text = palette.text();
        root.setBackground(background);
        header.setBackground(background);
        main.setBackground(background);
        leftPanel.setBackground(background);
        rightPanel.setBackground(background);
        brandName.setForeground(text);
        leftCaption.setForeground(palette.muted());
        heading.setForeground(text);
        eyebrow.setForeground(palette.accent());
        subtitle.setForeground(palette.muted());
        usernameLabel.setForeground(text);
        passwordLabel.setForeground(text);
        confirmPasswordLabel.setForeground(text);
        accountPrompt.setForeground(palette.muted());
        statusBanner.setBackground(palette.banner());
        statusMessage.setForeground(palette.bannerText());
        usernameLabel.setForeground(text);
        styleInput(usernameField);
        styleInput(passwordField);
        styleInput(confirmPasswordField);
        submitButton.setForeground(Color.WHITE);
        submitButton.setBackground(palette.button());
        modeButton.setForeground(palette.accent());
        themeToggle.setForeground(text);
        themeToggle.setText(AppTheme.isDarkMode() ? "Light mode" : "Dark mode");
        themeToggle.setIcon(new SwitchIcon());
        repaint();
    }

    private void styleInput(JTextField field) {
        field.setForeground(palette.text());
        field.setBackground(palette.field());
        field.setCaretColor(palette.text());
        field.setSelectionColor(palette.accent());
        field.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(palette.fieldBorder(), 1, true),
                BorderFactory.createEmptyBorder(10, 11, 10, 11)));
    }

    private final class SwitchIcon implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            boolean selected = themeToggle.isSelected();
            g.setColor(selected ? palette.accent() : new Color(132, 145, 136));
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

    private final class WorksheetPaperIcon implements Icon {
        @Override
        public void paintIcon(Component component, Graphics graphics, int x, int y) {
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

            int pad = 5;
            int width = 24;
            int height = 24;
            int left = x + pad;
            int top = y + pad;

            g.setColor(new Color(244, 240, 223));
            g.fillRoundRect(left, top, width, height, 4, 4);

            g.setColor(new Color(203, 215, 185));
            g.drawRoundRect(left, top, width, height, 4, 4);

            g.setColor(new Color(140, 164, 123));
            g.fillRoundRect(left + 4, top + 4, width - 8, 3, 2, 2);
            g.fillRoundRect(left + 4, top + 9, width - 8, 3, 2, 2);
            g.fillRoundRect(left + 4, top + 14, width - 8, 3, 2, 2);

            g.setColor(new Color(102, 135, 110));
            g.fillRect(left + 18, top + 4, 1, 13);
            g.fillRect(left + 5, top + 18, 14, 1);
            g.fillRect(left + 12, top + 5, 1, 13);

            g.dispose();
        }

        @Override
        public int getIconWidth() {
            return 34;
        }

        @Override
        public int getIconHeight() {
            return 34;
        }
    }

    private final class LedgerIllustration extends JPanel {
        private double animationPhase;
        private final Timer animationTimer = new Timer(70, event -> {
            animationPhase += 0.08;
            repaint();
        });

        private LedgerIllustration() {
            setOpaque(false);
            setPreferredSize(new Dimension(500, 400));
            animationTimer.setCoalesce(true);
        }

        @Override
        public void addNotify() {
            super.addNotify();
            animationTimer.start();
        }

        @Override
        public void removeNotify() {
            animationTimer.stop();
            super.removeNotify();
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            Graphics2D g = (Graphics2D) graphics.create();
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            double scale = Math.min(getWidth() / 500.0, getHeight() / 400.0);
            g.translate((getWidth() - 500 * scale) / 2, (getHeight() - 400 * scale) / 2);
            g.scale(scale, scale);
            paintLedger(g);
            Graphics2D person = (Graphics2D) g.create();
            person.translate(0, Math.sin(animationPhase) * 2.5);
            paintPerson(person, animationPhase);
            person.dispose();
            paintSpeechBubble(g, animationPhase);
            g.dispose();
        }

        private void paintLedger(Graphics2D g) {
            Color card = palette.surface();
            Color cardEdge = palette.cardEdge();
            g.setColor(new Color(card.getRed(), card.getGreen(), card.getBlue(), 210));
            g.fill(new RoundRectangle2D.Float(142, 34, 292, 292, 9, 9));
            g.setColor(cardEdge);
            g.setStroke(new BasicStroke(1.2f));
            g.draw(new RoundRectangle2D.Float(142, 34, 292, 292, 9, 9));
            drawText(g, "A", 164, 60, 13, Font.BOLD, palette.accent());
            drawText(g, "SAMPLE LEDGER / 01", 185, 59, 8, Font.PLAIN, palette.muted());
            drawText(g, "ILLUSTRATION", 368, 59, 7, Font.PLAIN, palette.muted());
            drawText(g, "GENERAL LEDGER", 163, 96, 7, Font.PLAIN, palette.muted());
            drawText(g, "OCTOBER / 2026", 355, 96, 7, Font.PLAIN, palette.muted());
            drawText(g, "Every entry,", 164, 123, 18, Font.BOLD, palette.text());
            drawText(g, "in balance.", 164, 144, 18, Font.BOLD, palette.text());

            g.setColor(palette.ledgerHighlight());
            g.fill(new RoundRectangle2D.Float(161, 158, 252, 46, 6, 6));
            drawText(g, "TRIAL BALANCE", 171, 173, 7, Font.PLAIN, palette.muted());
            drawText(g, "$8,800.00", 171, 192, 13, Font.BOLD, palette.accent());
            g.setColor(palette.button());
            g.fill(new RoundRectangle2D.Float(345, 174, 57, 20, 4, 4));
            drawText(g, "+ BALANCED", 351, 187, 6, Font.BOLD, Color.WHITE);

            drawText(g, "ACCOUNT TITLE", 164, 225, 6, Font.PLAIN, palette.muted());
            drawText(g, "DEBIT", 330, 225, 6, Font.PLAIN, palette.muted());
            drawText(g, "CREDIT", 382, 225, 6, Font.PLAIN, palette.muted());
            String[] accounts = {"Cash", "Supplies", "Service revenue", "Operating costs"};
            String[] debits = {"12,400", "2,300", "—", "4,100"};
            String[] credits = {"—", "—", "18,800", "—"};
            for (int i = 0; i < accounts.length; i++) {
                int y = 244 + i * 21;
                g.setColor(palette.rule());
                g.drawLine(162, y + 5, 413, y + 5);
                drawText(g, accounts[i], 165, y, 8, Font.PLAIN, palette.text());
                drawText(g, debits[i], 330, y, 7, Font.PLAIN, palette.muted());
                drawText(g, credits[i], 386, y, 7, Font.PLAIN, palette.muted());
            }
            drawText(g, "DEBIT = CREDIT", 163, 316, 6, Font.PLAIN, palette.muted());
            int[] heights = {6, 8, 7, 11, 10, 13, 12};
            for (int i = 0; i < heights.length; i++) {
                g.setColor(palette.accent());
                g.fillRoundRect(222 + i * 23, 318 - heights[i], 18, heights[i], 3, 3);
            }
            drawText(g, "01 / 04", 383, 316, 6, Font.PLAIN, palette.muted());
        }

        private void paintPerson(Graphics2D g, double phase) {
            g.setStroke(new BasicStroke(11, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
            g.setColor(new Color(205, 111, 82));
            g.drawLine(138, 250, 151, 211);
            double waveAngle = -Math.PI / 2 + Math.sin(phase * 1.8) * 0.42;
            int handX = 151 + (int) Math.round(Math.cos(waveAngle) * 34);
            int handY = 211 + (int) Math.round(Math.sin(waveAngle) * 34);
            g.drawLine(151, 211, handX, handY);
            g.setColor(new Color(225, 157, 112));
            g.fillOval(handX - 10, handY - 10, 20, 20);
            g.setColor(new Color(199, 106, 81));
            g.fillRoundRect(73, 228, 73, 105, 14, 14);
            g.setColor(new Color(227, 158, 113));
            g.fillOval(84, 167, 49, 62);
            g.setColor(new Color(67, 53, 43));
            g.fillOval(95, 190, 3, 4);
            g.fillOval(119, 190, 3, 4);
            g.setColor(new Color(163, 91, 69));
            g.drawArc(100, 197, 19, 14, 10, 160);
            g.setColor(new Color(239, 190, 153));
            g.setStroke(new BasicStroke(2));
            g.drawLine(101, 249, 121, 260);
            g.drawLine(121, 260, 121, 246);
        }

        private void paintSpeechBubble(Graphics2D g, double phase) {
            g.setColor(palette.surface());
            g.fill(new RoundRectangle2D.Float(55, 119, 111, 39, 8, 8));
            g.fillPolygon(new int[]{145, 157, 148}, new int[]{156, 166, 154}, 3);
            g.setColor(palette.cardEdge());
            g.draw(new RoundRectangle2D.Float(55, 119, 111, 39, 8, 8));
            drawText(g, Math.sin(phase) >= 0 ? "Hello!" : "Welcome!", 66, 143, 11, Font.BOLD, palette.text());
        }

        private void drawText(Graphics2D g, String text, int x, int y, int size, int style, Color color) {
            g.setColor(color);
            g.setFont(new Font(Font.SANS_SERIF, style, size));
            g.drawString(text, x, y);
        }
    }

    private record Palette(
            Color background,
            Color surface,
            Color field,
            Color text,
            Color muted,
            Color accent,
            Color button,
            Color banner,
            Color bannerText,
            Color fieldBorder,
            Color cardEdge,
            Color ledgerHighlight,
            Color rule) {
        private static Palette dark() {
            return new Palette(DARK_BACKGROUND, DARK_SURFACE, DARK_FIELD, DARK_TEXT, DARK_MUTED,
                    DARK_ACCENT, DARK_BUTTON, new Color(34, 58, 46), new Color(197, 220, 201),
                    new Color(74, 92, 101),
                    new Color(48, 68, 57), new Color(37, 58, 47), new Color(48, 65, 54));
        }

        private static Palette light() {
            return new Palette(LIGHT_BACKGROUND, LIGHT_SURFACE, LIGHT_FIELD, LIGHT_TEXT, LIGHT_MUTED,
                    LIGHT_ACCENT, LIGHT_BUTTON, new Color(225, 239, 227), new Color(43, 92, 59),
                    new Color(197, 210, 201),
                    new Color(216, 225, 216), new Color(234, 243, 235), new Color(224, 231, 222));
        }
    }
}
