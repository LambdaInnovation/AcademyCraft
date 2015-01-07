/**
 * 
 */
package cn.academy.core.item;

import net.minecraft.client.Minecraft;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemSword;
import net.minecraft.world.World;
import net.minecraftforge.common.MinecraftForge;
import cn.academy.api.ctrl.EventHandlerClient;
import cn.academy.core.register.ACItems;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent.ClientTickEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * The 'void' item. This hacking item replaces player's inventory when and only when
 * player is using ability and inventory is empty, in client side. The hacking is then
 * used in RenderVoid to support hand motion and ability effect rendering.
 * @author WeathFolD
 */
public class ItemVoid extends Item {

	public ItemVoid() {
		super();
		setUnlocalizedName("void");
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
