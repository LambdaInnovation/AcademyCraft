package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.entity.EntityMdBall;
import cn.academy.entity.EntityMdRaySmall;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11n;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.Raytrace;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

public class ElectronBomb extends Skill {
    public static final ElectronBomb Instance = new ElectronBomb();

    private ElectronBomb() {
        super("electron_bomb", 1);
    }

    @Override
    public void activate(ClientRuntime rt, int keyID) {
        activateSingleKey2(rt, keyID, Ctx::new);
    }

    static final String MsgEffect = "effect";
    static final int Life = 20, LifeImproved = 5;
    static final double Distance = 15;

    static Vec3d getDest(EntityPlayer player) {
        return Raytrace.getLookingPos(player, Distance).getLeft();
    }

    public static class Ctx extends Context<ElectronBomb> {

        public Ctx(EntityPlayer _player) {
            super(_player, Instance);
        }

        @Listener(channel = MSG_MADEALIVE, side = Side.SERVER)
        private void s_Execute() {
            float exp = ctx.getSkillExp();
            EntityMdBall ball = new EntityMdBall(
                player,
                ctx.getSkillExp() > 0.8f ? LifeImproved : Life,
                target -> {
                    RayTraceResult trace = Raytrace.perform(player.world, new Vec3d(target.posX, target.posY + player.eyeHeight, target.posZ),
                        getDest(player), EntitySelectors.exclude(player).and(EntitySelectors.of(EntityMdBall.class).negate()));
                    if (trace != null && trace.entityHit != null)
                        MDDamageHelper.attack(ctx, trace.entityHit, getDamage(exp));

                    NetworkMessage.sendToAllAround(
                        TargetPoints.convert(player, 20),
                        EffectDelegate.Instance,
                        MsgEffect,
                        target
                    );
                });
            player.world.spawnEntity(ball);

            ctx.addSkillExp(.005f);
            ctx.setCooldown((int) MathUtils.lerpf(20, 10, exp));
            terminate();
        }

        private float getDamage(float exp) {
            return MathUtils.lerpf(6, 12, exp);
        }

    }

    public enum EffectDelegate {
        Instance;

        EffectDelegate() {}

        @StateEventCallback
        private static void preInit(FMLPreInitializationEvent ev) {
            NetworkS11n.addDirectInstance(Instance);
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel = MsgEffect, side = Side.CLIENT)
        private void onSpawnEffect(EntityMdBall ball) {
            EntityPlayer player = ball.getSpawner();
            if (player == null)
                return;
            Vec3d dest = Raytrace.getLookingPos(player, Distance).getLeft();
            EntityMdRaySmall ray = new EntityMdRaySmall(ball.getEntityWorld());
            ray.setFromTo(ball.posX, ball.posY + player.eyeHeight, ball.posZ, dest.x, dest.y, dest.z);
            ray.viewOptimize = false;
            player.world.spawnEntity(ray);
        }
    }
}
