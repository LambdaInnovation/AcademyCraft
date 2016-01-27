/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.ability.client.skilltree;

import cn.academy.ability.api.Skill;
import cn.academy.ability.develop.DevelopData;
import cn.academy.ability.develop.IDeveloper;
import cn.academy.ability.develop.action.DevelopActionLevel;
import cn.academy.ability.develop.action.DevelopActionSkill;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;

@Registrant
public class Syncs {
    
    @RegNetworkCall(side = Side.SERVER)
    static void startLearningSkill(@Instance EntityPlayer player, @Instance IDeveloper developer, @Instance Skill skill) {
        data(player).startDeveloping(developer, new DevelopActionSkill(skill));
    }
    
    @RegNetworkCall(side = Side.SERVER)
    static void startUpgradingLevel(@Instance EntityPlayer player, @Instance IDeveloper developer) {
        data(player).startDeveloping(developer, new DevelopActionLevel());
    }
    
    @RegNetworkCall(side = Side.SERVER)
    static void abort(@Instance EntityPlayer player) {
        data(player).abort();
    }

    private static DevelopData data(EntityPlayer player) {
        return DevelopData.get(player);
    }

}
