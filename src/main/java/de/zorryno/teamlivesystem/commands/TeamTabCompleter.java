package de.zorryno.teamlivesystem.commands;

import de.zorryno.teamlivesystem.Main;
import de.zorryno.teamlivesystem.listener.LiveListener;
import de.zorryno.teamlivesystem.util.teams.Team;
import de.zorryno.teamlivesystem.util.teams.TeamInvite;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TeamTabCompleter implements TabCompleter {
    public TeamTabCompleter(Plugin plugin) {
        this.plugin = plugin;
    }

    private Plugin plugin;

    /*
    Normal Commands
    ** /team
    ** /team help
    ** /team info <teamName>
    ** /team accept
    ** /team deny
    *
    Member Commands
    ** /team lock
    ** /team unlock
    *
    Owner Commands
    ** /team alliance <teamName>
    ** /team alliance <teamName>
    ** /team setDisplayName <name>
    ** /team setPrefix <prefix>
    *
    OP Commands
    ** /team setOwner <teamName> <player>
    ** /team create <teamName> <displayName> <prefix> <owner>
    ** /team kick <teamName> <player>
    ** /team invite <teamName> <player>
    ** /team reload
    */

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        List<String> commands = new ArrayList<>();
        List<String> completions = new ArrayList<>();

        if (!(sender instanceof Player)) {
            return Collections.emptyList();
        }

        Player player = (Player) sender;
        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        boolean isTeamMember = team != null;
        boolean isTeamOwner = team != null && player.getUniqueId().equals(team.getOwner());
        TeamInvite invite = TeamInvite.getInvite(player.getUniqueId());
        if (args.length == 1) { //NoCondition
            commands.add("info");
            commands.add("help");

            if (isTeamMember) { //In A Team
                commands.add("lock");
                commands.add("unlock");
            }

            if (invite != null) { //openInvite
                commands.add("accept");
                commands.add("deny");
            }

            if (isTeamOwner) { //team Owner
                commands.add("setDisplayName");
                commands.add("setPrefix");
                commands.add("alliance");
            }

            if (player.isOp()) { //OP
                commands.add("setOwner");
                commands.add("create");
                commands.add("kick");
                commands.add("invite");
                commands.add("setdeathspawn");
                commands.add("setlive");
                commands.add("addlive");
                commands.add("livesystem");
                commands.add("reload");
            }
        }

        if (args.length == 2) {
            switch (args[0].toLowerCase()) {
                case "setowner":
                case "kick":
                case "invite":
                case "setlive":
                case "addlive":
                    if (!player.isOp())
                        break;
                case "info":
                    commands.addAll(Main.getSaver().getNames());
                    break;

                case "alliance":
                    if (!isTeamOwner)
                        break;

                    commands.addAll(Main.getSaver().getNames());
                    commands.remove(team.getName());
                    break;
                case "livesystem":
                    if (!player.isOp())
                        break;

                    if (LiveListener.isLiveSystemActive())
                        commands.add("off");
                    else
                        commands.add("on");
                    break;
            }
        }

        if (args.length == 3) {
            Team targetTeam = Team.getTeamByName(args[1]);
            if (targetTeam != null && player.isOp()) {
                switch (args[0].toLowerCase()) {
                    case "kick":
                        commands.addAll(targetTeam.getMemberNames());
                        break;

                    case "setowner":
                        commands.addAll(targetTeam.getMemberNames());
                        commands.remove(Bukkit.getOfflinePlayer(targetTeam.getOwner()).getName());
                        break;

                    case "invite":
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> commands.add(onlinePlayer.getName()));
                        break;
                }
            }
        }

        if (args.length == 5) {
            if (player.isOp()) {
                switch (args[0].toLowerCase()) {
                    case "create":
                        Bukkit.getOnlinePlayers().forEach(onlinePlayer -> commands.add(onlinePlayer.getName()));
                        break;
                }
            }
        }

        StringUtil.copyPartialMatches(args[args.length - 1] , commands, completions);
        Collections.sort(completions);
        return completions;
    }
}
