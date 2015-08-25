package cn.academy.misc.achievements.conds;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * @author EAirPeter
 */
public final class CondItemCrafted {

	public final Item item;
	public final int meta;
	public final int amount;
	
	public CondItemCrafted(Block pBlock) {
		this(pBlock, -1);
	}
	public CondItemCrafted(Item pItem) {
		this(pItem, -1);
	}
	public CondItemCrafted(Block pBlock, int pMeta) {
		this(pBlock, pMeta, 1);
	}
	public CondItemCrafted(Item pItem, int pMeta) {
		this(pItem, pMeta, 1);
	}
	public CondItemCrafted(Block pBlock, int pMeta, int pAmount) {
		this(Item.getItemFromBlock(pBlock), pMeta, pAmount);
	}
	public CondItemCrafted(Item pItem, int pMeta, int pAmount) {
		item = pItem;
		meta = pMeta;
		amount = pAmount;
	}
	
	public boolean acItemStack(ItemStack is) {
		return item == is.getItem() && (meta == -1 || meta == is.getItemDamage()) && amount <= is.stackSize;
	}
	
}
