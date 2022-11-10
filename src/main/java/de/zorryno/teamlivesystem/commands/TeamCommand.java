package de.zorryno.teamlivesystem.commands;

import de.zorryno.teamlivesystem.Main;
import de.zorryno.teamlivesystem.listener.LiveListener;
import de.zorryno.teamlivesystem.listener.LockEngine;
import de.zorryno.teamlivesystem.util.teams.Team;
import de.zorryno.teamlivesystem.util.teams.TeamInvite;
import org.bukkit.*;
import org.bukkit.block.*;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;


public class TeamCommand implements CommandExecutor {
    public TeamCommand(Plugin plugin) {
        this.plugin = plugin;
        BLACKLIST = plugin.getConfig().getStringList("blackList");
    }

    private final Plugin plugin;
    private List<String> BLACKLIST;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player player))
            return true;

        Block targetBlock = player.getTargetBlock(null, 5);
        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        boolean isTeamMember = team != null;
        TeamInvite invite = TeamInvite.getInvite(player.getUniqueId());

        // Send Team List
        if (args.length == 0) { // /team
            player.sendMessage(Main.getMessages().getCache().get("TeamListHeader"));
            Main.getSaver().getTeams().forEach(existingTeam -> player.sendMessage(existingTeam.getName()));
            return true;
        }

        switch (args[0].toLowerCase()) {
            // Send Plugin Help
            case "help" -> // /Team
                    // help
                    sendHelp(player);


            // Send Team
            // Info
            case "info" -> { // /Team
                // info <Team
                // Name>
                if (args.length == 1) {
                    if (isTeamMember)
                        team.sendTeamInfo(player);
                    else
                        player.sendMessage(Main.getMessages().getCache().get("NotATeamMember"));
                    return true;
                }
                if (args.length != 2) {
                    player.sendMessage("/Team info <TeamName>");
                    return true;
                }
                Team targetTeam = Team.getTeamByName(args[1]);
                if (targetTeam == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamNotFound"));
                    return true;
                }
                targetTeam.sendTeamInfo(player);
            }

            // Create a new Team

            case "create" -> { // /Team
                // create <name> <displayName> <prefix> <owner>
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }
                if (args.length != 5) {
                    player.sendMessage("/Team create <name> <displayName> <prefix> <owner>");
                    return true;
                }
                List<Illegal> illegals = getIllegals(args[1], args[2], args[3], false);
                if (illegals.isEmpty()) {
                    Player owner = Bukkit.getPlayer(args[4]);
                    if (owner == null) {
                        player.sendMessage(Main.getMessages().getCache().get("PlayerNotFound"));
                        return true;
                    }
                    if (Team.getTeamFromPlayer(owner.getUniqueId()) != null) {
                        player.sendMessage(Main.getMessages().getCache().get("AlreadyInATeamOther"));
                        return true;
                    }

                    new Team(plugin, owner.getUniqueId(), args[1], args[2], args[3]);
                    player.sendMessage(Main.getMessages().getCache().get("TeamCreated").replace("%name%", args[1]));
                } else {
                    illegals.forEach(illegal -> player.sendMessage(Main.getMessages().getCache().get("IllegalTeamName").replace("%part%", illegal.name)));
                }
                return true;
            }


            // Accept Invite
            case "accept" -> { // /Team
                // accept
                if (invite == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamInvite.NotInvited"));
                    return true;
                }
                if (team != null) {
                    player.sendMessage(Main.getMessages().getCache().get("AlreadyInATeam"));
                    return true;
                }
                invite.accept();
                invite.getInviter().sendMessage(Main.getMessages().getCache().get("TeamInvite.InviteAcceptInviter").replace("%name%", player.getName()));
                player.sendMessage(Main.getMessages().getCache().get("TeamInvite.InviteAcceptInvited"));
            }

            // Deny Invite
            case "deny" -> { // /Team
                // deny
                if (invite == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamInvite.NotInvited"));
                    return true;
                }
                if (team != null) {
                    player.sendMessage(Main.getMessages().getCache().get("AlreadyInATeam"));
                    return true;
                }
                invite.deny();
                invite.getInviter().sendMessage(Main.getMessages().getCache().get("TeamInvite.InviteDenyInviter").replace("%name%", player.getName()));
                player.sendMessage(Main.getMessages().getCache().get("TeamInvite.InviteDenyInvited"));
            }
            case "lock" -> { // /Team
                // lock
                if (team == null) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamMember"));
                    break;
                }
                if (targetBlock == null || !(targetBlock.getState() instanceof Container container))
                    break;
                Team lockTeam = LockEngine.getOwnerTeam(targetBlock.getLocation());
                if (lockTeam != null && !lockTeam.equals(team)) {
                    player.sendMessage(Main.getMessages().getCache().get("ProtectedBlock.NoPermission").replace("%Team%", lockTeam.getDisplayName()));
                    break;
                }

                boolean locked = false;
                if (targetBlock.getState() instanceof Chest chest && chest.getInventory() instanceof DoubleChestInventory) {
                    DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
                    if (doubleChest != null) {
                        doubleChest.getInventory().getViewers().forEach(HumanEntity::closeInventory);
                        Location rightLocation = ((Chest) doubleChest.getRightSide()).getLocation();
                        Location leftLocation = ((Chest) doubleChest.getLeftSide()).getLocation();

                        locked = team.addBlock(rightLocation) | team.addBlock(leftLocation);
                    }
                } else {
                    if (team.addBlock(targetBlock.getLocation())) {
                        container.getInventory().getViewers().forEach(HumanEntity::closeInventory);
                        locked = true;
                    }
                }
                if (locked)
                    player.sendMessage(Main.getMessages().getCache().get("ProtectedBlock.Locked"));
                else
                    player.sendMessage(Main.getMessages().getCache().get("ProtectedBlock.AlreadyLocked"));
            }
            case "unlock" -> { // /Team
                // unlock
                if (team == null) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamMember"));
                    break;
                }
                if (targetBlock == null || !(targetBlock.getState() instanceof Container))
                    break;
                Team unlockTeam = LockEngine.getOwnerTeam(targetBlock.getLocation());
                if (unlockTeam != null && !unlockTeam.equals(team)) {
                    player.sendMessage(Main.getMessages().getCache().get("ProtectedBlock.NoPermission").replace("%Team%", unlockTeam.getDisplayName()));
                    break;
                }

                boolean unlocked = false;
                if (targetBlock.getState() instanceof Chest chest && chest.getInventory() instanceof DoubleChestInventory) {
                    DoubleChest doubleChest = (DoubleChest) chest.getInventory().getHolder();
                    if (doubleChest != null) {
                        doubleChest.getInventory().getViewers().forEach(HumanEntity::closeInventory);
                        Location rightLocation = ((Chest) doubleChest.getRightSide()).getLocation();
                        Location leftLocation = ((Chest) doubleChest.getLeftSide()).getLocation();

                        unlocked = team.removeBlock(rightLocation) | team.removeBlock(leftLocation);
                    }
                } else {
                    if (team.removeBlock(targetBlock.getLocation()))
                        unlocked = true;
                }
                if (unlocked)
                    player.sendMessage(Main.getMessages().getCache().get("ProtectedBlock.Unlocked"));
                else
                    player.sendMessage(Main.getMessages().getCache().get("ProtectedBlock.AlreadyUnlocked"));
                team.removeBlock(targetBlock.getLocation());
            }

            // Invite a Player in your Team

            case "invite" -> { // /Team
                // invite <team> <player>
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }
                if (args.length != 3) {
                    player.sendMessage("/Team invite <team> <player>");
                    return true;
                }
                Team targetTeam = Team.getTeamByName(args[1]);
                if (targetTeam == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamNotFound"));
                    return true;
                }

                Player inviteTarget = Bukkit.getPlayer(args[2]);
                if (inviteTarget == null) {
                    player.sendMessage(Main.getMessages().getCache().get("PlayerNotFound"));
                    return true;
                }

                if (targetTeam.getMembers().contains(inviteTarget.getUniqueId())) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamInvite.AlreadyInTeam"));
                    return true;
                }
                //send invite
                new TeamInvite(targetTeam, player, inviteTarget, plugin, 20 * 60 * 5).sendInvite();
                player.sendMessage(Main.getMessages().getCache().get("TeamInvite.InvitePlayer").replace("%name%", inviteTarget.getName()));
            }

            // Kick A Player
            case "kick" -> { // /Team
                // kick <team> <player>
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }
                if (args.length != 3) {
                    player.sendMessage("/Team kick <team> <player>");
                    return true;
                }
                Team targetTeam = Team.getTeamByName(args[1]);
                if (targetTeam == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamNotFound"));
                    return true;
                }
                UUID kickTarget = getUUID(args[2]);
                if (kickTarget == null) {
                    player.sendMessage(Main.getMessages().getCache().get("PlayerNotFound"));
                    return true;
                }

                if (targetTeam.removeMember(kickTarget))
                    player.sendMessage(Main.getMessages().getCache().get("MemberRemoved"));
                else
                    player.sendMessage(Main.getMessages().getCache().get("MemberNotInTeam"));
            }

            case "setlive" -> {
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }
                if (args.length != 3) {
                    player.sendMessage("/Team setlive <team> <amount>");
                    return true;
                }
                Team targetTeam = Team.getTeamByName(args[1]);
                if (targetTeam == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamNotFound"));
                    return true;
                }

                int lives = 0;
                try {
                    lives = Integer.parseInt(args[2]);
                } catch (NumberFormatException exception) {
                    player.sendMessage("§cInvalid number");
                    break;
                }

                targetTeam.setLives(Math.max(0, lives));
                player.sendMessage(targetTeam.getName() + " Lives: " + targetTeam.getLives());
            }

            case "setdeathspawn" -> {
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }
                Main.setDeathSpawn(player.getLocation());
                player.sendMessage("Death Spawn set to your current Location");
            }

            case "livesystem" -> {
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }

                if (args.length == 1) {
                    String state = LiveListener.isLiveSystemActive() ? "Enabled" : "Disabled";
                    player.sendMessage("Livesystem: " + state);
                    return true;
                }

                if (args.length != 2) {
                    player.sendMessage("/Team livesystem [on|off]");
                    return true;
                }

                switch (args[1].toLowerCase()) {
                    case "on" -> LiveListener.setLiveSystemActive(true);
                    case "off" -> LiveListener.setLiveSystemActive(false);
                    default -> {
                        player.sendMessage("/Team livesystem [on|off]");
                        return true;
                    }
                }

                String state = LiveListener.isLiveSystemActive() ? "Enabled" : "Disabled";
                player.sendMessage("Livesystem: " + state);
            }

            //Owner Commands

            // Sets the Owner of this Team

            case "setowner" -> { // /Team
                // setOwner <team> <player>
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }
                if (args.length != 3) {
                    player.sendMessage("/Team setOwner <team> <player>");
                    return true;
                }
                Team targetTeam = Team.getTeamByName(args[1]);
                if (targetTeam == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamNotFound"));
                    return true;
                }
                UUID ownerTarget = getUUID(args[2]);
                if (ownerTarget == null) {
                    player.sendMessage(Main.getMessages().getCache().get("PlayerNotFound"));
                    return true;
                }
                if (!targetTeam.isInTeam(ownerTarget)) {
                    player.sendMessage(Main.getMessages().getCache().get("MemberNotInTeam"));
                    return true;
                }
                if (targetTeam.setOwner(ownerTarget))
                    player.sendMessage(Main.getMessages().getCache().get("SetOwner"));
            }

            // Sets the displayName of this Team

            case "setdisplayname" -> { // /Team
                // setDisplayName <name>
                if (team == null) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamMember"));
                    return true;
                }
                if (!player.getUniqueId().equals(team.getOwner())) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamOwner"));
                    return true;
                }
                if (args.length != 2) {
                    player.sendMessage("/Team setDisplayName <name>");
                    return true;
                }
                List<Illegal> illegalDisplayName = getIllegals(team.getName(), args[1], team.getPrefix().substring(1, team.getPrefix().length() - 1), true);
                illegalDisplayName.forEach(illegal -> player.sendMessage(Main.getMessages().getCache().get("IllegalTeamName").replace("%part%", illegal.name)));
                if (!illegalDisplayName.isEmpty())
                    return true;
                team.setDisplayName(args[1]);
            }

            // Sets the Prefix of this Team

            case "setprefix" -> { // /Team
                // setPrefix <prefix>
                if (team == null) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamMember"));
                    return true;
                }
                if (!player.getUniqueId().equals(team.getOwner())) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamOwner"));
                    return true;
                }
                if (args.length != 2) {
                    player.sendMessage("/Team setPrefix <prefix>");
                    return true;
                }
                List<Illegal> illegalPrefix = getIllegals(team.getName(), team.getDisplayName(), args[1], true);
                illegalPrefix.forEach(illegal -> player.sendMessage(Main.getMessages().getCache().get("IllegalTeamName").replace("%part%", illegal.name)));
                if (!illegalPrefix.isEmpty())
                    return true;
                team.setPrefix(args[1]);
            }

            case "alliance" -> {
                if (team == null) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamMember"));
                    return true;
                }
                if (!player.getUniqueId().equals(team.getOwner())) {
                    player.sendMessage(Main.getMessages().getCache().get("NotATeamOwner"));
                    return true;
                }
                if (args.length != 2) {
                    player.sendMessage("/Team alliance <TeamName>");
                    return true;
                }

                Team targetTeam = Main.getSaver().getTeamByName(args[1]);
                if (targetTeam == null) {
                    player.sendMessage(Main.getMessages().getCache().get("TeamNotFound"));
                    return true;
                }

                if (team.equals(targetTeam)) {
                    player.sendMessage(Main.getMessages().getCache().get("Alliance.CantAllianceSelf"));
                    return true;
                }

                if (team.isInAlliance(targetTeam)) {
                    team.removeAlliance(targetTeam);
                    player.sendMessage(Main.getMessages().getCache().get("Alliance.Removed").replace("%Team%", targetTeam.getName()));
                } else {
                    team.addAlliance(targetTeam);
                    player.sendMessage(Main.getMessages().getCache().get("Alliance.Created").replace("%Team%", targetTeam.getName()));
                }
            }


            //OP Commands

            //Reload messages and Config
            case "reload" -> {  // /Team
                // reload
                if (!player.isOp()) {
                    player.sendMessage(Main.getMessages().getCache().get("NoPermission"));
                    return true;
                }

                Main.reload();
                BLACKLIST = plugin.getConfig().getStringList("blackList");
                player.sendMessage("Config reloaded");
            }
        }
        return true;
    }

    private void sendHelp(Player player) {
        Main.getMessages().getMessagesList("HelpPage").forEach(player::sendMessage);
    }

    /**
     * Let a Player leave his Team
     *
     * @param player the Player
     */
    private void leave(Player player) {
        Team team = Team.getTeamFromPlayer(player.getUniqueId());
        if (team == null) {
            player.sendMessage(Main.getMessages().getCache().get("NotATeamMember"));
            return;
        }

        if (team.removeMember(player.getUniqueId())) {
            player.sendMessage(Main.getMessages().getCache().get("PlayerLeaveTeam"));
            if (player.getUniqueId().equals(team.getOwner())) {
                team.ownerLeave();
            }
        }
    }

    /**
     * Get the UUID from A name
     *
     * @param name The Players Name
     * @return The UUID from this Player or null if the Player does not exist
     */
    public static UUID getUUID(String name) {
        if (name == null) return null;
        for (OfflinePlayer offlinePlayer : Bukkit.getOfflinePlayers()) {
            if (name.equals(offlinePlayer.getName())) {
                return offlinePlayer.getUniqueId();
            }
        }
        return null;
    }

    /**
     * Returns Illegals with this Names
     *
     * @param name        the Team
     *                    Name
     * @param displayName The DisplayName
     * @param prefix      The Team
     *                    Prefix
     * @return Illegals
     */
    private List<Illegal> getIllegals(String name, String displayName, String prefix, boolean ignoreExistingTeams) {
        List<Illegal> illegal = new ArrayList<>();

        for (String badName : BLACKLIST) {
            if (name.contains(badName))
                illegal.add(Illegal.NAME);

            if (displayName.contains(badName))
                illegal.add(Illegal.DISPLAYNAME);

            if (prefix.contains(badName) || ChatColor.stripColor(ChatColor.translateAlternateColorCodes('&', prefix)).length() > 6)
                illegal.add(Illegal.PREFIX);
        }

        if (!ignoreExistingTeams && Main.getSaver().getNames().contains(name)) {
            illegal.add(Illegal.NAME);
        }

        return illegal;
    }
}

/**
 * represents Illegal States
 */
enum Illegal {
    NAME("Name"),
    DISPLAYNAME("DisplayName"),
    PREFIX("Prefix");

    String name;

    Illegal(String name) {
        this.name = name;
    }
}
