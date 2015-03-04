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
package cn.academy.core.event;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraftforge.event.entity.player.EntityInteractEvent;
import net.minecraftforge.event.entity.player.PlayerEvent.BreakSpeed;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.event.entity.player.PlayerUseItemEvent;
import net.minecraftforge.event.world.BlockEvent;
import cn.academy.api.IOverrideItemUse;
import cn.academy.api.data.AbilityDataMain;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegEventHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;

/**
 * @author WeathFolD
 */
@RegistrationClass
@RegEventHandler
public class ACEventListener {
	
	@SubscribeEvent
	public void killBreakSpeed(BreakSpeed haha) {
		if(activated(haha.entityPlayer))
			haha.setCanceled(true);
	}

	@SubscribeEvent
	public void onBreakBlock(BlockEvent.BreakEvent event) {
		if(activated(event.getPlayer()))
			event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void onPlayerInteract(PlayerInteractEvent event) {
		ItemStack stack = event.entityPlayer.getCurrentEquippedItem();
		System.out.println("interact " + event.entityPlayer.worldObj.isRemote);
		if(activated(event.entityPlayer) && !override(stack))
			event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void onStartUseItem(PlayerUseItemEvent.Start event) {
		if(activated(event.entityPlayer) && override(event.item))
			event.setCanceled(true);
	}
	
	@SubscribeEvent
	public void onInteractEntity(EntityInteractEvent event) {
		if(activated(event.entityPlayer))
			event.setCanceled(true);
	}
	
	private boolean override(ItemStack stack) {
		return stack != null && stack.getItem() instanceof IOverrideItemUse;
	}
	
	private boolean activated(EntityPlayer player) {
		return AbilityDataMain.getData(player).isActivated();
	}
}
