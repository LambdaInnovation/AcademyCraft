package cn.academy.medicine.buffs;


import cn.academy.medicine.api.BuffApplyData;
import cn.academy.medicine.api.BuffPerTick;
import cn.academy.medicine.api.RegBuff;
import cn.lambdalib.annoreg.core.Registrant;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.DamageSource;

@Registrant
@RegBuff
public class BuffHeal extends BuffPerTick {

    public BuffHeal(float healPerTick){
        super("heal");
        perTick = healPerTick;
    }

    @Override
    public void onBegin(EntityPlayer player) {

    }

    @Override
    public void onTick(EntityPlayer player, BuffApplyData applyData){
        if (perTick >= 0) {
            player.heal(perTick);
        } else {
            player.attackEntityFrom(DamageSource.magic, perTick);
        }
    }

    @Override
    public void onEnd(EntityPlayer player) {

    }
}