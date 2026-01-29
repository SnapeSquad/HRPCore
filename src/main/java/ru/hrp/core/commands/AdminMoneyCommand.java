package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.economy.EconomyService;

import java.math.BigDecimal;
import java.util.UUID;

public class AdminMoneyCommand implements SubCommand {
    private final EconomyService economyService;
    private final MessageService messageService;

    public AdminMoneyCommand(EconomyService economyService, MessageService messageService) {
        this.economyService = economyService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 3) {
            messageService.sendRawMessage(sender, "<red>Usage: /hrp admin money <set|give|take> <player> <amount></red>");
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        BigDecimal amount;
        try {
            amount = new BigDecimal(args[2]);
        } catch (NumberFormatException e) {
            messageService.sendMessage(sender, "admin.invalid_amount");
            return;
        }

        UUID uuid = target.getUniqueId();
        switch (action) {
            case "set" -> economyService.setBalance(uuid, amount);
            case "give" -> economyService.deposit(uuid, amount);
            case "take" -> economyService.withdraw(uuid, amount);
            default -> {
                messageService.sendRawMessage(sender, "<red>Invalid action. Use set, give, or take.</red>");
                return;
            }
        }

        messageService.sendMessage(sender, "admin.money_updated",
            Placeholder.parsed("player", target.getName()),
            Placeholder.parsed("amount", economyService.getBalance(uuid).toPlainString())
        );
    }

    @Override
    public String getPermission() {
        return "hrp.admin.money";
    }
}
