/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.core.util.RangedRayDamage;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.academy.vanilla.meltdowner.entity.EntityMDRay;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.VecUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.Vec3;

import static cn.lambdalib.util.generic.RandUtils.ranged;
import static cn.lambdalib.util.generic.RandUtils.rangei;

/**
 * @author WeAthFolD
 */
public class Meltdowner extends Skill {
    
    static final int TICKS_MIN = 20, TICKS_MAX = 40, TICKS_TOLE = 100;

    public static final Meltdowner instance = new Meltdowner();
    
    private Meltdowner() {
        super("meltdowner", 3);
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new MDAction()).setEstmCP(
            instance.getConsumption(AbilityData.get(player)));
    }
    
    public static class MDAction extends SkillSyncAction {
        
        int ticks;

        public MDAction() {}
        
        @Override
        public void onStart() {
            super.onStart();
            
            if(isRemote) startEffect();
        }
        
        @Override
        public void onTick() {
            ticks++;
            if(isRemote)
                updateEffect();
            if(!cpData.perform(0, instance.getConsumption(aData)) && !isRemote)
                ActionManager.abortAction(this);
            
            if(ticks > TICKS_TOLE)
                ActionManager.abortAction(this);
        }
        
        @Override
        public void writeNBTFinal(NBTTagCompound tag) {
            tag.setInteger("t", ticks);
        }
        
        @Override
        public void readNBTFinal(NBTTagCompound tag) {
            ticks = tag.getInteger("t");
        }

        private float timeRate(int ct) {
            return MathUtils.lerpf(0.8f, 1.2f, (ct - 20.0f) / 20.0f);
        }

        private float getEnergy(int ct) {
            return timeRate(ct) * MathUtils.lerpf(300, 700, aData.getSkillExp(instance));
        }

        private float getDamage(int ct) {
            return timeRate(ct) * MathUtils.lerpf(20, 50, aData.getSkillExp(instance));
        }

        private int getCooldown(int ct) {
            return (int)(timeRate(ct) * 20 * MathUtils.lerpf(15, 7, aData.getSkillExp(instance)));
        }

        private float getExpIncr(int ct) {
            return timeRate(ct) * 0.002f;
        }

        @Override
        public void onEnd() {
            if(ticks < TICKS_MIN) {
                // N/A
            } else {
                cpData.perform(instance.getOverload(aData), 0);
                int ct = toChargeTicks();
                
                if(isRemote) {
                    spawnRay();
                } else {
                    RangedRayDamage rrd = new RangedRayDamage(player, 
                        instance.callFloatWithExp("range", aData),
                        getEnergy(ct));
                    rrd.startDamage = getDamage(ct);
                    rrd.perform();
                }
                
                setCooldown(instance, getCooldown(ct));
                aData.addSkillExp(instance, getExpIncr(ct));
            }
        }
        
        @Override
        public void onFinalize() {
            if(isRemote)
                endEffect();
        }
        
        private int toChargeTicks() {
            return Math.min(ticks, TICKS_MAX);
        }
        
        // CLIENT
        
        @SideOnly(Side.CLIENT)
        FollowEntitySound sound;
        
        @SideOnly(Side.CLIENT)
        void spawnRay() {
            EntityMDRay ray = new EntityMDRay(player);
            ACSounds.playClient(player, "md.meltdowner", 0.5f);
            world.spawnEntityInWorld(ray);
        }
        
        @SideOnly(Side.CLIENT)
        void startEffect() {
            sound = new FollowEntitySound(player, "md.md_charge").setVolume(1.0f);
            ACSounds.playClient(sound);
        }
        
        @SideOnly(Side.CLIENT)
        void updateEffect() {
            if(isLocal()) {
                player.capabilities.setPlayerWalkSpeed(0.1f - ticks * 0.001f);
            }
            
            // Particles surrounding player
            int count = rangei(2, 3);
            while(count --> 0) {
                double r = ranged(0.7, 1);
                double theta = ranged(0, Math.PI * 2);
                double h = ranged(-1.2, 0);
                Vec3 pos = VecUtils.add(VecUtils.vec(player.posX, 
                    player.posY + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), player.posZ), 
                    VecUtils.vec(r * Math.sin(theta), h, r * Math.cos(theta)));
                Vec3 vel = VecUtils.vec(ranged(-.03, .03), ranged(.01, .05), ranged(-.03, .03));
                world.spawnEntityInWorld(MdParticleFactory.INSTANCE.next(world, pos, vel));
            }
        }
        
        @SideOnly(Side.CLIENT)
        void endEffect() {
            if(isLocal()) {
                player.capabilities.setPlayerWalkSpeed(0.1f);
            }
            
            sound.stop();
        }
        
    }

}
