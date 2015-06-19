package cn.academy.ability.api.ctrl;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEventHandler;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegEventHandler.Bus;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cn.annoreg.mc.s11n.StorageOption.Target;
import cn.annoreg.mc.s11n.StorageOption.Target.RangeOption;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author EAirPeter
 */
@Registrant
public class ActionManager {
	
	private static final AMServer AMS = new AMServer();
	@SideOnly(Side.CLIENT)
	private static final AMClient AMC = new AMClient();
	
	public static void startAction(SyncAction action) {
		msg("startAction");
		getActionManager().startAction(action);
	}
	
	public static void endAction(SyncAction action) {
		msg("endAction");
		getActionManager().endAction(action);
	}
	
	public static void abortAction(SyncAction action) {
		msg("abortAction");
		getActionManager().abortAction(action);
	}
	
	private static IActionManager getActionManager() {
		return FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER) ? AMS : AMC;
	}
	
	//NETWORK CALLS
	@RegNetworkCall(side = Side.SERVER)
	static void startAtServer(@Instance EntityPlayer player, @Data String className, @Data NBTTagCompound tag, @Data Future res) {
		msg("startAtServer");
		res.setAndSync(AMS.startFromClient(player, className, tag));
	}

	@RegNetworkCall(side = Side.SERVER)
	static void endAtServer(@Instance EntityPlayer player, @Data Integer id) {
		msg("endAtServer");
		AMS.endFromClient(player, id);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void abortAtServer(@Instance EntityPlayer player, @Data Integer id) {
		msg("abortAtServer");
		AMS.abortFromClient(player, id);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	static void startAtClient(@Target(range = RangeOption.EXCEPT) EntityPlayer player, @Data String className, @Data NBTTagCompound tag) {
		msg("startAtClient");
		AMC.startFromServer(className, tag);
	}

	@RegNetworkCall(side = Side.CLIENT)
	static void updateAtClient(@Data Integer id, @Data NBTTagCompound tag) {
		msg("updateAtClient");
		AMC.updateFromServer(id, tag);
	}
	
	@RegNetworkCall(side = Side.CLIENT)
	static void terminateAtClient(@Data Integer id, @Data NBTTagCompound tag) {
		msg("terminateAtClient");
		AMC.terminateFromServer(id, tag);
	}
	
	//TODO TREMOVE
	public static void msg(String msg) {
		cn.academy.ability.api.ctrl.test.TM.msg("AM", msg);
	}
	
}
