package ru.hrp.roles;

import ru.hrp.config.ConfigService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.logging.Logger;

public class AbilityManager implements AbilityService {
    private final Logger logger;
    private final ConfigService configService;
    private final CooldownService cooldownService;
    private final RoleService roleService;

    private final Map<String, AbilityDefinition> definitions = new HashMap<>();
    private final Map<String, AbilityExecutor> executors = new HashMap<>();

    public AbilityManager(Logger logger, ConfigService configService, CooldownService cooldownService, RoleService roleService) {
        this.logger = logger;
        this.configService = configService;
        this.cooldownService = cooldownService;
        this.roleService = roleService;
    }

    @Override
    public void loadDefinitions() {
        definitions.clear();
        var config = configService.getAbilities();
        for (String key : config.getKeys(false)) {
            try {
                String name = config.getString(key + ".name", key);
                String description = config.getString(key + ".description", "");
                AbilityType type = AbilityType.valueOf(config.getString(key + ".type", "ACTIVE"));
                AbilityTrigger trigger = AbilityTrigger.valueOf(config.getString(key + ".trigger", "MANUAL"));
                long cooldown = config.getLong(key + ".cooldown", 0);

                definitions.put(key, new AbilityDefinition(key, name, description, type, trigger, cooldown));
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid ability definition for: " + key);
            }
        }
        logger.info("Loaded " + definitions.size() + " ability definitions.");
    }

    @Override
    public void registerExecutor(String abilityId, AbilityExecutor executor) {
        executors.put(abilityId, executor);
    }

    @Override
    public boolean activate(UUID uuid, String abilityId, AbilityContext context) {
        AbilityDefinition definition = definitions.get(abilityId);
        if (definition == null) return false;

        // 1. Check if player has the role that provides this ability
        RoleId roleId = roleService.getActiveRoleId(uuid);
        RoleDefinition roleDef = roleService.getDefinition(roleId);
        if (roleDef == null || !roleDef.abilities().contains(abilityId)) {
            return false;
        }

        // 2. Check cooldown
        if (cooldownService.isOnCooldown(uuid, abilityId)) {
            return false;
        }

        // 3. Get executor
        AbilityExecutor executor = executors.get(abilityId);
        if (executor == null) {
            logger.warning("No executor registered for ability: " + abilityId);
            return false;
        }

        // 4. Execute
        boolean success = executor.execute(uuid, context);

        // 5. Set cooldown if successful
        if (success && definition.cooldownSeconds() > 0) {
            cooldownService.setCooldown(uuid, abilityId, definition.cooldownSeconds());
        }

        return success;
    }

    @Override
    public AbilityDefinition getDefinition(String abilityId) {
        return definitions.get(abilityId);
    }
}
