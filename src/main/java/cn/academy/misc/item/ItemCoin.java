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

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.api.event.ThrowCoinEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.misc.entity.EntityThrowingCoin;
import cn.liutils.util.GenericUtils;

/**
 * The coin from the game center which is used by Misaka Mikoto for her proud Railgun skill!
 * ~\(≧▽≦)/~
 * @author KSkun, WeAthFolD
 */
public class ItemCoin extends Item {
	
	public ItemCoin() {
		setUnlocalizedName("ac_coin");
		setTextureName("academy:coin-front");
		setCreativeTab(AcademyCraft.cct);
		//setMaxDamage(0);
	}
	
    @Override
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean equipped) {
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	if(!(entity instanceof EntityPlayer) || !equipped) {
    		reset(nbt);
    		return;
    	}
    	if(!nbt.getBoolean("throwing"))
    		return;
    	EntityThrowingCoin etc = getThrowingEntity(world, stack);
    	if(etc == null || etc.isDead) {
    		reset(nbt);
    		return;
    	}
    	((EntityPlayer)entity).isSwingInProgress = false;
    }

    @Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	if(nbt.getBoolean("throwing")) return stack;
    	//Spawn at both side, not syncing for render effect purpose
    	EntityThrowingCoin etc = new EntityThrowingCoin(player, stack);
    	world.spawnEntityInWorld(etc);
    	MinecraftForge.EVENT_BUS.post(new ThrowCoinEvent(player, etc));
    	nbt.setInteger("entID", etc.getEntityId());
    	nbt.setBoolean("throwing", true);
    	player.playSound("academy:flipcoin", 0.5F, 1.0F);
    	if(!player.capabilities.isCreativeMode) {
    		--stack.stackSize;
    	}
        return stack;
    }

	private void reset(NBTTagCompound nbt) {
		nbt.setBoolean("throwing", false);
	}
	
	private EntityThrowingCoin getThrowingEntity(World world, ItemStack is) {
		NBTTagCompound nbt = is.getTagCompound();
		Entity e = world.getEntityByID(nbt.getInteger("entID"));
		
		if(e == null || !(e instanceof EntityThrowingCoin))
			return null;
		return (EntityThrowingCoin) e;
	}
	
	public boolean inProgress(ItemStack is) {
		return GenericUtils.loadCompound(is).getBoolean("throwing");
	}

}
