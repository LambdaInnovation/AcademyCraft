package cn.academy.ability.vanilla.meltdowner.skill;

import cn.academy.ability.AbilityContext;
import cn.academy.datapart.AbilityData;
import cn.academy.ability.vanilla.meltdowner.CatMeltdowner;
import cn.academy.client.render.particle.MdParticleFactory;
import cn.academy.ability.vanilla.meltdowner.passiveskill.RadiationIntensify$;
import cn.lambdalib2.registry.StateEventCallback;
import cn.lambdalib2.s11n.network.NetworkMessage;
import cn.lambdalib2.s11n.network.NetworkMessage.Listener;
import cn.lambdalib2.s11n.network.NetworkS11nType;
import cn.lambdalib2.s11n.network.TargetPoints;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@NetworkS11nType
public class MDDamageHelper {
    
    private static final String MARKID = "md_marktick", RATEID = "md_markrate";

    @StateEventCallback
    private static void init(FMLInitializationEvent event) {
        MinecraftForge.EVENT_BUS.register(new Events());
    }
    
    static void attack(AbilityContext ctx, Entity target, float dmg) {
        EntityPlayer player = ctx.player;

        ctx.attack(target, dmg);
        AbilityData aData = AbilityData.get(player);
        if(aData.isSkillLearned(CatMeltdowner.radIntensify)) {
            int marktick = Math.max(60, getMarkTick(player));

            setMarkTick(target, marktick);
            setMarkRate(target, RadiationIntensify$.MODULE$.getRate(aData));
            NetworkMessage.sendToAllAround(
                    TargetPoints.convert(player, 20),
                    NetworkMessage.staticCaller(MDDamageHelper.class),
                    "sync", player, marktick
            );
        }
    }
    
    private static int getMarkTick(Entity player) {
        if(player.getEntityData().hasKey(MARKID))
            return player.getEntityData().getInteger(MARKID);
        else
            return 0;
    }

    private static float getMarkRate(Entity entity) {
        if(entity.getEntityData().hasKey(RATEID))
            return entity.getEntityData().getFloat(RATEID);
        else
            return 0;
    }

    private static void setMarkRate(Entity entity, float amt) {
        entity.getEntityData().setFloat(RATEID, amt);
    }

    @Listener(channel="sync", side= Side.CLIENT)
    private static void setMarkTick(Entity player, int ticks) {
        player.getEntityData().setInteger(MARKID, ticks);
    }
    
    public static class Events {
        
        @SubscribeEvent
        public void onLivingUpdate(LivingUpdateEvent event) {
            int tick = getMarkTick(event.getEntity());
            if(tick > 0)
                setMarkTick(event.getEntity(), tick - 1);
        }
        
        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onUpdateClient(LivingUpdateEvent event) {
            Entity e = event.getEntity();
            if(e.getEntityWorld().isRemote) {
                if(getMarkTick(e) > 0) {
                    int times = RandUtils.rangei(0, 3);
                    while(times --> 0) {
                        double r = RandUtils.ranged(.6, .7) * e.width;
                        double theta = RandUtils.nextDouble() * 2 * Math.PI;
                        double h = RandUtils.ranged(0, e.height);
                        
                        Vec3d pos = VecUtils.add(new Vec3d(e.posX, e.posY, e.posZ),
                            new Vec3d(r * Math.sin(theta), h, r * Math.cos(theta)));
                        Vec3d vel = VecUtils.multiply(VecUtils.random(), 0.02);
                        e.getEntityWorld().spawnEntity(MdParticleFactory.INSTANCE.next(e.getEntityWorld(), pos, vel));
                    }
                }
            }
        }
        
        @SubscribeEvent
        public void onLivingAttack(LivingHurtEvent event) {
            if(getMarkTick(event.getEntityLiving()) > 0) {
                event.setAmount(event.getAmount() * getMarkRate(event.getEntityLiving()));
            }
        }
        
    }
}