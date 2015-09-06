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
package cn.academy.ability.client.render;

import org.lwjgl.opengl.GL11;

import cn.academy.core.client.Resources;
import cn.liutils.api.render.model.TileEntityModelCustom;
import cn.liutils.template.block.RenderBlockMultiModel;
import cn.liutils.util.client.RenderUtils;
import net.minecraft.tileentity.TileEntity;

/**
 * @author WeAthFolD
 */
public class RenderDeveloperNormal extends RenderBlockMultiModel {
	
	public RenderDeveloperNormal() {
		super(
			new TileEntityModelCustom(Resources.getModel("developer_normal")), 
			Resources.getTexture("models/developer_normal"));
		this.scale = 0.5f;
		this.rotateY = 180f;
	}
	
	@Override
	public void drawAtOrigin(TileEntity te) {
		GL11.glDisable(GL11.GL_CULL_FACE);
		GL11.glTranslated(0, 0, -.8);
		super.drawAtOrigin(te);
		GL11.glEnable(GL11.GL_CULL_FACE);
	}

}
