package ru.hrp.roles;

import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import java.util.HashMap;
import java.util.Map;

public class AbilityBridge implements Listener {
    private final AbilityService abilityService;
    private final RoleService roleService;
    private final RoleCardFactory cardFactory;

    public AbilityBridge(AbilityService abilityService, RoleService roleService, RoleCardFactory cardFactory) {
        this.abilityService = abilityService;
        this.roleService = roleService;
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

        RoleId roleId;
        try {
            roleId = RoleId.valueOf(roleIdStr);
        } catch (IllegalArgumentException e) {
            return;
        }

        RoleDefinition definition = roleService.getDefinition(roleId);
        if (definition == null) return;

        AbilityContext context = createContext(event);

        for (String abilityId : definition.abilities()) {
            abilityService.activate(event.getPlayer().getUniqueId(), abilityId, context);
        }
    }

    /**
     * Helper to wrap data for AbilityContext.
     */
    private AbilityContext createContext(PlayerInteractEvent event) {
        Map<String, Object> data = new HashMap<>();
        Player player = event.getPlayer();
        data.put("player_name", player.getName());
        data.put("action_name", event.getAction().name());

        // Target detection
        Entity target = player.getTargetEntity(5); // 5 block range
        if (target instanceof Player targetPlayer) {
            data.put("target_uuid", targetPlayer.getUniqueId());
        }

        return new AbilityContext(data);
    }
}
