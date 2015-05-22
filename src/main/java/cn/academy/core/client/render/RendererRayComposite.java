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
package cn.academy.core.client.render;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 *
 */
public class RendererRayComposite extends Render {
	
	public RendererRaySimple renderSimple;
	public RendererRayCylinder cylinder;
	
	public RendererRayComposite(String name) {
		renderSimple = RendererRaySimple.createFromName("railgun");
		cylinder = new RendererRayCylinder();
	}

	@Override
	public void doRender(Entity e, double x,
			double y, double z, float a, float b) {
		renderSimple.doRender(e, x, y, z, a, b);
		cylinder.doRender(e, x, y, z, a, b);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
