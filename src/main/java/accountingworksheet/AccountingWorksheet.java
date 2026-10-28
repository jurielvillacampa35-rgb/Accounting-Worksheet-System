package accountingworksheet;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Font;
import java.io.Console;
import java.security.GeneralSecurityException;
import java.sql.SQLException;
import java.util.Arrays;

public final class AccountingWorksheet {
    private AccountingWorksheet() {
    }

    public static void main(String[] args) {
        if (args.length > 0 && "--create-admin".equals(args[0])) {
            createAdminFromTerminal(args);
            return;
        }
        System.setProperty("swing.aatext", "true");
        System.setProperty("awt.useSystemAAFontSettings", "on");
        try {
            Database.initialize();
        } catch (SQLException | RuntimeException exception) {
            System.err.println("Could not initialize the accounting database: " + exception.getMessage());
            exception.printStackTrace();
            return;
        }

        SwingUtilities.invokeLater(() -> {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception exception) {
                System.err.println("Could not apply the system look and feel: " + exception.getMessage());
            }
            Font readableFont = new Font("Segoe UI", Font.PLAIN, 15);
            UIManager.put("Label.font", readableFont);
            UIManager.put("Button.font", readableFont);
            UIManager.put("ToggleButton.font", readableFont);
            UIManager.put("TextField.font", readableFont);
            UIManager.put("PasswordField.font", readableFont);
            UIManager.put("ComboBox.font", readableFont);
            UIManager.put("List.font", readableFont);
            UIManager.put("OptionPane.messageFont", readableFont);
            UIManager.put("OptionPane.buttonFont", readableFont);
            UIManager.put("ToolTip.font", readableFont);
            UIManager.put("Table.font", readableFont);
            UIManager.put("TableHeader.font", readableFont);
            UIManager.put("TabbedPane.font", readableFont);
            new LoginFrame().setVisible(true);
        });
    }

    private static void createAdminFromTerminal(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: mvn exec:java -Dexec.args=\"--create-admin <username>\"");
            return;
        }
        char[] password = null; 
        char[] confirmation = null;
        try {
            Database.initialize();
            Console console = System.console();
            if (console == null) {
                throw new IllegalStateException(
                        "A terminal console is required so the admin password is not echoed. "
                                + "Run this command directly in a terminal.");
            }
            password = console.readPassword("Admin password (at least 8 characters): ");
            confirmation = console.readPassword("Confirm admin password: ");
            if (!Arrays.equals(password, confirmation)) {
                throw new IllegalArgumentException("The passwords do not match.");
            }
            new UserRepository().createAdmin(args[1], password);
            console.printf("Admin account created for %s.%n", args[1]);
        } catch (SQLException | GeneralSecurityException | IllegalArgumentException
                 | IllegalStateException exception) {
            System.err.println("Could not create admin account: " + exception.getMessage());
        } finally {
            if (password != null) {
                Arrays.fill(password, '\0');
            }
            if (confirmation != null) {
                Arrays.fill(confirmation, '\0');
            }
        }
    }
}
