package ru.hrp.core;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;
import java.util.function.Function;

public interface DatabaseService {
    void initialize() throws SQLException;
    void shutdown();

    <T> CompletableFuture<T> queryAsync(Function<Connection, T> function);
    CompletableFuture<Void> executeAsync(Consumer<Connection> consumer);
}
