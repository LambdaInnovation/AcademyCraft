/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.AbilityPipeline;
import cn.academy.ability.api.Skill;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.context.Context;
import cn.academy.ability.api.cooldown.CooldownData;
import cn.academy.core.client.ACRenderingHelper;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.core.client.sound.FollowEntitySound;
import cn.academy.core.util.RangedRayDamage;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.academy.vanilla.meltdowner.entity.EntityMDRay;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.Vec3;

import static cn.lambdalib.util.generic.RandUtils.*;
import static cn.lambdalib.util.generic.MathUtils.*;

/**
 * @author WeAthFolD
 */
public class Meltdowner extends Skill {

    public static final Meltdowner instance = new Meltdowner();
    
    private Meltdowner() {
        super("meltdowner", 3);
    }
    
    @Override
    public void activate(ClientRuntime rt, int keyid) {
        activateSingleKey2(rt, keyid, MDContext::new);
    }

    public static class MDContext extends Context {

        private static final String
            MSG_PERFORM = "perform",
            MSG_REFLECTED = "reflect";

        int ticks;

        static final int TICKS_MIN = 20, TICKS_MAX = 40, TICKS_TOLE = 100;

        final float exp = aData().getSkillExp(instance);
        final float tickConsumption = lerpf(15, 27, exp);

        @SideOnly(Side.CLIENT)
        FollowEntitySound sound;

        public MDContext(EntityPlayer player) {
            super(player);
        }

        @Listener(channel=Context.MSG_KEYUP, side=Side.CLIENT)
        void l_keyUp() {
            if (ticks >= TICKS_MIN) {
                sendToServer(MSG_PERFORM);
            } else {
                terminate();
            }
        }

        @Listener(channel=Context.MSG_KEYABORT, side=Side.CLIENT)
        void l_keyAbort() {
            terminate();
        }

        @Listener(channel=MSG_TICK, side={Side.CLIENT, Side.SERVER})
        void g_tick() {
            ++ticks;

            if (!isRemote()) {
                if (!cpData().perform(0, tickConsumption) || ticks > TICKS_TOLE) {
                    terminate();
                }
            }
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        void c_start() {
            sound = new FollowEntitySound(player, "md.md_charge").setVolume(1.0f);
            ACSounds.playClient(sound);
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        void c_tick() {
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
                world().spawnEntityInWorld(MdParticleFactory.INSTANCE.next(world(), pos, vel));
            }
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        void c_terminate() {
            if (isLocal()) {
                player.capabilities.setPlayerWalkSpeed(0.1f);
            }
            sound.stop();
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        void s_perform() {
            float overload = lerpf(300, 200, exp);

            cpData().perform(overload, 0);
            int ct = toChargeTicks();

            double length[] = new double[] { 30 }; // for lambda mod
            RangedRayDamage rrd = new RangedRayDamage.Reflectible(
                    player,
                    instance,
                    lerpf(2, 3, exp),
                    getEnergy(ct),
                    (reflector) -> {
                        length[0] = Math.min(length[0], reflector.getDistanceToEntity(player));

                        s_reflected(reflector);
                        sendToClient(MSG_REFLECTED, reflector);
                    });
            rrd.startDamage = getDamage(ct);
            rrd.perform();

            aData().addSkillExp(instance, getExpIncr(ct));

            CooldownData.of(player).set(instance, getCooldown(ct));
            sendToClient(MSG_PERFORM, ct, length[0]);

            terminate();
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        void c_perform(int ct, double length) {
            EntityMDRay ray = new EntityMDRay(player, length);
            ACSounds.playClient(player, "md.meltdowner", 0.5f);
            world().spawnEntityInWorld(ray);
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_REFLECTED, side=Side.CLIENT)
        void c_reflected(Entity reflector) {
            Vec3 playerLook = player.getLookVec().normalize();
            double distance = VecUtils.entityHeadPos(player).distanceTo(VecUtils.entityHeadPos(reflector));
            Vec3 spawnPos = VecUtils.add(VecUtils.entityHeadPos(player), VecUtils.multiply(playerLook, distance));

            System.out.println("Reflected distance=" + distance);

            Motion3D mo = new Motion3D(reflector, true);
            mo.setPosition(spawnPos.xCoord, spawnPos.yCoord, spawnPos.zCoord);

            EntityMDRay ray = new EntityMDRay(player, mo, 10);

            world().spawnEntityInWorld(ray);
        }

        void s_reflected(Entity reflector) {
            MovingObjectPosition result = Raytrace.traceLiving(reflector, 10);

            if (result != null && result.typeOfHit == MovingObjectType.ENTITY) {
                AbilityPipeline.attack(player, instance, result.entityHit, 0.5f * lerpf(20, 50, exp));
            }
        }

        private float timeRate(int ct) {
            return MathUtils.lerpf(0.8f, 1.2f, (ct - 20.0f) / 20.0f);
        }

        private float getEnergy(int ct) {
            return timeRate(ct) * MathUtils.lerpf(300, 700, exp);
        }

        private float getDamage(int ct) {
            return timeRate(ct) * MathUtils.lerpf(20, 50, exp);
        }

        private int getCooldown(int ct) {
            return (int)(timeRate(ct) * 20 * MathUtils.lerpf(15, 7, exp));
        }

        private float getExpIncr(int ct) {
            return timeRate(ct) * 0.002f;
        }

        private int toChargeTicks() {
            return Math.min(ticks, TICKS_MAX);
        }
    }

}
