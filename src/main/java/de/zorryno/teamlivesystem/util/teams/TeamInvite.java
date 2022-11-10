package de.zorryno.teamlivesystem.util.teams;

import de.zorryno.teamlivesystem.Main;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.plugin.Plugin;

import java.util.HashMap;
import java.util.UUID;

/**
 * Represents a TeamInvite
 * @see Team
 */
public class TeamInvite implements Listener {
    private static HashMap<UUID, TeamInvite> invites = new HashMap<>();
    private final Team team;
    private final Player inviter;
    private final Player invitedPlayer;
    private final Plugin plugin;
    private boolean accepted;
    private long aliveTicks;

    /**
     * Creates a new {@link TeamInvite}
     *
     * @param team the {@link Team} this invite belongs to
     * @param invitedPlayer the {@link Player} that should be invited
     * @param plugin {@link Plugin} witch creates this invite
     * @param aliveTicks the ticks this Invite should be alive
     */
    public TeamInvite(Team team, Player inviter, Player invitedPlayer, Plugin plugin, long aliveTicks) {
        this.team = team;
        this.inviter = inviter;
        this.invitedPlayer = invitedPlayer;
        this.plugin = plugin;
        this.aliveTicks = aliveTicks;
    }


    /**
     * Get the {@link Team} this {@link TeamInvite} belongs to
     *
     * @return the {@link Team}
     */
    public Team getTeam() {
        return team;
    }

    /**
     * Gets the {@link Player} witch sends this {@link TeamInvite}
     *
     * @return the {@link Player}
     */
    public Player getInviter() {
        return inviter;
    }

    /**
     * Gets the Invited {@link Player}
     *
     * @return the {@link Player}
     */
    public Player getInvitedPlayer() {
        return invitedPlayer;
    }

    /**
     * Shows if this {@link TeamInvite} is accepted or denied
     *
     * @return if this {@link TeamInvite} is accepted or {@code null} if this {@link TeamInvite} is open
     */
    public boolean isAccepted() {
        return accepted;
    }

    /**
     * Accept this {@link TeamInvite}
     */
    public void accept() {
        invites.remove(invitedPlayer.getUniqueId());
        team.addMember(invitedPlayer.getUniqueId());
        this.accepted = true;
    }

    /**
     * Deny this {@link TeamInvite}
     */
    public void deny() {
        invites.remove(invitedPlayer.getUniqueId());
        this.accepted = false;
    }

    /**
     * Sends this {@link TeamInvite} to the {@link Player}
     */
    public void sendInvite() {
        TextComponent accept = new TextComponent(Main.getMessages().getCache().get("TeamInvite.Accept"));
        accept.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team accept"));

        TextComponent deny = new TextComponent(Main.getMessages().getCache().get("TeamInvite.Deny"));
        deny.setClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/team deny"));

        TextComponent message = new TextComponent(Main.getMessages().getCache().get("TeamInvite.Message").replace("%team%", team.getName()));
        message.addExtra("\n");
        message.addExtra(accept);
        message.addExtra(" §r| ");
        message.addExtra(deny);

        invitedPlayer.spigot().sendMessage(message);

        Bukkit.getScheduler().runTask(plugin, () -> invites.put(invitedPlayer.getUniqueId(), this));
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, () -> invites.remove(invitedPlayer.getUniqueId()) ,aliveTicks);
    }

    /**
     * Gets open {@link TeamInvite}s from this {@link Player}{@link UUID}
     *
     * @param uuid the {@link UUID} from the {@link Player}
     * @return the {@link TeamInvite} or {@code null} if no {@link TeamInvite} exists
     */
    public static TeamInvite getInvite(UUID uuid) {
        return invites.get(uuid);
    }
}
