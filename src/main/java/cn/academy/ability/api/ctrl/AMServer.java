package cn.academy.ability.api.ctrl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import net.minecraft.nbt.NBTTagCompound;
import cn.academy.ability.api.ctrl.SyncAction.State;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.gameevent.TickEvent.ServerTickEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

class AMServer implements IActionManager {

	AMServer() {
	}
	
	Map<Integer, SyncAction> map = new HashMap<Integer, SyncAction>();
	int nextId = 0;
	
	@Override
	public void startAction(SyncAction action) {
		action.id = nextId++;
		action.state = State.IDENTIFIED;
		NBTTagCompound tag = action.getNBTStart();
		ActionManager.startAtClient(action.getClass().getName(), tag);
		map.put(action.id, action);
	}

	@Override
	public void endAction(SyncAction action) {
		SyncAction _action = map.get(action.id);
		_action.state = State.ENDED;
		ActionManager.terminateAtClient(_action.id, _action.getNBTFinal());
	}

	@Override
	public void abortAction(SyncAction action) {
		SyncAction _action = map.get(action.id);
		_action.state = State.ABORTED;
		ActionManager.terminateAtClient(_action.id, _action.getNBTFinal());
	}
	
	@Override
	public void abortActionLocally(SyncAction action) {
		map.get(action.id).state = State.ABORTED;
	}
	
	int curTick = 0;
	
	@SubscribeEvent
	public void onTick(ServerTickEvent event) {
		if (event.phase.equals(Phase.START))
			return;
		++curTick;
		Collection<SyncAction> actions = map.values();
		for (Iterator<SyncAction> i = actions.iterator(); i.hasNext(); ) {
			SyncAction action = i.next();
			switch (action.state) {
			case IDENTIFIED:
				action.lastInformed = curTick;
				action.state = State.STARTED;
				action.onStart();
				//onStart() and onTick()
				//or?
				//onStart(), next tick onTick()
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
	}
	
	int startFromClient(SyncAction action) {
		action.id = nextId++;
		action.state = State.IDENTIFIED;
		map.put(action.id, action);
		return action.id;
	}

	NBTTagCompound endFromClient(int id) {
		SyncAction action = map.get(id);
		if (action == null)
			return SyncAction.TAG_ABORTED;
		if (action.state.equals(State.STARTED))
			action.state = State.ENDED;
		else
			action.state = State.ABORTED;
		return action.getNBTFinal();
	}

	NBTTagCompound abortFromClient(int id) {
		SyncAction action = map.get(id);
		if (action == null)
			return SyncAction.TAG_ABORTED;
		action.state = State.ABORTED;
		return action.getNBTFinal();
	}

}
