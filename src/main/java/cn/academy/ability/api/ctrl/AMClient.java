package cn.academy.ability.api.ctrl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import cn.academy.ability.api.ctrl.SyncAction.State;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.Future.FutureCallback;
import net.minecraft.nbt.NBTTagCompound;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.simpleimpl.IMessage;


/* TODO: client operation?
 * send to server
 * server send to all
 */
/**
 * @author EAirPeter
 */
class AMClient implements IActionManager {

	AMClient() {
	}
	
	Map<Integer, SyncAction> map = new HashMap<Integer, SyncAction>();
	
	@Override
	public void startAction(SyncAction action) {
		NBTTagCompound tag = action.getNBTStart();
		ActionManager.startAtServer(action.getClass().getName(), tag, Future.create(new FutureCallback() {
			@Override
			public void onReady(Object val) {
				action.id = (int) val;
				//just ignore?
				if (action.id >= 0) {
					action.state = State.IDENTIFIED;
					map.put(action.id, action);
				}
			}
		}));
	}

	@Override
	public void endAction(SyncAction action) {
		ActionManager.endAtServer(action.id, Future.create(new FutureCallback() {
			@Override
			public void onReady(Object val) {
				map.get(action.id).setNBTFinal((NBTTagCompound) val);
			}
		}));
	}

	@Override
	public void abortAction(SyncAction action) {
		ActionManager.abortAtServer(action.id, Future.create(new FutureCallback() {
			@Override
			public void onReady(Object val) {
				map.get(action.id).setNBTFinal((NBTTagCompound) val);
			}
		}));
	}

	@Override
	public void abortActionLocally(SyncAction action) {
		map.get(action.id).state = State.ABORTED;
	}
	
	@SubscribeEvent
	public void onTick(ClientTickEvent event) {
		if (event.phase.equals(Phase.START))
			return;
		Collection<SyncAction> actions = map.values();
		for (Iterator<SyncAction> i = actions.iterator(); i.hasNext(); ) {
			SyncAction action = i.next();
			switch (action.state) {
			case IDENTIFIED:
				action.state = State.STARTED;
				action.onStart();
				break;
			case STARTED:
				action.onTick();
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
	
	void startFromServer(SyncAction action) {
		if (action.id >= 0) {
			action.state = State.IDENTIFIED;
			map.put(action.id, action);
		}
	}

	void updateFromServer(int id, NBTTagCompound tag) {
		SyncAction action = map.get(id);
		if (action != null)
			action.setNBTUpdate(tag);
	}
	
	void terminateFromServer(int id, NBTTagCompound tag) {
		SyncAction action = map.get(id);
		if (action != null)
			action.setNBTFinal(tag);
	}

}
