package ru.hrp.gui;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import ru.hrp.bank.BankService;
import ru.hrp.bank.CreditRecord;
import ru.hrp.config.ConfigService;
import ru.hrp.economy.EconomyService;
import ru.hrp.core.MessageService;
import ru.hrp.crime.CrimeRecord;
import ru.hrp.crime.CrimeService;
import ru.hrp.jail.JailService;
import ru.hrp.medical.MedicalData;
import ru.hrp.medical.MedicalService;
import ru.hrp.medical.MedicalState;
import ru.hrp.player.RPPlayer;
import ru.hrp.player.PlayerDataService;
import ru.hrp.roles.RoleDefinition;
import ru.hrp.roles.RoleId;
import ru.hrp.roles.RoleService;
import ru.hrp.talents.TalentDefinition;
import ru.hrp.talents.TalentId;
import ru.hrp.talents.TalentService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class GuiManager implements GuiService {
    private final PlayerDataService playerDataService;
    private final RoleService roleService;
    private final TalentService talentService;
    private final EconomyService economyService;
    private final BankService bankService;
    private final CrimeService crimeService;
    private final JailService jailService;
    private final MedicalService medicalService;
    private final MessageService messageService;
    private final ConfigService configService;

    public GuiManager(PlayerDataService playerDataService, RoleService roleService, TalentService talentService,
                      EconomyService economyService, BankService bankService, CrimeService crimeService,
                      JailService jailService, MedicalService medicalService, MessageService messageService,
                      ConfigService configService) {
        this.playerDataService = playerDataService;
        this.roleService = roleService;
        this.talentService = talentService;
        this.economyService = economyService;
        this.bankService = bankService;
        this.crimeService = crimeService;
        this.jailService = jailService;
        this.medicalService = medicalService;
        this.messageService = messageService;
        this.configService = configService;
    }

    @Override
    public void openGui(Player player, GuiType type) {
        Inventory inv = switch (type) {
            case STATUS -> createStatusGui(player);
            case ROLE -> createRoleGui(player);
            case TALENT -> createTalentGui(player);
            case BANK -> createBankGui(player);
        };
        player.openInventory(inv);
    }

    private Inventory createStatusGui(Player player) {
        Inventory inv = Bukkit.createInventory(new HrpGuiHolder(GuiType.STATUS), 27, messageService.parse("gui.status.title"));
        UUID uuid = player.getUniqueId();

        // 1. Role Item
        RoleId roleId = roleService.getActiveRoleId(uuid);
        RoleDefinition roleDef = roleService.getDefinition(roleId);
        inv.setItem(10, createDisplayItem(Material.NAME_TAG, "gui.status.item_role",
            List.of("<gray>Current Role: <yellow>" + (roleDef != null ? roleDef.name() : "None") + "</yellow>")));

        // 2. Talent Item
        inv.setItem(11, createDisplayItem(Material.BOOK, "gui.status.item_talents",
            List.of("<gray>Manage your talents and progression.</gray>")));

        // 3. Bank Item
        inv.setItem(12, createDisplayItem(Material.GOLD_INGOT, "gui.status.item_bank",
            List.of("<gray>Bank Balance: <gold>$" + bankService.getBankBalance(uuid).toPlainString() + "</gold>")));

        // 4. Medical Item
        MedicalState medicalState = medicalService.getMedicalState(uuid);
        Material medMat = switch (medicalState) {
            case ALIVE -> Material.APPLE;
            case CRITICAL -> Material.RED_DYE;
            case DEAD -> Material.SKELETON_SKULL;
        };
        inv.setItem(14, createDisplayItem(medMat, "gui.status.item_medical",
            List.of("<gray>Condition: <red>" + medicalState.name() + "</red>")));

        // 5. Jail Item
        boolean jailed = jailService.isJailed(uuid);
        inv.setItem(15, createDisplayItem(Material.IRON_BARS, "gui.status.item_jail",
            List.of("<gray>Jail Status: <red>" + (jailed ? "Jailed" : "Free") + "</red>")));

        return inv;
    }

    private Inventory createRoleGui(Player player) {
        Inventory inv = Bukkit.createInventory(new HrpGuiHolder(GuiType.ROLE), 27, messageService.parse("gui.role.title"));
        UUID uuid = player.getUniqueId();

        RoleId roleId = roleService.getActiveRoleId(uuid);
        RoleDefinition roleDef = roleService.getDefinition(roleId);

        if (roleDef != null) {
            List<String> lore = new ArrayList<>(roleDef.description());
            lore.add("");
            lore.add("<gray>Abilities:</gray>");
            for (String ability : roleDef.abilities()) {
                lore.add("<gray>- <blue>" + ability + "</blue></gray>");
            }
            inv.setItem(13, createDisplayItem(Material.PAPER, roleDef.name(), lore));
        } else {
            inv.setItem(13, createDisplayItem(Material.BARRIER, "<red>No Active Role</red>", List.of()));
        }

        addBackButton(inv);
        return inv;
    }

    private Inventory createTalentGui(Player player) {
        Inventory inv = Bukkit.createInventory(new HrpGuiHolder(GuiType.TALENT), 27, messageService.parse("gui.talent.title"));
        UUID uuid = player.getUniqueId();

        Map<TalentId, Integer> talents = talentService.getTalents(uuid);
        int slot = 10;
        for (TalentId id : TalentId.values()) {
            TalentDefinition def = talentService.getDefinition(id);
            if (def == null) continue;

            int level = talents.getOrDefault(id, 0);
            List<String> lore = new ArrayList<>(def.description());
            lore.add("");
            lore.add("<gray>Level: <yellow>" + level + " / " + def.maxLevel() + "</yellow></gray>");

            inv.setItem(slot++, createDisplayItem(Material.ENCHANTED_BOOK, def.name(), lore));
            if (slot > 16) break;
        }

        addBackButton(inv);
        return inv;
    }

    private Inventory createBankGui(Player player) {
        Inventory inv = Bukkit.createInventory(new HrpGuiHolder(GuiType.BANK), 27, messageService.parse("gui.bank.title"));
        UUID uuid = player.getUniqueId();

        inv.setItem(11, createDisplayItem(Material.GOLD_INGOT, "<gold>Bank Balance</gold>",
            List.of("<gray>Stored: <gold>$" + bankService.getBankBalance(uuid).toPlainString() + "</gold>")));

        List<CreditRecord> credits = bankService.getCredits(uuid);
        if (!credits.isEmpty()) {
            CreditRecord credit = credits.get(0); // Show first for Step 11
            List<String> lore = List.of(
                "<gray>Principal: <gold>$" + credit.principal().toPlainString() + "</gold></gray>",
                "<gray>Remaining: <red>$" + credit.remainingAmount().toPlainString() + "</red></gray>",
                "<gray>Status: " + credit.status().name() + "</gray>"
            );
            inv.setItem(15, createDisplayItem(Material.WRITABLE_BOOK, "<yellow>Active Credit</yellow>", lore));
        } else {
            inv.setItem(15, createDisplayItem(Material.MAP, "<gray>No active credits</gray>", List.of()));
        }

        addBackButton(inv);
        return inv;
    }

    private void addBackButton(Inventory inv) {
        inv.setItem(22, createDisplayItem(Material.ARROW, "gui.common.back", List.of()));
    }

    private ItemStack createDisplayItem(Material material, String nameKey, List<String> loreStrings) {
        ItemStack item = new ItemStack(material);
        ItemMeta meta = item.getItemMeta();
        if (meta != null) {
            // Try parsing as key, if failed use as raw string (simulated)
            Component name = messageService.parse(nameKey);
            meta.displayName(name);

            List<Component> lore = new ArrayList<>();
            for (String s : loreStrings) {
                lore.add(net.kyori.adventure.text.minimessage.MiniMessage.miniMessage().deserialize(s));
            }
            meta.lore(lore);
            item.setItemMeta(meta);
        }
        return item;
    }

    // Custom InventoryHolder to identify our GUIs
    public static class HrpGuiHolder implements InventoryHolder {
        private final GuiType type;
        public HrpGuiHolder(GuiType type) { this.type = type; }
        @Override public Inventory getInventory() { return null; }
        public GuiType getType() { return type; }
    }
}
