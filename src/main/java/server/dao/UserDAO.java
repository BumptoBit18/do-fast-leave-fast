package server.dao;

import server.model.entity.Admin;
import server.model.entity.Bidder;
import server.model.entity.Seller;
import server.model.entity.User;
import server.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class UserDAO {
    public UserDAO() {
    }

    public List<User> loadAll() {
        DatabaseManager.initialize();
        List<User> users = new ArrayList<>();
        String sql = """
                select id, username, password, role, full_name, wallet_balance
                from users
                order by sort_order asc, username asc
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                users.add(mapUser(result));
            }
            return users;
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai danh sach users tu PostgreSQL.", ex);
        }
    }

    public User findByUsername(String username) {
        DatabaseManager.initialize();
        String sql = """
                select id, username, password, role, full_name, wallet_balance
                from users
                where lower(username) = lower(?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapUser(result);
                }
                return null;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai user theo username tu PostgreSQL.", ex);
        }
    }

    public User findByCredentials(String username, String password, String role) {
        DatabaseManager.initialize();
        String sql = """
                select id, username, password, role, full_name, wallet_balance
                from users
                where lower(username) = lower(?)
                  and password = ?
                  and lower(role) = lower(?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            statement.setString(2, password);
            statement.setString(3, role);
            try (ResultSet result = statement.executeQuery()) {
                if (result.next()) {
                    return mapUser(result);
                }
                return null;
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai user theo thong tin dang nhap tu PostgreSQL.", ex);
        }
    }

    public boolean existsByUsername(String username) {
        DatabaseManager.initialize();
        String sql = """
                select 1
                from users
                where lower(username) = lower(?)
                limit 1
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                return result.next();
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the kiem tra username trong PostgreSQL.", ex);
        }
    }

    public void insert(User user) {
        DatabaseManager.initialize();
        String sql = """
                insert into users (id, username, password, role, full_name, wallet_balance, sort_order)
                values (?, ?, ?, ?, ?, ?, (
                    select coalesce(max(sort_order), -1) + 1
                    from users
                ))
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, user.getId());
            statement.setString(2, user.getUsername());
            statement.setString(3, user.getPassword());
            statement.setString(4, user.getRole());
            statement.setString(5, user.getFullName());
            statement.setDouble(6, user.getWalletBalance());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the them user vao PostgreSQL.", ex);
        }
    }

    public void updateWalletBalance(String username, double walletBalance) {
        DatabaseManager.initialize();
        String sql = """
                update users
                set wallet_balance = ?
                where lower(username) = lower(?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDouble(1, walletBalance);
            statement.setString(2, username);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the cap nhat vi user trong PostgreSQL.", ex);
        }
    }

    public void saveAll(List<User> users) {
        DatabaseManager.initialize();
        String deleteSql = "delete from users";
        String insertSql = """
                insert into users (id, username, password, role, full_name, wallet_balance, sort_order)
                values (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                deleteStatement.executeUpdate();

                for (int index = 0; index < users.size(); index++) {
                    User user = users.get(index);
                    insertStatement.setString(1, user.getId());
                    insertStatement.setString(2, user.getUsername());
                    insertStatement.setString(3, user.getPassword());
                    insertStatement.setString(4, user.getRole());
                    insertStatement.setString(5, user.getFullName());
                    insertStatement.setDouble(6, user.getWalletBalance());
                    insertStatement.setInt(7, index);
                    insertStatement.addBatch();
                }

                insertStatement.executeBatch();
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the luu users vao PostgreSQL.", ex);
        }
    }

    private User mapUser(ResultSet result) throws Exception {
        String role = result.getString("role").toUpperCase(Locale.ROOT);
        return switch (role) {
            case "ADMIN" -> new Admin(
                    result.getString("id"),
                    result.getString("username"),
                    result.getString("password"),
                    result.getString("full_name"),
                    result.getDouble("wallet_balance")
            );
            case "SELLER" -> new Seller(
                    result.getString("id"),
                    result.getString("username"),
                    result.getString("password"),
                    result.getString("full_name"),
                    result.getDouble("wallet_balance")
            );
            case "BIDDER" -> new Bidder(
                    result.getString("id"),
                    result.getString("username"),
                    result.getString("password"),
                    result.getString("full_name"),
                    result.getDouble("wallet_balance")
            );
            default -> throw new IllegalStateException("Role khong hop le trong database: " + role);
        };
    }
}
