package ru.hrp.roles;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AbilityBridge implements Listener {
    private final AbilityService abilityService;
    private final RoleCardFactory cardFactory;

    public AbilityBridge(AbilityService abilityService, RoleCardFactory cardFactory) {
        this.abilityService = abilityService;
        this.cardFactory = cardFactory;
    }

    @EventHandler
    public void onCardInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }

        ItemStack item = event.getItem();
        if (item == null || !cardFactory.isRoleCard(item)) {
            return;
        }

        String roleIdStr = cardFactory.getRoleIdFromCard(item);
        if (roleIdStr == null) return;

        AbilityContext context = createContext(event);

        // At this stage, we assume MANUAL/ACTIVE abilities are triggered by card interaction
        // Logic will be refined when concrete abilities are added.
        abilityService.activate(event.getPlayer().getUniqueId(), "TEMPLATE_ABILITY", context);
    }

    /**
     * Helper to wrap data for AbilityContext.
     */
    private AbilityContext createContext(PlayerInteractEvent event) {
        Map<String, Object> data = new HashMap<>();
        data.put("player_name", event.getPlayer().getName());
        data.put("action_name", event.getAction().name());
        return new AbilityContext(data);
    }
}
