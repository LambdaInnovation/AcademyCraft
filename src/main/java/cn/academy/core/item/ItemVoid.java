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
package cn.academy.core.item;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;
import cn.academy.core.ctrl.EventHandlerClient;
import cn.academy.core.register.ACItems;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The 'void' item. This hacking item replaces player's inventory when and only when
 * player is using ability and inventory is empty, on client side. The hacking is then
 * used by RenderVoid to support hand motion and ability effect rendering.
 * @author WeathFolD
 */
public class ItemVoid extends ItemSword {

	public ItemVoid() {
		super(ToolMaterial.EMERALD);
		setTextureName("academy:void");
		FMLCommonHandler.instance().bus().register(this);
		this.setFull3D();
	}
	
	@SubscribeEvent
	@SideOnly(Side.CLIENT)
	public void replace(ClientTickEvent event) {
		EntityPlayer player = Minecraft.getMinecraft().thePlayer;
		if(player == null) return;
		int curSlot = player.inventory.currentItem;
		ItemStack cur = player.getCurrentEquippedItem();
		if(cur == null && EventHandlerClient.isSkillEnabled()) { //Switch the slot
			player.inventory.setInventorySlotContents(curSlot, new ItemStack(ACItems.ivoid));
		}
	}
	
    @Override
	public ItemStack onItemRightClick(ItemStack stack, World par2World, EntityPlayer par3EntityPlayer) {
    	return stack;
    }
	
    @Override
    @SideOnly(Side.CLIENT)
	public void onUpdate(ItemStack stack, World world, Entity entity, int slot, boolean equipped) {
    	if(!(entity instanceof EntityPlayer))
    		return;
    	EntityPlayer player = (EntityPlayer) entity;
    	//Set to null if not currently equipped or skill not enabled
    	if(!equipped || !EventHandlerClient.isSkillEnabled()) {
    		player.inventory.setInventorySlotContents(slot, null);
    		return;
    	}
    	//Disable swing
    	player.isSwingInProgress = false;
    }

}
