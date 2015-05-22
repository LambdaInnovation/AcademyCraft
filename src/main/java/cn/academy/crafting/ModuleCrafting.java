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
package cn.academy.crafting;

import cn.academy.crafting.item.ItemMatterUnit;
import cn.academy.crafting.item.ItemMatterUnit.MatterMaterial;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.annoreg.mc.RegSubmoduleInit;

/**
 * @author WeAthFolD
 */
@Registrant
@RegSubmoduleInit
public class ModuleCrafting {
	
	@RegItem
	@RegItem.HasRender
	public static ItemMatterUnit matterUnit;

	public static void init() {
		String[] materialNames = {
			"imag_ionic"	
		};
		for(String name : materialNames) {
			ItemMatterUnit.addMatterMaterial(new MatterMaterial(name));
		}
	}
	
}
