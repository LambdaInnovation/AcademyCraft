package cn.academy.misc.achievements.conds;

import net.minecraft.block.Block;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;

/**
 * @author EAirPeter
 */
public final class ConItemCrafted {

	public final Item item;
	public final int meta;
	public final int amount;
	
	public ConItemCrafted(Block _block) {
		this(_block, -1);
	}
	public ConItemCrafted(Item _item) {
		this(_item, -1);
	}
	public ConItemCrafted(Block _block, int _meta) {
		this(_block, _meta, 1);
	}
	public ConItemCrafted(Item _item, int _meta) {
		this(_item, _meta, 1);
	}
	public ConItemCrafted(Block _block, int _meta, int _amount) {
		this(Item.getItemFromBlock(_block), _meta, _amount);
	}
	public ConItemCrafted(Item _item, int _meta, int _amount) {
		item = _item;
		meta = _meta;
		amount = _amount;
	}
	
	public boolean acItemStack(ItemStack is) {
		return item == is.getItem() && (meta == -1 || meta == is.getItemDamage()) && amount <= is.stackSize;
	}
	
}
