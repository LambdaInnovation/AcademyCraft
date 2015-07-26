package cn.academy.ability.api.ctrl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.academy.ability.api.ctrl.SyncAction.State;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLServerStoppingEvent;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedInEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

/**
 * @author EAirPeter
 */
public class AMServer implements IActionManager {

	AMServer() {
		FMLCommonHandler.instance().bus().register(this);
		map.put(dummy, new HashMap<UUID, SyncAction>());
	}
	
	private Map<UUID, Map<UUID, SyncAction>> map = new HashMap<UUID, Map<UUID, SyncAction>>();
	private List<UUID> off = new LinkedList<UUID>();
	
	private static final UUID dummy = UUID.randomUUID();
	
	@Override
	public void startAction(SyncAction action) {
		//System.out.println("AMS#INT_START");
		action.uuid = UUID.randomUUID();
		action.player = null;
		NBTTagCompound tag = action.getNBTStart();
		ActionManager.startAtClient(null, action.getClass().getName(), tag);
		map.get(dummy).put(action.uuid, action);
		action.start();
	}

	@Override
	public void endAction(SyncAction action) {
		//System.out.println("AMS#INT_END");
		SyncAction _action = map.get(playerUUID(action)).get(action.uuid);
		if (_action == null)
			ActionManager.endAtClient(action.uuid.toString(), SyncAction.TAG_EMPTY);
		else
			ActionManager.endAtClient(_action.uuid.toString(), _action.end());
	}

	@Override
	public void abortAction(SyncAction action) {
		//System.out.println("AMS#INT_ABORT");
		SyncAction _action = map.get(playerUUID(action)).get(action.uuid);
		if (_action == null)
			ActionManager.abortAtClient(action.uuid.toString(), SyncAction.TAG_EMPTY);
		else
			ActionManager.abortAtClient(_action.uuid.toString(), _action.abort());
	}
	
	@Override
	public SyncAction findAction(EntityPlayer player, Class clazz) {
		Map<UUID, SyncAction> _map = map.get(playerUUID(player));
		if (_map == null)
			return null;
		for (SyncAction action : _map.values())
			if (clazz.isInstance(action))
				return action;
		return null;
	}
	
	boolean startFromClient(EntityPlayer player, String className, NBTTagCompound tag) {
		//System.out.println("AMS#NET_START");
		SyncAction action = null;
		try {
			action = (SyncAction) Class.forName(className).newInstance();
			action.player = player;
			action.setNBTStart(tag);
			ActionManager.startAtClient(player, className, action.getNBTStart());
			map.get(playerUUID(action)).put(action.uuid, action);
			action.start();
			return true;
		}
		catch (Throwable e) {
			e.printStackTrace();
			return false;
		}
	}

	void endFromClient(EntityPlayer player, UUID uuid) {
		//System.out.println("AMS#NET_END");
		SyncAction action = map.get(player.getUniqueID()).get(uuid);
		if (action == null)
			ActionManager.abortAtClient(uuid.toString(), SyncAction.TAG_EMPTY);
		else
			endAction(action);
	}

	void abortFromClient(EntityPlayer player, UUID uuid) {
		//System.out.println("AMS#NET_ABORT");
		SyncAction action = map.get(player.getUniqueID()).get(uuid);
		if (action == null)
			ActionManager.abortAtClient(uuid.toString(), SyncAction.TAG_EMPTY);
		else
			abortAction(action);
	}
	
	void abortPlayer(EntityPlayer player) {
		//System.out.println("AMS#NET_APLAYER");
		Map<UUID, SyncAction> _map = map.get(player.getUniqueID());
		if (_map != null)
			for (SyncAction action : _map.values())
				abortAction(action);
	}

	private UUID playerUUID(SyncAction action) {
		return playerUUID(action.player);
	}
	
	private UUID playerUUID(EntityPlayer player) {
		return player == null ? dummy : player.getUniqueID();
	}
	
	private int curTick = 0;
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if (event.phase.equals(Phase.START))
			return;
		++curTick;
		for (Map<UUID, SyncAction>  m : map.values())
			for (Iterator<SyncAction> i = m.values().iterator(); i.hasNext(); ) {
				SyncAction action = i.next();
				switch (action.getState()) {
				case CREATED:
					throw new IllegalStateException();
				case STARTED:
					action.onTick();
					if (action.intv > 0 && curTick - action.lastInformed >= action.intv) {
						action.lastInformed = curTick;
						ActionManager.updateAtClient(action.uuid.toString(), action.getNBTUpdate());
					}
					break;
				case ENDED:
				case ABORTED:
					i.remove();
					break;
				default:
					break;
				}
			}
		for (UUID u : off)
			map.remove(u);
		off.clear();
	}
	
	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		abortPlayer(event.player);
	}
	
	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		map.put(event.player.getUniqueID(), new HashMap<UUID, SyncAction>());
	}
	
	@SubscribeEvent
	public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
		abortPlayer(event.player);
		off.add(event.player.getUniqueID());
	}

}
