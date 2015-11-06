package cn.academy.ability.client.skilltree;

import cn.academy.ability.api.Skill;
import cn.academy.ability.developer.DevelopTypeLevel;
import cn.academy.ability.developer.DevelopTypeSkill;
import cn.academy.ability.developer.Developer;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
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
	
	@RegNetworkCall(side = Side.SERVER)
	static void abort(@Instance Developer developer) {
		developer.abort();
	}

}
