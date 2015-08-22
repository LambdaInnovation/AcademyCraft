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
package cn.academy.ability;

import cn.academy.ability.block.BlockDeveloper;
import cn.academy.ability.developer.DeveloperType;
import cn.academy.ability.item.ItemDeveloper;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegBlock;
import cn.annoreg.mc.RegInit;
import cn.annoreg.mc.RegItem;

/**
 * The ability module init class.
 * @author WeAthFolD
 */
@Registrant
@RegInit
public class ModuleAbility {
	
	@RegItem
	@RegItem.HasRender
	public static ItemDeveloper
		developerPortable;
	
	@RegBlock
	public static BlockDeveloper 
		developerNormal = new BlockDeveloper(DeveloperType.NORMAL),
		developerAdvanced = new BlockDeveloper(DeveloperType.ADVANCED);
	
	public static void init() {
	}
	
}
