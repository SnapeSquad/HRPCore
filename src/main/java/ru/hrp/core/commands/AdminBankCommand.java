package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.bank.BankService;
import ru.hrp.bank.CreditRecord;
import ru.hrp.core.MessageService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public class AdminBankCommand implements SubCommand {
    private final BankService bankService;
    private final MessageService messageService;

    public AdminBankCommand(BankService bankService, MessageService messageService) {
        this.bankService = bankService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageService.sendRawMessage(sender, "<red>Usage: /hrp admin bank <set|info> <player> [amount]</red>");
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        UUID uuid = target.getUniqueId();
        if (action.equals("set")) {
            if (args.length < 3) {
                messageService.sendRawMessage(sender, "<red>Usage: /hrp admin bank set <player> <amount></red>");
                return;
            }
            BigDecimal amount = new BigDecimal(args[2]);
            bankService.withdrawFromBank(uuid, bankService.getBankBalance(uuid)); // reset
            bankService.depositToBank(uuid, amount);
            messageService.sendMessage(sender, "admin.bank_updated",
                Placeholder.parsed("player", target.getName()),
                Placeholder.parsed("amount", amount.toPlainString())
            );
        } else if (action.equals("info")) {
            BigDecimal balance = bankService.getBankBalance(uuid);
            List<CreditRecord> credits = bankService.getCredits(uuid);
            messageService.sendRawMessage(sender, "<blue>Bank Info for " + target.getName() + ":</blue>");
            messageService.sendRawMessage(sender, "<gray>Balance: <gold>$" + balance.toPlainString() + "</gold></gray>");
            messageService.sendRawMessage(sender, "<gray>Active Credits: " + credits.size() + "</gray>");
            for (CreditRecord credit : credits) {
                messageService.sendRawMessage(sender, "<gray>- ID: " + credit.id() + " | Rem: <red>$" + credit.remainingAmount().toPlainString() + "</red> | Status: " + credit.status() + "</gray>");
            }
        }
    }

    @Override
    public String getPermission() {
        return "hrp.admin.bank";
    }
}
