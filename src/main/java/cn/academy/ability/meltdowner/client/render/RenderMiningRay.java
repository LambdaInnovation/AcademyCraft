/**
 * Copyright (c) Lambda Innovation, 2013-2015
 * 本作品版权由Lambda Innovation所有。
 * http://www.lambdacraft.cn/
 *
 * AcademyCraft is open-source, and it is distributed under 
 * the terms of GNU General Public License. You can modify
 * and distribute freely as long as you follow the license.
 * AcademyCraft是一个开源项目，且遵循GNU通用公共授权协议。
 * 在遵照该协议的情况下，您可以自由传播和修改。
 * http://www.gnu.org/licenses/gpl.html
 */
package cn.academy.ability.meltdowner.client.render;

import cn.academy.ability.meltdowner.entity.EntityMdRayBase;
import cn.academy.ability.meltdowner.entity.EntityMiningRay;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * @author WeathFolD
 *
 */
@SideOnly(Side.CLIENT)
public class RenderMiningRay extends RenderMdRayBase<EntityMiningRay> {
	{
		this.setWidthFp(0.3);
		this.setWidthTp(0.5);
	}
}
