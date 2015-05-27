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
package cn.academy.vanilla.electromaster.item;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import cn.academy.vanilla.electromaster.client.renderer.RendererCoinThrowing;
import cn.academy.vanilla.electromaster.entity.EntityCoinThrowing;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.liutils.util.mc.StackUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 
 * @author KSkun
 */
@Registrant
public class ItemCoin extends Item {
	
	@RegItem.Render
	@SideOnly(Side.CLIENT)
	public static RendererCoinThrowing.ItemRender renderCoin;
	
	Map<EntityPlayer, Integer> client = new HashMap(), server = new HashMap();
	
	public ItemCoin() {
		setTextureName("academy:coin_front");
		FMLCommonHandler.instance().bus().register(this);
	}
	
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
    	EntityPlayer player = event.player;
		Map<EntityPlayer, Integer> map = getMap(player);
		if(getMap(player).containsKey(player)) {
			Integer i = map.remove(player);
			if(i > 0)
				map.put(player, i - 1);
		}
    }

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(getMap(player).containsKey(player)) {
			System.err.println(player.getDisplayName());
			return stack;
		}
		
    	NBTTagCompound nbt = StackUtils.loadTag(stack);
    	//Spawn at both side, not syncing for render effect purpose
    	EntityCoinThrowing etc = new EntityCoinThrowing(player, stack);
    	world.spawnEntityInWorld(etc);
    	
    	player.playSound("academy:flipcoin", 0.5F, 1.0F);
    	getMap(player).put(player, 50);
    	if(!player.capabilities.isCreativeMode) {
    		--stack.stackSize;
    	}
        return stack;
    }
	
	private Map<EntityPlayer, Integer> getMap(EntityPlayer player) {
		return player.worldObj.isRemote ? client : server;
	}


}
