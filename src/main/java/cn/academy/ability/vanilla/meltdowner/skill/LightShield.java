package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.Skill;
import cn.academy.ability.context.ClientRuntime;
import cn.academy.ability.context.Context;
import cn.academy.ability.context.ContextManager;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.academy.client.sound.ACSounds;
import cn.academy.client.sound.FollowEntitySound;
import cn.academy.entity.EntityMdShield;
import cn.lambdalib2.particle.Particle;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.util.*;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.potion.Potion;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.DamageSource;
import net.minecraft.util.SoundCategory;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;

import static cn.lambdalib2.util.MathUtils.lerpf;
import static cn.lambdalib2.util.RandUtils.ranged;

public class LightShield extends Skill
{
    public static final LightShield INSTANCE = new LightShield();
    private static final int ACTION_INTERVAL = 18;


    public LightShield()
    {
        super("light_shield", 2);
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public void activate(ClientRuntime rt, int keyID)
    {
        activateSingleKey2(rt, keyID, LSContext::new);
    }

    @SubscribeEvent
    public void onPlayerAttacked(LivingHurtEvent event) {
        if(event.getEntityLiving() instanceof EntityPlayer)
        {
            Optional<LSContext> context = ContextManager.instance.find(LSContext.class);
            if(context.isPresent())
            {
                event.setAmount(context.get().handleAttacked(event.getSource(), event.getAmount()));
                if (event.getAmount() == 0) 
                    event.setCanceled(true);
            }
        }
    }
    
    public static class LSContext extends Context
    {
        private Predicate<Entity> basicSelector = EntitySelectors.everything();
        private int ticks = 0;
        private int lastAbsorb = -1;// The tick last the shield absorbed damage.
        private float exp = ctx.getSkillExp();
        
        private final float MAX_TIME = lerpf(120, 180, exp);
        private int getCooldown(int ct){return (int)lerpf(2*ct, ct, exp);}
        private float overloadKeep = 0f;
        
        public LSContext(EntityPlayer p)
        {
            super(p, LightShield.INSTANCE);
        }

        @Listener(channel=MSG_KEYUP, side=Side.CLIENT)
        private void l_onEnd()
        {
            terminate();
        }

        @Listener(channel=MSG_KEYABORT, side=Side.CLIENT) 
        private void l_onAbort()
        {
            terminate();
        }

        @Listener(channel=MSG_MADEALIVE, side=Side.SERVER) 
        private void s_madeAlive()
        {
            float overload = lerpf(110, 60, exp);
            ctx.consume(overload, 0);
            overloadKeep = ctx.cpData.getOverload();
        }

        @Listener(channel=MSG_TICK, side=Side.SERVER) 
        private void s_tick()
        {
        if(ctx.cpData.getOverload() < overloadKeep)
            ctx.cpData.setOverload(overloadKeep);
        ticks += 1;
        if(ticks > MAX_TIME)
            terminate();

        float cp = lerpf(9, 4, exp);
        if (!ctx.consume(0, cp))
            terminate();
        ctx.addSkillExp(1e-6f);

        // Find the entities that are 'colliding' with the shield.
        List<Entity> candidates = WorldUtils.getEntities(player, 3, basicSelector.and(t -> isEntityReachable(t)).and(EntitySelectors.exclude(player)));
        for (Entity e : candidates) {
            if (e.hurtResistantTime <= 0 && ctx.consume(getAbsorbOverload(), getAbsorbConsumption()))
            {
                MDDamageHelper.attack(ctx, e, getTouchDamage());
                ctx.addSkillExp(.001f);
            }
        }
    }

        @Listener(channel=MSG_TERMINATED, side=Side.SERVER) 
        private void s_onEnd(){ 

        player.addPotionEffect(new PotionEffect(Potion.getPotionFromResourceLocation("slowness"), 100, 1));
        ctx.setCooldown(getCooldown(ticks));
    }

        float handleAttacked(DamageSource src, float damage)
        {
            float result = damage;
            if (damage == 0 || lastAbsorb != -1 && ticks - lastAbsorb <= ACTION_INTERVAL)
                return damage;
            Entity entity  = src.getImmediateSource();
            boolean perform = false;
            if (entity != null)
            {
                if (isEntityReachable(entity))
                    perform = true;
            }
            else
                perform = true;

            if (perform)
            {
                lastAbsorb = ticks;
                if (ctx.consume(getAbsorbConsumption(), getAbsorbOverload()))
                {
                    float amt = getAbsorbDamage();
                    result -= Math.min(damage, amt);
                }
            }
            ctx.addSkillExp(.001f);
            return result;
        }

        private float getAbsorbDamage(){return lerpf(15, 50, exp);}

        private float getTouchDamage(){return lerpf(2, 6, exp);}

        private float getAbsorbOverload(){return lerpf(5, 3, exp);}

        private float getAbsorbConsumption(){return lerpf(50, 30, exp);}

        private boolean isEntityReachable(Entity e)
        {
            double dx = e.posX - player.posX;
            //dy = e.posY - player.posY,
            double dz = e.posZ - player.posZ;
            double yaw = -MathUtils.toDegrees(Math.atan2(dx, dz));
            return Math.abs(yaw - player.rotationYaw) % 360 < 60;
        }

        @SideOnly(Side.CLIENT)
        private EntityMdShield shield;
        @SideOnly(Side.CLIENT)
        private FollowEntitySound loopSound;

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_MADEALIVE, side=Side.CLIENT)
        private void c_spawn()
        {
            shield = new EntityMdShield(player);
            world().spawnEntity(shield);
            ACSounds.playClient(player, "md.shield_startup", SoundCategory.AMBIENT, 0.5f);
            loopSound = new FollowEntitySound(player, "md.shield_loop", SoundCategory.AMBIENT).setLoop();
            ACSounds.playClient(loopSound);
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_TICK, side=Side.CLIENT)
        private void c_update()
        {
            if (RandUtils.nextFloat() < 0.3f) {
                double s = 0.5;
                Vec3d mo = VecUtils.lookingPos(player, 1).add(
                        ranged(-s, s), ranged(-s, s), ranged(-s, s)
                );
                Particle p = MdParticleFactory.INSTANCE.next(world(), new Vec3d(mo.x, mo.y, mo.z),
                        new Vec3d(ranged(-.02, .02), ranged(-.01, .05), ranged(-.02, .02)));
                world().spawnEntity(p);
            }
        }

        @SideOnly(Side.CLIENT)
        @Listener(channel=MSG_TERMINATED, side=Side.CLIENT)
        private void c_end(){
            shield.setDead();
            loopSound.stop();
        }
    }
}
