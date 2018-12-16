package cn.academy.ability.vanilla.teleporter.client;

import cn.academy.client.render.particle.FormulaParticleFactory;
import cn.academy.ability.vanilla.teleporter.util.TPSkillHelper.TPCritHitEvent;
import cn.lambdalib2.registry.mc.RegEventHandler;
import cn.lambdalib2.util.RandUtils;
import cn.lambdalib2.util.VecUtils;
import net.minecraft.entity.Entity;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
public class CriticalHitEffect {
    private CriticalHitEffect() {}

    @RegEventHandler
    private static CriticalHitEffect instance = new CriticalHitEffect();

    @SubscribeEvent
    public void onTPCritHit(TPCritHitEvent event) {
        World world = event.player.getEntityWorld();
        Entity t = event.target;
        if (world.isRemote) {
            int count = RandUtils.rangei(5, 8);
            while (count-- > 0) {
                double angle = RandUtils.ranged(0, Math.PI * 2);
                double r = RandUtils.ranged(t.width * .5, t.width * .7);
                double h = RandUtils.ranged(0, 1) * event.target.height;

                world.spawnEntity(FormulaParticleFactory.instance.next(world,
                        new Vec3d(t.posX + r * Math.sin(angle), t.posY + h, t.posZ + r * Math.cos(angle)),
                        VecUtils.multiply(VecUtils.random(), 0.03)));
            }
        }
    }

}