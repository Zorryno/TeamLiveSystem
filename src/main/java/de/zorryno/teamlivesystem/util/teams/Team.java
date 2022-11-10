package de.zorryno.teamlivesystem.util.teams;

import de.zorryno.teamlivesystem.Main;
import de.zorryno.teamlivesystem.listener.LiveListener;
import org.bukkit.Bukkit;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Represents a Team
 */
public class Team {
    private Plugin plugin;
    private UUID owner;
    private List<UUID> admin;
    private List<UUID> members;
    private final org.bukkit.scoreboard.Team team;
    private String prefix;

    private int lives;
    private List<UUID> deathPlayers;

    private List<Location> protectedBlocks;
    private List<String> alliance;

    /**
     * Creates a new Team
     *
     * @param plugin      The Plugin
     * @param owner       The UUID from the Owner
     * @param name        The Team name for the Scoreboard
     * @param displayName The Name of the Team
     * @param prefix      The Prefix without [ and ] (added in this Method)
     * @see #Team(Plugin, UUID, String, String, String, List, List, List, List, int, List)
     */
    public Team(Plugin plugin, UUID owner, String name, String displayName, String prefix) {
        this(plugin, owner, name, displayName, "§r[" + prefix + "§r] ", null, null, null, null, Main.getTeamStartLives(), null);
    }

    /**
     * Creates a new Team with Admins and Members
     *
     * @param plugin          The Plugin
     * @param owner           The UUID from the Owner
     * @param name            The Team name for the Scoreboard
     * @param displayName     The Name of the Team
     * @param prefix          The Prefix with [ and ] surround it
     * @param admins          The UUID List with the Admins
     * @param members         The UUID List with all Members
     * @param protectedBlocks The Blocks that are protected
     * @param alliance        The Teams List that can access protected Blocks
     * @param lives           The Lives this Team have
     * @see #Team(Plugin, UUID, String, String, String)
     */
    public Team(Plugin plugin, UUID owner, String name, String displayName, String prefix, List<UUID> admins, List<UUID> members, List<Location> protectedBlocks, List<String> alliance, int lives, List<UUID> deathPlayers) {
        if (Bukkit.getScoreboardManager() == null)
            throw new NullPointerException("This can't happen call the police or something \n Bukkit.getScoreboardManager() is null");

        team = getOrCreateTeam(name);

        team.setPrefix(ChatColor.translateAlternateColorCodes('&', prefix));
        this.prefix = prefix.substring(1, prefix.length() - 1);
        team.setDisplayName(ChatColor.translateAlternateColorCodes('&', displayName));
        this.plugin = plugin;
        this.owner = owner;
        this.admin = admins != null ? new ArrayList<>(admins) : new ArrayList<>();
        this.members = members != null ? new ArrayList<>(members) : new ArrayList<>();
        this.protectedBlocks = protectedBlocks != null ? new ArrayList<>(protectedBlocks) : new ArrayList<>();
        for (UUID uuid : this.members) {
            String entryName = Bukkit.getOfflinePlayer(uuid).getName();
            if (entryName != null)
                team.addEntry(entryName);
        }
        this.alliance = alliance != null ? new ArrayList<>(alliance) : new ArrayList<>();

        this.lives = lives;
        this.deathPlayers = deathPlayers != null ? new ArrayList<>(deathPlayers) : new ArrayList<>();

        String ownerName = Bukkit.getOfflinePlayer(owner).getName() != null ? Bukkit.getOfflinePlayer(owner).getName() : owner.toString();

        if (!this.members.contains(owner))
            this.members.add(owner);
        if (!this.admin.contains(owner))
            this.admin.add(owner);
        team.addEntry(ownerName);
        Main.getSaver().addTeam(this);

        if (Main.isDebugMode()) {
            plugin.getLogger().info(" Name : " + getName());
            plugin.getLogger().info(" DisplayName : " + getDisplayName());
            plugin.getLogger().info(" Prefix : " + getPrefix());
            plugin.getLogger().info(" Owner : " + getOwner());
            plugin.getLogger().info(" Admins : " + getAdmins());
            plugin.getLogger().info(" Members : " + getMembers());
            plugin.getLogger().info(" Alliance : " + getAlliances());
            plugin.getLogger().info( " Lives : " + getLives() + "/" + getMaxLives());
            plugin.getLogger().info(" Death Players : " + getDeathPlayers());
        }
    }

    /**
     * Returns the Lives that this Team have
     *
     * @return the remaining Lives
     */
    public int getLives() {
        return lives;
    }

    /**
     * Sets the Lives that this Team have
     *
     * @param lives
     */
    public void setLives(int lives) {
        this.lives = lives;
        checkRevive();
    }

    /**
     * Removes the given amount of lives
     *
     * @param lives the amount of lives to remove
     * @return the new amount of lives
     */
    public int removeLives(int lives) {
        this.lives -= lives;
        return this.lives;
    }

    /**
     * Adds the given amount of lives
     *
     * @param lives the amount of lives to add
     * @return the new amount of lives
     */
    public int addLives(int lives) {
        this.lives += lives;
        checkRevive();
        return this.lives;
    }

    /**
     * Returns the maximal amount of lives this Team can have
     * @return the maximal amount of lives
     */
    public int getMaxLives() {
        int maxLives = Main.getTeamStartLives();
        if(getMembers().size() > maxLives)
            maxLives = getMembers().size();
        return maxLives;
    }

    /**
     * Returns the amount of Lives that should add automatically
     * 1-7 -> 2 Leben
     * 8-9 -> 3 Leben
     * 10-12 -> 4 Leben
     * 13-14 -> 5 Leben
     * 15-17 -> 6 Leben
     * @return the amount of Lives that should add automatically
     */
    public int getAddingLives() {
        int size = getMembers().size();
        int addingLives = (int) (size * (40d / 100));

        return Math.max(addingLives, 2);
    }

    public void checkRevive() {
        List<UUID> deathPlayersCopy = new ArrayList<>(deathPlayers);
        for (UUID uuid : deathPlayersCopy) {
            if (lives <= 0)
                continue;

            Player player = Bukkit.getPlayer(uuid);
            if (player == null || !player.isOnline())
                continue;

            player.sendMessage(Main.getMessages().getCache().get("Revive"));
            LiveListener.releasePlayer(player);
            removeLives(1);
            removeDeathPlayer(uuid);
        }
    }

    /**
     * Gets the Death Players
     * Changes have no Effect
     *
     * @return the Death Player List
     */
    public List<UUID> getDeathPlayers() {
        return new ArrayList<>(deathPlayers);
    }

    /**
     * Adds a Death Player to the List
     *
     * @param uuid the UUID of the Player
     * @return if the Player was added
     */
    public boolean addDeathPlayer(UUID uuid) {
        return deathPlayers.add(uuid);
    }

    /**
     * Removes a Death Player from the List
     *
     * @param uuid the UUID of the Player
     * @return if the Player was removed
     */
    public boolean removeDeathPlayer(UUID uuid) {
        return deathPlayers.remove(uuid);
    }

    /**
     * Unregisters and deletes this Team
     *
     * @return true if the Team is unregistered
     */
    public boolean unregister() {
        String name = getName();
        try {
            Main.getSaver().removeTeam(this);
            admin = new ArrayList<>();
            members = new ArrayList<>();
            owner = null;
            team.unregister();
        } catch (IllegalStateException e) {
            return false;
        }
        if (Main.isDebugMode())
            plugin.getLogger().info("unregisterd Team " + name);

        return true;
    }

    /**
     * Removes an Admin from this Team
     *
     * @param uuid The UUID of the admin
     * @return if the admins have changed
     */
    public boolean removeAdmin(UUID uuid) {
        if (Main.isDebugMode())
            plugin.getLogger().info("Team " + getName() + " : Admin removed " + uuid);
        return admin.remove(uuid);
    }

    /**
     * Adds an Admin to this Team if the user is a Member
     *
     * @param uuid the UUID of the Member
     * @return if the admins have changed
     */
    public boolean addAdmin(UUID uuid) {
        if (!members.contains(uuid) || admin.contains(uuid)) return false;

        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.getName() == null)
            return false;

        if (Main.isDebugMode())
            plugin.getLogger().info("Team " + getName() + " : Admin added " + uuid);
        return admin.add(uuid);
    }

    /**
     * Removes a Member from this Team
     *
     * @param uuid the UUID of the Member
     * @return if the members have changed
     */
    public boolean removeMember(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);

        admin.remove(uuid);
        members.remove(uuid);
        if (player.getName() == null)
            return false;

        if (Main.isDebugMode())
            plugin.getLogger().info("Team " + getName() + " : Member removed " + uuid);

        if (owner.equals(uuid)) {
            ownerLeave();
            return true;
        }

        return team.removeEntry(player.getName());
    }

    /**
     * Adds a Member to this Team
     *
     * @param uuid the UUID of the Member to add
     * @return if the members have changed
     */
    public boolean addMember(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.getName() == null || team.hasEntry(player.getName())) {
            return false;
        }
        members.add(uuid);
        team.addEntry(player.getName());
        return true;
    }

    public void ownerLeave() {
        if (Main.isDebugMode())
            plugin.getLogger().info("Team " + team.getName() + " : Owner left Team");
        unregister();

    }

    /**
     * Removes a protected Block
     *
     * @param location the Location of the Block to remove
     */
    public boolean removeBlock(Location location) {
        return protectedBlocks.remove(location);
    }

    /**
     * Adds a protected Block
     *
     * @param location the Location of the Block to add
     */
    public boolean addBlock(Location location) {
        if (!protectedBlocks.contains(location)) {
            protectedBlocks.add(location);
            return true;
        }
        return false;
    }

    /**
     * Gets the Name of the Team
     *
     * @return the Name
     */
    public String getName() {
        return team.getName();
    }

    /**
     * Returns the Prefix without extra Characters
     *
     * @return the pure Prefix without extra Characters
     */
    public String getCleanPrefix() {
        return prefix;
    }

    /**
     * Gets the Prefix of the Team
     *
     * @return the Prefix
     */
    public String getPrefix() {
        return team.getPrefix();
    }

    /**
     * Sets the Prefix of the Team with [prefix]
     *
     * @param prefix the Prefix
     */
    public void setPrefix(String prefix) {
        team.setPrefix(ChatColor.translateAlternateColorCodes('&', "&r[" + prefix + "&r] "));
        this.prefix = prefix;
    }

    /**
     * Gets the DisplayName of the Team
     *
     * @return the DisplayName
     */
    public String getDisplayName() {
        return team.getDisplayName();
    }

    /**
     * Sets the DisplayName
     *
     * @param name the DisplayName
     */
    public void setDisplayName(String name) {
        team.setDisplayName(ChatColor.translateAlternateColorCodes('&', name));
    }

    /**
     * Gets the Owner UUID if this Team
     *
     * @return the Owner UUID
     */
    public UUID getOwner() {
        return owner;
    }

    /**
     * Sets the Owner of the Team
     *
     * @param uuid the UUID from this Member
     * @return if the Owner was updated
     */
    public boolean setOwner(UUID uuid) {
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.getName() == null || !members.contains(uuid))
            return false;

        team.addEntry(player.getName());
        admin.remove(owner);
        this.owner = uuid;
        admin.add(uuid);
        return true;
    }

    /**
     * Gets the Admins
     * Changes have no Effect
     *
     * @return the Admin List
     */
    public List<UUID> getAdmins() {
        return new ArrayList<>(admin);
    }

    /**
     * Gets the AdminNames
     * Changes have no Effect
     *
     * @return the Admin List (Names)
     */
    public List<String> getAdminNames() {
        List<String> names = new ArrayList<>();
        getAdmins().forEach(uuid -> names.add(Bukkit.getOfflinePlayer(uuid).getName()));
        return names;
    }

    /**
     * Gets the Members
     * Changes have no Effect
     *
     * @return the Member List
     */
    public List<UUID> getMembers() {
        return new ArrayList<>(members);
    }

    /**
     * Gets the MemberNames
     * Changes have no Effect
     *
     * @return the Member List (Names)
     */
    public List<String> getMemberNames() {
        List<String> names = new ArrayList<>();
        getMembers().forEach(uuid -> names.add(Bukkit.getOfflinePlayer(uuid).getName()));
        return names;
    }

    /**
     * Gets the Members without the Admins and the Owner
     * Changes have no Effect
     *
     * @return the Member List without Admins and the Owner
     */
    public List<UUID> getMembersOnly() {
        List<UUID> membersOnly = new ArrayList<>(members);
        membersOnly.removeAll(admin);
        membersOnly.remove(owner);
        return membersOnly;
    }

    /**
     * Gets the protected Blocks from this Team
     * Changes have no Effect
     *
     * @return the Location from the protected Blocks
     */
    public List<Location> getProtectedBlocks() {
        return new ArrayList<>(protectedBlocks);
    }

    /**
     * Gets the Scoreboard Team
     *
     * @return the Team
     */
    public org.bukkit.scoreboard.Team getTeam() {
        return team;
    }

    /**
     * Checks if the UUID is in the Team
     *
     * @param uuid the UUID to Check
     * @return if the UUID is a Member of this Team
     */
    public boolean isInTeam(UUID uuid) {
        return members.contains(uuid);
    }

    /**
     * Sends this Team info to the Player
     *
     * @param player the Player to send the Team info
     */
    public void sendTeamInfo(Player player) {

        String adminNames = "";
        for (UUID adminUUID : admin) {
            if (adminUUID.equals(owner)) continue;
            OfflinePlayer admin = Bukkit.getOfflinePlayer(adminUUID);
            adminNames += admin.getName() != null ? admin.getName() : admin.getUniqueId();
            adminNames += "\n";
        }

        String memberNames = "";
        for (UUID memberUUID : getMembersOnly()) {
            if (memberUUID.equals(owner)) continue;
            OfflinePlayer member = Bukkit.getOfflinePlayer(memberUUID);
            memberNames += member.getName() != null ? member.getName() : member.getUniqueId();
            memberNames += "\n";
        }

        String alliances = "";
        for (String teamName : alliance) {
            alliances += teamName;
            alliances += "\n";
        }

        String finalMemberNames = memberNames;
        String finalAdminNames = adminNames;
        String finalAlliances = alliances;
        OfflinePlayer offlineOwner = Bukkit.getOfflinePlayer(owner);
        String ownerName = offlineOwner.getName() != null ? offlineOwner.getName() : owner.toString();
        Main.getMessages().getMessagesList("TeamInfo").forEach((message) -> player.sendMessage(message.
                replace("%name%", getName()).
                replace("%displayName%", getDisplayName()).
                replace("%prefix%", getPrefix()).
                replace("%owner%", ownerName).
                replace("%admins%", finalAdminNames).
                replace("%members%", finalMemberNames).
                replace("%alliances%", finalAlliances).
                replace("%lives%", getLives() + "/" + getMaxLives()))
        );
    }

    /**
     * Checks if a Player is a Admin in this Team
     *
     * @param uuid the UUID from the Player
     * @return if the Player is an Admin
     */
    public boolean isAdmin(UUID uuid) {
        return admin.contains(uuid) || owner.equals(uuid);
    }

    /**
     * Adds an Alliance to this Team
     *
     * @param team the second Team
     */
    public void addAlliance(Team team) {
        if (!alliance.contains(team.getName()))
            alliance.add(team.getName());
    }

    /**
     * Removes an Alliance from this Team
     *
     * @param team
     */
    public void removeAlliance(Team team) {
        alliance.remove(team.getName());
    }

    /**
     * Get all Alliances this Team have
     *
     * @return a List with the Team names
     */
    public List<String> getAlliances() {
        return new ArrayList<>(alliance);
    }

    /**
     * Checks if the Teams are in an Alliance
     *
     * @param team the second Team
     * @return if the Teams are in an Alliance
     */
    public boolean isInAlliance(Team team) {
        return team != null && alliance.contains(team.getName());
    }

    /**
     * Gets or Creates a Team with this Name
     *
     * @param name the Name of the Team
     * @return the Team
     */
    private static org.bukkit.scoreboard.Team getOrCreateTeam(String name) {
        if (Bukkit.getScoreboardManager() == null)
            throw new NullPointerException("This can't happen call the police or something \n Bukkit.getScoreboardManager() is null");

        org.bukkit.scoreboard.Team team = Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name) != null ?
                Bukkit.getScoreboardManager().getMainScoreboard().getTeam(name) :
                Bukkit.getScoreboardManager().getMainScoreboard().registerNewTeam(name);

        team.setCanSeeFriendlyInvisibles(false);
        return team;
    }

    /**
     * Gets the Team in which the Player is
     *
     * @param uuid the Players UUID
     * @return the Team or null if none was found
     */
    public static Team getTeamFromPlayer(UUID uuid) {
        Team playerTeam = null;
        for (Team team : Main.getSaver().getTeams()) {
            if (team.isInTeam(uuid))
                playerTeam = team;
        }
        return playerTeam;
    }

    /**
     * Gets the Team by his name
     *
     * @param name the Team Name
     * @return the Team or null if none was found
     */
    public static Team getTeamByName(String name) {
        Team targetTeam = null;
        for (Team team : Main.getSaver().getTeams()) {
            if (team.getName().equals(name))
                targetTeam = team;
        }
        return targetTeam;
    }

    /**
     * Returns all Death Players at this Time
     *
     * @return all Death Players
     */
    public static List<UUID> getAllDeathPlayers() {
        List<UUID> deathPlayers = new ArrayList<>();
        for (Team team : Main.getSaver().getTeams())
            deathPlayers.addAll(team.getDeathPlayers());

        return deathPlayers;
    }

    /**
     * Unregister all Teams
     *
     * @return if the Teams are unregistered
     */
    public static boolean unregisterAllTeams() {
        if (Bukkit.getScoreboardManager() == null)
            return false;

        for (org.bukkit.scoreboard.Team team : Bukkit.getScoreboardManager().getMainScoreboard().getTeams()) {
            team.unregister();
        }
        return true;
    }
}
