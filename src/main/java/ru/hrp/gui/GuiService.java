package ru.hrp.gui;

import org.bukkit.entity.Player;

public interface GuiService {
    /**
     * Opens a specific GUI for a player, optionally viewing another player's data.
     * @param viewer The player who sees the GUI.
     * @param type The GUI type to open.
     * @param target The player whose data is being viewed.
     */
    void openGui(Player viewer, GuiType type, Player target);

    default void openGui(Player player, GuiType type) {
        openGui(player, type, player);
    }
}
