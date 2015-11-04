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
package cn.academy.vanilla.meltdowner.entity;

import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.particle.Particle;
import cn.lambdalib.util.generic.RandUtils;
import cn.lambdalib.util.generic.VecUtils;
import cn.lambdalib.util.helper.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityMDRay extends EntityRayBase {
	
	@RegEntity.Render
	public static MDRayRender renderer;
	
	public EntityMDRay(EntityPlayer _player) {
		super(_player);
		
		Motion3D mo = new Motion3D(_player, true);
		Vec3 start = mo.getPosVec(), end = mo.move(25).getPosVec();
		this.setFromTo(start, end);
		this.blendInTime = 200;
		this.blendOutTime = 700;
		this.life = 50;
		this.length = 30.0;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		if(RandUtils.nextDouble() < 0.8) {
			Particle p = MdParticleFactory.INSTANCE.next(worldObj,
					new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
					VecUtils.vec(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
			worldObj.spawnEntityInWorld(p);
		}
	}
	
	public static class MDRayRender extends RendererRayComposite {

		public MDRayRender() {
		super("mdray");
			this.cylinderIn.width = 0.17;
			this.cylinderIn.color.setColor4i(216, 248, 216, 230);
			
			this.cylinderOut.width = 0.22;
			this.cylinderOut.color.setColor4i(106, 242, 106, 50);
			
			this.glow.width = 1.5;
			this.glow.color.a = 0.8;
		}
		
	}
}