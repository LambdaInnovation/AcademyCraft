package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ACItems;
import cn.academy.AcademyCraft;
import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.advancements.ACAdvancements;
import cn.academy.client.render.misc.TPParticleFactory;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityMarker;
import cn.lambdalib2.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.item.EntityItem;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import org.lwjgl.util.Color;

import static cn.lambdalib2.util.MathUtils.lerpf;

public class ThreateningTeleport extends Skill
{
    public static final ThreateningTeleport instance = new ThreateningTeleport();
    public ThreateningTeleport()
    {
        super("threatening_teleport", 1);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void activate(ClientRuntime rt, int keyID)
    {
        activateSingleKey2(rt, keyID, TTContext::new);
    }

    public static class TTContext extends Context
    {
        public static final String MSG_EXECUTE = "execute";
        private float exp = ctx.getSkillExp();
        private boolean attacked =false;

        public TTContext(EntityPlayer p)
        {
            super(p, ThreateningTeleport.instance);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp()
        {
            sendToServer(MSG_EXECUTE);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort()
        {
            terminate();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER)
        private void s_madeAlive()
        {
            if(player.getHeldItemMainhand().isEmpty()) terminate();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick()
        {
            if(player.getHeldItemMainhand().isEmpty())
                terminate();
        }

        @Listener(channel=MSG_EXECUTE, side=Side.SERVER)
        private void s_execute()
        {
            ItemStack curStack = player.getHeldItemMainhand();
            if(!curStack.isEmpty() && ctx.consume(getOverload(exp), getConsumption(exp)))
            {
                attacked = true;
                TraceResult result = calcDropPos();
                double dropProb = 1.0;
                boolean attacked_ = false;
                if(result.target != null) {
                    attacked_ = true;
                    TPSkillHelper.attackIgnoreArmor(ctx, result.target, getDamage(curStack));
                    dropProb = 0.3;
                    }
                if(!player.capabilities.isCreativeMode) {
                    curStack.shrink(1);
                }
                if(RandUtils.ranged(0, 1) < dropProb) {
                    ItemStack drop = curStack.copy();
                    drop.setCount(1);
                    player.world.spawnEntity(new EntityItem(player.world, result.x, result.y, result.z, drop));
                }
//                if(exp>0.5) ACAdvancements.trigger(player, ACAdvancements.ac_milestone.ID);
                ctx.addSkillExp(getExpIncr(attacked_));
                ctx.setCooldown((int)lerpf(30, 15, exp));
            }
            sendToClient(MSG_EXECUTE, attacked);
            terminate();
        }

        private float getConsumption(float exp)
        {
            return lerpf(35, 100, exp);
        }

        private float getRange(float exp)
        {
            return lerpf(8, 15, exp);
        }

        private float getExpIncr(boolean attacked)
        {
            return (attacked?1:0.2f)*0.003f;
        }

        private float getDamage(ItemStack stack)
        {
            float dmg = lerpf(3, 6, ctx.getSkillExp());
            if(stack.getItem()== ACItems.needle) dmg *= 1.5f;
            return dmg;
        }

        private float getOverload(float exp)
        {
            return lerpf(18, 10, exp);
        }

        public TraceResult calcDropPos()
        {
            double range = getRange(exp);
            RayTraceResult pos = Raytrace.traceLiving(player, range, EntitySelectors.living(), BlockSelectors.filEverything);
            if(pos.typeOfHit == RayTraceResult.Type.MISS)
                pos = Raytrace.traceLiving(player, range, EntitySelectors.nothing());
            TraceResult ret = new TraceResult();
            if(pos.typeOfHit == RayTraceResult.Type.MISS) {
                Vec3d mo = VecUtils.add(player.getPositionEyes(1.0F), VecUtils.multiply(player.getLookVec(), range));
                ret.setPos(mo.x, mo.y, mo.z);
            }
            else if(pos.typeOfHit == RayTraceResult.Type.BLOCK) ret.setPos(pos.hitVec.x, pos.hitVec.y, pos.hitVec.z);
            else {
                Entity ent = pos.entityHit;
                ret.setPos(ent.posX, ent.posY + ent.height, ent.posZ);
                ret.target = ent;
            }
            return ret;
        }
    }

    @SideOnly(Side.CLIENT)
    @RegClientContext(TTContext.class)
    public static class TTContextC extends ClientContext
    {
        private Color COLOR_NORMAL = new Color(0xba,0xba,0xba,0xba);
        private Color COLOR_THREATENING = new Color(0xba,0xb2,0x23,0x2a);
        private EntityMarker marker = null;
        private TTContext par;
        private static final String MSG_EXECUTE = TTContext.MSG_EXECUTE;

        public TTContextC(TTContext par)
        {
            super(par);
            this.par = par;
        }

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        public void l_terminated()
        {
            if(isLocal() && marker != null) marker.setDead();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        public void l_start(){
            if(isLocal()) {
                marker = new EntityMarker(player.world);
                player.world.spawnEntity(marker);
                marker.setPosition(player.posX, player.posY, player.posZ);
                marker.width = 0.5f;
                marker.height = 0.5f;
            }
        }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        public void l_tick()
        {
            if(isLocal()) {
                TraceResult res = par.calcDropPos();
                if(res.target != null) res.y -= res.target.height;
                marker.setPosition(res.x, res.y, res.z);
                marker.target = res.target;
                marker.color = (marker.target != null)? COLOR_THREATENING : COLOR_NORMAL;
            }
        }

        @Listener(channel=MSG_EXECUTE, side=Side.CLIENT)
        public void c_end(boolean attacked)
        {
            if(isLocal()) marker.setDead();
            if(attacked)
            {
                ACSounds.playClient(player, "tp.tp", SoundCategory.AMBIENT, 0.5f);
                TraceResult dropPos = par.calcDropPos();
                double dx = dropPos.x + .5 - player.posX;
                double dy = dropPos.y + .5 - (player.posY - 0.5);
                double dz = dropPos.z + .5 - player.posZ;
                double dist = MathUtils.length(dx, dy, dz);
                double posX = player.posX;
                double posY = player.posY - 0.5;
                double posZ = player.posZ;
                Vec3d lookingVec = new Vec3d(dx, dy, dz).normalize();

                double move = 1;
                double x = move;
                while(x <= dist)
                {
                    posX += move*lookingVec.x;
                    posY += move*lookingVec.y;
                    posZ += move*lookingVec.z;
                    player.world.spawnEntity(TPParticleFactory.instance.next(player.world, new Vec3d(posX, posY, posZ),
                            new Vec3d(RandUtils.ranged(-.02, .02), RandUtils.ranged(-.02, .05), RandUtils.ranged(-.02, .02))));
                    move = RandUtils.ranged(1, 2);
                    x += move;
                }
            }
        }
    }

    public static class TraceResult {
        public double x = .0;
        public double y = .0;
        public double z = .0;
        public Entity target = null;

        public void setPos(double _x, double _y, double _z)
        {
            x = _x;
            y = _y;
            z = _z;
        }

    }
}
