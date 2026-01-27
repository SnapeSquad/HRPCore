package ru.hrp.core;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.NamespacedKey;
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
import ru.hrp.roles.AbilityBridge;
import ru.hrp.roles.AbilityManager;
import ru.hrp.roles.AbilityService;
import ru.hrp.roles.CooldownManager;
import ru.hrp.roles.CooldownService;
import ru.hrp.roles.RoleCardFactory;
import ru.hrp.crime.CrimeManager;
import ru.hrp.crime.CrimeService;
import ru.hrp.crime.JailListener;
import ru.hrp.crime.JailManager;
import ru.hrp.crime.JailService;
import ru.hrp.roles.RoleManager;
import ru.hrp.roles.RoleService;
import ru.hrp.talents.TalentManager;
import ru.hrp.talents.TalentService;
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
    private RoleService roleService;
    private CooldownService cooldownService;
    private AbilityService abilityService;
    private TalentService talentService;
    private CrimeService crimeService;
    private JailService jailService;
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

            // 6. Initialize Role Service
            RoleCardFactory cardFactory = new RoleCardFactory(new NamespacedKey(this, "role_id"));
            this.roleService = new RoleManager(getLogger(), databaseService, configService, cardFactory);
            this.roleService.loadDefinitions();

            // 7. Initialize Talent Service
            this.talentService = new TalentManager(getLogger(), databaseService, configService);
            this.talentService.loadDefinitions();

            // 8. Initialize Ability Service
            this.cooldownService = new CooldownManager();
            this.abilityService = new AbilityManager(getLogger(), configService, cooldownService, roleService, talentService);
            this.abilityService.loadDefinitions();

            // 9. Initialize Crime & Jail Services
            this.crimeService = new CrimeManager(getLogger(), databaseService, configService);
            this.crimeService.loadDefinitions();
            this.jailService = new JailManager(getLogger(), databaseService);

            // 10. Initialize Player Data Service
            this.playerDataService = new PlayerDataManager(
                getLogger(),
                databaseService,
                economyService,
                roleService,
                talentService,
                crimeService,
                jailService,
                runnable -> getServer().getScheduler().runTask(this, runnable)
            );

            // 11. Register Listeners
            getServer().getPluginManager().registerEvents(new PlayerListener(playerDataService, messageService), this);
            getServer().getPluginManager().registerEvents(new AbilityBridge(abilityService, cardFactory), this);
            getServer().getPluginManager().registerEvents(new JailListener(jailService, configService, getLogger()), this);

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
        if (roleService != null) {
            roleService.saveAll();
        }
        if (talentService != null) {
            talentService.saveAll();
        }
        if (crimeService != null) {
            crimeService.saveAll();
        }
        if (jailService != null) {
            jailService.saveAll();
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

    public RoleService getRoleService() {
        return roleService;
    }

    public AbilityService getAbilityService() {
        return abilityService;
    }

    public CooldownService getCooldownService() {
        return cooldownService;
    }

    public TalentService getTalentService() {
        return talentService;
    }

    public CrimeService getCrimeService() {
        return crimeService;
    }

    public JailService getJailService() {
        return jailService;
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
