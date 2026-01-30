package ru.hrp.jobs;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.Ageable;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;

public class JobListener implements Listener {
    private final JobService jobService;

    public JobListener(JobService jobService) {
        this.jobService = jobService;
    }

    @EventHandler(priority = EventPriority.MONITOR, ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
        Block block = event.getBlock();
        Material type = block.getType();

        // Farmer check: Only reward fully grown crops
        if (isCrop(type)) {
            if (block.getBlockData() instanceof Ageable ageable) {
                if (ageable.getAge() < ageable.getMaximumAge()) {
                    return; // Not fully grown
                }
            }
        }

        jobService.processAction(event.getPlayer().getUniqueId(), type.name());
    }

    private boolean isCrop(Material material) {
        return material == Material.WHEAT || material == Material.CARROTS ||
               material == Material.POTATOES || material == Material.PUMPKIN ||
               material == Material.MELON;
    }
}
