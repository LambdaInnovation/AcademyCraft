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
package cn.academy.ability.electro.client.render.entity;

import net.minecraft.util.ResourceLocation;

import org.lwjgl.opengl.GL11;

import cn.academy.ability.electro.entity.EntityArcBase;
import cn.academy.misc.client.render.RendererRayTiling;

/**
 * @author WeathFolD
 *
 */
public class RenderElecArc extends RendererRayTiling<EntityArcBase> {

	public RenderElecArc() {
		super(null);
		this.ratio = 6;
		this.widthFp = 0.5;
		this.widthTp = 1.0;
		this.alpha = 1.0;
	}
	
	@Override
	protected ResourceLocation nextTexture(EntityArcBase ent, int i) {
		return ent.getTexs()[ent.getIndex(i)];
	}
	
	@Override
	protected void drawPerBillboard(EntityArcBase ent, int i) {
		GL11.glRotated(ent.getRotation(i), 0, 0, 1);
	}

}
