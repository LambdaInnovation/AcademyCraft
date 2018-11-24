package cn.academy.ability.vanilla.teleporter.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientContext;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.RegClientContext;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper;
import cn.academy.client.render.util.ACRenderingHelper;
import cn.academy.client.sound.ACSounds;
import cn.academy.entity.EntityBloodSplash;
import cn.academy.entity.EntityMarker;
import cn.lambdalib2.s11n.SerializeIncluded;
import cn.lambdalib2.s11n.SerializeNullable;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.util.EntitySelectors;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.Raytrace;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.util.Color;

import static cn.lambdalib2.util.MathUtils.lerpf;

/**
 * @author WeAthFolD, KSkun
 */
public class FleshRipping extends Skill
{
    public static final FleshRipping instance = new FleshRipping();
    public FleshRipping()
    {
        super("flesh_ripping", 3);
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void activate(ClientRuntime rt , int keyID){
        activateSingleKey2(rt, keyID, FRContext::new);
    }

    static final String MSG_ABORT = "abort";
    static final String MSG_END = "end";
    static final String MSG_EFFECT_END = "effect_end";

    public static class FRContext extends Context{

        private float exp = ctx.getSkillExp();

        private AttackTarget target = null;

        public FRContext(EntityPlayer p)
        {
            super(p, instance);
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER)
        private void s_tick(){
            if(!ctx.canConsumeCP(getConsumption()))
                terminate();
            target = getAttackTarget();
        }

        @Listener(channel=MSG_END, side=Side.SERVER)
        private void s_end(){
            sendToClient(MSG_EFFECT_END, target);
            if(target.target == null)
                sendToSelf(MSG_ABORT);
            else {
                ctx.consumeWithForce(getOverload(), getConsumption());
                TPSkillHelper.attackIgnoreArmor(ctx, target.target, getDamage());
                if(RandUtils.ranged(0, 1) < getDisgustProb())
                    player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("nausea"), 100));
                ctx.setCooldown((int)lerpf(90, 40, exp));
                ctx.addSkillExp(.005f);
            }
            terminate();
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onKeyUp() {
            sendToServer(MSG_END);
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT)
        private void l_onKeyAbort(){
            sendToSelf(MSG_EFFECT_END, new AttackTarget());
            terminate();
        }

        @Listener(channel=MSG_ABORT, side=Side.SERVER)
        private void s_abort() {
            target = null;
        }

        private float getDamage(){
            return lerpf(5, 12, exp);
        }

        @Override
        public double getRange(){
            return lerpf(6, 14, exp);
        }

        private float getDisgustProb(){
            return .05f;
        }

        private float getConsumption() {
            return lerpf(130, 270, exp);
        }

        private float getOverload() {
            return lerpf(60, 50, exp);
        }

        public AttackTarget getAttackTarget(){
            double range = getRange();
            RayTraceResult trace  = Raytrace.traceLiving(player, range, EntitySelectors.living());
            Entity target  = null;
            Vec3d dest  = null;
            if (trace != null) {
                target = trace.entityHit;
                dest = trace.hitVec;
            }
            else dest = VecUtils.add(player.getPositionVector(), VecUtils.multiply(player.getLookVec(), range));
            return new AttackTarget(dest, target, player);
        }
    }

    @SideOnly(Side.CLIENT)
    @RegClientContext(FRContext.class)
    static class FRContextC extends ClientContext
    {
        FRContext par;
        public FRContextC(FRContext par)
        {
            super(par);
            this.par = par;
        }


        private EntityMarker marker = null;

        private Color DISABLED_COLOR  = new Color(74, 74, 74, 160);
        private Color THREATENING_COLOR = new Color(185, 25, 25, 180);

        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void l_terminated(){
            if(marker != null)
                marker.setDead();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void l_startEffect(){
        if(isLocal()) {
            marker = new EntityMarker(player.world);
            marker.setPosition(player.posX, player.posY, player.posZ);
            marker.color = DISABLED_COLOR;
            player.world.spawnEntity(marker);
        }
      }

        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void l_updateEffect(){
        if(isLocal()) {
            AttackTarget at  = par.getAttackTarget();
            marker.setPosition(at.dest.x, at.dest.y, at.dest.z);
            if (at.target == null) {
                marker.color = DISABLED_COLOR;
                marker.width = 1.0f;
                marker.height = 1.0f;
            }
            else {
                marker.color = THREATENING_COLOR;
                marker.width = at.target.width * 1.2f;
                marker.height = at.target.height * 1.2f;
            }
        }
      }

        @Listener(channel=MSG_EFFECT_END, side=Side.CLIENT)
        private void c_endEffect(AttackTarget target){
            if(isLocal())
                marker.setDead();

            if(target != null && target.target != null) {
                ACSounds.playClient(player, "tp.guts", SoundCategory.AMBIENT, 0.6f);
                Entity e  = target.target;
                for(int i= 0,max = RandUtils.rangei(4, 6);i<max;i++) {
                    double y = e.posY + RandUtils.ranged(0, 1) * e.height;
                    if(e instanceof EntityPlayer )
                        y += ACRenderingHelper.getHeightFix((EntityPlayer) e);
                    double theta = RandUtils.ranged(0, Math.PI * 2);
                    double r = 0.5 * RandUtils.ranged(0.8 * e.width, e.width);
                    EntityBloodSplash splash = new EntityBloodSplash(player.world);
                    splash.setPosition(e.posX + r * Math.sin(theta), y, e.posZ + r * Math.cos(theta));
                    player.world.spawnEntity(splash);
                }
            }
        }

    }


    @NetworkS11nType
    static class AttackTarget{
        @SerializeIncluded
        Vec3d dest = null;
        @SerializeIncluded
        @SerializeNullable
        Entity target = null;
        @SerializeIncluded
        EntityPlayer player = null;

        public AttackTarget(){ }

        public AttackTarget(Vec3d _dest , Entity _target, EntityPlayer _player) {
            dest = _dest;
            target = _target;
            player = _player;
        }

        public AttackTarget(NBTTagCompound tag, EntityPlayer _player) {
            this(new Vec3d(tag.getFloat("x"), tag.getFloat("y"), tag.getFloat("z")),
                _player.world.getEntityByID(tag.getInteger("i")), _player);
        }

        private NBTTagCompound toNBT(){
            NBTTagCompound ret = new NBTTagCompound();
            ret.setFloat("x", (float) dest.x);
            ret.setFloat("y", (float) dest.y);
            ret.setFloat("z", (float) dest.z);
            ret.setInteger("i", (target == null)? 0 : target.getEntityId());
            return ret;
        }

    }
}
