package cn.academy.ability.client.skilltree;

import cn.academy.ability.api.Skill;
import cn.academy.ability.developer.DevelopTypeLevel;
import cn.academy.ability.developer.DevelopTypeSkill;
import cn.academy.ability.developer.refactor.DevelopData;
import cn.academy.ability.developer.refactor.IDeveloper;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;

@Registrant
public class Syncs {
	
	@RegNetworkCall(side = Side.SERVER)
	static void startLearningSkill(@Instance EntityPlayer player, @Instance IDeveloper developer, @Instance Skill skill) {
        data(player).startDeveloping(developer, new DevelopTypeSkill(skill));
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void startUpgradingLevel(@Instance EntityPlayer player, @Instance IDeveloper developer) {
        data(player).startDeveloping(developer, new DevelopTypeLevel());
	}
	
	@RegNetworkCall(side = Side.SERVER)
	static void abort(@Instance EntityPlayer player) {
		data(player).abort();
	}

    private static DevelopData data(EntityPlayer player) {
        return DevelopData.get(player);
    }

}
