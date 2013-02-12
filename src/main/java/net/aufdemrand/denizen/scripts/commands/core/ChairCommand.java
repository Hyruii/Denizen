package net.aufdemrand.denizen.scripts.commands.core;

import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.PacketContainer;
import com.comphenix.protocol.wrappers.WrappedDataWatcher;
import net.aufdemrand.denizen.exceptions.CommandExecutionException;
import net.aufdemrand.denizen.exceptions.InvalidArgumentsException;
import net.aufdemrand.denizen.scripts.ScriptEntry;
import net.aufdemrand.denizen.scripts.commands.AbstractCommand;
import net.aufdemrand.denizen.utilities.arguments.aH;
import net.aufdemrand.denizen.utilities.debugging.dB;
import net.citizensnpcs.api.ai.event.NavigationBeginEvent;
import net.citizensnpcs.api.npc.NPC;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerTeleportEvent.TeleportCause;

import java.lang.reflect.InvocationTargetException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ChairCommand extends AbstractCommand implements Listener {
	
	enum ChairAction { SIT, STAND }
	
	public ConcurrentHashMap<NPC, Block> chairRegistry = new ConcurrentHashMap<NPC, Block>();

	@Override
	public void onEnable() {
		denizen.getServer().getPluginManager().registerEvents(this, denizen);
	}
	
	@Override
	public void parseArgs(ScriptEntry scriptEntry)
			throws InvalidArgumentsException {
		
		Block chairBlock = null;
		ChairAction chairAction = ChairAction.SIT;
		
		for (String arg : scriptEntry.getArguments()) {
			if (aH.matchesLocation(arg)) {
				chairBlock = aH.getLocationFrom(arg).getBlock();
				dB.echoDebug("...sit location set");
			} else if (aH.matchesArg("SIT, STAND", arg)) {
				chairAction = ChairAction.valueOf(aH.getStringFrom(arg).toUpperCase());
				dB.echoDebug("...npc will " + chairAction.name());
			} else throw new InvalidArgumentsException ("Invalid argument specified!");
		}
		
		scriptEntry.addObject("chairBlock", chairBlock);
		scriptEntry.addObject("chairAction", chairAction.name());
	}
	
	@Override
	public void execute(ScriptEntry scriptEntry)
			throws CommandExecutionException {
		
		Block chairBlock = (Block) scriptEntry.getObject("chairBlock");
		ChairAction chairAction = ChairAction.valueOf(((String) scriptEntry.getObject("chairAction")).toUpperCase());
		NPC npc = scriptEntry.getNPC().getCitizen();
		
		switch (chairAction) {
			case SIT:
				if (isChair(chairBlock)) {
					dB.echoError("...location is already being sat on!");
					return;
				}
				
				if (isSitting(npc)) {
					dB.echoError("...NPC is already sitting!");
					return;
				}
				
				/*
				 * Teleport NPC to the chair and
				 * make him sit. Good NPC!				
				 */
				npc.getBukkitEntity().teleport(chairBlock.getLocation().add(0.5, 0, 0.5), TeleportCause.PLUGIN);
				
				makeSitAllPlayers(npc, chairBlock);
				dB.echoDebug("...NPC sits!");
				
				break;
				
			case STAND:
				if (!isSitting(npc)) {
					dB.echoError("...NPC is already standing!");
					return;
				}
				
				makeStand(npc);
				dB.echoDebug("...NPC stands!");
				break;
		}
	}

	/*
	 * Sends packet via ProtocolLib to all online
	 * players so they can see the NPC as sitting.
	 */
	public void makeSitAllPlayers(NPC npc, Block block) {
		try {
			PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(40);
			packet.getSpecificModifier(int.class).write(0, npc.getBukkitEntity().getEntityId());
			WrappedDataWatcher watcher = new WrappedDataWatcher();
			watcher.setObject(0, (byte) 4);
			packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
			
			for (Player player : denizen.getServer().getOnlinePlayers()) {
                if (npc.getBukkitEntity().getWorld().equals(player.getWorld())) {
                    try {
                        ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                    } catch (InvocationTargetException e) { 
                    	dB.echoError("...error sending packet to player: " + player.getName()); 
                    }
                }
            }
			
		} catch (Error e) {
			dB.echoError("ProtocolLib required for SIT command!!");
		}
		
		chairRegistry.put(npc, block);
	}
	
	/*
	 * Sends the sit packet to a specific player.
	 */
	public void makeSitSpecificPlayer(NPC npc, Player player) {
		try {
			PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(40);
			packet.getSpecificModifier(int.class).write(0, npc.getBukkitEntity().getEntityId());
			WrappedDataWatcher watcher = new WrappedDataWatcher();
			watcher.setObject(0, (byte) 4);
			packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
			
            if (npc.getBukkitEntity().getWorld().equals(player.getWorld())) {
                try {
                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
                } catch (InvocationTargetException e) { 
                	dB.echoError("...error sending packet to player: " + player.getName()); 
                }
            }
            
			
		} catch (Error e) {
			dB.echoError("ProtocolLib required for SIT command!!");
		}
	}
	
	/*
	 * Sends packet via ProtocolLib to all online
	 * players so they can see the NPC as standing.
	 */
	public void makeStand(NPC npc) {
		try {
			PacketContainer packet = ProtocolLibrary.getProtocolManager().createPacket(40);
			packet.getSpecificModifier(int.class).write(0, npc.getBukkitEntity().getEntityId());
	        WrappedDataWatcher watcher = new WrappedDataWatcher();
	        watcher.setObject(0, (byte) 0);
	        packet.getWatchableCollectionModifier().write(0, watcher.getWatchableObjects());
	
	        for (Player player : denizen.getServer().getOnlinePlayers()) {
	            if (npc.getBukkitEntity().getWorld().equals(player.getWorld())) {
	                try {
	                    ProtocolLibrary.getProtocolManager().sendServerPacket(player, packet);
	                } catch (InvocationTargetException e) {
	                	dB.echoError("...error sending packet to player: " + player.getName()); 
	                }
	            }
	        }
		} catch (Error e) {
			dB.echoError("ProtocolLib required for SIT command!!");
		}
		
		chairRegistry.remove(npc);
	}
	
	/*
	 * Checks if given NPC is registered as sitting.
	 */
	public Boolean isSitting(NPC npc) {
		if (chairRegistry.containsKey(npc)) return true;
		return false;
	}
	
	/*
	 * Checks if given Block is registered as a chair.
	 */
	public Boolean isChair(Block block) {
		if (chairRegistry.containsValue(block)) return true;
		return false;
	}
	
	@EventHandler
	public void onNavigationBeginEvent(NavigationBeginEvent event) {
		/*
		 * If the NPC starts to navigate, and
		 * he is sitting, he better stand up.
		 */
		if (isSitting(event.getNPC())) {
			makeStand(event.getNPC());
		}
	}
	
	@EventHandler(ignoreCancelled = true)
    public void onBlockBreak(BlockBreakEvent event) {
		/*
		 * If someone tries to break the poor
		 * NPC'c chair we need to stop them!
		 */
		if (isChair(event.getBlock())) {
			event.setCancelled(true);
			dB.echoDebug("..." + event.getPlayer().getName() + " tried to break an NPCs chair!");
		}
	}
	
	@EventHandler
	public void onPlayerLoginEvent(PlayerJoinEvent event) {
		Set<NPC> npcs = chairRegistry.keySet();
		
		/*
		 * Send packets for all currently sitting NPCs
		 * to all new players who join.
		 */
		for (NPC npc : npcs) {
			makeSitSpecificPlayer(npc, event.getPlayer());
		}
		
		dB.echoDebug("..." + event.getPlayer().getName() + " joined, sending sit packets.");
	}
}