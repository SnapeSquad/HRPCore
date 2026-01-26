package ru.hrp.core;

import org.bukkit.plugin.java.JavaPlugin;
import ru.hrp.config.ConfigManager;
import ru.hrp.config.ConfigService;
import ru.hrp.player.PlayerDataManager;
import ru.hrp.player.PlayerDataService;
import ru.hrp.player.PlayerListener;

import java.sql.SQLException;
import java.util.logging.Level;

public final class HRPCore extends JavaPlugin {
    private ConfigService configService;
    private DatabaseService databaseService;
    private MessageService messageService;
    private PlayerDataService playerDataService;

    @Override
    public void onEnable() {
        try {
            // 1. Initialize Configuration
            this.configService = new ConfigManager(this);
            this.configService.loadConfigs();

            // 2. Initialize Database
            this.databaseService = new SQLiteDatabaseService(this, configService);
            this.databaseService.initialize();

            // 3. Initialize Message Service
            this.messageService = new MessageManager(configService);

            // 4. Initialize Player Data Service
            this.playerDataService = new PlayerDataManager(this, databaseService);

            // 5. Register Listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(playerDataService, messageService), this);

            getLogger().info("HRPCore has been enabled successfully!");
        } catch (SQLException e) {
            getLogger().log(Level.SEVERE, "Failed to initialize database. Disabling plugin...", e);
            getServer().getPluginManager().disablePlugin(this);
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "An error occurred during plugin startup. Disabling plugin...", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (databaseService != null) {
            databaseService.shutdown();
        }
        getLogger().info("HRPCore has been disabled.");
    }

    public ConfigService getConfigService() {
        return configService;
    }

    public DatabaseService getDatabaseService() {
        return databaseService;
    }

    public MessageService getMessageService() {
        return messageService;
    }

    public PlayerDataService getPlayerDataService() {
        return playerDataService;
    }
}
