/**
* Copyright (c) Lambda Innovation, 2013-2016
* This file is part of the AcademyCraft mod.
* https://github.com/LambdaInnovation/AcademyCraft
* Licensed under GPLv3, see project root for more information.
*/
package cn.academy.vanilla.teleporter.client;

import cn.academy.vanilla.teleporter.util.TPSkillHelper.TPCritHitEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
public class CriticalHitEffect {

    private static CriticalHitEffect instance = new CriticalHitEffect();

    private CriticalHitEffect() {
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void onTPCritHit(TPCritHitEvent event) {
        World world = event.player.worldObj;
        Entity t = event.target;
        if (world.isRemote) {
            int count = RandUtils.rangei(5, 8);
            while (count-- > 0) {
                double angle = RandUtils.ranged(0, Math.PI * 2);
                double r = RandUtils.ranged(t.width * .5, t.width * .7);
                double h = RandUtils.ranged(0, 1) * event.target.height;

                world.spawnEntityInWorld(FormulaParticleFactory.instance.next(world,
                        VecUtils.vec(t.posX + r * Math.sin(angle), t.posY + h, t.posZ + r * Math.cos(angle)),
                        VecUtils.multiply(VecUtils.random(), 0.03)));
            }
        }
    }

}
