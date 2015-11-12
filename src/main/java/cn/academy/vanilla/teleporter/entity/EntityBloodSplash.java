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
package cn.academy.vanilla.teleporter.entity;

import net.minecraft.entity.Entity;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.core.client.Resources;
import cn.lambdalib.annoreg.core.Registrant;
import cn.lambdalib.annoreg.mc.RegEntity;
import cn.lambdalib.template.client.render.entity.RenderIcon;
import cn.lambdalib.util.entityx.EntityAdvanced;
import cn.lambdalib.util.generic.MathUtils;
import cn.lambdalib.util.generic.RandUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegEntity(clientOnly = true)
@RegEntity.HasRender
public class EntityBloodSplash extends EntityAdvanced {

	static ResourceLocation[] SPLASH = Resources.getEffectSeq("blood_splash", 10);

	@RegEntity.Render
	public static SplashRenderer render;

	int frame;

	public EntityBloodSplash(World world) {
		super(world);
		ignoreFrustumCheck = true;
		this.width = this.height = RandUtils.rangef(0.8f, 1.3f);
	}

	@Override
	public void onUpdate() {
		if (++frame == SPLASH.length) {
			setDead();
		}
		super.onUpdate();
	}

	@Override
	protected void readEntityFromNBT(NBTTagCompound tag) {
	}

	@Override
	protected void writeEntityToNBT(NBTTagCompound tag) {
	}

	@Override
	public boolean shouldRenderInPass(int pass) {
		return pass == 1;
	}

	public static class SplashRenderer extends RenderIcon {

		public SplashRenderer() {
			super(null);
			setSize(1.0f);
			this.color.setColor4i(213, 29, 29, 200);
		}

		@Override
		public void doRender(Entity entity, double x, double y, double z, float a, float b) {
			EntityBloodSplash splash = (EntityBloodSplash) entity;
			icon = (SPLASH[MathUtils.clampi(0, SPLASH.length - 1, splash.frame)]);
			this.size = splash.width;
			super.doRender(entity, x, y, z, a, b);
		}

	}

}
