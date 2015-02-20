/**
 * 
 */
package cn.academy.misc.item;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import cn.academy.core.AcademyCraft;
import cn.academy.core.proxy.ACClientProps;
import cn.academy.misc.entity.EntityMagHook;
import cn.annoreg.core.RegistrationClass;
import cn.annoreg.mc.RegItem;
import cn.liutils.api.render.model.IItemModel;
import cn.liutils.api.render.model.ItemModelCustom;
import cn.liutils.template.client.render.item.RenderModelItem;

/**
 * @author WeathFolD
 */
@RegistrationClass
public class ItemMagHook extends Item {
	
	@RegItem.Render
	public static HookRender renderer;

	public ItemMagHook() {
		setCreativeTab(AcademyCraft.cct);
		setUnlocalizedName("ac_maghook");
		setTextureName("academy:maghook");
	}
	
    public ItemStack onItemRightClick(ItemStack stack, World world, EntityPlayer player) {
    	if(!world.isRemote) {
    		world.spawnEntityInWorld(new EntityMagHook(player));
    		stack.damageItem(1, player);
    	}
        return stack;
    }
    
    public static class HookRender extends RenderModelItem {

		public HookRender() {
			super(new ItemModelCustom(ACClientProps.MDL_MAGHOOK), ACClientProps.TEX_MDL_MAGHOOK);
			System.out.println("AAAAAAAAAAAAAAAAAPOI");
			this.setScale(0.3d);
			this.setStdRotation(0, 0, 90);
		}
    	
    }

}
