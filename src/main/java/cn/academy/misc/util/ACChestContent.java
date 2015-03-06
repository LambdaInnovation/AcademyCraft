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
package cn.academy.misc.util;

import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.WeightedRandomChestContent;

/**
 * @author KSkun
 */
public class ACChestContent extends WeightedRandomChestContent {

	public ACChestContent(Item item, int par2, int par3, int par4, int par5) {
		super(item, par2, par3, par4, par5);
	}
	
    public ACChestContent(ItemStack is, int par2, int par3, int par4) {
        super(is, par2, par3, par4);
    }

}
