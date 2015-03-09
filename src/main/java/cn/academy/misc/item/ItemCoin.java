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
package cn.academy.misc.item;

import java.util.HashMap;
import java.util.Map;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.api.IOverrideItemUse;
import cn.academy.api.event.ThrowCoinEvent;
import cn.academy.core.AcademyCraft;
import cn.academy.misc.client.render.RendererCoin;
import cn.academy.misc.entity.EntityThrowingCoin;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegItem;
import cn.liutils.util.GenericUtils;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.PlayerTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The coin from the game center which is used by Misaka Mikoto for her proud Railgun skill!
 * ~\(≧▽≦)/~
 * @author KSkun, WeAthFolD
 */
@RegistrationClass
public class ItemCoin extends Item implements IOverrideItemUse {
	
	@RegItem.Render
	@SideOnly(Side.CLIENT)
	public RendererCoin.ItemRender renderCoin;
	
	Map<EntityPlayer, Integer> client = new HashMap(), server = new HashMap();
	
	public ItemCoin() {
		setUnlocalizedName("ac_coin");
		setTextureName("academy:coin-front");
		setCreativeTab(AcademyCraft.cct);
		FMLCommonHandler.instance().bus().register(this);
		this.hasSubtypes = false;
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
	public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player)
    {
		if(getMap(player).containsKey(player))
			return stack;
		
    	NBTTagCompound nbt = GenericUtils.loadCompound(stack);
    	//Spawn at both side, not syncing for render effect purpose
    	EntityThrowingCoin etc = new EntityThrowingCoin(player, stack);
    	world.spawnEntityInWorld(etc);
    	
    	MinecraftForge.EVENT_BUS.post(new ThrowCoinEvent(player, etc));
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
