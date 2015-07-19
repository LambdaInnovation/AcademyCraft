package cn.academy.ability.api.ctrl;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

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
		if (FMLCommonHandler.instance().getEffectiveSide().equals(Side.CLIENT)) {
			FMLCommonHandler.instance().bus().register(this);
			MinecraftForge.EVENT_BUS.register(this);
			map = new HashMap<UUID, SyncAction>();
			set = new HashSet<UUID>();
		}
	}
	
	private Map<UUID, SyncAction> map = null;
	//Optimized: abortPlayer
	private Set<UUID> set = null;
	
	@Override
	@SideOnly(Side.CLIENT)
	public void startAction(SyncAction action) {
		//System.out.println("AMC#INT_START");
		action.player = Minecraft.getMinecraft().thePlayer;
		NBTTagCompound tag = action.getNBTStart();
		ActionManager.startAtServer(Minecraft.getMinecraft().thePlayer, action.getClass().getName(), tag, Future.create(new FutureCallback() {
			@Override
			public void onReady(Object val) {
				if ((boolean) val) {
					action.state = State.VALIDATED;
					map.put(action.uuid, action);
					set.add(action.uuid);
				}
			}
		}));
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void endAction(SyncAction action) {
		//System.out.println("AMC#INT_END: " + action.uuid);
		ActionManager.endAtServer(Minecraft.getMinecraft().thePlayer, action.uuid.toString());
	}

	@Override
	@SideOnly(Side.CLIENT)
	public void abortAction(SyncAction action) {
		//System.out.println("AMC#INT_ABORT: " + action.uuid);
		ActionManager.abortAtServer(Minecraft.getMinecraft().thePlayer, action.uuid.toString());
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	public SyncAction findAction(EntityPlayer player, Class clazz) {
		for (SyncAction action : map.values())
			if (clazz.isInstance(action)) {
				if (player == null && action.player == null)
					return action;
				if (player != null && action.player != null && player.equals(action.player))
					return action;
			}
		return null;
	}
	
	@SideOnly(Side.CLIENT)
	void startFromServer(String className, NBTTagCompound tag) {
		//System.out.println("AMC#NET_START");
		SyncAction action = null;
		try {
			action = (SyncAction) Class.forName(className).newInstance();
			action.setNBTStart(tag);
			if (action.uuid != null) {
				action.state = State.VALIDATED;
				map.put(action.uuid, action);
			}
		}
		catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@SideOnly(Side.CLIENT)
	void updateFromServer(UUID uuid, NBTTagCompound tag) {
		//System.out.println("AMC#NET_UPDATE");
		SyncAction action = map.get(uuid);
		if (action != null) {
			if (action.state.equals(State.VALIDATED)) {
				action.state = State.STARTED;
				action.onStart();
			}
			action.setNBTUpdate(tag);
		}
	}
	
	@SideOnly(Side.CLIENT)
	void terminateFromServer(UUID uuid, NBTTagCompound tag) {
		//System.out.println("AMC#NET_TERMINATE");
		SyncAction action = map.get(uuid);
		if (action != null) {
			if (action.state.equals(State.VALIDATED)) {
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
			case VALIDATED:
				action.state = State.STARTED;
				action.onStart();
				break;
			case STARTED:
				action.onTick();
				break;
			case ENDED:
				action.onEnd();
				i.remove();
				set.remove(action.uuid);
				break;
			case ABORTED:
				action.onAbort();
				i.remove();
				set.remove(action.uuid);
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
		set.clear();
	}
	
	@SideOnly(Side.CLIENT)
	private void abortPlayer() {
		//System.out.println("AMC#PRI_APLAYER");
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
		if (event.gui != null && event.gui.isForeground())
			abortPlayer();
	}

}
