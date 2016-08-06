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
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.vanilla.meltdowner.entity.EntityMdBall;
import cn.academy.vanilla.meltdowner.entity.EntityMdRaySmall;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cn.lambdalib.s11n.network.TargetPoints;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.util.entityx.EntityCallback;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;

import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
@Registrant
@NetworkS11nType
public class ElectronBomb extends Skill {
    
    public static final ElectronBomb instance = new ElectronBomb();

    private static final Object delegate = NetworkMessage.staticCaller(ElectronBomb.class);

    private static final int LIFE = 20, LIFE_IMPROVED = 5;
    private static final double DISTANCE = 15;
    
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
    
    public static class EBAction extends SyncActionInstant<ElectronBomb> {

        public EBAction() {
            super(instance);
        }

        @Override
        public boolean validate() {
            float exp = ctx().getSkillExp();
            float overload = lerpf(39, 17, exp);
            float cp = lerpf(117, 135, exp);

            return ctx().consume(overload, cp);
        }

        @Override
        public void execute() {
            float exp = ctx().getSkillExp();

            if(!isRemote) {
                EntityMdBall ball = new EntityMdBall(player, ctx().getSkillExp() >= 0.8f ? LIFE_IMPROVED : LIFE,
                new EntityCallback<EntityMdBall>() {

                    @Override
                    public void execute(EntityMdBall ball) {
                        MovingObjectPosition trace = Raytrace.perform(player.worldObj, VecUtils.vec(ball.posX, ball.posY, ball.posZ), getDest(player),
                                EntitySelectors.exclude(player).and(EntitySelectors.of(EntityMdBall.class).negate()));
                        if(trace != null && trace.entityHit != null) {
                            MDDamageHelper.attack(ctx(), trace.entityHit, getDamage(exp));
                        }

                        NetworkMessage.sendToAllAround(TargetPoints.convert(player, 20),
                                delegate, "spawn_ray", player, ball);
                    }
                    
                });
                player.worldObj.spawnEntityInWorld(ball);
            }
            
            ctx().addSkillExp(.005f);
            ctx().setCooldown((int) lerpf(20, 10, exp));
        }
        
    }

    @SideOnly(Side.CLIENT)
    @Listener(channel="spawn_ray", side=Side.CLIENT)
    private static void hSpawnRay(EntityPlayer player, EntityMdBall ball) {
        Vec3 dest = getDest(player);
        EntityMdRaySmall raySmall = new EntityMdRaySmall(player.worldObj);
        raySmall.setFromTo(ball.posX, ball.posY + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), ball.posZ,
                dest.xCoord, dest.yCoord, dest.zCoord);
        raySmall.viewOptimize = false;
        player.worldObj.spawnEntityInWorld(raySmall);
    }
    
    private static Vec3 getDest(EntityPlayer player) {
        return Raytrace.getLookingPos(player, DISTANCE).getLeft();
    }

}
