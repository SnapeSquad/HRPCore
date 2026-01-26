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
