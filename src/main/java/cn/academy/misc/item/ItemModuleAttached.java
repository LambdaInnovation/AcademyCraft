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
package cn.academy.misc.item;

import cn.academy.core.AcademyCraft;
import net.minecraft.item.Item;

/**
 * Ability Developer Elec Card (Needs Maintenance)
 * @author WeAthFolD
 */
public class ItemModuleAttached extends Item {

	int attachID = 0;

	public ItemModuleAttached() {
		setCreativeTab(AcademyCraft.cct);
		setUnlocalizedName("ad_card");
		setTextureName("academy:card");
	}
	
/*	@Override
    public boolean onItemUse(ItemStack stack, EntityPlayer player, World world,
    		int x, int y, int z, int meta, float a, float b, float c)
    {
    	Block block = world.getBlock(x, y, z);
    	if(block != ACBlocks.ability_developer)
    		return false;
    	if(!player.worldObj.isRemote) {
    		TileAbilityDeveloper dev = (TileAbilityDeveloper) world.getTileEntity(x, y, z);
    		if(dev.insertAttachedModule(attachID)) {
    			player.inventory.decrStackSize(player.inventory.currentItem, 1);
    			return true;
    		}
    	}
        return false;
    }*/

}
