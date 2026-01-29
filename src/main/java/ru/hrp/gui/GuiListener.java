package ru.hrp.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

import java.util.UUID;

public class GuiListener implements Listener {
    private final GuiService guiService;

    public GuiListener(GuiService guiService) {
        this.guiService = guiService;
    }

    @EventHandler
    public void onClick(InventoryClickEvent event) {
        InventoryHolder holder = event.getInventory().getHolder();
        if (!(holder instanceof GuiManager.HrpGuiHolder hrpHolder)) {
            return;
        }

        event.setCancelled(true);
        Player viewer = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        GuiType type = hrpHolder.getType();
        UUID targetUuid = hrpHolder.getTargetUuid();
        Player target = Bukkit.getPlayer(targetUuid);
        if (target == null) {
            viewer.closeInventory();
            return;
        }

        // 1. Check for "Back" button (Slot 22)
        if (slot == 22 && type != GuiType.STATUS) {
            guiService.openGui(viewer, GuiType.STATUS, target);
            return;
        }

        // 2. Main Menu Navigation
        if (type == GuiType.STATUS) {
            switch (slot) {
                case 10 -> guiService.openGui(viewer, GuiType.ROLE, target);
                case 11 -> guiService.openGui(viewer, GuiType.TALENT, target);
                case 12 -> guiService.openGui(viewer, GuiType.BANK, target);
            }
        }
    }
}
