package ru.hrp.gui;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.InventoryHolder;

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
        Player player = (Player) event.getWhoClicked();

        int slot = event.getRawSlot();
        GuiType type = hrpHolder.getType();

        // 1. Check for "Back" button (Slot 22)
        if (slot == 22 && type != GuiType.STATUS) {
            guiService.openGui(player, GuiType.STATUS);
            return;
        }

        // 2. Main Menu Navigation
        if (type == GuiType.STATUS) {
            switch (slot) {
                case 10 -> guiService.openGui(player, GuiType.ROLE);
                case 11 -> guiService.openGui(player, GuiType.TALENT);
                case 12 -> guiService.openGui(player, GuiType.BANK);
            }
        }
    }
}
