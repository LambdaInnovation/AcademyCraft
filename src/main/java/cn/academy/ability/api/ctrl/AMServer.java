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
		map.put(dummy, new HashMap<Integer, SyncAction>());
	}
	
	private Map<UUID, Map<Integer, SyncAction>> map = new HashMap<UUID, Map<Integer, SyncAction>>();
	private List<UUID> off = new LinkedList<UUID>();
	
	private static final UUID dummy = UUID.randomUUID();
	
	int nextId = 0;
	
	@Override
	public void startAction(SyncAction action) {
		msg("startAction");
		action.id = nextId++;
		action.state = State.IDENTIFIED;
		action.player = null;
		NBTTagCompound tag = action.getNBTStart();
		ActionManager.startAtClient(null, action.getClass().getName(), tag);
		map.get(dummy).put(action.id, action);
	}

	@Override
	public void endAction(SyncAction action) {
		SyncAction _action = map.get(playerUUID(action)).get(action.id);
		if (_action == null)
			ActionManager.terminateAtClient(action.id, SyncAction.TAG_ABORTED);
		else {
			_action.state = State.ENDED;
			ActionManager.terminateAtClient(_action.id, _action.getNBTFinal());
		}
	}

	@Override
	public void abortAction(SyncAction action) {
		SyncAction _action = map.get(playerUUID(action)).get(action.id);
		if (_action == null)
			ActionManager.terminateAtClient(action.id, SyncAction.TAG_ABORTED);
		else {
			_action.state = State.ABORTED;
			ActionManager.terminateAtClient(_action.id, _action.getNBTFinal());
		}
	}
	
	int startFromClient(EntityPlayer player, String className, NBTTagCompound tag) {
		SyncAction action = null;
		try {
			action = (SyncAction) Class.forName(className).newInstance();
			action.setNBTStart(tag);
			action.player = player;
			action.id = nextId++;
			action.state = State.IDENTIFIED;
			ActionManager.startAtClient(player, className, action.getNBTStart());
			map.get(playerUUID(action)).put(action.id, action);
			return action.id;
		}
		catch (Throwable e) {
			e.printStackTrace();
			return -1;
		}
	}

	void endFromClient(EntityPlayer player, int id) {
		SyncAction action = map.get(player.getUniqueID()).get(id);
		if (action != null) {
			if (action.state.equals(State.STARTED))
				action.state = State.ENDED;
			else
				action.state = State.ABORTED;
			ActionManager.terminateAtClient(id, action.getNBTFinal());
		}
	}

	void abortFromClient(EntityPlayer player, int id) {
		SyncAction action = map.get(player.getUniqueID()).get(id);
		if (action != null) {
			action.state = State.ABORTED;
			ActionManager.terminateAtClient(id, action.getNBTFinal());
		}
	}
	
	void abortPlayer(EntityPlayer player) {
		for (Iterator<SyncAction> i = map.get(player.getUniqueID()).values().iterator(); i.hasNext(); )
			abortAction(i.next());
	}

	private UUID playerUUID(SyncAction action) {
		return action.player == null ? dummy : action.player.getUniqueID();
	}
	
	private int curTick = 0;
	
	@SubscribeEvent
	public void onServerTick(ServerTickEvent event) {
		if (event.phase.equals(Phase.START))
			return;
		++curTick;
		for (Map<Integer, SyncAction>  m : map.values())
			for (Iterator<SyncAction> i = m.values().iterator(); i.hasNext(); ) {
				SyncAction action = i.next();
				switch (action.state) {
				case CREATED:
					throw new IllegalStateException();
				case IDENTIFIED:
					action.lastInformed = curTick;
					action.state = State.STARTED;
					action.onStart();
					break;
				case STARTED:
					action.onTick();
					if (action.intv > 0 && curTick - action.lastInformed >= action.intv) {
						action.lastInformed = curTick;
						ActionManager.updateAtClient(action.id, action.getNBTUpdate());
					}
					break;
				case ENDED:
					action.onEnd();
					i.remove();
					break;
				case ABORTED:
					action.onAbort();
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
	
	//TODO abortAll
	@SubscribeEvent
	public void onPlayerChangedDimension(PlayerChangedDimensionEvent event) {
		log("onPlayerChangedDimension");
		abortPlayer(event.player);
	}
	
	@SubscribeEvent
	public void onPlayerLoggedIn(PlayerLoggedInEvent event) {
		log("onPlayerLoggedIn");
		map.put(event.player.getUniqueID(), new HashMap<Integer, SyncAction>());
	}
	
	@SubscribeEvent
	public void onPlayerLoggedOut(PlayerLoggedOutEvent event) {
		log("onPlayerLoggedOut");
		abortPlayer(event.player);
		off.add(event.player.getUniqueID());
	}
	
	//TODO TREMOVE
	public static void msg(String msg) {
		cn.academy.ability.api.ctrl.test.TM.msg("AMS", msg);
	}
	public static void log(String msg) {
		cn.academy.ability.api.ctrl.test.TM.log("AMS", msg);
	}
	
}
