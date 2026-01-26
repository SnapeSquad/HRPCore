package ru.hrp.player;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class PlayerListener implements Listener {
    private final PlayerService playerService;

    public PlayerListener(PlayerService playerService) {
        this.playerService = playerService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Disable default join message as we handle it in PlayerService
        event.joinMessage(null);
        playerService.handlePlayerJoin(event.getPlayer().getUniqueId(), event.getPlayer().getName());
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        // Disable default quit message
        event.quitMessage(null);
        playerService.handlePlayerQuit(event.getPlayer().getUniqueId());
    }
}
