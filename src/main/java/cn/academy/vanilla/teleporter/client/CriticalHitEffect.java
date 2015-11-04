/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * This project is open-source, and it is distributed under
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * 本项目是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.vanilla.teleporter.client;

import cn.academy.vanilla.teleporter.util.TPAttackHelper.TPCritHitEvent;
import cn.lambdalib.annoreg.core.Registrant;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;
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
		if(world.isRemote) {
			int count = RandUtils.rangei(5, 8);
			while(count --> 0) {
				double angle = RandUtils.ranged(0, Math.PI * 2);
				double r = RandUtils.ranged(t.width * .5, t.width * .7);
				double h = RandUtils.ranged(0, 1) * event.target.height;
				
				world.spawnEntityInWorld(FormulaParticleFactory.instance.next(
					world,
					VecUtils.vec(
						t.posX + r * Math.sin(angle), 
						t.posY + h, 
						t.posZ + r * Math.cos(angle)),
					VecUtils.multiply(VecUtils.random(), 0.03)
				));
			}
		}
	}

}
