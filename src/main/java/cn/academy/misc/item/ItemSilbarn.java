package cn.academy.misc.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.misc.entity.EntitySilbarn;

public class ItemSilbarn extends Item {
	
	public ItemSilbarn() {
		setCreativeTab(AcademyCraft.cct);
	}
	
    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
    	if(!world.isRemote) {
    		world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
    		world.spawnEntityInWorld(new EntitySilbarn(player));
    		if(!player.capabilities.isCreativeMode)
    			stack.damageItem(1, player);
    	}
        return stack;
    }
	
}
