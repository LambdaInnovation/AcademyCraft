package cn.academy.ability.api.ctrl;

import net.minecraft.nbt.NBTTagCompound;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;

@Registrant
public class ActionManager {

	@RegEventHandler(Bus.FML)
	private static final AMServer AMS = new AMServer();
	@RegEventHandler(Bus.FML)
	private static final AMClient AMC = new AMClient();
	
	public static void startAction(SyncAction action) {
		getActionManager().startAction(action);
	}
	
	public static void endAction(SyncAction action) {
		getActionManager().endAction(action);
	}
	
	public static void abortAction(SyncAction action) {
		getActionManager().abortAction(action);
	}
	
	public static void abortActionLocally(SyncAction action) {
		getActionManager().abortActionLocally(action);
	}
	
	private static IActionManager getActionManager() {
		return FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER) ? AMS : AMC;
	}
	
	//NETWORK CALLS
	@RegNetworkCall(side = Side.SERVER)
	static void startAtServer(@Data String className, @Data NBTTagCompound tag, @Data Future res) {
		try {
			SyncAction action = (SyncAction) Class.forName(className).newInstance();
			action.setNBTStart(tag);
			res.setAndSync(AMS.startFromClient(action));
		} catch (Throwable e) {
			res.setAndSync(-1);
			e.printStackTrace();
		}
	}

	@RegNetworkCall(side = Side.SERVER)
	static void endAtServer(@Data Integer id, @Data Future res) {
		res.setAndSync(AMS.endFromClient(id));
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void abortAtServer(@Data Integer id, @Data Future res) {
		res.setAndSync(AMS.abortFromClient(id));
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	static void startAtClient(@Data String className, @Data NBTTagCompound tag) {
		try {
			SyncAction action = (SyncAction) Class.forName(className).newInstance();
			action.setNBTStart(tag);
			AMC.startFromServer(action);
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}

	@RegNetworkCall(side = Side.CLIENT)
	static void updateAtClient(@Data Integer id, @Data NBTTagCompound tag) {
		AMC.updateFromServer(id, tag);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	static void terminateAtClient(@Data Integer id, @Data NBTTagCompound tag) {
		AMC.terminateFromServer(id, tag);
	}
	
}
