package de.scorezy.joinmessage;

import de.zorryno.teamlivesystem.Main;
import de.zorryno.teamlivesystem.util.teams.Team;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerQuitEvent;

public class JoinLeaveListener implements Listener {

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        String message = Main.getMessages().getCache().get("JoinMessage");
        if (team != null)
            message = message.replace("%name%", "§r" + team.getPrefix() + "§a" + player.getName());
        else
            message = message.replace("%name%", "§r" + "§a" + player.getName());

        message = PlaceholderAPI.setPlaceholders(player, message);
        event.setJoinMessage(message);
    }

    @EventHandler
    public void onLeave(PlayerQuitEvent event) {
        Player player = event.getPlayer();
        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        String message = Main.getMessages().getCache().get("LeaveMessage");
        if (team != null)
            message = message.replace("%name%", "§r" + team.getPrefix() + "§c" + player.getName());
        else
            message = message.replace("%name%", "§r" + "§c" + player.getName());

        message = PlaceholderAPI.setPlaceholders(player, message);
        event.setQuitMessage(message);
    }
}