/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.ctrl.SkillInstance;
import cn.academy.ability.api.ctrl.action.SyncActionInstant;
import cn.academy.ability.api.ctrl.instance.SkillInstanceInstant;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.vanilla.meltdowner.entity.EntityMdBall;
import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.networkcall.RegNetworkCall;
import cn.lambdalib.networkcall.s11n.StorageOption.Instance;
import cn.lambdalib.networkcall.s11n.StorageOption.RangedTarget;
import cn.lambdalib.util.entityx.EntityCallback;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraft.world.World;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
@Registrant
public class ElectronBomb extends Skill {
    
    public static final ElectronBomb instance = new ElectronBomb();

    static final int LIFE = 20, LIFE_IMPROVED = 5;
    static final double DISTANCE = 15;
    
    private ElectronBomb() {
        super("electron_bomb", 1);
    }
    
    static float getDamage(float exp) {
        return lerpf(12, 20, exp);
    }
    
    @Override
    public SkillInstance createSkillInstance(EntityPlayer player) {
        return new SkillInstanceInstant().addChild(new EBAction());
    }
    
    public static class EBAction extends SyncActionInstant {

        @Override
        public boolean validate() {
            float exp = aData.getSkillExp(instance);
            float overload = lerpf(39, 17, exp);
            float cp = lerpf(117, 135, exp);

            return cpData.perform(overload, cp);
        }

        @Override
        public void execute() {
            float exp = aData.getSkillExp(instance);

            if(!isRemote) {
                EntityMdBall ball = new EntityMdBall(player, aData.getSkillExp(instance) >= 0.8f ? LIFE_IMPROVED : LIFE, 
                new EntityCallback<EntityMdBall>() {

                    @Override
                    public void execute(EntityMdBall ball) {
                        MovingObjectPosition trace = Raytrace.perform(world, VecUtils.vec(ball.posX, ball.posY, ball.posZ), getDest(player),
                                EntitySelectors.exclude(player).and(EntitySelectors.of(EntityMdBall.class).negate()));
                        if(trace != null && trace.entityHit != null) {
                            MDDamageHelper.attack(player, instance, trace.entityHit, getDamage(exp));
                        }
                        actionClient(player, ball);
                    }
                    
                });
                world.spawnEntityInWorld(ball);
            }
            
            aData.addSkillExp(instance, .005f);
            setCooldown(instance, (int) lerpf(20, 10, exp));
        }
        
    }
    
    @RegNetworkCall(side = Side.CLIENT)
    static void actionClient(@RangedTarget(range = 20) EntityPlayer player, @Instance EntityMdBall ball) {
        Vec3 dest = getDest(player);
        spawnRay(player.worldObj, player, ball.posX, ball.posY, ball.posZ,
            dest.xCoord, dest.yCoord, dest.zCoord);
    }
    
    @SideOnly(Side.CLIENT)
    private static void spawnRay(World world, EntityPlayer player, double x0, double y0, double z0, double x1, double y1, double z1) {
        EntityMdRaySmall raySmall = new EntityMdRaySmall(world);
        raySmall.setFromTo(x0, y0 + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), z0,
                x1, y1, z1);
        raySmall.viewOptimize = false;
        world.spawnEntityInWorld(raySmall);
    }
    
    static Vec3 getDest(EntityPlayer player) {
        return Raytrace.getLookingPos(player, DISTANCE).getLeft();
    }

}
