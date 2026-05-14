package server.util;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public final class DatabaseManager {
    private static final String DEFAULT_SSL_MODE = "require";
    private static final Path PROPERTIES_PATH = Path.of("config", "database.properties");

    private static volatile boolean initialized;
    private static volatile Config config;

    private DatabaseManager() {
    }

    public static Connection getConnection() throws SQLException {
        initialize();
        return DriverManager.getConnection(config.jdbcUrl(), config.user(), config.password());
    }

    public static synchronized void initialize() {
        if (initialized) {
            return;
        }

        config = loadConfig();
        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException ex) {
            throw new IllegalStateException(
                    "Chua tim thay PostgreSQL JDBC driver. Can tai dependency org.postgresql:postgresql truoc khi chay app.",
                    ex
            );
        }

        try (Connection connection = DriverManager.getConnection(config.jdbcUrl(), config.user(), config.password());
             Statement statement = connection.createStatement()) {
            statement.execute("""
                    create table if not exists users (
                        id varchar(50) primary key,
                        username varchar(100) not null unique,
                        password varchar(255) not null,
                        role varchar(20) not null,
                        full_name varchar(255) not null,
                        wallet_balance double precision not null default 0,
                        sort_order integer not null default 0
                    )
                    """);
            statement.execute("""
                    create table if not exists auctions (
                        id varchar(50) primary key,
                        seller_username varchar(100) not null,
                        item_id varchar(50) not null,
                        item_category varchar(100) not null,
                        item_name varchar(255) not null,
                        item_description text not null,
                        item_starting_price double precision not null,
                        item_end_time timestamp not null,
                        item_image_hint varchar(255),
                        cancelled boolean not null default false,
                        paid boolean not null default false,
                        anti_snipe_triggered boolean not null default false,
                        close_notified boolean not null default false,
                        sort_order integer not null default 0
                    )
                    """);
            statement.execute("""
                    create table if not exists auction_bids (
                        id bigserial primary key,
                        auction_id varchar(50) not null references auctions(id) on delete cascade,
                        bid_type varchar(50) not null,
                        actor_username varchar(100) not null,
                        reference_id varchar(100) not null,
                        description text not null,
                        amount double precision not null,
                        event_time timestamp not null,
                        sort_order integer not null default 0
                    )
                    """);
            statement.execute("""
                    create table if not exists auction_auto_bids (
                        id bigserial primary key,
                        auction_id varchar(50) not null references auctions(id) on delete cascade,
                        bidder_username varchar(100) not null,
                        max_amount double precision not null,
                        increment_step double precision not null,
                        sort_order integer not null default 0
                    )
                    """);
            statement.execute("""
                    create table if not exists payments (
                        id bigserial primary key,
                        auction_id varchar(50) not null,
                        buyer_username varchar(100) not null,
                        seller_username varchar(100) not null,
                        amount double precision not null,
                        paid_at timestamp not null,
                        sort_order integer not null default 0
                    )
                    """);
            statement.execute("""
                    create table if not exists notifications (
                        id bigserial primary key,
                        username varchar(100) not null,
                        title varchar(255) not null,
                        message text not null,
                        event_time timestamp not null,
                        sort_order integer not null default 0
                    )
                    """);
            statement.execute("""
                    create table if not exists top_up_requests (
                        id varchar(50) primary key,
                        username varchar(100) not null,
                        amount double precision not null,
                        bank_name varchar(255) not null,
                        account_name varchar(255) not null,
                        account_number varchar(100) not null,
                        requested_at timestamp not null,
                        status varchar(30) not null,
                        approved_at timestamp null,
                        approved_by varchar(100) null,
                        credited_at timestamp null,
                        sort_order integer not null default 0
                    )
                    """);
            statement.execute("""
                    create table if not exists transactions (
                        id bigserial primary key,
                        transaction_type varchar(50) not null,
                        actor_username varchar(100) not null,
                        reference_id varchar(100) not null,
                        description text not null,
                        amount double precision not null,
                        event_time timestamp not null,
                        sort_order integer not null default 0
                    )
                    """);
        } catch (SQLException ex) {
            throw new IllegalStateException("Khong the khoi tao schema PostgreSQL.", ex);
        }

        initialized = true;
    }

    private static Config loadConfig() {
        Properties fileProperties = new Properties();
        if (Files.exists(PROPERTIES_PATH)) {
            try (var input = Files.newInputStream(PROPERTIES_PATH)) {
                fileProperties.load(input);
            } catch (Exception ex) {
                throw new IllegalStateException("Khong the doc file cau hinh database tai " + PROPERTIES_PATH + ".", ex);
            }
        }

        String host = readValue("auction.db.host", "AUCTION_DB_HOST", "db.host", fileProperties, null);
        String port = readValue("auction.db.port", "AUCTION_DB_PORT", "db.port", fileProperties, "5432");
        String database = readValue("auction.db.name", "AUCTION_DB_NAME", "db.name", fileProperties, "postgres");
        String user = readValue("auction.db.user", "AUCTION_DB_USER", "db.user", fileProperties, null);
        String password = readValue("auction.db.password", "AUCTION_DB_PASSWORD", "db.password", fileProperties, null);
        String sslMode = readValue("auction.db.sslmode", "AUCTION_DB_SSLMODE", "db.sslmode", fileProperties, DEFAULT_SSL_MODE);

        if (isBlank(host) || isBlank(user) || isBlank(password)) {
            throw new IllegalStateException("""
                    Thieu cau hinh database.
                    Can thiet lap host, user, password qua:
                    - file config/database.properties
                    - hoac system properties auction.db.*
                    - hoac bien moi truong AUCTION_DB_*
                    """.trim());
        }

        return new Config(host.trim(), Integer.parseInt(port.trim()), database.trim(), user.trim(), password, sslMode.trim());
    }

    private static String readValue(
            String propertyKey,
            String envKey,
            String fileKey,
            Properties fileProperties,
            String defaultValue
    ) {
        String propertyValue = System.getProperty(propertyKey);
        if (!isBlank(propertyValue)) {
            return propertyValue;
        }

        String envValue = System.getenv(envKey);
        if (!isBlank(envValue)) {
            return envValue;
        }

        String fileValue = fileProperties.getProperty(fileKey);
        if (!isBlank(fileValue)) {
            return fileValue;
        }

        return defaultValue;
    }

    private static boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record Config(String host, int port, String database, String user, String password, String sslMode) {
        private String jdbcUrl() {
            return "jdbc:postgresql://%s:%d/%s?sslmode=%s".formatted(host, port, database, sslMode);
        }
    }
}
