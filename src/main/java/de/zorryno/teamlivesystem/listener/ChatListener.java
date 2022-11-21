package de.zorryno.teamlivesystem.listener;

import de.zorryno.teamlivesystem.util.teams.Team;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;

public class ChatListener implements Listener {
    @EventHandler
    public void onChatMessage(AsyncPlayerChatEvent event) {
        Team team = Team.getTeamFromPlayer(event.getPlayer().getUniqueId());

        if(team == null)
            return;

        event.setCancelled(true);
        for(Player player : Bukkit.getOnlinePlayers())
            player.sendMessage(team.getPrefix() + " " + event.getPlayer().getName() + ": " + event.getMessage());
    }
}
