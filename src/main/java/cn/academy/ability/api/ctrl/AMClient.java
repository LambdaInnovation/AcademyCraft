package cn.academy.ability.api.ctrl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraftforge.client.event.GuiOpenEvent;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.ability.api.ctrl.SyncAction.State;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.Future.FutureCallback;
import cn.liutils.api.event.OpenAuxGuiEvent;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.common.gameevent.TickEvent.Phase;
import cpw.mods.fml.common.network.FMLNetworkEvent.ClientDisconnectionFromServerEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

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
	@SideOnly(Side.CLIENT)
	public void startAction(SyncAction action) {
		NBTTagCompound tag = action.getNBTStart();
		ActionManager.startAtServer(Minecraft.getMinecraft().thePlayer, action.getClass().getName(), tag, Future.create(new FutureCallback() {
			@Override
			public void onReady(Object val) {
				action.id = (int) val;
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
	@SideOnly(Side.CLIENT)
	public void endAction(SyncAction action) {
		ActionManager.endAtServer(Minecraft.getMinecraft().thePlayer, action.id);
	}

	@Override
	@SideOnly(Side.CLIENT)
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
	@SideOnly(Side.CLIENT)
	public synchronized void onClientTick(ClientTickEvent event) {
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
	@SideOnly(Side.CLIENT)
	public synchronized void onClientDisconnectionFromServer(ClientDisconnectionFromServerEvent event) {
		for (Iterator<SyncAction> i = map.values().iterator(); i.hasNext(); ) {
			SyncAction action = i.next();
			action.state = State.ABORTED;
			action.onAbort();
			i.remove();
		}
	}
	
	@SideOnly(Side.CLIENT)
	private void abortPlayer() {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if (player != null && !set.isEmpty())
			ActionManager.abortPlayerAtServer(player);
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onGuiOpen(GuiOpenEvent event) {
		if (event.gui != null)
			abortPlayer();
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void onOpenAuxGui(OpenAuxGuiEvent event) {
		if (event.gui != null)
			abortPlayer();
	}

}
