package accountingworksheet;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public final class AdjustmentWorksheetRepository {
    public long save(long userId, String title, List<AdjustmentWorksheet.Entry> entries) throws SQLException {
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try {
                long worksheetId;
                try (PreparedStatement insert = connection.prepareStatement(
                        "INSERT INTO adjustment_worksheets(user_id, title) VALUES (?, ?)",
                        PreparedStatement.RETURN_GENERATED_KEYS)) {
                    insert.setLong(1, userId);
                    insert.setString(2, title);
                    insert.executeUpdate();
                    try (ResultSet keys = insert.getGeneratedKeys()) {
                        if (!keys.next()) {
                            throw new SQLException("The database did not return the saved worksheet ID.");
                        }
                        worksheetId = keys.getLong(1);
                    }
                }

                try (PreparedStatement insertEntry = connection.prepareStatement(
                        "INSERT INTO adjustment_entries"
                                + "(worksheet_id, line_number, account_name, account_type, trial_debit, "
                                + "trial_credit, adjustment_debit, adjustment_credit) "
                                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?)")) {
                    for (int index = 0; index < entries.size(); index++) {
                        AdjustmentWorksheet.Entry entry = entries.get(index);
                        insertEntry.setLong(1, worksheetId);
                        insertEntry.setInt(2, index + 1);
                        insertEntry.setString(3, entry.accountName());
                        insertEntry.setString(4, entry.accountType().name());
                        insertEntry.setString(5, entry.trialBalanceDebit().toPlainString());
                        insertEntry.setString(6, entry.trialBalanceCredit().toPlainString());
                        insertEntry.setString(7, entry.adjustmentDebit().toPlainString());
                        insertEntry.setString(8, entry.adjustmentCredit().toPlainString());
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
        String sql = "SELECT id, title, created_at FROM adjustment_worksheets WHERE user_id = ? "
                + "ORDER BY created_at DESC, id DESC";
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setLong(1, userId);
            try (ResultSet results = statement.executeQuery()) {
                List<WorksheetSummary> summaries = new ArrayList<>();
                while (results.next()) {
                    summaries.add(new WorksheetSummary(results.getLong("id"),
                            results.getString("title"), results.getString("created_at")));
                }
                return summaries;
            }
        }
    }

    public List<AdminWorksheetSummary> listAllForAdmin(UserRepository.User requester) throws SQLException {
        UserRepository.requireAdmin(requester);
        String sql = "SELECT adjustment_worksheets.id, adjustment_worksheets.title, "
                + "adjustment_worksheets.created_at, users.username "
                + "FROM adjustment_worksheets JOIN users ON users.id = adjustment_worksheets.user_id "
                + "ORDER BY adjustment_worksheets.created_at DESC, adjustment_worksheets.id DESC";
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

    public AdjustmentWorksheet load(long userId, long worksheetId) throws SQLException {
        return load(worksheetId, "SELECT title FROM adjustment_worksheets WHERE id = ? AND user_id = ?", userId);
    }

    public AdjustmentWorksheet loadForAdmin(UserRepository.User requester, long worksheetId) throws SQLException {
        UserRepository.requireAdmin(requester);
        return load(worksheetId, "SELECT title FROM adjustment_worksheets WHERE id = ?", null);
    }

    private AdjustmentWorksheet load(long worksheetId, String sql, Long userId) throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement header = connection.prepareStatement(sql)) {
            header.setLong(1, worksheetId);
            if (userId != null) {
                header.setLong(2, userId);
            }
            try (ResultSet result = header.executeQuery()) {
                if (!result.next()) {
                    throw new SQLException("The selected worksheet was not found for this user.");
                }
                String title = result.getString("title");
                List<AdjustmentWorksheet.Entry> entries = new ArrayList<>();
                try (PreparedStatement lines = connection.prepareStatement(
                        "SELECT account_name, account_type, trial_debit, trial_credit, "
                                + "adjustment_debit, adjustment_credit FROM adjustment_entries "
                                + "WHERE worksheet_id = ? ORDER BY line_number")) {
                    lines.setLong(1, worksheetId);
                    try (ResultSet entryResults = lines.executeQuery()) {
                        while (entryResults.next()) {
                            entries.add(new AdjustmentWorksheet.Entry(
                                    entryResults.getString("account_name"),
                                    AdjustmentWorksheet.AccountType.valueOf(entryResults.getString("account_type")),
                                    new BigDecimal(entryResults.getString("trial_debit")),
                                    new BigDecimal(entryResults.getString("trial_credit")),
                                    new BigDecimal(entryResults.getString("adjustment_debit")),
                                    new BigDecimal(entryResults.getString("adjustment_credit"))));
                        }
                    }
                }
                return new AdjustmentWorksheet(worksheetId, title, entries);
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
