/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.teleport.entity.fx;

import net.minecraft.entity.Entity;
import net.minecraft.world.World;
import cn.academy.ability.electro.client.render.CubePointFactory;
import cn.academy.ability.electro.client.render.IPointFactory.NormalVert;
import cn.academy.core.proxy.ACClientProps;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEntity;
import cn.liutils.api.entityx.EntityX;
import cn.liutils.api.entityx.motion.FollowEntity;
import cn.liutils.api.entityx.motion.LifeTime;
import cn.liutils.template.client.render.entity.RenderIcon;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 */
@RegistrationClass
@RegEntity(clientOnly = true)
@RegEntity.HasRender
@SideOnly(Side.CLIENT)
public class EntityBloodSplash extends EntityX {

	@RegEntity.Render
	public static RenderBloodSplash rsp;
	
	public EntityBloodSplash(World world) {
		super(world);
		addDaemonHandler(new LifeTime(this, 10));
		ignoreFrustumCheck = true;
	}

	public static class RenderBloodSplash extends RenderIcon {

		public RenderBloodSplash() {
			super(null);
			this.alpha = .8;
			this.setSize(2f);
			this.setColorRGB(255, 0, 0);
		}
		
		@Override
		public void doRender(Entity par1Entity, double par2, double par4,
				double par6, float par8, float par9) {
			this.icon = ACClientProps.ANIM_BLOOD_SPLASH[Math.min(9, par1Entity.ticksExisted)];
			super.doRender(par1Entity, par2, par4, par6, par8, par9);
		}
		
	}
	
	static CubePointFactory pointFac = new CubePointFactory(1, 1, 1);
	public static void genSplashEffect(Entity e) {
		int n = GenericUtils.randIntv(1, 3);
		pointFac.setSize(e.width, e.height, e.width);
		for(int i = 0; i < n; ++i) {
			NormalVert delta = pointFac.next();
			EntityBloodSplash ebsp = new EntityBloodSplash(e.worldObj);
			ebsp.setPosition(e.posX, e.posY, e.posZ);
			ebsp.addDaemonHandler(new FollowEntity(ebsp, e)
				.setOffset(delta.vert.xCoord - e.width / 2, delta.vert.yCoord, delta.vert.zCoord - e.width / 2));
			e.worldObj.spawnEntityInWorld(ebsp);
		}
	}
	
}
