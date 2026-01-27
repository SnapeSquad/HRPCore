package ru.hrp.core;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.plugin.ServicePriority;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import ru.hrp.config.ConfigManager;
import ru.hrp.config.ConfigService;
import ru.hrp.economy.EconomyManager;
import ru.hrp.economy.EconomyService;
import ru.hrp.economy.PayDayManager;
import ru.hrp.economy.PayDayService;
import ru.hrp.economy.PayDayTask;
import ru.hrp.economy.VaultEconomyProvider;
import ru.hrp.player.PlayerDataManager;
import ru.hrp.player.PlayerDataService;
import ru.hrp.player.PlayerListener;

import java.sql.SQLException;
import java.util.logging.Level;

public final class HRPCore extends JavaPlugin {
    private ConfigService configService;
    private DatabaseService databaseService;
    private MessageService messageService;
    private EconomyService economyService;
    private PayDayService payDayService;
    private PlayerDataService playerDataService;
    private BukkitTask payDayTask;

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

            // 4. Initialize Economy Service
            this.economyService = new EconomyManager(getLogger(), databaseService);
            registerVault();

            // 5. Initialize PayDay Service
            this.payDayService = new PayDayManager(economyService);
            startPayDayTask();

            // 6. Initialize Player Data Service
            this.playerDataService = new PlayerDataManager(
                getLogger(),
                databaseService,
                economyService,
                runnable -> getServer().getScheduler().runTask(this, runnable)
            );

            // 6. Register Listeners
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
        if (payDayTask != null) {
            payDayTask.cancel();
        }

        // Save all data before shutting down database
        if (playerDataService != null) {
            playerDataService.saveAll();
        }
        if (economyService != null) {
            economyService.saveAll();
        }

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

    public EconomyService getEconomyService() {
        return economyService;
    }

    public PayDayService getPayDayService() {
        return payDayService;
    }

    private void startPayDayTask() {
        long interval = configService.getConfig().getLong("payday.interval", 15) * 60 * 20; // convert to ticks
        this.payDayTask = getServer().getScheduler().runTaskTimer(this,
            new PayDayTask(configService, payDayService, messageService), interval, interval);
        getLogger().info("PayDay task started with interval of " + (interval / 1200) + " minutes.");
    }

    private void registerVault() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            getLogger().warning("Vault not found! Economy integration disabled.");
            return;
        }
        getServer().getServicesManager().register(Economy.class, new VaultEconomyProvider(economyService), this, ServicePriority.Highest);
        getLogger().info("Registered HRPCore as Vault Economy provider.");
    }
}
