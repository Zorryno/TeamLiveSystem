package de.zorryno.teamlivesystem.placeholder;

import de.zorryno.teamlivesystem.util.teams.Team;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

public class TeamNamePlaceholder extends PlaceholderExpansion {
    @Override
    public @NotNull String getIdentifier() {
        return "TeamName";
    }

    @Override
    public @NotNull String getAuthor() {
        return "zorryno";
    }

    @Override
    public @NotNull String getVersion() {
        return "1.0";
    }

    @Override
    public String onRequest(OfflinePlayer player, String params) {
        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        if (team == null) return "-----";

        return team.getDisplayName();
    }

    @Override
    public boolean persist() {
        return true;
    }
}
