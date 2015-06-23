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
		getActionManager().startAction(action);
	}
	
	public static void endAction(SyncAction action) {
		getActionManager().endAction(action);
	}
	
	public static void abortAction(SyncAction action) {
		getActionManager().abortAction(action);
	}
	
	private static IActionManager getActionManager() {
		return FMLCommonHandler.instance().getEffectiveSide().equals(Side.SERVER) ? AMS : AMC;
	}
	
	//NETWORK CALLS
	@RegNetworkCall(side = Side.SERVER)
	static void startAtServer(@Instance EntityPlayer player, @Data String className, @Data NBTTagCompound tag, @Data Future res) {
		res.setAndSync(AMS.startFromClient(player, className, tag));
	}

	@RegNetworkCall(side = Side.SERVER)
	static void endAtServer(@Instance EntityPlayer player, @Data Integer id) {
		AMS.endFromClient(player, id);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void abortAtServer(@Instance EntityPlayer player, @Data Integer id) {
		AMS.abortFromClient(player, id);
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void abortPlayerAtServer(@Instance EntityPlayer player) {
		AMS.abortPlayer(player);
	};
	
	@RegNetworkCall(side = Side.CLIENT)
	static void startAtClient(@Target(range = RangeOption.EXCEPT) EntityPlayer player, @Data String className, @Data NBTTagCompound tag) {
		AMC.startFromServer(className, tag);
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
