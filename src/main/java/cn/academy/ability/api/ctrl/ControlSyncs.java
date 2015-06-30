package cn.academy.ability.api.ctrl;

import cn.academy.ability.api.data.CPData;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;

@Registrant
public class ControlSyncs {

	@RegNetworkCall(side = Side.SERVER)
	static void activateAtServer(@Instance CPData data) {
		data.activate();
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void deactivateAtServer(@Instance CPData data) {
		data.deactivate();
	}

}
