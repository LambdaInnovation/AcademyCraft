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
package cn.academy.core.entityx.event;

import net.minecraft.util.MovingObjectPosition;
import cn.academy.core.entityx.EntityEvent;

/**
 * @author WeAthFolD
 */
public class CollideEvent extends EntityEvent {

	public final MovingObjectPosition result;
	
	public CollideEvent(MovingObjectPosition mop) {
		result = mop;
	}
	
}
