package ru.hrp.core;

import org.bukkit.plugin.java.JavaPlugin;
import ru.hrp.config.ConfigService;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.logging.Level;

public class SQLiteDatabaseService implements DatabaseService {
    private final JavaPlugin plugin;
    private final ConfigService configService;
    private final ExecutorService executor;
    private Connection connection;

    public SQLiteDatabaseService(JavaPlugin plugin, ConfigService configService) {
        this.plugin = plugin;
        this.configService = configService;
        // Single thread executor to ensure serial access to SQLite connection
        this.executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public void initialize() throws SQLException {
        String fileName = configService.getConfig().getString("database.file", "database.db");
        File dbFile = new File(plugin.getDataFolder(), fileName);

        String url = "jdbc:sqlite:" + dbFile.getAbsolutePath();

        try {
            Class.forName("org.sqlite.JDBC");
            connection = DriverManager.getConnection(url);

            initSchema();
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found", e);
        }
    }

    private void initSchema() throws SQLException {
        try (Statement stmt = connection.createStatement()) {
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS players (
                    uuid TEXT PRIMARY KEY,
                    last_name TEXT NOT NULL,
                    first_join INTEGER NOT NULL,
                    last_seen INTEGER NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS economy_accounts (
                    uuid TEXT PRIMARY KEY,
                    balance TEXT NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_roles (
                    uuid TEXT PRIMARY KEY,
                    role_id TEXT NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_talents (
                    uuid TEXT NOT NULL,
                    talent_id TEXT NOT NULL,
                    level INTEGER NOT NULL,
                    PRIMARY KEY (uuid, talent_id)
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_crimes (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    crime_id TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    resolved INTEGER NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_jail (
                    uuid TEXT PRIMARY KEY,
                    release_timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS bank_accounts (
                    uuid TEXT PRIMARY KEY,
                    balance TEXT NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_credits (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    uuid TEXT NOT NULL,
                    principal TEXT NOT NULL,
                    interest_rate TEXT NOT NULL,
                    total_amount TEXT NOT NULL,
                    remaining_amount TEXT NOT NULL,
                    due_timestamp INTEGER NOT NULL,
                    status TEXT NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_medical (
                    uuid TEXT PRIMARY KEY,
                    state TEXT NOT NULL,
                    state_timestamp INTEGER NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_jobs (
                    uuid TEXT PRIMARY KEY,
                    job_id TEXT NOT NULL,
                    level INTEGER NOT NULL
                );
            """);
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS player_factions (
                    uuid TEXT PRIMARY KEY,
                    faction_id TEXT NOT NULL,
                    rank TEXT NOT NULL
                );
            """);
        }
    }

    @Override
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
            }
        } catch (SQLException e) {
            plugin.getLogger().log(Level.SEVERE, "Could not close SQLite connection", e);
        }
    }

    @Override
    public <T> CompletableFuture<T> queryAsync(Function<Connection, T> function) {
        return CompletableFuture.supplyAsync(() -> function.apply(connection), executor);
    }

    @Override
    public CompletableFuture<Void> executeAsync(Consumer<Connection> consumer) {
        return CompletableFuture.runAsync(() -> consumer.accept(connection), executor);
    }
}
