package accountingworksheet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class WorksheetRepository {
    public long save(long userId, String title, List<Worksheet.Entry> entries) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                long worksheetId;
                try (PreparedStatement insertWorksheet = connection.prepareStatement(
                        "INSERT INTO worksheets(user_id, title) VALUES (?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insertWorksheet.setLong(1, userId);
                    insertWorksheet.setString(2, title);
                    insertWorksheet.executeUpdate();
                    try (ResultSet keys = insertWorksheet.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The database did not return the saved worksheet ID.");
                        }
                        worksheetId = keys.getLong(1);
                    }
                }

                try (PreparedStatement insertEntry = connection.prepareStatement(
                        "INSERT INTO worksheet_entries"
                                + "(worksheet_id, line_number, entry_date, account_name, debit, credit) "
                                + "VALUES (?, ?, ?, ?, ?, ?)")) {
                    for (int index = 0; index < entries.size(); index++) {
                        Worksheet.Entry entry = entries.get(index);
                        insertEntry.setLong(1, worksheetId);
                        insertEntry.setInt(2, index + 1);
                        insertEntry.setString(3, entry.date());
                        insertEntry.setString(4, entry.accountName());
                        insertEntry.setString(5, entry.debit().toPlainString());
                        insertEntry.setString(6, entry.credit().toPlainString());
                        insertEntry.addBatch();
                    }
                    insertEntry.executeBatch();
                }
                connection.commit();
                return worksheetId;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public List<WorksheetSummary> listForUser(long userId) throws SQLException {
        String sql = "SELECT id, title, created_at FROM worksheets WHERE user_id = ? "
                + "ORDER BY created_at DESC, id DESC";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                List<WorksheetSummary> summaries = new ArrayList<>();
                while (results.next()) {
                    summaries.add(new WorksheetSummary(
                            results.getLong("id"),
                            results.getString("title"),
                            results.getString("created_at")));
                }
                return summaries;
            }
        }
    }

    public List<AdminWorksheetSummary> listAllForAdmin(UserRepository.User requester) throws SQLException {
        UserRepository.requireAdmin(requester);
        String sql = "SELECT worksheets.id, worksheets.title, worksheets.created_at, users.username "
                + "FROM worksheets JOIN users ON users.id = worksheets.user_id "
                + "ORDER BY worksheets.created_at DESC, worksheets.id DESC";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet results = statement.executeQuery()) {
            List<AdminWorksheetSummary> summaries = new ArrayList<>();
            while (results.next()) {
                summaries.add(new AdminWorksheetSummary(results.getLong("id"), results.getString("title"),
                        results.getString("username"), results.getString("created_at")));
            }
            return summaries;
        }
    }

    public Worksheet load(long userId, long worksheetId) throws SQLException {
        return load(worksheetId, "SELECT title FROM worksheets WHERE id = ? AND user_id = ?", userId);
    }

    public Worksheet loadForAdmin(UserRepository.User requester, long worksheetId) throws SQLException {
        UserRepository.requireAdmin(requester);
        return load(worksheetId, "SELECT title FROM worksheets WHERE id = ?", null);
    }

    private Worksheet load(long worksheetId, String headerSql, Long userId) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement header = connection.prepareStatement(headerSql)) {
            header.setLong(1, worksheetId);
            if (userId != null) {
                header.setLong(2, userId);
            }
            try (ResultSet result = header.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The selected worksheet was not found for this user.");
                }
                String title = result.getString("title");
                List<Worksheet.Entry> entries = new ArrayList<>();
                try (PreparedStatement lines = connection.prepareStatement(
                        "SELECT entry_date, account_name, debit, credit FROM worksheet_entries "
                                + "WHERE worksheet_id = ? ORDER BY line_number")) {
                    lines.setLong(1, worksheetId);
                    try (ResultSet entryResults = lines.executeQuery()) {
                        while (entryResults.next()) {
                            String date = entryResults.getString("entry_date");
                            entries.add(new Worksheet.Entry(
                                    date == null ? "" : date,
                                    entryResults.getString("account_name"),
                                    new BigDecimal(entryResults.getString("debit")),
                                    new BigDecimal(entryResults.getString("credit"))));
                        }
                    }
                }
                return new Worksheet(worksheetId, title, entries);
            }
        }
    }

    public record WorksheetSummary(long id, String title, String createdAt) {
        @Override
        public String toString() {
            return title + "  (" + createdAt + ")";
        }
    }

    public record AdminWorksheetSummary(long id, String title, String owner, String createdAt) {
        @Override
        public String toString() {
            return title + " — " + owner + "  (" + createdAt + ")";
        }
    }
}
