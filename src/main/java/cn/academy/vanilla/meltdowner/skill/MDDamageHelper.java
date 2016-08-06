/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.meltdowner.skill;

import cn.academy.ability.api.AbilityContext;
import cn.academy.ability.api.data.AbilityData;
import cn.academy.vanilla.meltdowner.CatMeltdowner;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegInitCallback;
import cn.lambdalib.s11n.network.TargetPoints;
import cn.lambdalib.s11n.network.NetworkMessage;
import cn.lambdalib.s11n.network.NetworkMessage.Listener;
import cn.lambdalib.s11n.network.NetworkS11n.NetworkS11nType;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.entity.living.LivingEvent.LivingUpdateEvent;
import net.minecraftforge.event.entity.living.LivingHurtEvent;

/**
 * @author WeAthFolD
 */
@Registrant
@NetworkS11nType
public class MDDamageHelper {
    
    private static final String MARKID = "md_marktick", RATEID = "md_markrate";

    @RegInitCallback
    private static void init() {
        MinecraftForge.EVENT_BUS.register(new Events());
    }
    
    static void attack(AbilityContext ctx, Entity target, float dmg) {
        EntityPlayer player = ctx.player;

        ctx.attack(target, dmg);
        AbilityData aData = AbilityData.get(player);
        if(aData.isSkillLearned(CatMeltdowner.radIntensify)) {
            int marktick = Math.max(60, getMarkTick(player));

            setMarkTick(target, marktick);
            setMarkRate(target, RadiationIntensify.instance.getRate(aData));
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

    @Listener(channel="sync", side=Side.CLIENT)
    private static void setMarkTick(Entity player, int ticks) {
        player.getEntityData().setInteger(MARKID, ticks);
    }
    
    public static class Events {
        
        @SubscribeEvent
        public void onLivingUpdate(LivingUpdateEvent event) {
            int tick = getMarkTick(event.entity);
            if(tick > 0)
                setMarkTick(event.entity, tick - 1);
        }
        
        @SideOnly(Side.CLIENT)
        @SubscribeEvent
        public void onUpdateClient(LivingUpdateEvent event) {
            Entity e = event.entity;
            if(e.worldObj.isRemote) {
                if(getMarkTick(e) > 0) {
                    int times = RandUtils.rangei(0, 3);
                    while(times --> 0) {
                        double r = RandUtils.ranged(.6, .7) * e.width;
                        double theta = RandUtils.nextDouble() * 2 * Math.PI;
                        double h = RandUtils.ranged(0, e.height);
                        
                        Vec3 pos = VecUtils.add(VecUtils.vec(e.posX, e.posY, e.posZ), 
                            VecUtils.vec(r * Math.sin(theta), h, r * Math.cos(theta)));
                        Vec3 vel = VecUtils.multiply(VecUtils.random(), 0.02);
                        e.worldObj.spawnEntityInWorld(MdParticleFactory.INSTANCE.next(e.worldObj, pos, vel));
                    }
                }
            }
        }
        
        @SubscribeEvent
        public void onLivingAttack(LivingHurtEvent event) {
            if(getMarkTick(event.entityLiving) > 0) {
                event.ammount *= getMarkRate(event.entityLiving);
            }
        }
        
    }
}
