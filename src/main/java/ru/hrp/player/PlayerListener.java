package ru.hrp.player;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import ru.hrp.core.MessageService;

public class PlayerListener implements Listener {
    private final PlayerDataService playerDataService;
    private final MessageService messageService;

    public PlayerListener(PlayerDataService playerDataService, MessageService messageService) {
        this.playerDataService = playerDataService;
        this.messageService = messageService;
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        event.joinMessage(null);

        playerDataService.loadPlayerData(event.getPlayer().getUniqueId(), event.getPlayer().getName())
            .thenAccept(player -> {
                // Future completes on main thread, safe to use Bukkit and MessageService
                if (player.firstJoin() == player.lastSeen()) {
                    messageService.sendMessage(event.getPlayer(), "player.first_join",
                        Placeholder.parsed("player_name", player.lastKnownName()));
                } else {
                    messageService.sendMessage(event.getPlayer(), "player.join",
                        Placeholder.parsed("player_name", player.lastKnownName()));
                }
            });
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent event) {
        event.quitMessage(null);
        playerDataService.unloadPlayerData(event.getPlayer().getUniqueId());
    }
}
