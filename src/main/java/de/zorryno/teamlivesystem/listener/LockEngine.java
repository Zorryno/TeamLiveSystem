package de.zorryno.teamlivesystem.listener;

import de.zorryno.teamlivesystem.Main;
import de.zorryno.teamlivesystem.util.teams.Team;
import org.bukkit.Location;
import org.bukkit.block.*;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.plugin.Plugin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class LockEngine implements Listener {
    private Plugin plugin;

    public LockEngine(Plugin plugin) {
        this.plugin = plugin;
    }

    @EventHandler
    public void onOpen(PlayerInteractEvent event) {
        if(event.getClickedBlock() == null || !(event.getClickedBlock().getState() instanceof Container))
            return;

        Team team = getOwnerTeam(event.getClickedBlock().getLocation());

        if(team == null)
            return;

        Team playerTeam = Team.getTeamFromPlayer(event.getPlayer().getUniqueId());

        if(playerTeam == null || (!team.equals(playerTeam) && !event.getPlayer().isOp() && !team.isInAlliance(playerTeam))) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Main.getMessages().getCache().get("ProtectedBlock.NoPermission").replace("%team%", team.getDisplayName()));
            return;
        }
    }

    @EventHandler
    public void onBlockExplosion(EntityExplodeEvent event) {
        for(Block block : event.blockList()) {
            if(getOwnerTeam(block.getLocation()) != null) {
                event.setCancelled(true);
                return;
            }
        }
    }

    @EventHandler
    public void onBlockBreak(BlockBreakEvent event) {
        if(!(event.getBlock().getState() instanceof Container))
            return;

        Team team = getOwnerTeam(event.getBlock().getLocation());

        if(team == null)
            return;

        if(!team.equals(Team.getTeamFromPlayer(event.getPlayer().getUniqueId())) && !event.getPlayer().isOp()) {
            event.setCancelled(true);
            event.getPlayer().sendMessage(Main.getMessages().getCache().get("ProtectedBlock.NoPermission").replace("%team%", team.getDisplayName()));
            return;
        }

        team.removeBlock(event.getBlock().getLocation());
    }

    private List<Block> getBlocksAround(Block block) {
        List<Block> blocks = new ArrayList<>();
        BlockFace[] faces = {BlockFace.NORTH, BlockFace.EAST, BlockFace.SOUTH, BlockFace.WEST, BlockFace.UP, BlockFace.DOWN};
        for(BlockFace face : faces)
            blocks.add(block.getRelative(face));
        return blocks;
    }

    @EventHandler
    public void onBlockPlace(BlockPlaceEvent event) {
        Team team = Team.getTeamFromPlayer(event.getPlayer().getUniqueId());
        if(!(event.getBlock().getState() instanceof Container))
            return;

        Iterator<Block> iterator = getBlocksAround(event.getBlock()).iterator();
        iterator.forEachRemaining(block -> {
            if(block.getState() instanceof Container) {
                Team blockTeam = LockEngine.getOwnerTeam(block.getLocation());
                if (blockTeam != null && !blockTeam.equals(team)) {
                    event.setCancelled(true);
                    event.getPlayer().sendMessage(Main.getMessages().getCache().get("ProtectedBlock.NoPermission").replace("%team%", blockTeam.getDisplayName()));
                }
            }
        });

        if(team == null)
            return;

        team.addBlock(event.getBlock().getLocation());
    }

    public static Team getOwnerTeam(Location location) {
        for(Team team : Main.getSaver().getTeams()) {
            if(team.getProtectedBlocks().contains(location))
                return team;
        }
        return null;
    }
}
