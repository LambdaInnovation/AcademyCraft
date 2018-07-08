package cn.academy.medicine.buffs;


import cn.academy.ability.api.cooldown.CooldownData;
import cn.academy.medicine.api.BuffApplyData;
import cn.academy.medicine.api.BuffPerTick;
import cn.academy.medicine.api.RegBuff;
import cn.lambdalib.annoreg.core.Registrant;
import net.minecraft.entity.player.EntityPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Registrant
@RegBuff
public class BuffCooldownRecovery extends BuffPerTick {

    public BuffCooldownRecovery(float percentPerTick)
    {
        super("cd_recovery");
        this.perTick = percentPerTick;
    }

    private Map<CooldownData.SkillCooldown,Float> accumMap = new HashMap<>();

    @Override
    public void onBegin(EntityPlayer player) {

    }

    @Override
    public void onTick(EntityPlayer player , BuffApplyData applyData){
        CooldownData cdData = CooldownData.of(player);

        Iterator itr = cdData.rawData().entrySet().iterator();
        while (itr.hasNext()) {
            CooldownData.SkillCooldown cd = (CooldownData.SkillCooldown) ((Map.Entry)itr.next()).getValue();

            if (!accumMap.containsKey(cd)) {
                accumMap.put(cd, 0f);
            }

            float next = accumMap.get(cd) + Math.abs(perTick) * cd.getMaxTick();
            cd.setTickLeft(cd.getTickLeft() - (int)next);
            accumMap.put(cd, next % 1);
        }
    }

    @Override
    public void onEnd(EntityPlayer player) {

    }

}
