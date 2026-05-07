package server.dao;

import server.model.PaymentRecord;
import server.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;

public class PaymentDAO {
    public PaymentDAO() {
    }

    public List<PaymentRecord> loadAll() {
        DatabaseManager.initialize();
        List<PaymentRecord> payments = new ArrayList<>();
        String sql = """
                select auction_id, buyer_username, seller_username, amount, paid_at
                from payments
                order by sort_order asc, paid_at desc
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet result = statement.executeQuery()) {
            while (result.next()) {
                payments.add(new PaymentRecord(
                        result.getString("auction_id"),
                        result.getString("buyer_username"),
                        result.getString("seller_username"),
                        result.getDouble("amount"),
                        result.getTimestamp("paid_at").toLocalDateTime()
                ));
            }
            return payments;
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai payments tu PostgreSQL.", ex);
        }
    }

    public void insert(PaymentRecord payment) {
        DatabaseManager.initialize();
        String sql = """
                insert into payments (
                    auction_id, buyer_username, seller_username, amount, paid_at, sort_order
                ) values (?, ?, ?, ?, ?, (
                    select coalesce(min(sort_order), 0) - 1
                    from payments
                ))
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, payment.getAuctionId());
            statement.setString(2, payment.getBuyerUsername());
            statement.setString(3, payment.getSellerUsername());
            statement.setDouble(4, payment.getAmount());
            statement.setTimestamp(5, Timestamp.valueOf(payment.getPaidAt()));
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the them payment vao PostgreSQL.", ex);
        }
    }

    public void saveAll(List<PaymentRecord> payments) {
        DatabaseManager.initialize();
        String deleteSql = "delete from payments";
        String insertSql = """
                insert into payments (
                    auction_id, buyer_username, seller_username, amount, paid_at, sort_order
                ) values (?, ?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                deleteStatement.executeUpdate();

                for (int index = 0; index < payments.size(); index++) {
                    PaymentRecord payment = payments.get(index);
                    insertStatement.setString(1, payment.getAuctionId());
                    insertStatement.setString(2, payment.getBuyerUsername());
                    insertStatement.setString(3, payment.getSellerUsername());
                    insertStatement.setDouble(4, payment.getAmount());
                    insertStatement.setTimestamp(5, Timestamp.valueOf(payment.getPaidAt()));
                    insertStatement.setInt(6, index);
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
            throw new IllegalStateException("Khong the luu payments vao PostgreSQL.", ex);
        }
    }
}
