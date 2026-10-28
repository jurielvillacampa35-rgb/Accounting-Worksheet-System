package accountingworksheet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class Database {
    private static final String URL = "jdbc:sqlite:accounting_worksheet.db";

    private Database() {
    }

    public static Connection connect() throws SQLException {
        Connection connection = DriverManager.getConnection(URL);
        try (Statement statement = connection.createStatement()) {
            statement.execute("PRAGMA foreign_keys = ON");
        }
        return connection;
    }

    public static void initialize() throws SQLException {
        String schema = readSchema();
        try (Connection connection = connect(); Statement statement = connection.createStatement()) {
            for (String command : schema.split(";")) {
                String sql = command.trim();
                if (!sql.isEmpty()) {
                    statement.execute(sql);
                }
            }
            boolean hasRoleColumn = false;
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(users)")) {
                while (columns.next()) {
                    if ("role".equalsIgnoreCase(columns.getString("name"))) {
                        hasRoleColumn = true;
                        break;
                    }
                }
            }
            if (!hasRoleColumn) {
                statement.execute("ALTER TABLE users ADD COLUMN role TEXT NOT NULL DEFAULT 'USER' "
                        + "CHECK (role IN ('USER', 'ADMIN'))");
            }
            boolean hasEntryDateColumn = false;
            try (ResultSet columns = statement.executeQuery("PRAGMA table_info(worksheet_entries)")) {
                while (columns.next()) {
                    if ("entry_date".equalsIgnoreCase(columns.getString("name"))) {
                        hasEntryDateColumn = true;
                        break;
                    }
                }
            }
            if (!hasEntryDateColumn) {
                statement.execute("ALTER TABLE worksheet_entries ADD COLUMN entry_date TEXT NOT NULL DEFAULT ''");
            }
        }
    }

    private static String readSchema() throws SQLException {
        try (InputStream stream = Database.class.getResourceAsStream("/schema.sql")) {
            if (stream == null) {
                throw new SQLException("The database schema resource /schema.sql is missing.");
            }
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                StringBuilder schema = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) {
                    schema.append(line).append('\n');
                }
                return schema.toString();
            }
        } catch (IOException exception) {
            throw new SQLException("Could not read the database schema.", exception);
        }
    }
}
