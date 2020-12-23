package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ability.AbilityContext;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.render.misc.TPParticleFactory;
import cn.academy.datapart.AbilityData;
import cn.academy.entity.EntityMarker;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.*;
import cn.lambdalib2.util.EntitySelectors;
import net.minecraft.block.Block;
import net.minecraft.block.state.IBlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.ItemBlock;
import net.minecraft.item.ItemStack;
import net.minecraft.util.*;
import net.minecraft.util.math.AxisAlignedBB;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.Color;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
  * @author WeAthFolD, KSkun
  */
public class ShiftTeleport extends Skill
{
    public static ShiftTeleport instance = new ShiftTeleport();
    public ShiftTeleport()
    {
      super("shift_tp", 4);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void activate(ClientRuntime rt, int keyID)
    {
      activateSingleKey2(rt, keyID, p -> new STContext(p));
    }

    @Override
    public float getSkillExp(AbilityData data){
        if(AbilityContext.of(data.getEntity(), this).isEntirelyDisableBreakBlock())
          return 1f;
        else
          return data.getSkillExp(this);
    }

    public static class STContext extends Context
    {
        private static final String MSG_EXECUTE = "execute";
        public STContext(EntityPlayer p)
        {
            super(p, ShiftTeleport.instance);
        }

        private float exp = ctx.getSkillExp();
        private boolean attacked = false;

        @Listener(channel=MSG_MADEALIVE, side= Side.SERVER)
        private void s_madeAlive(){
            if(!isHandValid())
                terminate();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp(){
            if(isHandValid())
                sendToServer(MSG_EXECUTE);
            else
                terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort(){
            sendToSelf(MSG_EXECUTE, false);
            terminate();
        }


        @Listener(channel=MSG_EXECUTE, side=Side.SERVER)
        private void s_execute(){
            ItemStack stack = player.getHeldItemMainhand();
            Block block = Block.getBlockFromItem(stack.getItem());
            attacked = (stack.getItem() != Items.AIR && (stack.getItem() instanceof ItemBlock ) && block != Blocks.AIR);
            if(!attacked)
            {
                terminate();
                return ;
            }
            sendToClient(MSG_EXECUTE, attacked);

            ItemBlock item = (ItemBlock) stack.getItem();
            RayTraceResult position = getTracePosition();
            if(ctx.consume(getOverload(exp), getConsumption(exp)))
            {
                if(item.getBlock().canPlaceBlockAt(player.world, position.getBlockPos().add(position.sideHit.getDirectionVec()))
                        && ctx.canBreakBlock(player.world, position.getBlockPos())
                )//Can place
                {
                    IBlockState state = block.getStateForPlacement(world(), position.getBlockPos()
                            , position.sideHit, (float)position.hitVec.x, (float)position.hitVec.y,
                            (float)position.hitVec.z, stack.getItemDamage(), player, EnumHand.MAIN_HAND);
                    item.placeBlockAt(stack, player, player.world, position.getBlockPos().add(position.sideHit.getDirectionVec())
                            , position.sideHit, (float)position.hitVec.x, (float)position.hitVec.y, (float)position.hitVec.z,
                            state);

                }
                else//Drop as item
                {
                    ItemStack drop = stack.copy();
                    drop.setCount(1);
                    player.world.spawnEntity(new EntityItem(player.world, position.hitVec.x, position.hitVec.y, position.hitVec.z, drop));
                }

                if(!player.capabilities.isCreativeMode) {
                    if (stack.getCount() <= 1) {
                        player.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY);
                    } else {
                        stack.setCount(stack.getCount() - 1);
                    }
                }

                List<Entity> list = getTargetsInLine();
                for(Entity target :list) {
                    TPSkillHelper.attack(ctx, target, getDamage(exp));
                }
                player.world.playSound(player, player.getPosition(),
                        new SoundEvent(new ResourceLocation("academy:tp.tp_shift")),
                        SoundCategory.AMBIENT, 0.5f, 1f);
                ctx.addSkillExp(getExpIncr(list.size()));
                ctx.setCooldown((int)lerpf(100, 60, exp));
            }
            terminate();
        }

        private boolean isHandValid()
        {
            ItemStack stack  = player.getHeldItemMainhand();
            return stack.getItem() != Items.AIR && (stack.getItem() instanceof ItemBlock) &&
                    Block.getBlockFromItem(stack.getItem()) != Blocks.AIR;
        }

        private float getExpIncr(int attackEntities)
        {
            return (1 + attackEntities) * 0.002f;
        }

        private float getDamage(float exp)
        {
            return lerpf(15, 35, exp);
        }

        private float getRange(float exp)
        {
          return lerpf(25, 35, exp);
        }

        private float getConsumption(float exp)
        {
          return lerpf(260, 320, exp);
        }

        private float getOverload(float exp)
        {
            return lerpf(40, 30, exp);
        }

      // TODO: Some boilerplate... Clean this up in case you aren't busy
        int[] getTraceDest()
        {
            double range = getRange(exp);
            RayTraceResult result = Raytrace.traceLiving(player, range, EntitySelectors.nothing());
            if(result != null) {
                EnumFacing dir =result.sideHit;
                return new int[]{result.getBlockPos().getX() + dir.getXOffset(),
                        result.getBlockPos().getY() + dir.getYOffset(),
                        result.getBlockPos().getZ() + dir.getZOffset()};
            }
            Vec3d mo = VecUtils.lookingPos(player, range);
            return new int[]{(int) mo.x, (int) mo.y, (int) mo.z};
        }

        RayTraceResult getTracePosition(){
            double range = getRange(exp);
            RayTraceResult result = Raytrace.traceLiving(player, range, EntitySelectors.nothing());
            if(result != null) {
              EnumFacing dir = result.sideHit;
              result.hitVec = result.hitVec.add(dir.getXOffset(),
                      dir.getYOffset(), dir.getZOffset());
              return result;
            }
            Vec3d mo = VecUtils.lookingPos(player, range);
            return new RayTraceResult(RayTraceResult.Type.ENTITY, mo,
                    EnumFacing.DOWN, new BlockPos(mo.x, mo.y, mo.z));
        }

        List<Entity> getTargetsInLine()
        {
            int[] dest = getTraceDest();
            Vec3d v0= new Vec3d(player.posX, player.posY, player.posZ);
            Vec3d v1 = new Vec3d(dest[0] + .5, dest[1] + .5, dest[2] + .5);
            AxisAlignedBB area = WorldUtils.minimumBounds(v0, v1);
            Predicate<Entity> pred = EntitySelectors.living().and(EntitySelectors.exclude(player)).and(new Predicate<Entity>() {

              @Override
              public boolean test(Entity entity){
                      double hw = entity.width / 2;
                      return VecUtils.checkLineBox(new Vec3d(entity.posX - hw, entity.posY, entity.posZ - hw),
                              new Vec3d(entity.posX + hw, entity.posY + entity.height, entity.posZ + hw), v0, v1) != null;
              }

            });
            return WorldUtils.getEntities(player.world, area, pred);
        }
    }


    @SideOnly(Side.CLIENT)
    @RegClientContext(STContext.class)
    public static class STContextC extends ClientContext
    {
        private Color CRL_BLOCK_MARKER = new Color(139, 139, 139, 180);
        private Color CRL_ENTITY_MARKER = new Color(235, 81, 81, 180);

        private EntityMarker blockMarker = null;
        private List<EntityMarker> targetMarkers = null;
        private int effTicker = 0;
        private final STContext par;

        public STContextC(STContext par)
        {
            super(par);
            this.par=par;
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_start(){
            targetMarkers = new ArrayList<>();
            if(isLocal()) {
                blockMarker = new EntityMarker(player.world);
                blockMarker.ignoreDepth = true;
                blockMarker.height = 1.2f;
                blockMarker.width = 1.2f;
                blockMarker.color = CRL_BLOCK_MARKER;
                blockMarker.setPosition(player.posX, player.posY, player.posZ);
                player.world.spawnEntity(blockMarker);
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_tick(){
            if(isLocal()) {
                effTicker += 1;
                if(effTicker== 3) {
                    effTicker = 0;

                    for(Entity em : targetMarkers) {
                        em.setDead();
                    }
                    targetMarkers.clear();
                    List<Entity> targetsInLine = par.getTargetsInLine();
                    for(Entity e : targetsInLine) {
                        EntityMarker em = new EntityMarker(e);
                        em.color = CRL_ENTITY_MARKER;
                        em.ignoreDepth = true;
                        player.world.spawnEntity(em);
                        targetMarkers.add(em);
                    }
                }
                int[] dest = par.getTraceDest();
                blockMarker.setPosition(dest[0] + 0.5, dest[1], dest[2] + 0.5);
            }
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void l_terminated(){
            if(isLocal()) {
                for(Entity em : targetMarkers)
                    em.setDead();
                if(blockMarker != null)
                    blockMarker.setDead();
            }
        }

      @Listener(channel=STContext.MSG_EXECUTE, side=Side.CLIENT)
      private void c_end(boolean attacked){
          if(isLocal()) {
              for(Entity em : targetMarkers) em.setDead();
              blockMarker.setDead();
          }

          if(attacked) {
              int[] dest = par.getTraceDest();
              double dx = dest[0] + .5 - player.posX;
              double dy = dest[1] + .5 - (player.posY - 0.5);
              double dz = dest[2] + .5 - player.posZ;
              double dist = MathUtils.length(dx, dy, dz);
              double posX = player.posX;
              double posY = player.posY - 0.5;
              double posZ = player.posZ;
              Vec3d dv = new Vec3d(dx, dy, dz).normalize();
              double move = 1;
              double x = move;
              while(x <= dist) {
                {
                  posX += dv.x * move;
                  posY += dv.y * move;
                  posZ += dv.z * move;
                  player.world.spawnEntity(TPParticleFactory.instance.next(player.world, new Vec3d(posX, posY, posZ),
                          new Vec3d(RandUtils.ranged(-.05, .05), RandUtils.ranged(-.02, .05),
                                  RandUtils.ranged(-.05, .05))));
                }
                move = RandUtils.ranged(0.6, 1);
                x += move;
              }
          }
      }
    }
}
