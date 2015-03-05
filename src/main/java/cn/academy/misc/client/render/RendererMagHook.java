/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.li-dev.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.misc.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.client.model.IModelCustom;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.proxy.ACModels;
import cn.academy.misc.entity.EntityMagHook;
import cn.liutils.util.RenderUtils;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class RendererMagHook extends Render {

	final IModelCustom model = ACModels.MDL_MAGHOOK,
			model_open = ACModels.MDL_MAGHOOK_OPEN;

	@Override
	public void doRender(Entity ent, double x, double y, double z, float a,
			float b) {
		EntityMagHook hook = (EntityMagHook) ent;
		IModelCustom realModel = model;
		if (hook.isHit) {
			realModel = model_open;
			hook.preRender();
			x = hook.posX - RenderManager.renderPosX;
			y = hook.posY - RenderManager.renderPosY;
			z = hook.posZ - RenderManager.renderPosZ;
		}

		GL11.glPushMatrix();
		RenderUtils.loadTexture(ACClientProps.TEX_MDL_MAGHOOK);
		GL11.glTranslated(x, y, z);
		GL11.glRotated(-hook.rotationYaw + 90, 0, 1, 0);
		GL11.glRotated(hook.rotationPitch - 90, 0, 0, 1);
		double scale = 0.0054;
		GL11.glScaled(scale, scale, scale);
		realModel.renderAll();
		GL11.glPopMatrix();
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity var1) {
		return null;
	}

}