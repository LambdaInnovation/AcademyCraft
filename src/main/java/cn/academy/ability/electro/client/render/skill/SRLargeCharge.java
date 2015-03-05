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
package cn.academy.ability.electro.client.render.skill;

import cn.academy.core.proxy.ACClientProps;

/**
 * @author WeathFolD
 *
 */
public class SRLargeCharge extends SRSmallCharge {

	{
		this.setTex(ACClientProps.ANIM_ARC_W);
	}
	
	public SRLargeCharge(int iten, double size) {
		super(iten, size);
	}

	public SRLargeCharge(int iten, double size, double sx, double sy, double sz) {
		super(iten, size, sx, sy, sz);
	}

}
