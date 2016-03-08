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
import cn.academy.vanilla.generic.entity.EntityRippleMark;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.academy.vanilla.meltdowner.entity.EntityDiamondShield;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Data;
import cn.lambdalib.networkcall.s11n.StorageOption.Target;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
@Registrant
public class JetEngine extends Skill {

    public static final JetEngine instance = new JetEngine();

    private JetEngine() {
        super("jet_engine", 4);
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        SkillInstance ret = new SkillInstance().addChild(new JEAction());
        ret.estimatedCP = getConsumption(AbilityData.get(player).getSkillExp(this));
        return ret;
    }

    private static float getConsumption(float exp) {
        return lerpf(170, 140, exp);
    }
    
    public static class JEAction extends SkillSyncAction {

        float exp;
        float consumption, overload;

        public JEAction() {
            super(-1);
        }
        
        @Override
        public void onStart() {
            super.onStart();

            exp = aData.getSkillExp(instance);
            consumption = getConsumption(exp);
            overload = lerpf(66, 42, exp);
            
            if(isRemote)
                startEffects();
        }
        
        @Override
        public void onTick() {
            if(isRemote)
                updateEffects();
            
            if(!cpData.canPerform(getConsumption(exp)))
                ActionManager.abortAction(this);
        }
        
        @Override
        public void onEnd() {
            if(cpData.perform(getConsumption(exp), overload)) {
                if(!isRemote) {
                    startTriggerAction(player, getDest().addVector(0, 1.65, 0));
                    aData.addSkillExp(instance, .004f);
                    instance.triggerAchievement(player);
                }
                setCooldown(instance, (int) (18 * lerpf(6, 3, exp)));
            }
        }
        
        @Override
        public void onFinalize() {
            if(isRemote)
                endEffects();
        }
        
        Vec3 getDest() {
            return Raytrace.getLookingPos(player, 12, EntitySelectors.nothing).getLeft();
        }
        
        // CLIENT
        @SideOnly(Side.CLIENT)
        EntityRippleMark mark;
        
        @SideOnly(Side.CLIENT)
        void startEffects() {
            if(isLocal()) {
                world.spawnEntityInWorld(mark = new EntityRippleMark(world));
                mark.color.setColor4d(0.2, 1.0, 0.2, 0.7);
            }
        }
        
        @SideOnly(Side.CLIENT)
        void updateEffects() {
            if(isLocal()) {
                Vec3 dest = getDest();
                mark.setPosition(dest.xCoord, dest.yCoord, dest.zCoord);
            }
        }
        
        @SideOnly(Side.CLIENT)
        void endEffects() {
            if(isLocal()) {
                mark.setDead();
            }
        }
        
    }
    
    public static class JETriggerAction extends SkillSyncAction {
        
        static final float TIME = 8, LIFETIME = 15;
        
        Vec3 target;
        Vec3 start;
        Vec3 velocity;
        int ticks;

        float exp;

        public JETriggerAction(Vec3 _target) {
            super(-1);
            target = _target;
        }
        
        public JETriggerAction() {
            super(-1);
        }
        
        @Override
        public void writeNBTStart(NBTTagCompound tag) {
            tag.setFloat("x", (float) target.xCoord);
            tag.setFloat("y", (float) target.yCoord);
            tag.setFloat("z", (float) target.zCoord);
        }
        
        @Override
        public void readNBTStart(NBTTagCompound tag) {
            target = VecUtils.vec(tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z"));
        }
        
        @Override
        public void onStart() {
            super.onStart();

            exp = aData.getSkillExp(instance);

            if(isRemote) {
                startEffects();
                
                start = VecUtils.vec(player.posX, player.posY, player.posZ);
                velocity = VecUtils.multiply(VecUtils.subtract(target, start), 1.0 / TIME);
            }
        }
        
        @Override
        public void onTick() {
            ++ticks;
            
            if(isRemote) {
                if(ticks >= LIFETIME) {
                    ActionManager.endAction(this);
                } else if(ticks <= TIME){
                    Vec3 pos = VecUtils.lerp(start, target, ticks / TIME);
                    player.setPosition(pos.xCoord, pos.yCoord, pos.zCoord);
                    player.motionX = velocity.xCoord;
                    player.motionY = velocity.yCoord;
                    player.motionZ = velocity.zCoord;
                    player.fallDistance = 0.0f;
                }
                
                updateEffects();
            } else {
                MovingObjectPosition pos = Raytrace.perform(world, 
                    VecUtils.vec(player.lastTickPosX, player.lastTickPosY, player.lastTickPosZ),
                    VecUtils.vec(player.posX, player.posY, player.posZ),
                    EntitySelectors.and(EntitySelectors.excludeOf(player), EntitySelectors.living)
                );
                if(pos != null && pos.entityHit != null) {
                    MDDamageHelper.attack(pos.entityHit, player, lerpf(15, 35, exp));
                }
            }
        }
        
        @Override
        public void onFinalize() {
            if(isRemote)
                endEffects();
        }
        
        // CLIENT
        @SideOnly(Side.CLIENT)
        EntityDiamondShield entity;
        
        @SideOnly(Side.CLIENT)
        private void startEffects() {
            world.spawnEntityInWorld(entity = new EntityDiamondShield(player));
        }
        
        @SideOnly(Side.CLIENT)
        private void updateEffects() {
            if(isLocal()) {
                player.capabilities.setPlayerWalkSpeed(0.07f);
            }
            
            {
                for(int i = 0; i < 10; ++i) {
                    Vec3 pos2 = VecUtils.lerp(start, target, 3 * ticks / TIME);
                    Particle p = MdParticleFactory.INSTANCE.next(world,
                        VecUtils.add(VecUtils.vec(player.posX, player.posY, player.posZ), 
                        VecUtils.vec(
                            RandUtils.ranged(-.3, .3), 
                            RandUtils.ranged(-.3, .3),
                            RandUtils.ranged(-.3, .3))), 
                        VecUtils.vec(
                            RandUtils.ranged(-.02, .02), 
                            RandUtils.ranged(-.02, .02),
                            RandUtils.ranged(-.02, .02)));
                    world.spawnEntityInWorld(p);
                }
            }
        }
        
        @SideOnly(Side.CLIENT)
        private void endEffects() {
            if(isLocal()) {
                player.capabilities.setPlayerWalkSpeed(0.1f);
            }
            entity.setDead();
        }
        
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    private static void startTriggerAction(@Target EntityPlayer player, @Data Vec3 vec) {
        ActionManager.startAction(new JETriggerAction(vec));
    }

}
