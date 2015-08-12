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

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.util.Vec3;
import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.academy.core.entity.EntityRayBase;
import cn.academy.vanilla.meltdowner.client.render.MdParticleFactory;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegEntity;
import cn.liutils.render.particle.Particle;
import cn.liutils.util.generic.MathUtils;
import cn.liutils.util.generic.RandUtils;
import cn.liutils.util.generic.VecUtils;
import cn.liutils.util.helper.Color;
import cn.liutils.util.helper.Motion3D;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity
@RegEntity.HasRender
public class EntityMineRayBasic extends EntityRayBase {
	
	@RegEntity.Render
	public static BasicMineRayRender renderer;
	
	public EntityMineRayBasic(EntityPlayer _player) {
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
		this.setFromTo(player.posX, player.posY, player.posZ, end.xCoord, end.yCoord, end.zCoord);
		if(RandUtils.nextDouble() < 0.5) {
			Particle p = MdParticleFactory.INSTANCE.next(worldObj,
					new Motion3D(this, true).move(RandUtils.ranged(0, 10)).getPosVec(),
					VecUtils.vec(RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03), RandUtils.ranged(-.03, .03)));
			worldObj.spawnEntityInWorld(p);
		}
	}
	
	@Override
	public double getWidth() {
		long dt = getDeltaTime();
		int blendTime = 500;

		if(dt > this.life * 50 - blendTime) {
			return MathUtils.lerp(1, 0, (double) (dt - (this.life * 50 - blendTime)) / blendTime);
		}
		
		return 1.0;
	}
	
	public static class BasicMineRayRender extends RendererRayComposite {

		public BasicMineRayRender() {
			super("mdray_small");
			this.cylinderIn.width = 0.03;
			this.cylinderIn.material.color = new Color().setColor4i(216, 248, 216, 230);
			
			this.cylinderOut.width = 0.045;
			this.cylinderOut.material.color = new Color().setColor4i(106, 242, 106, 50);
			
			this.glow.width = 0.3;
			this.glow.color.a = 0.5;
		}
		
	}
}
