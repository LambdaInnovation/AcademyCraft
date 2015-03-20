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
package cn.academy.ability.electro.client.render.entity;

import net.minecraft.entity.Entity;
import cn.academy.misc.entity.EntityRay;
import cn.annoreg.core.RegistrationClass;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@RegistrationClass
@SideOnly(Side.CLIENT)
public class ThinArcRender extends RenderElecArc {

	{
		widthFp = 2.0;
		widthTp = 2.0;
		ratio = 3;
	}
	
	@Override
	public void doRender(Entity var1, double x, double y, double z,
			float h, float a) {
		super.doRender(var1, x, y, z, h, a);
	}

}
