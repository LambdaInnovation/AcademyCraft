/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.teleporter.skills;

import java.util.Optional;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.context.Context;
import cn.academy.ability.api.context.ContextManager;
import cn.academy.ability.api.context.KeyDelegate;
import cn.academy.ability.api.data.CPData;
import cn.academy.ability.api.event.FlushControlEvent;
import cn.academy.core.client.Resources;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import com.google.common.base.Preconditions;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;

import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import cn.academy.ability.api.data.AbilityData;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.academy.vanilla.teleporter.util.GravityCancellor;
import cn.academy.vanilla.teleporter.util.TPAttackHelper;
import cn.lambdalib.util.deprecated.LIFMLGameEventDispatcher;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
public class Flashing extends Skill {

    public static final Flashing instance = new Flashing();

    private static final String
        MSG_PERFORM = "perform",
        KEY_GROUP = "TP_Flashing";

    private Flashing() {
        super("flashing", 5);
    }

    private static float getRange(AbilityData aData) {
        return instance.callFloatWithExp("range", aData);
    }

    @Override
    public void activate(ClientRuntime rt, int keyID) {
        rt.addKey(keyID, new KeyDelegate() {
            @Override
            public void onKeyDown() {
                Optional<MainContext> opt = ContextManager.instance.find(MainContext.class);
                if (!opt.isPresent()) {
                    ContextManager.instance.activate(new MainContext(getPlayer()));
                } else {
                    opt.get().terminate();
                }

                MinecraftForge.EVENT_BUS.post(new FlushControlEvent());
            }

            @Override
            public ResourceLocation getIcon() {
                return instance.getHintIcon();
            }
        });

        Optional<MainContext> opt = ContextManager.instance.find(MainContext.class);
        if (opt.isPresent()) {
            final MainContext ctx = opt.get();

            final String[] strs = new String[] { "a", "d", "w", "s"};
            final int[] keys = new int[] { Keyboard.KEY_A, Keyboard.KEY_D, Keyboard.KEY_W, Keyboard.KEY_S };
            for (int i = 0; i < 4; ++i) {
                final int localid = i;
                rt.addKey(KEY_GROUP, keys[i], new KeyDelegate() {
                    @Override
                    public void onKeyDown() {
                        ctx.localStart(localid);
                    }
                    @Override
                    public void onKeyUp() {
                        ctx.localEnd(localid);
                    }
                    @Override
                    public void onKeyAbort() {
                        ctx.localAbort(localid);
                    }
                    @Override
                    public ResourceLocation getIcon() {
                        return Resources.getTexture("abilities/teleporter/flashing/" + strs[localid]);
                    }
                });
            }
        }
    }

    static final Vec3[] dirs = new Vec3[] {
            VecUtils.vec(0, 0, -1),
            VecUtils.vec(0, 0, 1),
            VecUtils.vec(1, 0, 0),
            VecUtils.vec(-1, 0, 0)
    };

    public static class MainContext extends Context {

        int performingKey = -1;

        @SideOnly(Side.CLIENT)
        EntityTPMarking marking;

        @SideOnly(Side.CLIENT)
        GravityCancellor cancellor;

        final AbilityData aData;
        final CPData cpData;

        public MainContext(EntityPlayer player) {
            super(player);

            aData = aData();
            cpData = cpData();
        }

        @SideOnly(Side.CLIENT)
        void localStart(int keyid) {
            performingKey = keyid;

            startEffects();
        }

        @SideOnly(Side.CLIENT)
        void localEnd(int keyid) {
            if (keyid != performingKey) {
                return;
            }

            endEffects();

            sendToServer(MSG_PERFORM, performingKey);

            performingKey = -1;
        }

        @SideOnly(Side.CLIENT)
        void localAbort(int localid) {
            if (performingKey == localid) {
                performingKey = -1;
                endEffects();
            }
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        void localTick() {
            if (isLocal()) {
                if (performingKey != -1 && !consume(true)) {
                    performingKey = -1;
                    endEffects();
                } else {
                    if (marking != null) {
                        Vec3 dest = getDest(performingKey);
                        marking.setPosition(dest.xCoord, dest.yCoord, dest.zCoord);
                    }
                }

                if (cancellor != null && cancellor.isDead())
                    cancellor = null;
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        void serverPerform(int keyid) {
            if (cpData().perform(instance.getOverload(aData), instance.getConsumption(aData))) {
                Vec3 dest = getDest(keyid);
                player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);
                player.fallDistance = 0.0f;

                aData.addSkillExp(instance, instance.getFloat("expincr"));
                instance.triggerAchievement(player);
                TPAttackHelper.incrTPCount(player);

                sendToClient(MSG_PERFORM);
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        void clientPerform() {
            ACSounds.playClient(player, "tp.tp_flashing", 1.0f);
            if (isLocal()) {
                if (cancellor != null) {
                    cancellor.setDead();
                    cancellor = null;
                }
                cancellor = new GravityCancellor(player, 40);
                LIFMLGameEventDispatcher.INSTANCE.registerClientTick(cancellor);
                clientRuntime().setCooldownRaw(Flashing.instance, 5);
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        void localTerminate() {
            if (isLocal()) {
                clientRuntime().clearKeys(KEY_GROUP);
                endEffects();
            }
        }

        @SideOnly(Side.CLIENT)
        private void startEffects() {
            marking = new EntityTPMarking(player);
            world().spawnEntityInWorld(marking);
        }

        @SideOnly(Side.CLIENT)
        private void endEffects() {
            if (marking != null) {
                marking.setDead();
                marking = null;
            }
        }

        private boolean consume(boolean simulate) {
            float consumption = instance.getConsumption(aData);
            return simulate ? cpData.canPerform(consumption) : cpData.perform(instance.getOverload(aData), consumption);
        }

        private Vec3 getDest(int keyid) {
            Preconditions.checkState(keyid != -1);

            double dist = Flashing.getRange(aData);

            Vec3 dir = VecUtils.copy(dirs[keyid]);
            dir.rotateAroundZ(player.rotationPitch * MathUtils.PI_F / 180);
            dir.rotateAroundY((-90 - player.rotationYaw) * MathUtils.PI_F / 180);

            Motion3D mo = new Motion3D(player.posX, player.posY, player.posZ, dir.xCoord, dir.yCoord, dir.zCoord);

            MovingObjectPosition mop = Raytrace.perform(player.worldObj, mo.getPosVec(), mo.move(dist).getPosVec(),
                    EntitySelectors.and(EntitySelectors.living, EntitySelectors.excludeOf(player)));

            double x, y, z;

            if (mop != null) {
                x = mop.hitVec.xCoord;
                y = mop.hitVec.yCoord;
                z = mop.hitVec.zCoord;

                if (mop.typeOfHit == MovingObjectType.BLOCK) {
                    switch (mop.sideHit) {
                    case 0:
                        y -= 1.0;
                        break;
                    case 1:
                        y += 1.8;
                        break;
                    case 2:
                        z -= .6;
                        y = mop.blockY + 1.7;
                        break;
                    case 3:
                        z += .6;
                        y = mop.blockY + 1.7;
                        break;
                    case 4:
                        x -= .6;
                        y = mop.blockY + 1.7;
                        break;
                    case 5:
                        x += .6;
                        y = mop.blockY + 1.7;
                        break;
                    }
                    // check head
                    if (mop.sideHit > 1) {
                        int hx = (int) x, hy = (int) (y + 1), hz = (int) z;
                        if (!player.worldObj.isAirBlock(hx, hy, hz)) {
                            y -= 1.25;
                        }
                    }
                } else {
                    y += mop.entityHit.getEyeHeight();
                }
            } else {
                x = mo.px;
                y = mo.py;
                z = mo.pz;
            }

            return VecUtils.vec(x, y, z);
        }

    }

}
