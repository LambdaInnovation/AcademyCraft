package cn.academy.vanilla.teleporter.skill;

import cn.academy.ability.api.Skill;
import cn.academy.ability.api.context.ClientRuntime;
import cn.academy.ability.api.context.ClientRuntime.ActivateHandlers;
import cn.academy.ability.api.context.ClientRuntime.IActivateHandler;
import cn.academy.ability.api.context.Context;
import cn.academy.ability.api.context.ContextManager;
import cn.academy.ability.api.context.KeyDelegate;
import cn.academy.ability.api.event.FlushControlEvent;
import cn.academy.core.Resources;
import cn.academy.core.client.sound.ACSounds;
import cn.academy.vanilla.teleporter.entity.EntityTPMarking;
import cn.academy.vanilla.teleporter.util.GravityCancellor;
import cn.academy.vanilla.teleporter.util.TPSkillHelper;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import com.google.common.base.Preconditions;
import com.sun.javafx.geom.Vec3f;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3i;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Keyboard;

import java.util.Optional;


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

    private static final Vec3i[] dirs = new Vec3i[] {
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

        final float exp, consumption;
        final float overload_start, consumption_start;
        final int cooldown_time;
        float overloadKeep;
        final int max_time;
        int ticks = 0;

        public MainContext(EntityPlayer player) {
            super(player, instance);

            exp = ctx.getSkillExp();
            consumption = lerpf(13, 6, exp);
            overload_start = lerpf(250, 180, exp);
            consumption_start = lerpf(80, 60, exp);
            max_time = (int) lerpf(60, 150, exp);
            cooldown_time = (int) lerpf(900, 400, exp);
        }

        @Listener(channel=Context.MSG_MADEALIVE, side=Side.SERVER)
        void serverMadeAlive() {
            if(!ctx.consume(overload_start, consumption_start)) terminate(); else {
                overloadKeep = ctx.cpData.getOverload();
            }
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=Context.MSG_MADEALIVE, side=Side.CLIENT)
        void localMakeAlive() {
            if (isLocal()) {
                activateHandler = ActivateHandlers.terminatesContext(this);
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
                        Vec3f dest = getDest(performingKey);
                        marking.setPosition(dest.x, dest.y, dest.z);
                    }
                }

                if (cancellor != null && cancellor.isDead())
                    cancellor = null;
            }
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        void serverTick() {
            if(ctx.cpData.getOverload() < overloadKeep) ctx.cpData.setOverload(overloadKeep);
            if(ticks > max_time) terminate();
            ticks++;
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        void serverPerform(int keyid) {
            if (ctx.consume(0, consumption)) {
                Vec3f dest = getDest(keyid);
                if(player.isRiding())
                    player.ridingEntity=null;
                player.setPositionAndUpdate(dest.x, dest.y, dest.z);
                player.fallDistance = 0.0f;

                ctx.addSkillExp(.002f);
                instance.triggerAchievement(player);
                TPSkillHelper.incrTPCount(player);

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

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER)
        void serverTerminated() {
            ctx.setCooldown(cooldown_time);
        }

        @SideOnly(Side.CLIENT)
        private void startEffects() {
            endEffects();

            marking = new EntityTPMarking(player);
            Vec3f dest = getDest(performingKey);
            marking.setPosition(dest.x, dest.y, dest.z);

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
            return simulate ? ctx.canConsumeCP(consumption) : ctx.consume(0, consumption);
        }

        private Vec3f getDest(int keyid) {
            Preconditions.checkState(keyid != -1);

            double dist = lerpf(12, 18, exp);

            Vec3f dir = VecUtils.copy(dirs[keyid]);
            dir.rotateAroundZ(player.rotationPitch * MathUtils.PI_F / 180);
            dir.rotateAroundY((-90 - player.rotationYaw) * MathUtils.PI_F / 180);

            Motion3D mo = new Motion3D(player, true);
            mo.setMotion(dir.xCoord, dir.yCoord, dir.zCoord);

            RayTraceResult mop = Raytrace.perform(player.getEntityWorld(), mo.getPosVec(), mo.move(dist).getPosVec(),
                    EntitySelectors.living().and(EntitySelectors.exclude(player)));

            double x, y, z;

            if (mop != null) {
                x = mop.hitVec.x;
                y = mop.hitVec.y;
                z = mop.hitVec.z;

                if (mop.typeOfHit == RayTraceResult.Type.BLOCK) {
                    switch (mop.sideHit) {
                        case DOWN:
                            y -= 1.0;
                            break;
                        case UP:
                            y += 1.8;
                            break;
                        case NORTH:
                            z -= .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                        case SOUTH:
                            z += .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                        case WEST:
                            x -= .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                        case EAST:
                            x += .6;
                            y = mop.hitVec.y + 1.7;
                            break;
                    }
                    // check head
                    if (mop.sideHit.getIndex() > 1) {
                        int hx = (int) x, hy = (int) (y + 1), hz = (int) z;
                        if (!player.getEntityWorld().isAirBlock(new BlockPos(hx, hy, hz))) {
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