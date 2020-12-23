package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ability.AbilityPipeline;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityTPMarking;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.InputEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.input.Mouse;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class PenetrateTeleport extends Skill
{
    public static final PenetrateTeleport instance = new PenetrateTeleport();
    public PenetrateTeleport()
    {
        super("penetrate_teleport", 2);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void activate(ClientRuntime rt , int keyID)
    {
        activateSingleKey2(rt, keyID, PTContext::new);
    }

    public static class PTContext extends Context
    {
        public static final String MSG_EXECUTE = "execute";

        public PTContext(EntityPlayer p)
        {
            super(p, instance);
        }

        private Dest dest = null;
        private float exp = ctx.getSkillExp();
        private float minDist = 0.5f;
        private float maxDist = getMaxDistance(ctx.getSkillExp());
        private float curDist = maxDist;

        @SideOnly(Side.CLIENT)
        private EntityTPMarking mark;
        private float mwSpd=1;

        @Listener(channel=MSG_EXECUTE, side=Side.SERVER)
        private void s_execute(float dist)
        {
            curDist = dist;
            dest = getDest();
            if(!dest.available)
            {
                terminate();
            }

            double x = dest.pos.x;
            double y = dest.pos.y;
            double z = dest.pos.z;
            double distance = player.getDistance(x, y, z);
            float overload = lerpf(80, 50, exp);
            ctx.consumeWithForce(overload, (float) (distance * getConsumption(exp)));
            float expincr = (float) (0.00014f * distance);
            ctx.addSkillExp(expincr);
            ctx.setCooldown((int) lerpf(50, 30, exp));
            TPSkillHelper.incrTPCount(player);
            if(player.isRiding())
                player.dismountRidingEntity();
            player.setPositionAndUpdate(x,y,z);
            player.fallDistance = 0;

            terminate();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp()
        {
            ACSounds.playClient(player, "tp.tp", SoundCategory.AMBIENT, .5f);
            sendToServer(MSG_EXECUTE,curDist);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort()
        {
            terminate();
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_spawnMark()
        {
            if(isLocal()) {
                mark = new EntityTPMarking(player);
                player.world.spawnEntity(mark);
                MinecraftForge.EVENT_BUS.register(this);
            }
        }

        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onPlayerUseWheel(InputEvent.MouseInputEvent inputEvent)
        {
            if(AbilityPipeline.canUseMouseWheel()) {
                float offset=(Mouse.getEventDWheel() / 120.0f) * mwSpd;
                updateDistance(offset);
            }
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_updateMark()
        {
            if(isLocal()) {
                Dest dest = getDest();
                mark.available = dest.available;
                mark.setPosition(dest.pos.x, dest.pos.y + player.eyeHeight, dest.pos.z);
            }
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_endEffect()
        {
            MinecraftForge.EVENT_BUS.unregister(this);
            if(mark != null) mark.setDead();
        }

        private boolean hasPlace(World world, double x, double y, double z)
        {
            int ix = (int) x;
            int iy = (int) y;
            int iz = (int) z;
            BlockPos pos1 = new BlockPos( ix,iy,iz), pos2 = new BlockPos(ix,iy+1,iz);
            IBlockState state1 = world.getBlockState(pos1), state2 = world.getBlockState(pos2);
            Block b1 = state1.getBlock(), b2 = state2.getBlock();
            return !b1.canCollideCheck(state1, false) && !b2.canCollideCheck(state2, false);
        }

        private float getConsumption(float exp)
        {
            return lerpf(14, 9, exp);
        }

        private float getMaxDistance(float exp)
        {
            return lerpf(10, 35, exp);
        }

        public Dest getDest()
        {
            World world = player.world;
            double dist = curDist;
            double cplim = ctx.cpData.getCP() / getConsumption(ctx.getSkillExp());
            dist = Math.min(dist, cplim);
            final double STEP = 0.8;
            int stage = 0;
            int counter = 0;
            double x = player.posX;
            double y = player.posY;
            double z = player.posZ;
            Vec3d dir = player.getLookVec().normalize();

            double totalStep = 0.0;
            while(totalStep <= dist) {
                boolean b = hasPlace(world, x,y, z);
                if(stage == 0) {
                    if(!b) stage = 1;
                } else if(stage == 1) {
                    if(b) stage = 2;
                } else {
                    counter += 1;
                    if(!b || (counter > 4)) {
                        break;
                    }
                }
                totalStep += STEP;
                x += STEP*dir.x;
                y += STEP*dir.y;
                z += STEP*dir.z;
            }
            return new Dest(new Vec3d(x,y,z), stage != 1);
        }

        public void updateDistance(float dist){
            //AcademyCraft.log.info("current distance = "+curDist.toFloat+" max distance = "+maxDist + " offset = "+dist)
            if(dist+curDist>=minDist && dist+curDist<=maxDist){
                curDist+=dist;
            }
        }
    }
    public static class Dest {
        public Vec3d pos = null;
        public boolean available = false;

        public Dest(Vec3d _pos, boolean _available)
        {
            pos = _pos;
            available = _available;
        }
    }
}
