package de.zorryno.teamlivesystem.listener;

import de.zorryno.teamlivesystem.Main;
import de.zorryno.teamlivesystem.util.teams.Team;
import de.zorryno.zorrynosystems.playerhead.PlayerHeadAPI;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.SoundCategory;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.List;


public class LiveListener implements Listener {
    private Plugin plugin;
    private static boolean liveSystemActive = true;

    public LiveListener(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        if (!isLiveSystemActive()) {
            event.setKeepInventory(true);
            event.getDrops().clear();
            event.setKeepLevel(true);
            event.setDroppedExp(0);
            return;
        }

        Player player = event.getEntity();
        player.getWorld().strikeLightningEffect(player.getLocation());
        for(Player onlinePlayer : Bukkit.getOnlinePlayers())
            onlinePlayer.playSound(player.getLocation(), Sound.ENTITY_LIGHTNING_BOLT_THUNDER, SoundCategory.MASTER, 1, 1);

        List<String> deathMessages = Main.getMessages().getMessagesList("DeathMessages");
        if (!deathMessages.isEmpty()) {
            int random = (int) (Math.random() * deathMessages.size());
            String message = deathMessages.get(random).replace("%name%", event.getEntity().getName());
            event.setDeathMessage(" ");

            PlayerHeadAPI.broadcastPlayerHeadAsync(plugin, player, 8, message);
        }

        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        if (team == null)
            return;

        if (team.getDeathPlayers().contains(player.getUniqueId())) {
            team.removeDeathPlayer(player.getUniqueId());
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> holdPlayer(player), 10);
            event.setDeathMessage(null);
            return;
        }


        if (team.getLives() >= 1) {
            team.removeLives(1);
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> holdPlayer(player), 10);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Team team = Team.getTeamFromPlayer(event.getPlayer().getUniqueId());
        if (team == null) return;

        if (!team.getDeathPlayers().contains(event.getPlayer().getUniqueId())) return;

        if (team.getLives() <= 0) return;


        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> {
            releasePlayer(event.getPlayer());
            team.removeLives(1);
        });
    }

    public static void holdPlayer(Player player) {
        Team team = Team.getTeamFromPlayer(player.getUniqueId());

        team.addDeathPlayer(player.getUniqueId());
        player.spigot().respawn();

        Location location = Main.getDeathSpawn() != null ? Main.getDeathSpawn() : player.getWorld().getSpawnLocation();
        player.teleport(location); //Teleport to Specific Spawn or WorldSpawn
        player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, Integer.MAX_VALUE, 0, false, false, false));
    }

    public static void releasePlayer(Player player) {
        Team team = Team.getTeamFromPlayer(player.getUniqueId());

        team.removeDeathPlayer(player.getUniqueId());

        player.removePotionEffect(PotionEffectType.INVISIBILITY);
        player.teleport(getSpawnLocation(player)); //Teleport to Bed or WorldSpawn
    }

    private static Location getSpawnLocation(Player player) {
        Location location = player.getBedSpawnLocation() != null ? player.getBedSpawnLocation() : player.getWorld().getSpawnLocation();
        return location;
    }

    public static void setLiveSystemActive(boolean liveSystemActive) {
        LiveListener.liveSystemActive = liveSystemActive;
    }

    public static boolean isLiveSystemActive() {
        return liveSystemActive;
    }
}
