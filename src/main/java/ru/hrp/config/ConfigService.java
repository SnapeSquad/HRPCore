package ru.hrp.config;

import org.bukkit.configuration.file.FileConfiguration;

public interface ConfigService {
    void loadConfigs();
    void reloadConfigs();
    FileConfiguration getConfig();
    FileConfiguration getMessages();
    FileConfiguration getRoles();
    FileConfiguration getAbilities();
    FileConfiguration getTalents();
}
