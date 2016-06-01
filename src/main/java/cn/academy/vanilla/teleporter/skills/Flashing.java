/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.skills;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.context.ClientRuntime.IActivateHandler;
import cn.academy.ability.api.context.Context;
import cn.academy.ability.api.context.ContextManager;
import cn.academy.ability.api.context.KeyDelegate;
import cn.academy.ability.api.event.FlushControlEvent;
import cn.academy.core.client.Resources;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.academy.vanilla.teleporter.util.GravityCancellor;
import cn.academy.vanilla.teleporter.util.TPSkillHelper;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.util.deprecated.LIFMLGameEventDispatcher;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cn.lambdalib.util.mc.EntitySelectors;
import cn.lambdalib.util.mc.Raytrace;
import com.google.common.base.Preconditions;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.MovingObjectPosition.MovingObjectType;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import org.lwjgl.input.Keyboard;

import java.util.Optional;

import static cn.lambdalib.util.generic.MathUtils.lerpf;

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

    @SideOnly(Side.CLIENT)
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

            @Override
            public int createID() {
                return 0;
            }

            public Skill getSkill() {
                return instance;
            }
        });
    }

    private static final Vec3[] dirs = new Vec3[] {
            null,
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

        @SideOnly(Side.CLIENT)
        IActivateHandler activateHandler;

        final float exp, consumption, overload;

        public MainContext(EntityPlayer player) {
            super(player, instance);

            exp = ctx.getSkillExp();
            consumption = lerpf(100, 70, exp);
            overload = lerpf(90, 70, exp);
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=Context.MSG_MADEALIVE, side=Side.CLIENT)
        void localMakeAlive() {
            if (isLocal()) {
                activateHandler = new IActivateHandler() {
                    @Override
                    public boolean handles(EntityPlayer player) {
                        return true;
                    }
                    @Override
                    public void onKeyDown(EntityPlayer player) {
                        terminate();
                    }
                    @Override
                    public String getHint() {
                        return ENDSPECIAL;
                    }
                };
                clientRuntime().addActivateHandler(activateHandler);

                final String[] strs = new String[] { null, "a", "d", "w", "s"};
                final int[] keys = new int[] { -1, Keyboard.KEY_A, Keyboard.KEY_D, Keyboard.KEY_W, Keyboard.KEY_S };
                for (int i = 0; i < 4; ++i) {
                    final int localid = i + 1;
                    clientRuntime().addKey(KEY_GROUP, keys[localid], new KeyDelegate() {
                        @Override
                        public void onKeyDown() {
                            localStart(localid);
                        }
                        @Override
                        public void onKeyUp() {
                            localEnd(localid);
                        }
                        @Override
                        public void onKeyAbort() {
                            localAbort(localid);
                        }
                        @Override
                        public ResourceLocation getIcon() {
                            return Resources.getTexture("abilities/teleporter/flashing/" + strs[localid]);
                        }

                        @Override
                        public int createID() {
                            return localid;
                        }

                        public Skill getSkill() {
                            return instance;
                        }
                    });
                }
            }
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
            if (ctx.consume(overload, consumption)) {
                Vec3 dest = getDest(keyid);
                player.setPositionAndUpdate(dest.xCoord, dest.yCoord, dest.zCoord);
                player.fallDistance = 0.0f;

                ctx.addSkillExp(.002f);
                instance.triggerAchievement(player);
                TPSkillHelper.incrTPCount(player);

                ctx.setCooldownSub(keyid, 5);
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
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        void localTerminate() {
            if (isLocal()) {
                clientRuntime().removeActiveHandler(activateHandler);
                clientRuntime().clearKeys(KEY_GROUP);
                endEffects();
            }
        }

        @SideOnly(Side.CLIENT)
        private void startEffects() {
            endEffects();

            marking = new EntityTPMarking(player);
            Vec3 dest = getDest(performingKey);
            marking.setPosition(dest.xCoord, dest.yCoord, dest.zCoord);

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
            return simulate ? ctx.canConsumeCP(consumption) : ctx.consume(overload, consumption);
        }

        private Vec3 getDest(int keyid) {
            Preconditions.checkState(keyid != -1);

            double dist = lerpf(8, 15, exp);

            Vec3 dir = VecUtils.copy(dirs[keyid]);
            dir.rotateAroundZ(player.rotationPitch * MathUtils.PI_F / 180);
            dir.rotateAroundY((-90 - player.rotationYaw) * MathUtils.PI_F / 180);

            Motion3D mo = new Motion3D(player, true);
            mo.setMotion(dir.xCoord, dir.yCoord, dir.zCoord);

            MovingObjectPosition mop = Raytrace.perform(player.worldObj, mo.getPosVec(), mo.move(dist).getPosVec(),
                    EntitySelectors.living().and(EntitySelectors.exclude(player)));

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
