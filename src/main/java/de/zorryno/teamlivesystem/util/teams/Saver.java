package de.zorryno.teamlivesystem.util.teams;

import de.zorryno.teamlivesystem.Main;
import de.zorryno.zorrynosystems.config.Config;
import org.bukkit.Location;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.plugin.Plugin;

import java.util.*;

public class Saver {
    private List<Team> teams;
    private Config config;
    private Plugin plugin;

    public Saver(Plugin plugin) {
        this.plugin = plugin;
        config = new Config("Teams.yml", plugin);
        teams = new ArrayList<>();
    }

    public void addTeam(Team team) {
        if (Main.isDebugMode())
            plugin.getLogger().info("Addet Team " + team.getName());
        teams.add(team);
    }

    public void removeTeam(Team team) {
        if (Main.isDebugMode())
            plugin.getLogger().info("Removed Team " + team.getName());
        teams.remove(team);
    }

    public List<Team> getTeams() {
        return new ArrayList<>(teams);
    }

    public void save() {
        config.reset();
        if (Main.isDebugMode())
            plugin.getLogger().info("Total Teams: " + teams.size());

        for (Team team : teams) {
            List<String> admins = new ArrayList<>();
            for (UUID uuid : team.getAdmins())
                admins.add(uuid.toString());

            List<String> members = new ArrayList<>();
            for (UUID uuid : team.getMembers())
                members.add(uuid.toString());

            List<String> deathPlayers = new ArrayList<>();
            for (UUID uuid : team.getDeathPlayers())
                deathPlayers.add(uuid.toString());


            ConfigurationSection section = config.getConfig().createSection(team.getTeam().getName());
            section.set("Owner", team.getOwner().toString());
            section.set("DisplayName", team.getDisplayName());
            section.set("Prefix", team.getPrefix());
            section.set("Admins", admins);
            section.set("Members", members);
            ConfigurationSection locations = section.createSection("Blocks");

            Iterator<Location> iterator = team.getProtectedBlocks().iterator();
            for (int i = 0; iterator.hasNext(); i++) {
                locations.set(i + "", iterator.next());
            }

            section.set("Alliance", team.getAlliances());
            section.set("Lives", team.getLives());
            section.set("DeathPlayers", deathPlayers);
            if (Main.isDebugMode())
                plugin.getLogger().info("Team " + team.getName() + " successfully saved");
        }
        config.save();
    }

    public void load() {
        Set<String> keys = config.getConfig().getKeys(false);
        if (Main.isDebugMode())
            plugin.getLogger().info("Total Teams: " + keys.size());

        for (String key : keys) {
            ConfigurationSection section = config.getConfig().getConfigurationSection(key);
            UUID owner = UUID.fromString(section.getString("Owner"));
            String name = key;
            String displayName = section.getString("DisplayName");
            String prefix = section.getString("Prefix");

            List<UUID> admins = new ArrayList<>();
            for (String uuid : section.getStringList("Admins"))
                admins.add(UUID.fromString(uuid));

            List<UUID> members = new ArrayList<>();
            for (String uuid : section.getStringList("Members"))
                members.add(UUID.fromString(uuid));

            List<Location> protectedBlocks = new ArrayList<>();
            ConfigurationSection locations = section.getConfigurationSection("Blocks");
            if(locations != null) {
                for (String locationKey : locations.getKeys(false))
                    protectedBlocks.add(locations.getSerializable(locationKey, Location.class));
            }

            List<String> alliance = section.getStringList("Alliance");

            int lives = section.getInt("Lives", Main.getTeamStartLives());

            List<UUID> deathPlayers = new ArrayList<>();
            for (String uuid : section.getStringList("DeathPlayers"))
                deathPlayers.add(UUID.fromString(uuid));

            Team team = new Team(plugin, owner, name, displayName, prefix, admins, members, protectedBlocks, alliance, lives, deathPlayers);

            if (Main.isDebugMode())
                plugin.getLogger().info("Team " + team.getName() + " successfully loaded");
        }
    }

    public List<String> getNames() {
        List<String> names = new ArrayList<>();
        teams.forEach((team -> names.add(team.getName())));
        return names;
    }

    public Team getTeamByName(String name) {
        for (Team team : teams) {
            if (team.getName().equals(name))
                return team;
        }
        return null;
    }
}
