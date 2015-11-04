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
package cn.academy.crafting.block;

import java.util.Random;

import net.minecraft.block.material.Material;
import net.minecraft.item.Item;
import cn.academy.core.block.ACBlock;
import cn.lambdalib.util.generic.RandUtils;

/**
 * @author WeAthFolD
 */
public class BlockGenericOre extends ACBlock {
	
	int rangeFrom = 1, rangeTo = 2;
	Item itemDropped;

	public BlockGenericOre(String name, float hardness, int harvestLevel) {
		super(name, Material.rock);
		this.setHardness(hardness);
		this.setHarvestLevel("pickaxe", harvestLevel);
	}
	
	public BlockGenericOre setDropData(Item item) {
		itemDropped = item;
		
		return this;
	}
	
	public BlockGenericOre setDropData(Item item, int from, int to) {
		itemDropped = item;
		rangeFrom = from;
		rangeTo = to;
		
		return this;
	}
	
	@Override
	public int quantityDropped(Random r) {
		return RandUtils.rangei(rangeFrom, rangeTo);
	}
	
	@Override
	public Item getItemDropped(int a, Random b, int c) {
		if(itemDropped == null)
			itemDropped = Item.getItemFromBlock(this);
		return itemDropped;
	}

}
