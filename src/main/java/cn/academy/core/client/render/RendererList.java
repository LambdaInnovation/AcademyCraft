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

import java.util.ArrayList;
import java.util.List;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;

/**
 * @author WeAthFolD
 *
 */
public class RendererList extends Render {
	
	List<Render> renderers = new ArrayList();
	
	public RendererList(Render ...rs) {
		for(Render r : rs)
			renderers.add(r);
	}
	
	public RendererList append(Render e) {
		renderers.add(e);
		return this;
	}

	@Override
	public void doRender(Entity ent, double x,
			double y, double z, float a, float b) {
		for(Render r : renderers)
			r.doRender(ent, x, y, z, a, b);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return null;
	}

}
