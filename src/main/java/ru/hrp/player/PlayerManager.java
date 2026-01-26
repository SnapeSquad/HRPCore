package ru.hrp.player;

import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import ru.hrp.core.DatabaseService;
import ru.hrp.core.MessageService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Level;

public class PlayerManager implements PlayerService {
    private final JavaPlugin plugin;
    private final DatabaseService databaseService;
    private final MessageService messageService;

    public PlayerManager(JavaPlugin plugin, DatabaseService databaseService, MessageService messageService) {
        this.plugin = plugin;
        this.databaseService = databaseService;
        this.messageService = messageService;
    }

    @Override
    public CompletableFuture<Void> savePlayer(HRPPlayer player) {
        return databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO players (uuid, last_name, first_join, last_seen) VALUES (?, ?, ?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET last_name = EXCLUDED.last_name, last_seen = EXCLUDED.last_seen";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, player.uuid().toString());
                pstmt.setString(2, player.lastName());
                pstmt.setLong(3, player.firstJoin());
                pstmt.setLong(4, player.lastSeen());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to save player " + player.uuid(), e);
            }
        });
    }

    @Override
    public CompletableFuture<HRPPlayer> loadPlayer(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT * FROM players WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return new HRPPlayer(
                            UUID.fromString(rs.getString("uuid")),
                            rs.getString("last_name"),
                            rs.getLong("first_join"),
                            rs.getLong("last_seen")
                        );
                    }
                }
            } catch (SQLException e) {
                plugin.getLogger().log(Level.SEVERE, "Failed to load player " + uuid, e);
            }
            return null;
        });
    }

    @Override
    public void handlePlayerJoin(UUID uuid, String name) {
        loadPlayer(uuid).thenAccept(player -> {
            long now = System.currentTimeMillis();
            HRPPlayer updatedPlayer;
            final boolean firstJoin;

            if (player == null) {
                updatedPlayer = new HRPPlayer(uuid, name, now, now);
                firstJoin = true;
            } else {
                updatedPlayer = new HRPPlayer(uuid, name, player.firstJoin(), now);
                firstJoin = false;
            }

            savePlayer(updatedPlayer).thenRun(() -> {
                Bukkit.getScheduler().runTask(plugin, () -> {
                    var onlinePlayer = Bukkit.getPlayer(uuid);
                    if (onlinePlayer != null) {
                        if (firstJoin) {
                            messageService.sendMessage(onlinePlayer, "player.first_join",
                                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player_name", name));
                        } else {
                            messageService.sendMessage(onlinePlayer, "player.join",
                                net.kyori.adventure.text.minimessage.tag.resolver.Placeholder.parsed("player_name", name));
                        }
                    }
                });
            });
        });
    }

    @Override
    public void handlePlayerQuit(UUID uuid) {
        // Just update last seen
        loadPlayer(uuid).thenAccept(player -> {
            if (player != null) {
                HRPPlayer updatedPlayer = new HRPPlayer(uuid, player.lastName(), player.firstJoin(), System.currentTimeMillis());
                savePlayer(updatedPlayer);
            }
        });
    }
}
