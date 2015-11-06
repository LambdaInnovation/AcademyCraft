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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import cn.academy.core.client.ACRenderingHelper;
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

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityMineRayExpert extends EntityRayBase {
	
	@RegEntity.Render
	public static ExpertRayRenderer renderer;
	
	public EntityMineRayExpert(EntityPlayer _player) {
		super(_player);
		
		this.blendInTime = 200;
		this.blendOutTime = 400;
		this.life = 233333;
		this.length = 15.0;
	}
	
	@Override
	public void onUpdate() {
		super.onUpdate();
		EntityPlayer player = getPlayer();
		Vec3 end = new Motion3D(player, true).move(15).getPosVec();
		this.setFromTo(player.posX, player.posY + (ACRenderingHelper.isThePlayer(player) ? 0 : 1.6), player.posZ, end.xCoord, end.yCoord, end.zCoord);
		if(RandUtils.nextDouble() < 0.6) {
			Particle p = MdParticleFactory.INSTANCE.next(worldObj,
					new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
					VecUtils.vec(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
			worldObj.spawnEntityInWorld(p);
		}
	}
	
	public static class ExpertRayRenderer extends RendererRayComposite {

		public ExpertRayRenderer() {
			super("mdray_expert");
			this.cylinderIn.width = 0.045;
			this.cylinderIn.color.setColor4i(216, 248, 216, 230);
			
			this.cylinderOut.width = 0.056;
			this.cylinderOut.color.setColor4i(106, 242, 106, 50);
			
			this.glow.width = 0.5;
			this.glow.color.a = 0.7;
		}
		
		@Override
		public void doRender(Entity ent, double x,
				double y, double z, float a, float b) {
			this.cylinderIn.width = 0.045;
			this.cylinderIn.color.setColor4i(216, 248, 216, 180);
			
			this.cylinderOut.width = 0.056;
			this.cylinderOut.color.setColor4i(106, 242, 106, 50);
			
			this.glow.color.a = 0.5;
			super.doRender(ent, x, y, z, a ,b);
		}
		
	}
}
