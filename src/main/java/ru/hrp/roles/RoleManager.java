package ru.hrp.roles;

import org.bukkit.inventory.ItemStack;
import ru.hrp.config.ConfigService;
import ru.hrp.core.DatabaseService;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

public class RoleManager implements RoleService {
    private final Logger logger;
    private final DatabaseService databaseService;
    private final ConfigService configService;
    private final RoleCardFactory cardFactory;

    private final Map<RoleId, RoleDefinition> definitions = new HashMap<>();
    private final Map<UUID, RoleId> activeRoles = new ConcurrentHashMap<>();

    public RoleManager(Logger logger, DatabaseService databaseService, ConfigService configService, RoleCardFactory cardFactory) {
        this.logger = logger;
        this.databaseService = databaseService;
        this.configService = configService;
        this.cardFactory = cardFactory;
    }

    @Override
    public void loadDefinitions() {
        definitions.clear();
        var config = configService.getRoles();
        for (String key : config.getKeys(false)) {
            try {
                RoleId id = RoleId.valueOf(key);
                String name = config.getString(key + ".name", key);
                List<String> description = config.getStringList(key + ".description");
                definitions.put(id, new RoleDefinition(id, name, description));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid Role ID in roles.yml: " + key);
            }
        }
        logger.info("Loaded " + definitions.size() + " role definitions.");
    }

    @Override
    public CompletableFuture<RoleId> loadRole(UUID uuid) {
        return databaseService.queryAsync(connection -> {
            String sql = "SELECT role_id FROM player_roles WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        try {
                            return RoleId.valueOf(rs.getString("role_id"));
                        } catch (IllegalArgumentException e) {
                            return RoleId.NONE;
                        }
                    }
                }
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to load role for " + uuid, e);
            }
            return RoleId.NONE;
        }).thenApply(roleId -> {
            activeRoles.put(uuid, roleId);
            return roleId;
        });
    }

    @Override
    public void unloadRole(UUID uuid) {
        RoleId roleId = activeRoles.remove(uuid);
        if (roleId != null && roleId != RoleId.NONE) {
            saveRole(uuid, roleId);
        }
    }

    private void saveRole(UUID uuid, RoleId roleId) {
        databaseService.executeAsync(connection -> {
            String sql = "INSERT INTO player_roles (uuid, role_id) VALUES (?, ?) " +
                         "ON CONFLICT(uuid) DO UPDATE SET role_id = EXCLUDED.role_id";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.setString(2, roleId.name());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to save role for " + uuid, e);
            }
        });
    }

    @Override
    public RoleId getActiveRoleId(UUID uuid) {
        return activeRoles.getOrDefault(uuid, RoleId.NONE);
    }

    @Override
    public CompletableFuture<ItemStack> assignRole(UUID uuid, RoleId roleId) {
        activeRoles.put(uuid, roleId);
        saveRole(uuid, roleId);

        RoleDefinition definition = definitions.get(roleId);
        if (definition == null) {
            return CompletableFuture.completedFuture(null);
        }

        return CompletableFuture.completedFuture(cardFactory.createCard(definition));
    }

    @Override
    public CompletableFuture<Void> removeRole(UUID uuid) {
        activeRoles.put(uuid, RoleId.NONE);
        return databaseService.executeAsync(connection -> {
            String sql = "DELETE FROM player_roles WHERE uuid = ?";
            try (PreparedStatement pstmt = connection.prepareStatement(sql)) {
                pstmt.setString(1, uuid.toString());
                pstmt.executeUpdate();
            } catch (SQLException e) {
                logger.log(Level.SEVERE, "Failed to remove role for " + uuid, e);
            }
        });
    }

    @Override
    public void saveAll() {
        activeRoles.forEach((uuid, roleId) -> {
            if (roleId != RoleId.NONE) {
                saveRole(uuid, roleId);
            }
        });
    }
}
