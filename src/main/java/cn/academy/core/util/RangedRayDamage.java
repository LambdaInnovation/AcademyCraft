/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.core.util;

import cn.academy.ability.api.AbilityContext;
import cn.academy.ability.api.AbilityPipeline;
import cn.academy.core.event.BlockDestroyEvent;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.WorldUtils;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.entity.Entity;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.ForgeDirection;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static cn.lambdalib.util.generic.VecUtils.*;

/**
 * A super boomy ranged ray damage. it starts out a ranged ray in the given position and direction,
 *     and destroy blocks in the path, also damages entities. It takes account of global damage switches.
 * @author WeAthFolD
 */
public class RangedRayDamage {
    
    static final double STEP = 0.9;

    public final World world;
    public final Motion3D motion;
    public final AbilityContext ctx;


    public double range;
    public float totalEnergy; // decrements [hardness of block] when hit a block
    public int maxIncrement = 50;
    public float dropProb = 0.05f;
    
    public Predicate<Entity> entitySelector = EntitySelectors.everything();
    public float startDamage = 10.0f; // ATTN: LINEAR 1.0*startDamage at dist 0; 0.2 * startDamage at maxIncrement
    
    private Vec3 start, slope;
    
    public RangedRayDamage(AbilityContext ctx, double _range, float _energy) {
        this.ctx = ctx;

        this.motion = new Motion3D(ctx.player, true).move(0.1);
        this.world = ctx.player.worldObj;
        this.range = _range;
        this.totalEnergy = _energy;

        entitySelector = EntitySelectors.exclude(ctx.player);
    }
    
    /**
     * BOOM!
     */
    public void perform() {
        motion.normalize();
        Set<int[]> processed = new HashSet<>();
        
        float yaw = -MathUtils.PI_F * 0.5f - motion.getRotationYawRadians(), 
                pitch = motion.getRotationPitchRadians();
        
        start = motion.getPosVec();
        slope = motion.getMotionVec();
        
        Vec3 vp0 = VecUtils.vec(0, 0, 1);
        vp0.rotateAroundZ(pitch);
        vp0.rotateAroundY(yaw);
        
        Vec3 vp1 = VecUtils.vec(0, 1, 0);
        vp1.rotateAroundZ(pitch);
        vp1.rotateAroundY(yaw);

        double maxDistance = Double.MAX_VALUE;
        
        /* Apply Entity Damage */ {
            Vec3 v0 = add(start, add(multiply(vp0, -range), multiply(vp1, -range))),
                    v1 = add(start, add(multiply(vp0, range), multiply(vp1, -range))),
                    v2 = add(start, add(multiply(vp0, range), multiply(vp1, range))),
                    v3 = add(start, add(multiply(vp0, -range), multiply(vp1, range))),
                    v4 = add(v0, multiply(slope, maxIncrement)),
                    v5 = add(v1, multiply(slope, maxIncrement)),
                    v6 = add(v2, multiply(slope, maxIncrement)),
                    v7 = add(v3, multiply(slope, maxIncrement));
            AxisAlignedBB aabb = WorldUtils.minimumBounds(v0, v1, v2, v3, v4, v5, v6, v7);

            Predicate<Entity> areaSelector = target -> {
                Vec3 dv = subtract(vec(target.posX, target.posY, target.posZ), start);
                Vec3 proj = dv.crossProduct(slope);
                return proj.lengthVector() < range * 1.2;
            };
            List<Entity> targets = WorldUtils.getEntities(world, aabb, entitySelector.and(areaSelector));
            targets.sort((lhs, rhs) -> {
                double dist1 = ctx.player.getDistanceSq(lhs.posX, lhs.posY, lhs.posZ);
                double dist2 = ctx.player.getDistanceSq(rhs.posX, rhs.posY, rhs.posZ);
                return Double.valueOf(dist1).compareTo(dist2);
            });

            for(Entity e : targets) {
                if (!attackEntity(e)) {
                    maxDistance = e.getDistanceSqToEntity(ctx.player);
                    break;
                }
            }
        }

        if(AbilityPipeline.canBreakBlock()) {
            for(double s = -range; s <= range; s += STEP) {
                for(double t = -range; t <= range; t += STEP) {
                    double rr = range * RandUtils.ranged(0.9, 1.1);

                    if(s * s + t * t > rr * rr)
                        continue;
                    
                    Vec3 pos = VecUtils.add(start, 
                        VecUtils.add(
                            VecUtils.multiply(vp0, s),
                            VecUtils.multiply(vp1, t)));
                    
                    int[] coords = { (int) pos.xCoord, (int) pos.yCoord, (int) pos.zCoord };
                    if(processed.contains(coords))
                        continue;
                    
                    processed.add(coords);
                }
            }
            
            float ave = totalEnergy / processed.size();
            for(int[] coords : processed) {
                processLine(coords[0], coords[1], coords[2], 
                    slope, ave * RandUtils.rangef(0.95f, 1.05f), maxDistance);
            }
        }
        

    }
    
    private void processLine(int x0, int y0, int z0, Vec3 slope, float energy, double maxDistSq) {
        Plotter plotter = new Plotter(x0, y0, z0, slope.xCoord, slope.yCoord, slope.zCoord);
        int incrs = 0;
        for(int i = 0; i <= maxIncrement && energy > 0; ++i) {
            ++incrs;
            int[] coords = plotter.next();
            int x = coords[0], y = coords[1], z = coords[2];

            int dx = x0 - x, dy = y0 - y, dz = z0 - z;
            int dsq = dx*dx + dy*dy + dz*dz;
            if (dsq > maxDistSq) break;

            boolean snd = incrs < 20;
            
            energy = destroyBlock(energy, x, y, z, snd);
            
            if(RandUtils.ranged(0, 1) < 0.05) {
                ForgeDirection dir = ForgeDirection.values()[RandUtils.rangei(0, 6)];
                energy = destroyBlock(energy, x + dir.offsetX, y + dir.offsetY, z + dir.offsetZ, snd);
            }
        }
    }
    
    private float destroyBlock(float energy, int x, int y, int z, boolean snd) {
        Block block = world.getBlock(x, y, z);
        float hardness = block.getBlockHardness(world, x, y, z);
        if(hardness < 0)
            hardness = 233333;
        if(!MinecraftForge.EVENT_BUS.post(new BlockDestroyEvent(world, x, y, z)) && energy >= hardness) {
            if(block.getMaterial() != Material.air) {
                block.dropBlockAsItemWithChance(world, x, y, z, 
                    world.getBlockMetadata(x, y, z), dropProb, 0);
                
                if(snd && RandUtils.ranged(0, 1) < 0.1) {
                    world.playSoundEffect(x + 0.5F, y + 0.5F, 
                            z + 0.5F, 
                            block.stepSound.getBreakSound(), 
                            (block.stepSound.getVolume() + 1.0F) / 2.0F, 
                            block.stepSound.getPitch());
                }
            }
            world.setBlockToAir(x, y, z);
            return energy - hardness;
        }
        return 0;
    }

    protected boolean attackEntity(Entity target) {
        Vec3 dv = subtract(vec(target.posX, target.posY, target.posZ), start);
        float dist = Math.min(maxIncrement, (float) dv.crossProduct(slope).lengthVector());
        
        float realDmg = this.startDamage * MathUtils.lerpf(1, 0.2f, dist / maxIncrement);
        return applyAttack(target, realDmg);
    }

    protected boolean applyAttack(Entity target, float damage) {
        ctx.attack(target, damage);
        return true;
    }

    public static class Reflectible extends RangedRayDamage {

        public final Consumer<Entity> callback;

        public Reflectible(AbilityContext ctx,
                           double _range, float _energy, Consumer<Entity> callback) {
            super(ctx, _range, _energy);
            this.callback = callback;
        }

        @Override
        protected boolean applyAttack(Entity target, float damage) {
            boolean[] result = new boolean[] { true }; // for lambda modification
            ctx.attackReflect(target, damage, () -> {
                callback.accept(target);
                result[0] = false;
            });
            return result[0];
        }

    }

}
