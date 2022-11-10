package de.zorryno.teamlivesystem;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.ProtocolManager;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import de.scorezy.joinmessage.JoinLeaveListener;
import de.zorryno.teamlivesystem.commands.TeamCommand;
import de.zorryno.teamlivesystem.commands.TeamTabCompleter;
import de.zorryno.teamlivesystem.listener.ChatListener;
import de.zorryno.teamlivesystem.listener.LiveListener;
import de.zorryno.teamlivesystem.listener.LockEngine;
import de.zorryno.teamlivesystem.listener.SetScoreboardListener;
import de.zorryno.teamlivesystem.placeholder.TeamLivePlaceholder;
import de.zorryno.teamlivesystem.placeholder.TeamNamePlaceholder;
import de.zorryno.teamlivesystem.util.teams.Saver;
import de.zorryno.teamlivesystem.util.teams.Team;
import de.zorryno.zorrynosystems.config.Messages;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.event.HandlerList;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;

import java.time.DayOfWeek;
import java.time.LocalDateTime;

public final class Main extends JavaPlugin {

    private static Plugin plugin;
    private static Saver saver;
    private static Messages messages;
    private static boolean debugMode = false;
    private static int teamStartLives = 0;
    private static Location deathSpawn = null;

    @Override
    public void onLoad() {
        ProtocolManager protocolManager = ProtocolLibrary.getProtocolManager();
        protocolManager.addPacketListener(new PacketAdapter(
                this,
                ListenerPriority.NORMAL,
                PacketType.Play.Server.LOGIN
        ) {
            @Override
            public void onPacketSending(PacketEvent event) {
                event.getPacket().getBooleans().write(0, true);
            }
        });
    }

    @Override
    public void onEnable() {
        plugin = this;
        debugMode = getConfig().getBoolean("debugMode", false);
        if (debugMode)
            getLogger().info(ChatColor.DARK_RED + "DebugMode Enabled");
        teamStartLives = getConfig().getInt("TeamStartLives");
        deathSpawn = getConfig().getLocation("DeathSpawn");

        if (Bukkit.getPluginManager().getPlugin("PlaceholderAPI") != null) {
            new TeamLivePlaceholder().register();
            new TeamNamePlaceholder().register();
        }

        saver = new Saver(this);
        saver.load();
        messages = new Messages("messages.yml", this);
        saveDefaultConfig();
        getCommand("team").setExecutor(new TeamCommand(this));
        getCommand("team").setTabCompleter(new TeamTabCompleter(this));
        Bukkit.getPluginManager().registerEvents(new ChatListener(), this);
        Bukkit.getPluginManager().registerEvents(new SetScoreboardListener(), this);
        Bukkit.getPluginManager().registerEvents(new LockEngine(this), this);
        Bukkit.getPluginManager().registerEvents(new LiveListener(this), this);
        Bukkit.getPluginManager().registerEvents(new JoinLeaveListener(), this);


        Bukkit.getScheduler().scheduleSyncRepeatingTask(this, this::checkLiveRefill, 0, 20);
    }

    @Override
    public void onDisable() {
        saver.save();
        Team.unregisterAllTeams();
        HandlerList.unregisterAll(this);
        Bukkit.getScheduler().cancelTasks(this);
        HandlerList.unregisterAll(this);
    }

    public static Saver getSaver() {
        return saver;
    }

    public static Messages getMessages() {
        return messages;
    }

    public static boolean isDebugMode() {
        return debugMode;
    }

    public static int getTeamStartLives() {
        return teamStartLives;
    }

    public static Location getDeathSpawn() {
        return deathSpawn;
    }

    public static void setDeathSpawn(Location deathSpawn) {
        Main.deathSpawn = deathSpawn;
        plugin.getConfig().set("DeathSpawn", deathSpawn);
        plugin.saveConfig();
    }

    public static void reload() {
        getMessages().reload();
        plugin.reloadConfig();
        debugMode = plugin.getConfig().getBoolean("debugMode", false);
        if (debugMode)
            plugin.getLogger().info(ChatColor.DARK_RED + "DebugMode Enabled");

        teamStartLives = plugin.getConfig().getInt("TeamStartLives");
        deathSpawn = plugin.getConfig().getLocation("DeathSpawn");
    }

    public void checkLiveRefill() {
        LocalDateTime date = LocalDateTime.now();
        if (date.getDayOfWeek() != DayOfWeek.SUNDAY)
            return;

        if (date.getHour() != 23 || date.getMinute() != 59)
            return;

        if (date.getSecond() != 0)
            return;

        saver.getTeams().forEach(team -> {
            if(debugMode)
                getLogger().info("Adding Lives to Team " + team.getName());
            int maxLives = team.getMaxLives();
            for (int i = 0; i < team.getAddingLives(); i++) {
                if (team.getLives() < maxLives) {
                    team.addLives(1);
                    if (debugMode)
                        getLogger().info(team.getName() + " Lives: " + team.getLives() + "/" + maxLives);
                }
            }
        });
    }
}