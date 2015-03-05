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
package cn.academy.core.client.render;

import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.core.proxy.ACModels;
import cn.liutils.api.render.model.TileEntityModelCustom;
import cn.liutils.template.client.render.block.RenderDirMultiModelled;

/**
 * @author WeathFolD
 *
 */
public class RenderMagInducer extends RenderDirMultiModelled {
	
	private static ResourceLocation TEX = new ResourceLocation("academy:textures/models/magincr.png");

	public RenderMagInducer() {
		super(new TileEntityModelCustom(ACModels.MDL_MAGNET_MODULE));
		setModelTexture(TEX);
		this.scale = 0.003f;
	}
	
	@Override
	protected void renderAtOrigin(TileEntity te) {
		GL11.glRotated(90, 0, 1, 0);
		super.renderAtOrigin(te);
	}

}
