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

import cn.academy.core.item.ACItem;
import cn.academy.vanilla.electromaster.client.renderer.RendererCoinThrowing;
import cn.academy.vanilla.electromaster.entity.EntityCoinThrowing;
import cn.academy.vanilla.electromaster.event.CoinThrowEvent;
import cn.annoreg.core.Registrant;
import cn.annoreg.mc.RegItem;
import cn.liutils.util.mc.StackUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;

/**
 * 
 * @author KSkun
 */
@Registrant
public class ItemCoin extends ACItem {
	
	@RegItem.Render
	@SideOnly(Side.CLIENT)
	public static RendererCoinThrowing.ItemRender renderCoin;
	
	// Key: PlayerName
	static Map<String, EntityCoinThrowing> client = new HashMap(), server = new HashMap();
	
	public ItemCoin() {
		super("coin");
		setTextureName("academy:coin_front");
		FMLCommonHandler.instance().bus().register(this);
	}
	
    @SubscribeEvent
    public void onPlayerTick(PlayerTickEvent event) {
    	EntityPlayer player = event.player;
		Map<String, EntityCoinThrowing> map = getMap(player);
		EntityCoinThrowing etc = getPlayerCoin(player);
		if(etc != null) {
			if(etc.isDead || 
				etc.worldObj.provider.dimensionId != player.worldObj.provider.dimensionId) {
				map.remove(player.getCommandSenderName());
			}
		}
    }

	@Override
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
		if(getPlayerCoin(player) != null) {
			return stack;
		}
		
    	NBTTagCompound nbt = StackUtils.loadTag(stack);
    	//Spawn at both side, not syncing for render effect purpose
    	EntityCoinThrowing etc = new EntityCoinThrowing(player, stack);
    	world.spawnEntityInWorld(etc);
    	
    	player.playSound("academy:entity.flipcoin", 0.5F, 1.0F);
    	setPlayerCoin(player, etc);
    	
    	MinecraftForge.EVENT_BUS.post(new CoinThrowEvent(player, etc));
    	if(!player.capabilities.isCreativeMode) {
    		--stack.stackSize;
    	}
        return stack;
    }
	
	public static EntityCoinThrowing getPlayerCoin(EntityPlayer player) {
		EntityCoinThrowing etc = getMap(player).get(player.getCommandSenderName());
		if(etc != null && !etc.isDead)
			return etc;
		return null;
	}
	
	public static void setPlayerCoin(EntityPlayer player, EntityCoinThrowing etc) {
		Map<String, EntityCoinThrowing> map = getMap(player);
		map.put(player.getCommandSenderName(), etc);
	}
	
	private static Map<String, EntityCoinThrowing> getMap(EntityPlayer player) {
		return player.worldObj.isRemote ? client : server;
	}

}
