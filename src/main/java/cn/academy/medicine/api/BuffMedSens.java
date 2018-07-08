package cn.academy.medicine.api;

import cn.academy.ability.api.data.CPData;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.generic.MathUtils;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;

@Registrant
@RegBuff
public class BuffMedSens extends Buff {

    private float percentage = 0.0f;//大概是药敏系数

    public BuffMedSens() {
        super("med_sensitive");
        shouldDisplay = false;
    }

    @Override
    public void onBegin(EntityPlayer player) {

    }

    @Override
    public void onTick(EntityPlayer player, BuffApplyData applyData) {
        percentage = Math.max(0.0f, percentage - 0.0005f);
        if (percentage == 0) {
            applyData.setEnd();
        }
    }

    @Override
    public void onEnd(EntityPlayer player) {

    }

    @Override
    public void load(NBTTagCompound tag) {

    }

    @Override
    public void store(NBTTagCompound tag) {

    }
    //药敏系数增加
    public void increase(EntityPlayer player, float pct){
        float prev = percentage;
        percentage = MathUtils.clampf(0, 1, pct + percentage);

        if (prev < 1f && percentage >= 1f) {
            player.hurtResistantTime = -1;
            player.setHealth(0f);
        } else if (prev < 0.8f && percentage >= 0.8f) {
            CPData cpData = CPData.get(player);
            float realdmg = Math.min(player.getHealth() - 1, 10);
            player.attackEntityFrom(DamageSource.causePlayerDamage(player), realdmg);
            cpData.perform(cpData.getMaxOverload() * 0.6f, 0);
        } else if (prev < 0.6f && percentage >= 0.6f) {
            float realdmg = Math.min(player.getHealth() - 1, 5);
            player.attackEntityFrom(DamageSource.causePlayerDamage(player), realdmg);
            int ticks = 15 * 20;
            PotionEffect effect = new PotionEffect(Potion.hunger.id, ticks, 1);
            player.addPotionEffect(effect);
        } else if (prev < 0.4f && percentage >= 0.4f) {
            float realdmg = Math.min(player.getHealth() - 1, 3);
            player.attackEntityFrom(DamageSource.causePlayerDamage(player), realdmg);
            int ticks = 3 * 20;
            PotionEffect effect = new PotionEffect(Potion.hunger.id, ticks, 1);
            player.addPotionEffect(effect);
        }
    }
}
