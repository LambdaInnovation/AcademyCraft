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
package cn.academy.ability.impl.electromaster.client;

import cn.academy.ability.impl.electromaster.entity.EntityRailgunFX;
import cn.academy.core.client.render.ray.RendererRayComposite;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegSubmoduleInit;
import cn.liutils.util.helper.Color;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeAthFolD
 *
 */
@SideOnly(Side.CLIENT)
@Registrant
@RegSubmoduleInit
public class RenderInit {

	public static void init() {
		{
			RendererRayComposite render = new RendererRayComposite("railgun");
			render.glow.startFix = -0.3;
			render.glow.endFix = 0.3;
			render.glow.width = 1.1;
			
			render.cylinderIn.material.color = new Color().setColor4i(241, 240, 222, 200);
			render.cylinderIn.width = 0.09;
			
			render.cylinderOut.material.color = new Color().setColor4i(236, 170, 93, 60);
			render.cylinderOut.width = 0.13;
			
			RenderingRegistry.registerEntityRenderingHandler(EntityRailgunFX.class, render);
		}
	}
	
}
