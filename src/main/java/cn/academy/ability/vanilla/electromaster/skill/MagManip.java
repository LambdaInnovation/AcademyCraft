package cn.academy.ability.vanilla.electromaster.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.electromaster.CatElectromaster;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.MagManipEntityBlock;
import cn.lambdalib2.multiblock.BlockMulti;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.*;
import net.minecraft.block.Block;
import net.minecraft.block.BlockDoor;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import static cn.lambdalib2.util.VecUtils.*;


/**
 * Author: WeAthFold
 * Edited by Paindar at 2018.11.24:
 *   Rewrite it with Java.
 */
public class MagManip extends Skill
{
    public static MagManip INSTANCE = new MagManip();
    public MagManip()
    {
        super("mag_manip", 2);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void activate(ClientRuntime rt, int keyid)
    {
        activateSingleKey2(rt, keyid, MagManipContext::new);
    }

    public static boolean accepts(EntityPlayer player, Block block)
    {
        if(block instanceof BlockMulti)
            return false;
        else if (block instanceof BlockDoor)
            return false;
        else
            return CatElectromaster.isMetalBlock(block);
    }

    public static class MagManipContext extends Context
    {
        public MagManipContext(EntityPlayer _player)
        {
            super(_player, MagManip.INSTANCE);
        }

        enum State
        {
            StateMoving, StateCharging
        }

        final static String MSG_PERFORM = "perform";

        final static String MSG_SYNC_ENTITY_REQ = "req_sync_ent";
        final static String MSG_SYNC_ENTITY_RSP = "rsp_sync_ent";


        private float consumption = MathUtils.lerpf(140, 270, ctx.getSkillExp());
        private float overload = MathUtils.lerpf(35, 20, ctx.getSkillExp());
        private int cooldown = (int) MathUtils.lerpf(60, 40, ctx.getSkillExp());
        private float damage = MathUtils.lerpf(8, 15, ctx.getSkillExp());
        private float speed = MathUtils.lerpf(0.5f, 1.0f, ctx.getSkillExp());

        public State state = State.StateMoving;
        MagManipEntityBlock entity = null;
        boolean performed = false;

        int _entityId = -1;

        @Listener(channel = MSG_KEYUP, side = Side.CLIENT)
        public void l_keyUp()
        {
            sendToServer(MSG_PERFORM);
        }

        @Listener(channel = MSG_KEYABORT, side = Side.CLIENT)
        public void l_keyAbort()
        {
            terminate();
        }

        @Listener(channel = MSG_MADEALIVE, side = Side.SERVER)
        public void s_makeAlive()
        {
            ItemStack stack = player.getHeldItemMainhand();
            Block heldBlock = Block.getBlockFromItem(stack.getItem());
            if( heldBlock != Blocks.AIR && MagManip.accepts(player, heldBlock))
            {
                if (!player.capabilities.isCreativeMode)
                {
                    stack.setCount(stack.getCount() - 1);
                }
                entity = new MagManipEntityBlock(player, 10);
                entity.setBlock(heldBlock);
                Vec3d hPos = entityHeadPos(player);
                entity.setPosition(hPos.x, hPos.y, hPos.z);
                world().spawnEntity(entity);
                updateMoveTo();
            }
            else
            {
                RayTraceResult trace = Raytrace.traceLiving(player, 10, EntitySelectors.nothing(), (world, x, y, z, block1) -> accepts(player, block1));
                if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK)
                {
                    BlockPos pos = trace.getBlockPos();
                    IBlockState ibs = world().getBlockState(pos);
                    Block block = ibs.getBlock();
                    int x = pos.getX(), y = pos.getY(), z = pos.getZ();
                    if (block instanceof BlockDoor) // FIXME: Dirty hack
                    {
                        BlockPos bPos = new BlockPos(x, y - 1, z);
                        if (world().getBlockState(bPos).getBlock() == Blocks.IRON_DOOR)
                            world().setBlockToAir(bPos);
                        else
                            world().setBlockToAir(pos);
                    }
                    else
                        world().setBlockToAir(pos);
                    entity = new MagManipEntityBlock(player, 10);
                    entity.setBlock(block);
                    entity.setPosition((int) (x + 0.5), (int) (y + 0.5), (int) (z + 0.5));
                    world().spawnEntity(entity);
                    updateMoveTo();
                }
                else
                    terminate();
            }
        }

        @Listener(channel = MSG_MADEALIVE, side = Side.CLIENT)
        private void c_madeAlive() {
            // FIXME: If we currently sync entity in madeAlive(), the message will be ignored
            //  because at that time client is not yet alive.
            //  To make that work we need some kind of message queue, to save msgs from server and flushes after the context becomes alive.
            sendToServer(MSG_SYNC_ENTITY_REQ);
        }

        @Listener(channel = MSG_SYNC_ENTITY_REQ, side = Side.SERVER)
        private void s_onSyncEntityReq() {
            sendToClient(MSG_SYNC_ENTITY_RSP, entity.getEntityId());
        }

        @Listener(channel=MSG_SYNC_ENTITY_RSP, side=Side.CLIENT)
        private void c_syncEntity(int id) {
            _entityId = id;
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick() {
            updateMoveTo();
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_tick() {
            if (entity == null) {
                Entity e = world().getEntityByID(_entityId);
                if (e instanceof MagManipEntityBlock) {
                    entity = ((MagManipEntityBlock) e);
                    _entityId = 0;
                }
            }

            if (entity != null) {
                updateMoveTo();
            }
        }

        @Listener(channel=MSG_PERFORM, side=Side.SERVER)
        public void s_perform()
        {
            performed=true;
            entity.actionType = MagManipEntityBlock.ActNothing;
            entity.setPlaceFromServer(true);

            double distsq = player.getDistanceSq(entity);
            if(distsq < 25 && ctx.consume(overload, consumption))
            {
                Vec3d pos = Raytrace.getLookingPos(player, 20).getLeft();
                Vec3d delta = subtract(pos, entity.getPositionVector());
                Vec3d velocity = multiply(delta.normalize(), speed);
                setMotion(entity, velocity);

                ctx.setCooldown(cooldown);
                ctx.addSkillExp(0.005F);

                sendToClient(MSG_PERFORM, entity, velocity);
            }

            terminate();
        }

        @Listener(channel = MSG_TERMINATED, side=Side.SERVER)
        public void s_terminate()
        {
            if(!performed && entity !=null)
            {
                entity.actionType = MagManipEntityBlock.ActNothing;
                entity.setPlaceFromServer(true);
            }
        }

        private void updateMoveTo()
        {
            Vec3d origin = subtract(entityHeadPos(player),  new Vec3d(0, 0.1, 0));
            Vec3d look = player.getLookVec();

            Vec3d look2 = new Vec3d(look.x , 0, look.z)
                    .normalize().rotatePitch((float) (Math.PI/2));
            Vec3d pos = add(origin, multiply(player.getLookVec(), 2.0));
            entity.setMoveTo(pos.x, pos.y, pos.z);
        }

        /*
        private def wrap(args: Any*) = args.map(_.asInstanceOf[AnyRef])

  override def getConsumptionHint: Float = consumption
         */
    }

    @SideOnly(Side.CLIENT)
    @RegClientContext(MagManipContext.class)
    public static class MagManipContextC extends ClientContext
    {
        FollowEntitySound loopSound;

        public MagManipContextC(MagManipContext par)
        {
            super(par);
            loopSound = new FollowEntitySound(par.player, "em.lf_loop", SoundCategory.AMBIENT).setLoop();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        public void c_makeAlive()
        {
            ACSounds.playClient(loopSound);
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        public void c_terminate()
        {
            loopSound.stop();
        }

        @Listener(channel=MagManipContext.MSG_PERFORM, side=Side.CLIENT)
        public void c_perform(MagManipEntityBlock entity, Vec3d velocity)
        {
            entity.actionType = MagManipEntityBlock.ActNothing;
            entity.placeWhenCollide = true;
            VecUtils.setMotion(entity, velocity);

            ((MagManipContext) this.parent).entity = null;

            ACSounds.playClient(player, "em.mag_manip",  SoundCategory.AMBIENT, 1.0f);
        }
    }
}
