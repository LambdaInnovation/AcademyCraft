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
package cn.academy.misc.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.core.proxy.ACModels;
import cn.academy.misc.entity.EntityMagHook;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegItem;
import cn.liutils.api.render.model.ItemModelCustom;
import cn.liutils.template.client.render.item.RenderModelItem;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class ItemMagHook extends Item {

	public ItemMagHook() {
		setCreativeTab(AcademyCraft.cct);
		setUnlocalizedName("ac_maghook");
		setTextureName("academy:maghook");
	}
	
    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
    	if(!world.isRemote) {
    		world.playSoundAtEntity(player, "random.bow", 0.5F, 0.4F / (itemRand.nextFloat() * 0.4F + 0.8F));
    		world.spawnEntityInWorld(new EntityMagHook(player));
    		if(!player.capabilities.isCreativeMode)
    			--stack.stackSize;
    	}
        return stack;
    }

}
