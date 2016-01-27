/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.electromaster.client.effect.ArcPatterns;
import cn.academy.vanilla.electromaster.entity.EntityArc;
import cn.lambdalib.util.entityx.handlers.Life;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cn.lambdalib.util.mc.WorldUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.command.IEntitySelector;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import java.util.List;

/**
 * @author WeAthFolD
 */
public class ThunderBolt extends Skill {
    
    static final double RANGE = 20, AOE_RANGE = 8;
    
    public static final ThunderBolt instance = new ThunderBolt();

    private ThunderBolt() {
        super("thunder_bolt", 4);
    }
    
    static float getAOEDamage(AbilityData aData) {
        return instance.callFloatWithExp("aoe_damage", aData);
    }
    
    static float getDamage(AbilityData aData) {
        return instance.callFloatWithExp("damage", aData);
    }
    
    static float getExpIncr(boolean effective) {
        return instance.getFloat("expincr_" + (effective ? "effective" : "ineffective"));
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstanceInstant().addExecution(new ThunderBoltAction());
    }
    
    public static class ThunderBoltAction extends SyncActionInstant {

        @Override
        public boolean validate() {
            return cpData.perform(instance.getOverload(aData), instance.getConsumption(aData));
        }

        @Override
        public void execute() {
            if(isRemote) {
                spawnEffects();
                
            } else {
                AttackData ad = getAttackData();
                float exp = aData.getSkillExp(instance);
                
                boolean effective = false;
                
                if(ad.target != null) {
                    effective = true;
                    EMDamageHelper.attack(ad.target, player, getDamage(aData));
                    if(exp > 0.2 && RandUtils.ranged(0, 1) < 0.8 && ad.target instanceof EntityLivingBase) {
                        ((EntityLivingBase) ad.target)
                            .addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 40, 3));
                    }
                }
                
                for(Entity e : ad.aoes) {
                    effective = true;
                    EMDamageHelper.attack(e, player, getAOEDamage(aData));
                    
                    if(exp > 0.2 && RandUtils.ranged(0, 1) < 0.8 && ad.target instanceof EntityLivingBase) {
                        ((EntityLivingBase) ad.target)
                            .addPotionEffect(new PotionEffect(Potion.moveSlowdown.id, 20, 3));
                    }
                }
                
                aData.addSkillExp(instance, getExpIncr(effective));
                instance.triggerAchievement(player);
            }
            
            setCooldown(instance, instance.getCooldown(aData));
        }
        
        @SideOnly(Side.CLIENT)
        private void spawnEffects() {
            AttackData ad = getAttackData();
            
            for(int i = 0; i <= 2; ++i) {
                EntityArc mainArc = new EntityArc(player, ArcPatterns.strongArc);
                mainArc.length = RANGE;
                player.worldObj.spawnEntityInWorld(mainArc);
                mainArc.addMotionHandler(new Life(20));
            }
            
            for(Entity e : ad.aoes) {
                EntityArc aoeArc = new EntityArc(player, ArcPatterns.aoeArc);
                aoeArc.lengthFixed = false;
                aoeArc.setFromTo(ad.point.xCoord, ad.point.yCoord, ad.point.zCoord, 
                    e.posX, e.posY + e.getEyeHeight(), e.posZ);
                aoeArc.addMotionHandler(new Life(RandUtils.rangei(15, 25)));
                player.worldObj.spawnEntityInWorld(aoeArc);
            }
            
            ACSounds.playClient(player, "em.arc_strong", 0.6f);
        }
        
        private AttackData getAttackData() {
            MovingObjectPosition result = Raytrace.traceLiving(player, RANGE);
            Vec3 end;
            if(result == null) {
                end = new Motion3D(player).move(RANGE).getPosVec();
            } else {
                end = result.hitVec;
                if(result.typeOfHit == MovingObjectType.ENTITY) {
                    end.yCoord += result.entityHit.getEyeHeight();
                }
            }
            
            boolean hitEntity = !(result == null || result.entityHit == null);
            IEntitySelector exclusion = !hitEntity ? EntitySelectors.excludeOf(player) :
                EntitySelectors.excludeOf(player, result.entityHit);
            Entity target = hitEntity ? result.entityHit : null;
            List<Entity> aoes = WorldUtils.getEntities(
                player.worldObj, end.xCoord, end.yCoord, end.zCoord,
                AOE_RANGE, EntitySelectors.and(EntitySelectors.living, exclusion));
            
            return new AttackData(aoes, target, end);
        }
        
    }
    
    static class AttackData {
        final List<Entity> aoes;
        final Entity target;
        final Vec3 point;
        
        public AttackData(List<Entity> list, Entity t, Vec3 _point) {
            aoes = list;
            target = t;
            point = _point;
        }
    }

}
