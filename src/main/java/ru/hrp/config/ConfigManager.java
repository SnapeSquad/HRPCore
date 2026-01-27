package ru.hrp.config;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.logging.Level;

public class ConfigManager implements ConfigService {
    private final JavaPlugin plugin;
    private FileConfiguration config;
    private FileConfiguration messages;
    private FileConfiguration roles;
    private FileConfiguration abilities;
    private File configFile;
    private File messagesFile;
    private File rolesFile;
    private File abilitiesFile;

    public ConfigManager(JavaPlugin plugin) {
        this.plugin = plugin;
        this.configFile = new File(plugin.getDataFolder(), "config.yml");
        this.messagesFile = new File(plugin.getDataFolder(), "messages.yml");
        this.rolesFile = new File(plugin.getDataFolder(), "roles.yml");
        this.abilitiesFile = new File(plugin.getDataFolder(), "abilities.yml");
    }

    @Override
    public void loadConfigs() {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdirs();
        }

        saveDefaultConfig("config.yml");
        saveDefaultConfig("messages.yml");
        saveDefaultConfig("roles.yml");
        saveDefaultConfig("abilities.yml");

        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        roles = YamlConfiguration.loadConfiguration(rolesFile);
        abilities = YamlConfiguration.loadConfiguration(abilitiesFile);

        loadDefaults("messages.yml", messages);
    }

    private void saveDefaultConfig(String fileName) {
        File file = new File(plugin.getDataFolder(), fileName);
        if (!file.exists()) {
            plugin.saveResource(fileName, false);
        }
    }

    private void loadDefaults(String fileName, FileConfiguration yaml) {
        InputStream defConfigStream = plugin.getResource(fileName);
        if (defConfigStream != null) {
            yaml.setDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(defConfigStream, StandardCharsets.UTF_8)));
        }
    }

    @Override
    public void reloadConfigs() {
        config = YamlConfiguration.loadConfiguration(configFile);
        messages = YamlConfiguration.loadConfiguration(messagesFile);
        roles = YamlConfiguration.loadConfiguration(rolesFile);
        abilities = YamlConfiguration.loadConfiguration(abilitiesFile);
    }

    @Override
    public FileConfiguration getConfig() {
        return config;
    }

    @Override
    public FileConfiguration getMessages() {
        return messages;
    }

    @Override
    public FileConfiguration getRoles() {
        return roles;
    }

    @Override
    public FileConfiguration getAbilities() {
        return abilities;
    }
}
