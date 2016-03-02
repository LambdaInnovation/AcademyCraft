/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.ActionManager;
import cn.academy.ability.api.ctrl.action.SkillSyncAction;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.core.event.BlockDestroyEvent;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.util.entityx.handlers.Rigidbody;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

import static cn.lambdalib.util.generic.RandUtils.ranged;
import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
public abstract class MineRaysBase extends Skill {
    
    protected ResourceLocation particleTexture;
    
    final String postfix;

    public MineRaysBase(String _postfix, int atLevel) {
        super("mine_ray_" + _postfix, atLevel);
        postfix = _postfix;
    }
    
    public static abstract class MRAction extends SkillSyncAction {
        final MineRaysBase skill;
        
        int x = -1, y = -1, z = -1;
        float hardnessLeft = Float.MAX_VALUE;

        float exp;
        
        public MRAction(MineRaysBase _skill) {
            skill = _skill;
        }
        
        @Override
        public void onStart() {
            super.onStart();

            exp = aData.getSkillExp(skill);

            cpData.perform(lerpf(o_l, o_r, exp), 0);
            if(isRemote)
                startEffects();
        }
        
        @Override
        public void onTick() {
            if(!cpData.perform(0, lerpf(cp_l, cp_r, exp)) && !isRemote)
                ActionManager.abortAction(this);
            
            MovingObjectPosition result = Raytrace.traceLiving(player, range, EntitySelectors.nothing);
            if(result != null) {
                int tx = result.blockX, ty = result.blockY, tz = result.blockZ;
                if(tx != x || ty != y || tz != z) {
                    Block block = world.getBlock(tx, ty, tz);
                    if(!MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(player.worldObj, tx, ty, tz)) && 
                            block.getHarvestLevel(world.getBlockMetadata(x, y, z)) <= harvestLevel) {
                        x = tx; y = ty; z = tz;
                        hardnessLeft = block.getBlockHardness(world, tx, ty, tz);
                        
                        if(hardnessLeft < 0)
                            hardnessLeft = Float.MAX_VALUE;
                    } else {
                        x = y = z = -1;
                    }
                } else {
                    hardnessLeft -= lerpf(speed_l, speed_r, exp);
                    if(hardnessLeft <= 0) {
                        if(!isRemote) {
                            onBlockBreak(world, x, y, z, world.getBlock(x, y, z));
                            aData.addSkillExp(skill, expincr);
                        }
                        x = y = z = -1;
                    }
                    if(isRemote)
                        spawnParticles();
                }
            } else {
                x = y = z = -1;
            }
            
            if(isRemote)
                updateEffects();
        }
        
        @Override
        public void onFinalize() {
            if(isRemote)
                endEffects();
            setCooldown(skill, (int) lerpf(cd_l, cd_r, exp));
        }
        
        // CLIENT
        @SideOnly(Side.CLIENT)
        static FollowEntitySound loopSound;
        
        Entity ray;
        
        @SideOnly(Side.CLIENT)
        public void startEffects() {
            world.spawnEntityInWorld(ray = createRay());
            loopSound = new FollowEntitySound(player, "md.mine_loop").setLoop().setVolume(0.3f);
            ACSounds.playClient(loopSound);
            ACSounds.playClient(player, "md.mine_" + skill.postfix + "_startup", 0.4f);
        }
        
        @SideOnly(Side.CLIENT)
        public void updateEffects() {
            
        }
        
        @SideOnly(Side.CLIENT)
        public void spawnParticles() {
            for(int i = 0, max = RandUtils.rangei(2, 3); i < max; ++i) {
                double _x = x + ranged(-.2, 1.2),
                        _y = y + ranged(-.2, 1.2),
                        _z = z + ranged(-.2, 1.2);
                
                Particle p = MdParticleFactory.INSTANCE.next(world,
                        VecUtils.vec(_x, _y, _z),
                        VecUtils.vec(ranged(-.06, .06), ranged(-.06, .06), ranged(-.06, .06)));
                if(skill.particleTexture != null) {
                    p.texture = skill.particleTexture;
                }
                
                p.needRigidbody = false;
                Rigidbody rb = new Rigidbody();
                rb.gravity = 0.01;
                rb.entitySel = null;
                rb.blockFil = null;
                p.addMotionHandler(rb);
                
                world.spawnEntityInWorld(p);
            }
        }
        
        @SideOnly(Side.CLIENT)
        public void endEffects() {
            ray.setDead();
            loopSound.stop();
        }

        private float range;
        private float speed_l, speed_r;
        private float cp_l, cp_r;
        private float o_l, o_r;
        private float cd_l, cd_r;
        private float expincr;
        private int harvestLevel;

        protected void setRange(float _range) {
            range = _range;
        }

        protected void setHarvestLevel(int _level) {
            harvestLevel = _level;
        }

        protected void setSpeed(float l, float r) {
            speed_l = l; speed_r = r;
        }

        protected void setConsumption(float l, float r) {
            cp_l = l; cp_r = r;
        }

        protected void setOverload(float l, float r) {
            o_l = l; o_r = r;
        }

        protected void setCooldown(float l, float r) {
            cd_l = l; cd_r = r;
        }

        protected void setExpIncr(float amt) {
            expincr = amt;
        }

        protected abstract void onBlockBreak(World world, int x, int y, int z, Block block);

        @SideOnly(Side.CLIENT)
        protected abstract Entity createRay();
        
    }
}
