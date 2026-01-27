package ru.hrp.roles;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataType;

import java.util.List;
import java.util.stream.Collectors;

public class RoleCardFactory {
    private final NamespacedKey roleKey;
    private final MiniMessage miniMessage;

    public RoleCardFactory(NamespacedKey roleKey) {
        this.roleKey = roleKey;
        this.miniMessage = MiniMessage.miniMessage();
    }

    public ItemStack createCard(RoleDefinition definition) {
        ItemStack item = new ItemStack(Material.PAPER);
        ItemMeta meta = item.getItemMeta();

        if (meta != null) {
            meta.displayName(miniMessage.deserialize(definition.name()));

            List<Component> lore = definition.description().stream()
                .map(miniMessage::deserialize)
                .collect(Collectors.toList());
            meta.lore(lore);

            meta.getPersistentDataContainer().set(roleKey, PersistentDataType.STRING, definition.id().name());

            item.setItemMeta(meta);
        }

        return item;
    }

    public boolean isRoleCard(ItemStack item) {
        if (item == null || item.getItemMeta() == null) return false;
        return item.getItemMeta().getPersistentDataContainer().has(roleKey, PersistentDataType.STRING);
    }

    public String getRoleIdFromCard(ItemStack item) {
        if (!isRoleCard(item)) return null;
        return item.getItemMeta().getPersistentDataContainer().get(roleKey, PersistentDataType.STRING);
    }
}
