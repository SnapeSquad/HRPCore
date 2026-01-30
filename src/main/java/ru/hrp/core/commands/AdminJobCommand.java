package ru.hrp.core.commands;

import net.kyori.adventure.text.minimessage.tag.resolver.Placeholder;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import ru.hrp.core.MessageService;
import ru.hrp.jobs.JobId;
import ru.hrp.jobs.JobService;

import java.util.UUID;

public class AdminJobCommand implements SubCommand {
    private final JobService jobService;
    private final MessageService messageService;

    public AdminJobCommand(JobService jobService, MessageService messageService) {
        this.jobService = jobService;
        this.messageService = messageService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (args.length < 2) {
            messageService.sendMessage(sender, "admin.job.usage");
            return;
        }

        String action = args[0].toLowerCase();
        Player target = Bukkit.getPlayer(args[1]);
        if (target == null) {
            messageService.sendMessage(sender, "admin.player_not_found");
            return;
        }

        UUID uuid = target.getUniqueId();
        switch (action) {
            case "clear" -> {
                jobService.setJob(uuid, JobId.NONE);
                messageService.sendMessage(sender, "admin.job.cleared", Placeholder.unparsed("player", target.getName()));
            }
            case "set" -> {
                if (args.length < 3) {
                    messageService.sendMessage(sender, "admin.job.usage_set");
                    return;
                }
                try {
                    JobId jobId = JobId.valueOf(args[2].toUpperCase());
                    jobService.setJob(uuid, jobId);
                    messageService.sendMessage(sender, "admin.job.set",
                        TagResolver.resolver(Placeholder.unparsed("player", target.getName()), Placeholder.unparsed("job", jobId.name())));
                } catch (IllegalArgumentException e) {
                    messageService.sendMessage(sender, "admin.job.invalid_job");
                }
            }
            case "info" -> {
                JobId currentJob = jobService.getJob(uuid);
                messageService.sendMessage(sender, "admin.job.info.header", Placeholder.unparsed("player", target.getName()));
                messageService.sendMessage(sender, "admin.job.info.current", Placeholder.unparsed("job", currentJob.name()));
            }
            default -> messageService.sendMessage(sender, "admin.job.unknown_action");
        }
    }

    @Override
    public String getPermission() {
        return "hrp.admin.job";
    }
}
