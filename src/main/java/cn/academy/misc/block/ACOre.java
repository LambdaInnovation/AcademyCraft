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
package cn.academy.misc.block;

import java.util.Random;

import cn.academy.core.AcademyCraft;
import net.minecraft.block.Block;
import net.minecraft.block.material.Material;
import net.minecraft.item.Item;

/**
 * AcademyCraft Ore
 * @author KSkun, 江月
 */
public class ACOre extends Block {
	
	private Item itemDropped;
	
	public ACOre(int harvest, float hardness) {
		super(Material.rock);
		setHarvestLevel("pickaxe", harvest);
		setCreativeTab(AcademyCraft.cct);
		setStepSound(Block.soundTypePiston);
		setHardness(hardness);
		setResistance(5.0f);
	}
	
	public ACOre(int harvest) {
		this(harvest, 3.0f);
	}
	
	@Override
	public Item getItemDropped(int par1, Random par2Rand, int par3) {
		if(itemDropped == null) return Item.getItemById(Block.getIdFromBlock(this));
		return itemDropped;
	}
	
	public ACOre setItemDropped(Item itemDropped) {
		this.itemDropped = itemDropped;
		return this;
	}
	
    @Override
	public ACOre setLightLevel(float light) {
        super.setLightLevel(light);
        return this;
    }

}
