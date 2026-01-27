package ru.hrp.economy;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import org.bukkit.Bukkit;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.entity.Player;
import ru.hrp.config.ConfigService;
import ru.hrp.core.MessageService;

import java.math.BigDecimal;
import java.util.Map;

public class PayDayTask implements Runnable {
    private final ConfigService configService;
    private final PayDayService payDayService;
    private final MessageService messageService;

    public PayDayTask(ConfigService configService, PayDayService payDayService, MessageService messageService) {
        this.configService = configService;
        this.payDayService = payDayService;
        this.messageService = messageService;
    }

    @Override
    public void run() {
        BigDecimal baseSalary = new BigDecimal(configService.getConfig().getString("payday.base_salary", "500.00"));
        ConfigurationSection multiplierSection = configService.getConfig().getConfigurationSection("payday.multipliers");

        for (Player player : Bukkit.getOnlinePlayers()) {
            double multiplier = 1.0;

            if (multiplierSection != null) {
                for (String permission : multiplierSection.getKeys(false)) {
                    if (player.hasPermission(permission)) {
                        double val = multiplierSection.getDouble(permission);
                        if (val > multiplier) {
                            multiplier = val;
                        }
                    }
                }
            }

            PayDayResult result = payDayService.processPayDay(player.getUniqueId(), baseSalary, multiplier);

            if (result.success()) {
                messageService.sendMessage(player, "payday.received",
                    Placeholder.parsed("amount", result.amount().toPlainString()),
                    Placeholder.parsed("multiplier", String.valueOf(result.multiplier()))
                );
            }
        }
    }
}
