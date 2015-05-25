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
package cn.academy.ability.impl.electromaster.client.renderer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minecraft.client.renderer.entity.Render;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import cn.academy.test.arc.ArcFactory;
import cn.academy.test.arc.ArcFactory.Arc;

/**
 * This class will combine a set of pre-generated arcs to achive the given ray length.
 * @author WeAthFolD
 */
public class RendererArcConnector extends Render {
	
	int PER_GENERATION = 20;
	double[] SUPPORTED_LENS = { 3, 10, 20 };
	Map<Double, List<Arc>> buffer = new HashMap();
	
	public RendererArcConnector(ArcFactory factory) {
		
		for(double len : SUPPORTED_LENS) {
			List<Arc> dp = new ArrayList();
			
			for(int i = 0; i < PER_GENERATION; ++i) {
				dp.add(factory.generate(len));
			}
			
			buffer.put(len, dp);
		}
	}

	@Override
	public void doRender(Entity entity, double x,
			double y, double z, float a, float b) {
		
	}

	@Override
	protected ResourceLocation getEntityTexture(Entity entity) {
		return null;
	}

}
