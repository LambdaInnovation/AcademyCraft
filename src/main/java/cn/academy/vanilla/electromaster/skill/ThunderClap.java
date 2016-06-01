/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.electromaster.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc;
import cn.academy.vanilla.electromaster.entity.EntitySurroundArc.ArcType;
import cn.academy.vanilla.generic.entity.EntityRippleMark;
import cn.lambdalib.util.entityx.EntityCallback;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.effect.EntityLightningBolt;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;

import static cn.lambdalib.util.generic.MathUtils.lerpf;

/**
 * @author WeAthFolD
 */
public class ThunderClap extends Skill {
    
    static final int MIN_TICKS = 40, MAX_TICKS = 60;
    
    public static final ThunderClap instance = new ThunderClap();

    private ThunderClap() {
        super("thunder_clap", 5);
    }
    
    private static float getDamage(float exp, int ticks) {
        return lerpf(36, 72, exp) *
                MathUtils.lerpf(1.0f, 1.2f, (ticks - 40.0f) / 60.0f);
    }
    
    private static float getRange(float exp) {
        return lerpf(15, 30, exp);
    }
    
    private static int getCooldown(float exp, int ticks) {
        return (int) (ticks * lerpf(10, 6, exp));
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstance().addChild(new ThunderClapAction());
    }
    
    public static class ThunderClapAction extends SkillSyncAction<ThunderClap> {

        float exp;
        
        int ticks;
        double hitX, hitY, hitZ;

        public ThunderClapAction() {
            super(instance);
        }
        
        @Override
        public void onStart() {
            super.onStart();
            
            if(isRemote)
                startEffects();

            exp = ctx().getSkillExp();

            float overload = lerpf(390, 252, exp);
            ctx().consume(overload, 0);
        }
        
        @Override
        public void onTick() {
            if(isRemote)
                updateEffects();

            final double DISTANCE = 40.0;
            MovingObjectPosition pos = Raytrace.traceLiving(player, 40.0, EntitySelectors.nothing());
            if(pos != null) {
                hitX = pos.hitVec.xCoord;
                hitY = pos.hitVec.yCoord;
                hitZ = pos.hitVec.zCoord;
            } else {
                Motion3D mo = new Motion3D(player, true).move(DISTANCE);
                hitX = mo.px;
                hitY = mo.py;
                hitZ = mo.pz;
            }

            ticks++;

            float consumption = lerpf(100, 120, exp);
            if(ticks <= MIN_TICKS && !ctx().consume(0, consumption))
                ActionManager.abortAction(this);
            if(!isRemote) {
                if(ticks >= MAX_TICKS) {
                    ActionManager.endAction(this);
                }
            }
        }
        
        @Override
        public void writeNBTFinal(NBTTagCompound tag) {
            tag.setByte("t", (byte) ticks);
            tag.setFloat("x", (float) hitX);
            tag.setFloat("y", (float) hitY);
            tag.setFloat("z", (float) hitZ);
        }
        
        @Override
        public void readNBTFinal(NBTTagCompound tag) {
            ticks = tag.getByte("t");
            hitX = tag.getFloat("x");
            hitY = tag.getFloat("y");
            hitZ = tag.getFloat("z");
        }
        
        @Override
        public void onEnd() {
            if(ticks < MIN_TICKS) {
                onAbort();
                return;
            }
            
            if(isRemote)
                endEffects();
            
            EntityLightningBolt lightning = new EntityLightningBolt(
                    player.worldObj, hitX, hitY, hitZ);
            player.worldObj.spawnEntityInWorld(lightning);
            if(!isRemote) {
                ctx().attackRange(hitX, hitY, hitZ,
                        getRange(exp), getDamage(exp, ticks),
                        EntitySelectors.exclude(player));
            }
            
            ctx().setCooldown(getCooldown(exp, ticks));
            ctx().addSkillExp(0.003f);
            instance.triggerAchievement(player);
        }
        
        @Override
        public void onAbort() {
            if(isRemote)
                endEffects();
        }
        
        //CLIENT
        @SideOnly(Side.CLIENT)
        EntitySurroundArc surroundArc;
        
        @SideOnly(Side.CLIENT)
        EntityRippleMark mark;
        
        @SideOnly(Side.CLIENT)
        private void startEffects() {
            surroundArc = new EntitySurroundArc(player).setArcType(ArcType.BOLD);
            player.worldObj.spawnEntityInWorld(surroundArc);
            
            if(isLocal()) {
                mark = new EntityRippleMark(player.worldObj);

                player.worldObj.spawnEntityInWorld(mark);
                mark.color.setColor4d(0.8, 0.8, 0.8, 0.7);
                mark.setPosition(hitX, hitY, hitZ);
            }
        }
        
        @SideOnly(Side.CLIENT)
        private void updateEffects() {
            if(isLocal()) {
                final float max = 0.1f, min = 0.001f;
                player.capabilities.setPlayerWalkSpeed(Math.max(min, max - (max - min) / 60 * ticks));
                mark.setPosition(hitX, hitY, hitZ);
            }
        }
        
        @SideOnly(Side.CLIENT)
        private void endEffects() {
            player.capabilities.setPlayerWalkSpeed(0.1f);
            if(surroundArc != null)
                surroundArc.executeAfter(new EntityCallback() {
                    @Override
                    public void execute(Entity target) {
                        target.setDead();
                    }
                }, 10);
            
            if(isLocal()) {
                mark.setDead();
            }
        }
        
    }

}
