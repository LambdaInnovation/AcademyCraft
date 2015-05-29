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
package cn.academy.core.client.render.ray;

import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import cn.academy.core.client.render.RendererList;
import cn.academy.core.entity.IRay;

/**
 * @author WeAthFolD
 *
 */
public class RendererRayComposite extends RendererList {
	
	public RendererRayGlow glow;
	public RendererRayCylinder cylinderIn, cylinderOut;
	
	public RendererRayComposite(String name) {
		append(glow = RendererRayGlow.createFromName(name));
		append(cylinderIn = new RendererRayCylinder(0.05f));
		append(cylinderOut = new RendererRayCylinder(0.08f));
		cylinderIn.headFix = 0.98;
	}
	
	@Override
	public void doRender(Entity ent, double x,
			double y, double z, float a, float b) {
		((IRay)ent).onRenderTick();
		super.doRender(ent, x, y, z, a, b);
	}
	
	public void plainDoRender(Entity ent, double x,
			double y, double z, float a, float b) {
		super.doRender(ent, x, y, z, a, b);
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity p_110775_1_) {
		return null;
	}

}
