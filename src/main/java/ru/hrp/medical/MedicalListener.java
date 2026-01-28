package ru.hrp.medical;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import org.bukkit.Bukkit;

import java.util.UUID;

public class MedicalListener implements Listener, MedicalStateChangeListener {
    private final MedicalService medicalService;

    public MedicalListener(MedicalService medicalService) {
        this.medicalService = medicalService;
        this.medicalService.registerChangeListener(this);
    }

    @EventHandler
    public void onDeath(PlayerDeathEvent event) {
        Player player = event.getPlayer();
        MedicalState state = medicalService.getMedicalState(player.getUniqueId());

        if (state == MedicalState.ALIVE) {
            // Cancel death and enter CRITICAL
            event.setCancelled(true);
            player.setHealth(1.0); // Keep alive with minimal health
            medicalService.setMedicalState(player.getUniqueId(), MedicalState.CRITICAL);
            applyCriticalEffects(player);
        } else if (state == MedicalState.CRITICAL) {
            // Already critical, now DEAD
            event.setCancelled(false); // Allow normal death flow
            medicalService.setMedicalState(player.getUniqueId(), MedicalState.DEAD);
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (!event.hasChangedBlock()) return;

        if (medicalService.getMedicalState(event.getPlayer().getUniqueId()) == MedicalState.CRITICAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent event) {
        if (medicalService.getMedicalState(event.getPlayer().getUniqueId()) == MedicalState.CRITICAL) {
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onCommand(PlayerCommandPreprocessEvent event) {
        if (medicalService.getMedicalState(event.getPlayer().getUniqueId()) == MedicalState.CRITICAL) {
            // Allow some basic commands if needed, but for now block all
            event.setCancelled(true);
        }
    }

    @EventHandler
    public void onRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (medicalService.getMedicalState(player.getUniqueId()) == MedicalState.DEAD) {
            // Reset to ALIVE after successful respawn
            medicalService.setMedicalState(player.getUniqueId(), MedicalState.ALIVE);
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    private void applyCriticalEffects(Player player) {
        player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, PotionEffect.INFINITE_DURATION, 1, false, false));
        player.addPotionEffect(new PotionEffect(PotionEffectType.SLOWNESS, PotionEffect.INFINITE_DURATION, 4, false, false));
    }

    @Override
    public void onEnterCritical(UUID uuid) {
        // Effects applied in onDeath event
    }

    @Override
    public void onRevive(UUID uuid) {
        Player player = Bukkit.getPlayer(uuid);
        if (player != null) {
            player.removePotionEffect(PotionEffectType.BLINDNESS);
            player.removePotionEffect(PotionEffectType.SLOWNESS);
        }
    }

    @Override
    public void onDeathFinal(UUID uuid) {
        // Handled in onRespawn or by natural death
    }
}
