package ru.hrp.crime;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import ru.hrp.config.ConfigService;

import java.util.logging.Logger;

public class JailListener implements Listener {
    private final JailService jailService;
    private final ConfigService configService;
    private final Logger logger;

    public JailListener(JailService jailService, ConfigService configService, Logger logger) {
        this.jailService = jailService;
        this.configService = configService;
        this.logger = logger;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        if (jailService.isJailed(player.getUniqueId())) {
            teleportToJail(player);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        Player player = event.getPlayer();
        if (jailService.isJailed(player.getUniqueId())) {
            Location jailLoc = getJailLocation();
            if (jailLoc == null) return;

            // If player is too far from jail center (e.g. > 20 blocks), teleport back
            // This is a simple enforcement for Step 8
            if (!player.getWorld().equals(jailLoc.getWorld()) || player.getLocation().distanceSquared(jailLoc) > 400) {
                teleportToJail(player);
            }
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        if (jailService.isJailed(event.getPlayer().getUniqueId())) {
            Location jailLoc = getJailLocation();
            if (jailLoc != null) {
                event.setRespawnLocation(jailLoc);
            }
        }
    }

    private void teleportToJail(Player player) {
        Location loc = getJailLocation();
        if (loc != null) {
            player.teleport(loc);
        }
    }

    private Location getJailLocation() {
        ConfigurationSection section = configService.getConfig().getConfigurationSection("jail.location");
        if (section == null) {
            logger.warning("Jail location is not configured in config.yml!");
            return null;
        }

        String worldName = section.getString("world", "world");
        World world = Bukkit.getWorld(worldName);
        if (world == null) {
            logger.warning("Jail world '" + worldName + "' not found!");
            return null;
        }

        return new Location(
            world,
            section.getDouble("x"),
            section.getDouble("y"),
            section.getDouble("z"),
            (float) section.getDouble("yaw"),
            (float) section.getDouble("pitch")
        );
    }
}
