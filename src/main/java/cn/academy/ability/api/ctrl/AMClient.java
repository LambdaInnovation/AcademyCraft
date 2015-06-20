package cn.academy.ability.api.ctrl;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import cn.academy.ability.api.ctrl.SyncAction.State;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.Future.FutureCallback;
import cn.liutils.api.event.OpenAuxGuiEvent;
import cn.liutils.util.client.ClientUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerChangedDimensionEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent.PlayerLoggedOutEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.common.network.simpleimpl.IMessage;

/* TODO: client operation?
 * send to server
 * server send to all
 */
/**
 * @author EAirPeter
 */
public class AMClient implements IActionManager {

	AMClient() {
		FMLCommonHandler.instance().bus().register(this);
		MinecraftForge.EVENT_BUS.register(this);
	}
	
	private Map<Integer, SyncAction> map = new HashMap<Integer, SyncAction>();
	//Optimized: abortPlayer
	private Set<Integer> set = new HashSet<Integer>();
	
	@Override
	public void startAction(SyncAction action) {
		msg("startAction");
		NBTTagCompound tag = action.getNBTStart();
		ActionManager.startAtServer(Minecraft.getMinecraft().thePlayer, action.getClass().getName(), tag, Future.create(new FutureCallback() {
			@Override
			public void onReady(Object val) {
				msg("startAction.onReady");
				action.id = (int) val;
				//just ignore?
				if (action.id >= 0) {
					action.state = State.IDENTIFIED;
					action.player = Minecraft.getMinecraft().thePlayer;
					map.put(action.id, action);
					set.add(action.id);
				}
			}
		}));
	}

	@Override
	public void endAction(SyncAction action) {
		ActionManager.endAtServer(Minecraft.getMinecraft().thePlayer, action.id);
	}

	@Override
	public void abortAction(SyncAction action) {
		ActionManager.abortAtServer(Minecraft.getMinecraft().thePlayer, action.id);
	}
	
	void startFromServer(String className, NBTTagCompound tag) {
		SyncAction action = null;
		try {
			action = (SyncAction) Class.forName(className).newInstance();
			action.setNBTStart(tag);
			if (action.id >= 0) {
				action.state = State.IDENTIFIED;
				map.put(action.id, action);
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	void updateFromServer(int id, NBTTagCompound tag) {
		SyncAction action = map.get(id);
		if (action != null) {
			if (action.state.equals(State.IDENTIFIED)) {
				action.state = State.STARTED;
				action.onStart();
			}
			action.setNBTUpdate(tag);
		}
	}
	
	void terminateFromServer(int id, NBTTagCompound tag) {
		SyncAction action = map.get(id);
		if (action != null) {
			if (action.state.equals(State.IDENTIFIED)) {
				action.state = State.STARTED;
				action.onStart();
			}
			action.setNBTFinal(tag);
		}
	}
	
	@SubscribeEvent
	public void onClientTick(ClientTickEvent event) {
		if (Minecraft.getMinecraft().isGamePaused() || event.phase.equals(Phase.START))
			return;
		for (Iterator<SyncAction> i = map.values().iterator(); i.hasNext(); ) {
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
				set.remove(action.id);
				break;
			case ABORTED:
				action.onAbort();
				i.remove();
				set.remove(action.id);
				break;
			default:
				break;
			}
		}
	}
	
	@SubscribeEvent
	public void onClientDisconnectionFromServer(ClientDisconnectionFromServerEvent event) {
		log("onClientDisconnectionFromServer: " + Minecraft.getMinecraft().thePlayer.getUniqueID().toString());
		for (Iterator<SyncAction> i = map.values().iterator(); i.hasNext(); ) {
			SyncAction action = i.next();
			action.state = State.ABORTED;
			action.onAbort();
			i.remove();
		}
	}
	
	private void abortPlayer() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if (player != null && !set.isEmpty())
			ActionManager.abortPlayerAtServer(player);
	}
	
	@SubscribeEvent
	public void onGuiOpen(GuiOpenEvent event) {
		if (event.gui != null)
			abortPlayer();
	}
	
	@SubscribeEvent
	public void onOpenAuxGui(OpenAuxGuiEvent event) {
		if (event.gui != null)
			abortPlayer();
	}
	
	//TODO TREMOVE
	public static void msg(String msg) {
		cn.academy.ability.api.ctrl.test.TM.msg("AMC", msg);
	}
	public static void log(String msg) {
		cn.academy.ability.api.ctrl.test.TM.log("AMC", msg);
	}
}
