package accountingworksheet;

import java.security.GeneralSecurityException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class UserRepository {
    public boolean hasUsers() throws SQLException {
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement("SELECT EXISTS(SELECT 1 FROM users)");
             ResultSet results = statement.executeQuery()) {
            return results.next() && results.getInt(1) == 1;
        }
    }

    public void createUser(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Enter an email address or username.");
        }
        if (password.length < 8) {
            throw new IllegalArgumentException("Use a password with at least 8 characters.");
        }

        PasswordHasher.HashedPassword hashedPassword = PasswordHasher.hash(password);
        try (Connection connection = Database.connect()) {
            connection.setAutoCommit(false);
            try (PreparedStatement insert = connection.prepareStatement(
                    "INSERT INTO users(username, password_salt, password_hash, role) VALUES (?, ?, ?, 'USER')")) {
                insert.setString(1, normalizedUsername);
                insert.setString(2, hashedPassword.salt());
                insert.setString(3, hashedPassword.hash());
                insert.executeUpdate();
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                if (exception.getErrorCode() == 19) {
                    throw new IllegalArgumentException(
                            "An account with this email address or username already exists.", exception);
                }
                throw exception;
            } catch (RuntimeException exception) {
                connection.rollback();
                throw exception;
            }
        }
    }

    public void createAdmin(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        String normalizedUsername = normalizeUsername(username);
        if (normalizedUsername.isBlank()) {
            throw new IllegalArgumentException("Enter an email address or username.");
        }
        if (password.length < 8) {
            throw new IllegalArgumentException("Use a password with at least 8 characters.");
        }

        PasswordHasher.HashedPassword hashedPassword = PasswordHasher.hash(password);
        try (Connection connection = Database.connect();
             PreparedStatement insert = connection.prepareStatement(
                     "INSERT INTO users(username, password_salt, password_hash, role) "
                             + "VALUES (?, ?, ?, 'ADMIN')")) {
            insert.setString(1, normalizedUsername);
            insert.setString(2, hashedPassword.salt());
            insert.setString(3, hashedPassword.hash());
            insert.executeUpdate();
        } catch (SQLException exception) {
            if (exception.getErrorCode() == 19) {
                throw new IllegalArgumentException(
                        "An account with this email address or username already exists.", exception);
            }
            throw exception;
        }
    }

    public User authenticate(String username, char[] password)
            throws SQLException, GeneralSecurityException {
        String normalizedUsername = normalizeUsername(username);
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, username, password_salt, password_hash, role FROM users WHERE username = ?")) {
            statement.setString(1, normalizedUsername);
            try (ResultSet results = statement.executeQuery()) {
                if (!results.next()) {
                    return null;
                }
                if (!PasswordHasher.verify(password, results.getString("password_salt"),
                        results.getString("password_hash"))) {
                    return null;
                }
                return new User(results.getLong("id"), results.getString("username"),
                        "ADMIN".equals(results.getString("role")));
            }
        }
    }

    public List<UserSummary> listUsers(User requester) throws SQLException {
        requireAdmin(requester);
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT id, username, role, created_at FROM users ORDER BY username");
             ResultSet results = statement.executeQuery()) {
            List<UserSummary> users = new ArrayList<>();
            while (results.next()) {
                users.add(new UserSummary(results.getLong("id"), results.getString("username"),
                        results.getString("role"), results.getString("created_at")));
            }
            return users;
        }
    }

    static void requireAdmin(User requester) throws SQLException {
        if (requester == null || !requester.admin()) {
            throw new SQLException("Administrator access is required.");
        }
        try (Connection connection = Database.connect();
             PreparedStatement statement = connection.prepareStatement(
                     "SELECT role FROM users WHERE id = ?")) {
            statement.setLong(1, requester.id());
            try (ResultSet result = statement.executeQuery()) {
                if (!result.next() || !"ADMIN".equals(result.getString("role"))) {
                    throw new SQLException("Administrator access is required.");
                }
            }
        }
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim().toLowerCase(Locale.ROOT);
    }

    public record User(long id, String username, boolean admin) {
    }

    public record UserSummary(long id, String username, String role, String createdAt) {
        @Override
        public String toString() {
            return username + "  [" + role + "]  " + createdAt;
        }
    }
}
