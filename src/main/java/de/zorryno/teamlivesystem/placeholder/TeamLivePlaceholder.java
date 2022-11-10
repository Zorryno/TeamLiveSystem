package de.zorryno.teamlivesystem.placeholder;

import de.zorryno.teamlivesystem.util.teams.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;

public class TeamLivePlaceholder extends PlaceholderExpansion {
    @Override
    public String getIdentifier() {
        return "TeamLives";
    }

    @Override
    public String getAuthor() {
        return "zorryno";
    }

    @Override
    public String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        if(team == null) return "--/--";

        return team.getLives() + "/" + team.getMaxLives();
    }

    @Override
    public boolean persist() {
        return true;
    }
}
