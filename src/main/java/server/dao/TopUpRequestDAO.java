package server.dao;

import server.model.TopUpRequestRecord;
import server.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class TopUpRequestDAO {
    public TopUpRequestDAO() {
    }

    public List<TopUpRequestRecord> loadAll() {
        DatabaseManager.initialize();
        List<TopUpRequestRecord> requests = new ArrayList<>();
        String sql = """
                select id, username, amount, bank_name, account_name, account_number,
                       requested_at, status, approved_at, approved_by, credited_at
                from top_up_requests
                order by sort_order asc, requested_at desc
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                requests.add(new TopUpRequestRecord(
                        result.getString("id"),
                        result.getString("username"),
                        result.getDouble("amount"),
                        result.getString("bank_name"),
                        result.getString("account_name"),
                        result.getString("account_number"),
                        result.getTimestamp("requested_at").toLocalDateTime(),
                        result.getString("status"),
                        toLocalDateTime(result.getTimestamp("approved_at")),
                        result.getString("approved_by"),
                        toLocalDateTime(result.getTimestamp("credited_at"))
                ));
            }
            return requests;
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai top-up requests tu PostgreSQL.", ex);
        }
    }

    public void insert(TopUpRequestRecord request) {
        DatabaseManager.initialize();
        String sql = """
                insert into top_up_requests (
                    id, username, amount, bank_name, account_name, account_number,
                    requested_at, status, approved_at, approved_by, credited_at, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (
                    select coalesce(min(sort_order), 0) - 1
                    from top_up_requests
                ))
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, request.getId());
            statement.setString(2, request.getUsername());
            statement.setDouble(3, request.getAmount());
            statement.setString(4, request.getBankName());
            statement.setString(5, request.getAccountName());
            statement.setString(6, request.getAccountNumber());
            statement.setTimestamp(7, Timestamp.valueOf(request.getRequestedAt()));
            statement.setString(8, request.getStatus());
            setTimestampOrNull(statement, 9, request.getApprovedAt());
            statement.setString(10, request.getApprovedBy());
            setTimestampOrNull(statement, 11, request.getCreditedAt());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the them top-up request vao PostgreSQL.", ex);
        }
    }

    public void markApproved(String requestId, String approvedBy, LocalDateTime approvedAt) {
        DatabaseManager.initialize();
        String sql = """
                update top_up_requests
                set status = 'APPROVED',
                    approved_at = ?,
                    approved_by = ?
                where lower(id) = lower(?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(approvedAt));
            statement.setString(2, approvedBy);
            statement.setString(3, requestId);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the duyet top-up request trong PostgreSQL.", ex);
        }
    }

    public void markCredited(String requestId, LocalDateTime creditedAt) {
        DatabaseManager.initialize();
        String sql = """
                update top_up_requests
                set status = 'CREDITED',
                    credited_at = ?
                where lower(id) = lower(?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setTimestamp(1, Timestamp.valueOf(creditedAt));
            statement.setString(2, requestId);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the cong tien top-up request trong PostgreSQL.", ex);
        }
    }

    public void saveAll(List<TopUpRequestRecord> requests) {
        DatabaseManager.initialize();
        String deleteSql = "delete from top_up_requests";
        String insertSql = """
                insert into top_up_requests (
                    id, username, amount, bank_name, account_name, account_number,
                    requested_at, status, approved_at, approved_by, credited_at, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                deleteStatement.executeUpdate();

                for (int index = 0; index < requests.size(); index++) {
                    TopUpRequestRecord request = requests.get(index);
                    insertStatement.setString(1, request.getId());
                    insertStatement.setString(2, request.getUsername());
                    insertStatement.setDouble(3, request.getAmount());
                    insertStatement.setString(4, request.getBankName());
                    insertStatement.setString(5, request.getAccountName());
                    insertStatement.setString(6, request.getAccountNumber());
                    insertStatement.setTimestamp(7, Timestamp.valueOf(request.getRequestedAt()));
                    insertStatement.setString(8, request.getStatus());
                    setTimestampOrNull(insertStatement, 9, request.getApprovedAt());
                    insertStatement.setString(10, request.getApprovedBy());
                    setTimestampOrNull(insertStatement, 11, request.getCreditedAt());
                    insertStatement.setInt(12, index);
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
            throw new IllegalStateException("Khong the luu top-up requests vao PostgreSQL.", ex);
        }
    }

    private void setTimestampOrNull(PreparedStatement statement, int index, LocalDateTime value) throws Exception {
        if (value == null) {
            statement.setTimestamp(index, null);
            return;
        }
        statement.setTimestamp(index, Timestamp.valueOf(value));
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }
}
