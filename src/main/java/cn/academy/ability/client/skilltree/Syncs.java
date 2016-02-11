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
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cpw.mods.fml.relauncher.Side;
import net.minecraft.entity.player.EntityPlayer;

@Registrant
@NetworkS11nType
public enum Syncs {
    instance;

    void startLearningSkill(EntityPlayer player, IDeveloper developer, Skill skill) {
        NetworkMessage.sendToServer(instance, "learn_skill", player, developer, skill);
    }

    void startUpgradingLevel(EntityPlayer player, IDeveloper developer) {
        NetworkMessage.sendToServer(instance, "upgrade_level", player, developer);
    }

    void abort(EntityPlayer player) {
        NetworkMessage.sendToServer(instance, "abort", player);
    }
    
    @Listener(channel="learn_skill", side=Side.SERVER)
    private void hStartLearningSkill(EntityPlayer player, IDeveloper developer, Skill skill) {
        data(player).startDeveloping(developer, new DevelopActionSkill(skill));
    }
    
    @Listener(channel="upgrade_level", side=Side.SERVER)
    private void hStartUpgradingLevel(EntityPlayer player, IDeveloper developer) {
        data(player).startDeveloping(developer, new DevelopActionLevel());
    }
    
    @Listener(channel="abort", side=Side.SERVER)
    private void hAbort(@Instance EntityPlayer player) {
        data(player).abort();
    }

    private static DevelopData data(EntityPlayer player) {
        return DevelopData.get(player);
    }

}
