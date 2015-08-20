package cn.academy.ability.client.skilltree;

import cn.academy.ability.api.Skill;
import cn.academy.ability.developer.DevelopTypeLevel;
import cn.academy.ability.developer.DevelopTypeSkill;
import cn.academy.ability.developer.Developer;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.network.Future;
import cn.annoreg.mc.network.RegNetworkCall;
import cn.annoreg.mc.s11n.StorageOption.Data;
import cn.annoreg.mc.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;

@Registrant
public class Syncs {
	
	@RegNetworkCall(side = Side.SERVER)
	static void startLearningSkill(@Instance Developer developer, @Instance Skill skill) {
		developer.startDevelop(new DevelopTypeSkill(skill));
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void startUpgradingLevel(@Instance Developer developer) {
		developer.startDevelop(new DevelopTypeLevel());
	}

}
