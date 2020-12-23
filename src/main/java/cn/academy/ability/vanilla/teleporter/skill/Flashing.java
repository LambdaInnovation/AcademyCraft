package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.ClientRuntime.ActivateHandlers;
import cn.academy.ability.context.ClientRuntime.IActivateHandler;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.ability.context.KeyDelegate;
import cn.academy.event.ability.FlushControlEvent;
import cn.academy.Resources;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityTPMarking;
import cn.academy.ability.vanilla.teleporter.util.GravityCancellor;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.MathUtils;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.VecUtils;
import com.google.common.base.Preconditions;
import net.minecraft.client.Minecraft;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.Optional;

import static cn.lambdalib2.util.MathUtils.lerpf;


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

    private static final Vec3d[] dirs = new Vec3d[] {
            null,
            new Vec3d(0, 0, -1),
            new Vec3d(0, 0, 1),
            new Vec3d(1, 0, 0),
            new Vec3d(-1, 0, 0)
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
                Minecraft mc = Minecraft.getMinecraft();
                GameSettings settings = mc.gameSettings;
                final int[] keys = new int[] {
                    -1,
                    settings.keyBindLeft.getKeyCode(),
                    settings.keyBindRight.getKeyCode(),
                    settings.keyBindForward.getKeyCode(),
                    settings.keyBindBack.getKeyCode()
                };
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
                        Vec3d dest = getDest(performingKey);
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
                Vec3d dest = getDest(keyid);
                if(player.isRiding())
                    player.dismountRidingEntity();
                player.setPositionAndUpdate(dest.x, dest.y, dest.z);
                player.fallDistance = 0.0f;

                ctx.addSkillExp(.002f);
                TPSkillHelper.incrTPCount(player);

                sendToClient(MSG_PERFORM);
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.CLIENT)
        @SideOnly(Side.CLIENT)
        void clientPerform() {
            ACSounds.playClient(player, "tp.tp_flashing", SoundCategory.AMBIENT, 1.0f);
            if (isLocal()) {
                if (cancellor != null) {
                    cancellor.setDead();
                    cancellor = null;
                }
                cancellor = new GravityCancellor(player, 40);
                MinecraftForge.EVENT_BUS.register(cancellor);
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        @SideOnly(Side.CLIENT)
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
            Vec3d dest = getDest(performingKey);
            marking.setPosition(dest.x, dest.y, dest.z);

            world().spawnEntity(marking);
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

        private Vec3d getDest(int keyid) {
            Preconditions.checkState(keyid != -1);

            double dist = lerpf(12, 18, exp);

            Vec3d dir = VecUtils.copy(dirs[keyid]);
            dir = VecUtils.rotateAroundZ(dir, player.rotationPitch * MathUtils.PI_F / 180)
                    .rotateYaw((-90 - player.rotationYaw) * MathUtils.PI_F / 180);


            Vec3d dst = VecUtils.add(player.getPositionEyes(1F),VecUtils.multiply(dir, dist));
            RayTraceResult mop = Raytrace.perform(player.getEntityWorld(), player.getPositionVector(),
                    dst, EntitySelectors.living().and(EntitySelectors.exclude(player)));

            double x, y, z;

            if (mop.typeOfHit != RayTraceResult.Type.MISS) {
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
                x = dst.x;
                y = dst.y;
                z = dst.z;
            }

            return new Vec3d(x, y, z);
        }

    }

}