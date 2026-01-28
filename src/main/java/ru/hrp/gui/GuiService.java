package ru.hrp.gui;

import org.bukkit.entity.Player;

public interface GuiService {
    /**
     * Opens a specific GUI for a player.
     * @param player The player.
     * @param type The GUI type to open.
     */
    void openGui(Player player, GuiType type);
}
