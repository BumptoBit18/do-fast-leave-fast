package server.dao;

import server.model.BidTransaction;
import server.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class BidTransactionDAO {
    public BidTransactionDAO() {
    }

    public List<BidTransaction> loadAll() {
        DatabaseManager.initialize();
        List<BidTransaction> transactions = new ArrayList<>();
        String sql = """
                select transaction_type, actor_username, reference_id, description, amount, event_time
                from transactions
                order by sort_order asc, event_time desc
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                transactions.add(new BidTransaction(
                        result.getString("transaction_type"),
                        result.getString("actor_username"),
                        result.getString("reference_id"),
                        result.getString("description"),
                        result.getDouble("amount"),
                        result.getTimestamp("event_time").toLocalDateTime()
                ));
            }
            return transactions;
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai transactions tu PostgreSQL.", ex);
        }
    }

    public void insert(BidTransaction transaction) {
        DatabaseManager.initialize();
        String sql = """
                insert into transactions (
                    transaction_type, actor_username, reference_id, description, amount, event_time, sort_order
                ) values (?, ?, ?, ?, ?, ?, (
                    select coalesce(min(sort_order), 0) - 1
                    from transactions
                ))
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, transaction.getType());
            statement.setString(2, transaction.getActorUsername());
            statement.setString(3, transaction.getReferenceId());
            statement.setString(4, transaction.getDescription());
            statement.setDouble(5, transaction.getAmount());
            statement.setTimestamp(6, Timestamp.valueOf(transaction.getTime()));
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the them transaction vao PostgreSQL.", ex);
        }
    }

    public void saveAll(List<BidTransaction> transactions) {
        DatabaseManager.initialize();
        String deleteSql = "delete from transactions";
        String insertSql = """
                insert into transactions (
                    transaction_type, actor_username, reference_id, description, amount, event_time, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                deleteStatement.executeUpdate();

                for (int index = 0; index < transactions.size(); index++) {
                    BidTransaction transaction = transactions.get(index);
                    insertStatement.setString(1, transaction.getType());
                    insertStatement.setString(2, transaction.getActorUsername());
                    insertStatement.setString(3, transaction.getReferenceId());
                    insertStatement.setString(4, transaction.getDescription());
                    insertStatement.setDouble(5, transaction.getAmount());
                    insertStatement.setTimestamp(6, Timestamp.valueOf(transaction.getTime()));
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
            throw new IllegalStateException("Khong the luu transactions vao PostgreSQL.", ex);
        }
    }
}
