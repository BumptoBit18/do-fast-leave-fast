package server.dao;

import server.model.NotificationRecord;
import server.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class NotificationDAO {
    public NotificationDAO() {
    }

    public List<NotificationRecord> loadAll() {
        DatabaseManager.initialize();
        List<NotificationRecord> notifications = new ArrayList<>();
        String sql = """
                select username, title, message, event_time
                from notifications
                order by sort_order asc, event_time desc
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                notifications.add(new NotificationRecord(
                        result.getString("username"),
                        result.getString("title"),
                        result.getString("message"),
                        result.getTimestamp("event_time").toLocalDateTime()
                ));
            }
            return notifications;
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai notifications tu PostgreSQL.", ex);
        }
    }

    public List<NotificationRecord> loadForUser(String username) {
        DatabaseManager.initialize();
        List<NotificationRecord> notifications = new ArrayList<>();
        String sql = """
                select username, title, message, event_time
                from notifications
                where lower(username) = lower(?) or lower(username) = 'all'
                order by sort_order asc, event_time desc
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, username);
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    notifications.add(new NotificationRecord(
                            result.getString("username"),
                            result.getString("title"),
                            result.getString("message"),
                            result.getTimestamp("event_time").toLocalDateTime()
                    ));
                }
            }
            return notifications;
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai notifications theo user tu PostgreSQL.", ex);
        }
    }

    public void insert(NotificationRecord notification) {
        DatabaseManager.initialize();
        String sql = """
                insert into notifications (
                    username, title, message, event_time, sort_order
                ) values (?, ?, ?, ?, (
                    select coalesce(min(sort_order), 0) - 1
                    from notifications
                ))
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, notification.getUsername());
            statement.setString(2, notification.getTitle());
            statement.setString(3, notification.getMessage());
            statement.setTimestamp(4, Timestamp.valueOf(notification.getTime()));
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the them notification vao PostgreSQL.", ex);
        }
    }

    public void saveAll(List<NotificationRecord> notifications) {
        DatabaseManager.initialize();
        String deleteSql = "delete from notifications";
        String insertSql = """
                insert into notifications (
                    username, title, message, event_time, sort_order
                ) values (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                deleteStatement.executeUpdate();

                for (int index = 0; index < notifications.size(); index++) {
                    NotificationRecord notification = notifications.get(index);
                    insertStatement.setString(1, notification.getUsername());
                    insertStatement.setString(2, notification.getTitle());
                    insertStatement.setString(3, notification.getMessage());
                    insertStatement.setTimestamp(4, Timestamp.valueOf(notification.getTime()));
                    insertStatement.setInt(5, index);
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
            throw new IllegalStateException("Khong the luu notifications vao PostgreSQL.", ex);
        }
    }
}
