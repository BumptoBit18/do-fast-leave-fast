package server.dao;

import server.controller.ItemController;
import server.model.Auction;
import server.model.AutoBid;
import server.model.BidTransaction;
import server.model.item.Item;
import server.util.DatabaseManager;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AuctionDAO {
    private final ItemController itemController = new ItemController();

    public AuctionDAO() {
    }

    public List<Auction> loadAll() {
        DatabaseManager.initialize();
        String auctionSql = """
                select id, seller_username, item_id, item_category, item_name, item_description,
                       item_starting_price, item_end_time, item_image_hint, cancelled, paid,
                       anti_snipe_triggered, close_notified
                from auctions
                order by sort_order asc, id asc
                """;
        return loadAuctionsByQuery(auctionSql, statement -> {
        });
    }

    public Auction findById(String auctionId) {
        DatabaseManager.initialize();
        String auctionSql = """
                select id, seller_username, item_id, item_category, item_name, item_description,
                       item_starting_price, item_end_time, item_image_hint, cancelled, paid,
                       anti_snipe_triggered, close_notified
                from auctions
                where lower(id) = lower(?)
                """;
        List<Auction> auctions = loadAuctionsByQuery(auctionSql, statement -> statement.setString(1, auctionId));
        return auctions.isEmpty() ? null : auctions.get(0);
    }

    public List<Auction> search(String keyword, String category) {
        DatabaseManager.initialize();
        String normalizedKeyword = keyword == null ? "" : keyword.trim().toLowerCase();
        String normalizedCategory = category == null ? "Tat ca" : category;

        StringBuilder sql = new StringBuilder("""
                select id, seller_username, item_id, item_category, item_name, item_description,
                       item_starting_price, item_end_time, item_image_hint, cancelled, paid,
                       anti_snipe_triggered, close_notified
                from auctions
                where 1=1
                """);

        List<String> params = new ArrayList<>();
        if (!normalizedKeyword.isBlank()) {
            sql.append("""
                     and (
                        lower(item_name) like ?
                        or lower(item_description) like ?
                        or lower(seller_username) like ?
                     )
                    """);
            String pattern = "%" + normalizedKeyword + "%";
            params.add(pattern);
            params.add(pattern);
            params.add(pattern);
        }

        if (!"tat ca".equalsIgnoreCase(normalizedCategory)) {
            sql.append(" and lower(item_category) = ? ");
            params.add(normalizedCategory.toLowerCase());
        }

        sql.append(" order by item_end_time asc ");

        return loadAuctionsByQuery(sql.toString(), statement -> {
            for (int index = 0; index < params.size(); index++) {
                statement.setString(index + 1, params.get(index));
            }
        });
    }

    public List<Auction> loadBySeller(String sellerUsername) {
        DatabaseManager.initialize();
        String auctionSql = """
                select id, seller_username, item_id, item_category, item_name, item_description,
                       item_starting_price, item_end_time, item_image_hint, cancelled, paid,
                       anti_snipe_triggered, close_notified
                from auctions
                where lower(seller_username) = lower(?)
                order by item_end_time desc
                """;
        return loadAuctionsByQuery(auctionSql, statement -> statement.setString(1, sellerUsername));
    }

    public List<Auction> loadByBidder(String bidderUsername) {
        DatabaseManager.initialize();
        String auctionSql = """
                select distinct a.id, a.seller_username, a.item_id, a.item_category, a.item_name, a.item_description,
                       a.item_starting_price, a.item_end_time, a.item_image_hint, a.cancelled, a.paid,
                       a.anti_snipe_triggered, a.close_notified
                from auctions a
                join auction_bids b on b.auction_id = a.id
                where lower(b.actor_username) = lower(?)
                order by a.item_end_time desc
                """;
        return loadAuctionsByQuery(auctionSql, statement -> statement.setString(1, bidderUsername));
    }

    public List<Auction> loadWonByBidder(String bidderUsername) {
        DatabaseManager.initialize();
        String auctionSql = """
                with ranked_bids as (
                    select auction_id,
                           actor_username,
                           amount,
                           row_number() over (
                               partition by auction_id
                               order by amount desc, event_time asc
                           ) as rank_no
                    from auction_bids
                )
                select a.id, a.seller_username, a.item_id, a.item_category, a.item_name, a.item_description,
                       a.item_starting_price, a.item_end_time, a.item_image_hint, a.cancelled, a.paid,
                       a.anti_snipe_triggered, a.close_notified
                from auctions a
                join ranked_bids rb on rb.auction_id = a.id and rb.rank_no = 1
                where a.cancelled = false
                  and a.item_end_time <= ?
                  and lower(rb.actor_username) = lower(?)
                order by a.item_end_time desc
                """;
        return loadAuctionsByQuery(auctionSql, statement -> {
            statement.setTimestamp(1, Timestamp.valueOf(LocalDateTime.now()));
            statement.setString(2, bidderUsername);
        });
    }

    public void saveAll(List<Auction> auctions) {
        DatabaseManager.initialize();
        String deleteAutoBidsSql = "delete from auction_auto_bids";
        String deleteBidsSql = "delete from auction_bids";
        String deleteAuctionsSql = "delete from auctions";
        String insertAuctionSql = """
                insert into auctions (
                    id, seller_username, item_id, item_category, item_name, item_description,
                    item_starting_price, item_end_time, item_image_hint, cancelled, paid,
                    anti_snipe_triggered, close_notified, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertBidSql = """
                insert into auction_bids (
                    auction_id, bid_type, actor_username, reference_id, description, amount, event_time, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        String insertAutoBidSql = """
                insert into auction_auto_bids (
                    auction_id, bidder_username, max_amount, increment_step, sort_order
                ) values (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteAutoBidsStatement = connection.prepareStatement(deleteAutoBidsSql);
                 PreparedStatement deleteBidsStatement = connection.prepareStatement(deleteBidsSql);
                 PreparedStatement deleteAuctionsStatement = connection.prepareStatement(deleteAuctionsSql);
                 PreparedStatement insertAuctionStatement = connection.prepareStatement(insertAuctionSql);
                 PreparedStatement insertBidStatement = connection.prepareStatement(insertBidSql);
                 PreparedStatement insertAutoBidStatement = connection.prepareStatement(insertAutoBidSql)) {
                deleteAutoBidsStatement.executeUpdate();
                deleteBidsStatement.executeUpdate();
                deleteAuctionsStatement.executeUpdate();

                for (int auctionIndex = 0; auctionIndex < auctions.size(); auctionIndex++) {
                    Auction auction = auctions.get(auctionIndex);
                    insertAuctionStatement.setString(1, auction.getId());
                    insertAuctionStatement.setString(2, auction.getSellerUsername());
                    insertAuctionStatement.setString(3, auction.getItem().getId());
                    insertAuctionStatement.setString(4, auction.getItem().getCategory());
                    insertAuctionStatement.setString(5, auction.getItem().getName());
                    insertAuctionStatement.setString(6, auction.getItem().getDescription());
                    insertAuctionStatement.setDouble(7, auction.getItem().getStartingPrice());
                    insertAuctionStatement.setTimestamp(8, Timestamp.valueOf(auction.getItem().getEndTime()));
                    insertAuctionStatement.setString(9, auction.getItem().getImageHint());
                    insertAuctionStatement.setBoolean(10, auction.isCancelled());
                    insertAuctionStatement.setBoolean(11, auction.isPaid());
                    insertAuctionStatement.setBoolean(12, auction.isAntiSnipeTriggered());
                    insertAuctionStatement.setBoolean(13, auction.isCloseNotified());
                    insertAuctionStatement.setInt(14, auctionIndex);
                    insertAuctionStatement.addBatch();

                    for (int bidIndex = 0; bidIndex < auction.getBidHistory().size(); bidIndex++) {
                        BidTransaction bid = auction.getBidHistory().get(bidIndex);
                        insertBidStatement.setString(1, auction.getId());
                        insertBidStatement.setString(2, bid.getType());
                        insertBidStatement.setString(3, bid.getActorUsername());
                        insertBidStatement.setString(4, bid.getReferenceId());
                        insertBidStatement.setString(5, bid.getDescription());
                        insertBidStatement.setDouble(6, bid.getAmount());
                        insertBidStatement.setTimestamp(7, Timestamp.valueOf(bid.getTime()));
                        insertBidStatement.setInt(8, bidIndex);
                        insertBidStatement.addBatch();
                    }

                    for (int autoBidIndex = 0; autoBidIndex < auction.getAutoBids().size(); autoBidIndex++) {
                        AutoBid autoBid = auction.getAutoBids().get(autoBidIndex);
                        insertAutoBidStatement.setString(1, auction.getId());
                        insertAutoBidStatement.setString(2, autoBid.getBidderUsername());
                        insertAutoBidStatement.setDouble(3, autoBid.getMaxAmount());
                        insertAutoBidStatement.setDouble(4, autoBid.getIncrementStep());
                        insertAutoBidStatement.setInt(5, autoBidIndex);
                        insertAutoBidStatement.addBatch();
                    }
                }

                insertAuctionStatement.executeBatch();
                insertBidStatement.executeBatch();
                insertAutoBidStatement.executeBatch();
                connection.commit();
            } catch (Exception ex) {
                connection.rollback();
                throw ex;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the luu auctions vao PostgreSQL.", ex);
        }
    }

    public void insert(Auction auction) {
        DatabaseManager.initialize();
        String sql = """
                insert into auctions (
                    id, seller_username, item_id, item_category, item_name, item_description,
                    item_starting_price, item_end_time, item_image_hint, cancelled, paid,
                    anti_snipe_triggered, close_notified, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, (
                    select coalesce(min(sort_order), 0) - 1
                    from auctions
                ))
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindAuction(statement, auction);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the them auction vao PostgreSQL.", ex);
        }
    }

    public void updateAuction(Auction auction) {
        DatabaseManager.initialize();
        String sql = """
                update auctions
                set seller_username = ?,
                    item_id = ?,
                    item_category = ?,
                    item_name = ?,
                    item_description = ?,
                    item_starting_price = ?,
                    item_end_time = ?,
                    item_image_hint = ?,
                    cancelled = ?,
                    paid = ?,
                    anti_snipe_triggered = ?,
                    close_notified = ?
                where lower(id) = lower(?)
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auction.getSellerUsername());
            statement.setString(2, auction.getItem().getId());
            statement.setString(3, auction.getItem().getCategory());
            statement.setString(4, auction.getItem().getName());
            statement.setString(5, auction.getItem().getDescription());
            statement.setDouble(6, auction.getItem().getStartingPrice());
            statement.setTimestamp(7, Timestamp.valueOf(auction.getItem().getEndTime()));
            statement.setString(8, auction.getItem().getImageHint());
            statement.setBoolean(9, auction.isCancelled());
            statement.setBoolean(10, auction.isPaid());
            statement.setBoolean(11, auction.isAntiSnipeTriggered());
            statement.setBoolean(12, auction.isCloseNotified());
            statement.setString(13, auction.getId());
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the cap nhat auction trong PostgreSQL.", ex);
        }
    }

    public void insertBid(String auctionId, BidTransaction bid) {
        DatabaseManager.initialize();
        String sql = """
                insert into auction_bids (
                    auction_id, bid_type, actor_username, reference_id, description, amount, event_time, sort_order
                ) values (?, ?, ?, ?, ?, ?, ?, (
                    select coalesce(max(sort_order), -1) + 1
                    from auction_bids
                    where lower(auction_id) = lower(?)
                ))
                """;

        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, auctionId);
            statement.setString(2, bid.getType());
            statement.setString(3, bid.getActorUsername());
            statement.setString(4, bid.getReferenceId());
            statement.setString(5, bid.getDescription());
            statement.setDouble(6, bid.getAmount());
            statement.setTimestamp(7, Timestamp.valueOf(bid.getTime()));
            statement.setString(8, auctionId);
            statement.executeUpdate();
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the them bid vao PostgreSQL.", ex);
        }
    }

    public void replaceAutoBids(String auctionId, List<AutoBid> autoBids) {
        DatabaseManager.initialize();
        String deleteSql = "delete from auction_auto_bids where lower(auction_id) = lower(?)";
        String insertSql = """
                insert into auction_auto_bids (
                    auction_id, bidder_username, max_amount, increment_step, sort_order
                ) values (?, ?, ?, ?, ?)
                """;

        try (Connection connection = DatabaseManager.getConnection()) {
            connection.setAutoCommit(false);
            try (PreparedStatement deleteStatement = connection.prepareStatement(deleteSql);
                 PreparedStatement insertStatement = connection.prepareStatement(insertSql)) {
                deleteStatement.setString(1, auctionId);
                deleteStatement.executeUpdate();

                for (int index = 0; index < autoBids.size(); index++) {
                    AutoBid autoBid = autoBids.get(index);
                    insertStatement.setString(1, auctionId);
                    insertStatement.setString(2, autoBid.getBidderUsername());
                    insertStatement.setDouble(3, autoBid.getMaxAmount());
                    insertStatement.setDouble(4, autoBid.getIncrementStep());
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
            throw new IllegalStateException("Khong the cap nhat auto bids trong PostgreSQL.", ex);
        }
    }

    private void bindAuction(PreparedStatement statement, Auction auction) throws Exception {
        statement.setString(1, auction.getId());
        statement.setString(2, auction.getSellerUsername());
        statement.setString(3, auction.getItem().getId());
        statement.setString(4, auction.getItem().getCategory());
        statement.setString(5, auction.getItem().getName());
        statement.setString(6, auction.getItem().getDescription());
        statement.setDouble(7, auction.getItem().getStartingPrice());
        statement.setTimestamp(8, Timestamp.valueOf(auction.getItem().getEndTime()));
        statement.setString(9, auction.getItem().getImageHint());
        statement.setBoolean(10, auction.isCancelled());
        statement.setBoolean(11, auction.isPaid());
        statement.setBoolean(12, auction.isAntiSnipeTriggered());
        statement.setBoolean(13, auction.isCloseNotified());
    }

    private List<Auction> loadAuctionsByQuery(String auctionSql, SqlBinder binder) {
        try (Connection connection = DatabaseManager.getConnection();
             PreparedStatement statement = connection.prepareStatement(auctionSql)) {
            binder.bind(statement);
            try (ResultSet result = statement.executeQuery()) {
                Map<String, Auction> auctionsById = new LinkedHashMap<>();
                while (result.next()) {
                    Item item = itemController.createItem(
                            result.getString("item_category"),
                            result.getString("item_id"),
                            result.getString("item_name"),
                            result.getString("item_description"),
                            result.getDouble("item_starting_price"),
                            toLocalDateTime(result.getTimestamp("item_end_time")),
                            result.getString("item_image_hint")
                    );
                    Auction auction = new Auction(
                            result.getString("id"),
                            result.getString("seller_username"),
                            item
                    );
                    if (result.getBoolean("cancelled")) {
                        auction.cancel();
                    }
                    if (result.getBoolean("paid")) {
                        auction.markPaid();
                    }
                    if (result.getBoolean("anti_snipe_triggered")) {
                        auction.extendAuctionSeconds(0);
                    }
                    if (result.getBoolean("close_notified")) {
                        auction.markCloseNotified();
                    }
                    auctionsById.put(auction.getId(), auction);
                }

                loadBidsAndAutoBids(connection, auctionsById);
                return new ArrayList<>(auctionsById.values());
            }
        } catch (Exception ex) {
            throw new IllegalStateException("Khong the tai danh sach auctions tu PostgreSQL.", ex);
        }
    }

    private void loadBidsAndAutoBids(Connection connection, Map<String, Auction> auctionsById) throws Exception {
        if (auctionsById.isEmpty()) {
            return;
        }

        String placeholders = String.join(", ", java.util.Collections.nCopies(auctionsById.size(), "?"));
        String bidSql = """
                select auction_id, bid_type, actor_username, reference_id, description, amount, event_time
                from auction_bids
                where auction_id in (%s)
                order by auction_id asc, sort_order asc, event_time asc
                """.formatted(placeholders);
        String autoBidSql = """
                select auction_id, bidder_username, max_amount, increment_step
                from auction_auto_bids
                where auction_id in (%s)
                order by auction_id asc, sort_order asc
                """.formatted(placeholders);

        try (PreparedStatement statement = connection.prepareStatement(bidSql)) {
            bindAuctionIds(statement, auctionsById.keySet());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Auction auction = auctionsById.get(result.getString("auction_id"));
                    if (auction == null) {
                        continue;
                    }
                    auction.addBid(new BidTransaction(
                            result.getString("bid_type"),
                            result.getString("actor_username"),
                            result.getString("reference_id"),
                            result.getString("description"),
                            result.getDouble("amount"),
                            toLocalDateTime(result.getTimestamp("event_time"))
                    ));
                }
            }
        }

        try (PreparedStatement statement = connection.prepareStatement(autoBidSql)) {
            bindAuctionIds(statement, auctionsById.keySet());
            try (ResultSet result = statement.executeQuery()) {
                while (result.next()) {
                    Auction auction = auctionsById.get(result.getString("auction_id"));
                    if (auction == null) {
                        continue;
                    }
                    auction.addOrReplaceAutoBid(new AutoBid(
                            result.getString("bidder_username"),
                            result.getDouble("max_amount"),
                            result.getDouble("increment_step")
                    ));
                }
            }
        }
    }

    private void bindAuctionIds(PreparedStatement statement, Iterable<String> auctionIds) throws Exception {
        int index = 1;
        for (String auctionId : auctionIds) {
            statement.setString(index++, auctionId);
        }
    }

    private LocalDateTime toLocalDateTime(Timestamp value) {
        return value == null ? null : value.toLocalDateTime();
    }

    @FunctionalInterface
    private interface SqlBinder {
        void bind(PreparedStatement statement) throws Exception;
    }
}
